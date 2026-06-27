# Alarm (تنبيهات) — دليل التطبيق للجلسة المخصّصة

ابدأ من هنا عند فتح جلسة تطوير جديدة لهذا التطبيق.

## الهويّة
- **الاسم:** Alarm (تنبيهات)
- **applicationId:** `com.alaoufi.alarm` (يتعايش مع التطبيقات الأخرى على الجهاز)
- **اسم حزمة Dart الداخليّ:** `mudhakkarati` (بقي كما هو عمدًا؛ أعِد تسميته لاحقًا إن رغبت)
- **النسخة:** `1.0.0+1`
- **مصدر التحديث الذاتيّ:** `apk-dist-alarm` / إصدار `alarm-latest` (مستقلّ — يحتاج إعداد CI لاحقًا)

## الأصل ونقطة التراجع
منبثق عن التطبيق الموحّد **Alaoufi Notes** (`mudhakkarati/`). نقطة التراجع للأصل:
وسم `v1.8.9-combined` / بصمة `d28bb1e`.

## الوضع الحاليّ (المرحلة ١)
نسخة **كاملة تعمل** من مذكراتي (كل الميزات)، تحلّل بلا أخطاء (`flutter analyze`).
البنية والوثائق نفسها: `docs/DEVELOPER_GUIDE.md` و`docs/DATABASE.md` و`docs/schema.sql`.

## المرحلة ٢ — التخصيص (تمّت ✅)
أصبح التطبيق متخصّصًا في **التذكيرات والمنبّهات** فقط:
- **نقطة الدخول الآن:** `RootScreen` ⇐ `RemindersScreen` (شاشة التنبيهات + قائمة جانبية).
- **أُبقِيَ:** التذكيرات (`features/reminders`)، الأدوية (`features/meds`)، التقويم
  (`features/calendar` — يعرض التنبيهات فقط)، شاشة المنبّه + الموثوقيّة، مكتبة النغمات
  (`features/sounds`)، مركز الإشعارات، خدمة الإشعارات (`services/notification_service`)،
  الإعدادات، الأمان، النسخ الاحتياطي/المزامنة.
- **حُذِف:** المحرّر الغنيّ (`features/editor`) وأنواع الملاحظات، شاشة الملاحظات
  (`features/home/home_screen`)، التصنيفات/الوسوم/القوالب/المفضّلة/البحث/السلّة/التنظيف/
  الرؤى/المعلومات/الروابط، وبطاقات الملاحظات (`widgets/note_card`, `note_actions`)،
  وخدمات تصدير الملاحظات (PDF/Word) والإملاء — مع تنظيف القائمة الجانبية والتنقّل.
- **التنبيهات مستقلّة:** عنوان + وقت + تكرار/أهميّة (+ كورسات الأدوية)، بلا ربط ملاحظة.
- **طبقة البيانات** (`data/`، `notes_provider`) أُبقيت دون واجهة لأن خدمات النسخ/المزامنة
  تعتمد عليها — غير ظاهرة للمستخدم. أُزيلت الحزم غير المستعملة من `pubspec.yaml`
  (flutter_quill، signature، flutter_colorpicker، flutter_staggered_grid_view،
  speech_to_text، pdf).

> ⚠️ لم تُتحقَّق الترجمة بالمُصرِّف في هذه البيئة (لا يوجد Flutter SDK). **شغّل قبل أي اعتماد:**
> `flutter pub get` ثم `flutter analyze` ثم `flutter test`. تبقّت لمسات تجميلية اختيارية:
> بطاقات «الملاحظة الافتراضية» في الإعدادات، واستيراد EasyNotes في النسخ الاحتياطي،
> ونصوص دليل المساعدة التي تذكر المحرّر — كلّها تُصرَّف لكنها بقايا من نسخة الملاحظات.

## التشغيل والبناء
```bash
cd alarm
flutter pub get
flutter test                 # بوّابة الجودة
flutter run
flutter build apk --release --split-per-abi
```

## أين تبدأ
- نقطة الإقلاع: `lib/main.dart` → `lib/app.dart`.
- جوهر التطبيق: `lib/services/notification_service.dart` + `lib/features/reminders/`
  + `lib/features/meds/`.
- القائمة الجانبية/التنقّل: `lib/widgets/app_drawer.dart`.
- نصوص الواجهة: `lib/core/l10n/app_strings.dart`.
