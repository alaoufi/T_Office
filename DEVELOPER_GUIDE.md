# دليل المطوّر — تطبيق «مكتبي» (T_Office)

> نسخة الوثيقة: مرافقة للإصدار 1.14
> تطبيق مكتبي شخصي لأندرويد، يعمل **أوفلاين 100%**، واجهته عربية RTL.

---

## ١) نظرة عامة معمارية

| الموضوع | القرار |
|---------|--------|
| المنصة | Android فقط (APK) — لا خادم |
| اللغة | Kotlin |
| الواجهة | Jetpack Compose + Material 3 |
| الاتجاه | RTL عربي افتراضي |
| قاعدة البيانات | Room (SQLite) محلية — أوفلاين 100% |
| المعمارية | MVVM + طبقات (data / model / ui) |
| الحقن | Hilt (DI) |
| التنقّل | Navigation Compose |
| الحدّ الأدنى | minSdk 26 (Android 8) — target/compileSdk 34 |

### الطبقات
```
com.toffice.app
├── data/                 ← قاعدة البيانات (Room): الكيانات + DAO
│   ├── AppDatabase.kt
│   ├── document/         ← DocumentEntity + DocumentDao
│   └── task/             ← Task + TaskDao
├── di/                   ← Hilt: DatabaseModule
├── core/
│   ├── navigation/       ← Routes + AppNavHost
│   └── settings/         ← SettingsRepository (DataStore)
├── feature/              ← الوحدات (كل وحدة: Screen + ViewModel)
│   ├── editor/           ← محرّر Word (الأكبر)
│   │   ├── io/           ← قراءة/كتابة DOCX/DOC/PDF/الصور
│   │   └── model/        ← نموذج المستند + التسلسل + النصّ الغني
│   ├── tasks/  dashboard/  settings/  common/
│   └── ...
├── ui/theme/             ← الألوان + الثيم + الخطوط
├── MainActivity.kt       ← نقطة الدخول + التقاط «فتح بواسطة»
├── ExternalOpen.kt       ← حامل الملف الخارجي (VIEW/SEND)
└── TOfficeApplication.kt ← @HiltAndroidApp
```

---

## ٢) قاعدة البيانات (Room)

- **اسم الملف:** `t_office.db`
- **الإصدار:** 3 — `fallbackToDestructiveMigration()` (يُعاد بناؤها عند تغيّر المخطّط؛ غيّرها إلى ترحيل حقيقي `Migration` قبل الإصدار العام لتفادي فقد بيانات المستخدم).
- **exportSchema = false** (فعّلها لاحقاً لتوليد ملفات المخطّط للترحيلات).

### الجدول: `documents`
| العمود | النوع | ملاحظات |
|--------|-------|---------|
| `id` | INTEGER (PK, autoGenerate) | المفتاح الأساسي |
| `title` | TEXT | عنوان المستند |
| `contentJson` | TEXT | محتوى المستند مسلسلاً (صيغة التطبيق الداخلية — انظر §٤) |
| `sourceUri` | TEXT? | رابط ملف DOCX الأصلي (للحفظ بنفس الملف) — قد يكون null |
| `updatedAt` | INTEGER | آخر تعديل (ملّي ثانية) |
| `createdAt` | INTEGER | الإنشاء (ملّي ثانية) |

استعلامات `DocumentDao`: `observeAll()` (Flow، مرتّب بالأحدث), `getById`, `insert`, `update`, `delete`.

### الجدول: `tasks`
| العمود | النوع | ملاحظات |
|--------|-------|---------|
| `id` | INTEGER (PK, autoGenerate) | |
| `title` | TEXT | عنوان المهمّة |
| `notes` | TEXT | ملاحظات |
| `isDone` | INTEGER (Boolean) | مُنجزة؟ |
| `priority` | TEXT (enum: LOW/NORMAL/HIGH) | الأولوية |
| `dueDate` | INTEGER? | تاريخ الاستحقاق (nullable) |
| `createdAt` | INTEGER | |

استعلامات `TaskDao`: `observeAll()` (مرتّب: غير المنجز أولاً ثم الأولوية ثم الاستحقاق), `getById`, `insert (REPLACE)`, `update`, `delete`.

### الحقن (Hilt)
`DatabaseModule` يوفّر `AppDatabase` (Singleton) و DAOs. لإضافة جدول جديد:
1. أنشئ الكيان `@Entity` والـ `@Dao`.
2. أضِفه إلى `entities` في `AppDatabase` وارفع `version`.
3. أضِف `@Provides` للـ DAO في `DatabaseModule`.
4. اكتب `Migration` حقيقياً (لا تعتمد على الحذف المدمّر في الإنتاج).

---

## ٣) وحدة المحرّر (الأهم)

### القراءة/الكتابة (`feature/editor/io/`)
- **`DocxReader`** — يقرأ `.docx` (OOXML/ZIP): المتن + التنسيق + الهوامش + الترويسة/التذييل + الجداول + **الخلفية الملوّنة** + **استخراج الصور**.
  - يقرأ ZIP عبر **الفهرس المركزي (Central Directory)** لا `ZipInputStream` — ليتحمّل ملفات docx بنمط التدفّق (data descriptor / CRC صفري) التي ترمي "invalid entry CRC".
- **`DocReader`** — يقرأ `.doc` الثنائي القديم (OLE2 Compound File) عبر حاوية CFB + **جدول القطع (piece table)** من FIB. يستخرج **النصّ فقط** (بلا تنسيق غني). `readAny(bytes)` يكشف الصيغة تلقائياً (docx vs doc).
- **`DocxWriter`** — يكتب/يصدّر `.docx` بكتل مرتّبة (فقرات/جداول/صور).
- **`PdfExporter`** — يصدّر المستند إلى PDF.
- **`ImageStore`** — نسخ داخلي للصور + فكّ ترميز آمن (RGB_565، حدّ أقصى للأبعاد) + **تنظيف الصور اليتيمة**.
- **`PdfViewerScreen`** — قارئ PDF (PdfRenderer) بتحميل كسول + تكبير بإصبعين (**العرض المطابق لـ Word يتم عبر PDF مُصدَّر من Word**).

### النموذج (`feature/editor/model/`)
- **`DocumentContent.kt`** — `PageSettings`, `DocBundle`, `DocBlock` (نص/جدول/صورة), و`DocSerializer` (التسلسل JSON).
- **`RichText.kt` / `RichTextOps.kt`** — النصّ الغني (`AnnotatedString` ↔ سمات الحروف/الفقرات) وعمليات التنسيق.
- **`TableData.kt` / `TableOps.kt`** — الجداول.
- **`Pagination.kt`** — حساب عدد الصفحات.

### حدود واقعية (مهم توثيقها للمستخدم)
- **`.docx`**: يُحفظ أغلب التنسيق، **ليس تطابقاً 100%** للمستندات المعقّدة (صور عائمة، تخطيط معقّد).
- **`.doc`**: نصّ فقط (الصيغة الثنائية معقّدة جداً).
- **المطابقة التامة لـ Word**: **غير ممكنة** في محرّر Compose خفيف — الطريق الوحيد هو **تصدير Word إلى PDF ثم عرضه** بقارئ PDF.

---

## ٤) صيغة المحتوى الداخلية (`contentJson`)

`DocSerializer` يسلسل `DocBundle` إلى JSON:
```
{
  "body":   { ...AnnotatedString منسّق... },
  "page":   { "pW","pH","mL","mR","mT","mB","pn","rtl","bg" },
  "header": {...}, "footer": {...},
  "tables": [...], "after": {...},
  "images": [ { "p": مسار, "w", "h" } ],
  "blocks": [ { "type":"text|table|image", ... } ]
}
```
- متوافق رجعياً: المستندات القديمة (نص خام) تُقرأ أيضاً.
- `parse()` لا يرمي استثناءً أبداً (يحمي المحرّر من التلف).

---

## ٥) البناء والتشغيل

```bash
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk

./gradlew assembleDebug      # ينتج app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # اختبارات الوحدة
./gradlew lint               # فحص الجودة
```

---

## ٦) ⭐ القاعدة الذهبية للمطوّر (طبّقها في كل تطبيق)

> **كل مورد تفتحه — أغلقه. كل شيء كبير — صغّره وحرّره فور انتهائك منه. لا تُبقِ في الذاكرة إلا ما يُعرَض الآن. كل تغيير يُبنى ويُختبر قبل التسليم، وتُبلَّغ حدوده بصراحة.**

### أ) الذاكرة والأداء
1. **أغلق كل مورد** (`InputStream/Cursor/File/PdfRenderer/Bitmap/Database`) في `use{}` أو `finally` أو `onDispose{}`.
2. **الصور (Bitmap) أخطر مصدر**:
   - فُكّها بأبعاد العرض عبر `inSampleSize`، لا بحجمها الأصلي.
   - استخدم `RGB_565` بدل `ARGB_8888` عند عدم الحاجة للشفافية (نصف الذاكرة).
   - `recycle()` عند خروج العنصر من الشاشة.
   - احسب حجمها: `العرض × الارتفاع × 4` بايت — صفحة A4 قد تبلغ عشرات الميغابايت.
3. **القوائم الطويلة كسولة** (`LazyColumn`) — لا تُركّب إلا المرئي.
4. **المحتوى الكبير يُجزّأ**، والعمليات الثقيلة على خيط خلفي (`Dispatchers.IO/Default`) لا الرئيسي (يجمّد الواجهة/لوحة المفاتيح).
5. **لا تُراكم ملفات** — احذف ملفات المورد عند حذفه، ونظّف «الأيتام» غير المرجعية عند بدء التطبيق.
6. **تعامل مع ضغط الذاكرة** (`onTrimMemory/onLowMemory`).
7. **لا حلقات قياس/تحديث لا نهائية** (تسبب ANR).

### ب) الجودة والاستقرار
8. **كل تغيير يُبنى بنجاح ويُختبر فعلياً** قبل الدفع.
9. **اكتب اختبارات للحدود**: ملف ضخم، صورة عملاقة، ١٠٠٠ عنصر، مدخل تالف.
10. **لا تُسقط التطبيق على مدخل سيّئ** — `try/catch` وإرجاع قيمة آمنة (كما في `DocSerializer.parse`).
11. **وحدة واحدة لكل دفعة**، صغيرة ومركّزة.
12. **راجع نموذج البيانات (Room) أولاً** قبل بناء الواجهة، واكتب Migration حقيقياً.

### ج) الصراحة مع المستخدم
13. **أبلغ عن الحدود بوضوح** (مثل: `.doc` نصّ فقط، والمطابقة التامة عبر PDF).
14. **لا تَعِد بما لا يُنجَز** بنية التطبيق (محرّر خفيف ≠ محرّك Word).

---

## ٧) ترتيب العمل الموصى به
1. الهيكل: Gradle + Compose + Room + Hilt + Navigation + RTL + الشاشة الرئيسية.
2. الوحدات: المهام، الملاحظات، التقويم، المصروفات، المستندات، القفل، النسخ الاحتياطي.
3. APK يعمل ويُختبر على الحدود.
4. محرّر النصوص (DOCX/PDF) كمرحلة مستقلة.
