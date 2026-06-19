package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.AnnotatedString
import org.json.JSONObject

/** نقاط لكل سنتيمتر (1 بوصة = 72 نقطة = 2.54 سم). */
const val PT_PER_CM = 28.3465f

/** تحويل النقاط إلى twips (وحدة DOCX): 1 نقطة = 20 twip. */
fun Float.ptToTwips(): Int = (this * 20f).toInt()
fun Int.twipsToPt(): Float = this / 20f

/** إعدادات صفحة المستند (المقاسات بالنقاط pt). صفحة A4 افتراضياً. */
data class PageSettings(
    val pageWidthPt: Float = 595f,   // A4 = 21.0 سم
    val pageHeightPt: Float = 842f,  // A4 = 29.7 سم
    val marginLeftPt: Float = 72f,
    val marginRightPt: Float = 72f,
    val marginTopPt: Float = 72f,
    val marginBottomPt: Float = 72f,
    val showPageNumber: Boolean = false,
    val rtlPage: Boolean = true, // اتجاه الصفحة كاملة (عربي افتراضياً)
)

/** قياس صفحة جاهز (الأبعاد بالنقاط، وضع عمودي). */
data class PageSizePreset(val id: String, val label: String, val widthPt: Float, val heightPt: Float)

val PAGE_SIZES = listOf(
    PageSizePreset("A4", "A4 — ٢١ × ٢٩٫٧ سم", 595f, 842f),
    PageSizePreset("LETTER", "Letter — ٨٫٥ × ١١ بوصة", 612f, 792f),
    PageSizePreset("LEGAL", "Legal — ٨٫٥ × ١٤ بوصة", 612f, 1008f),
    PageSizePreset("A5", "A5 — ١٤٫٨ × ٢١ سم", 420f, 595f),
    PageSizePreset("A3", "A3 — ٢٩٫٧ × ٤٢ سم", 842f, 1191f),
)

fun pageSizeById(id: String): PageSizePreset = PAGE_SIZES.firstOrNull { it.id == id } ?: PAGE_SIZES[0]

/** يطبّق قياساً واتجاهاً على إعدادات الصفحة. */
fun PageSettings.withSize(preset: PageSizePreset, landscape: Boolean): PageSettings {
    val shortSide = minOf(preset.widthPt, preset.heightPt)
    val longSide = maxOf(preset.widthPt, preset.heightPt)
    return copy(
        pageWidthPt = if (landscape) longSide else shortSide,
        pageHeightPt = if (landscape) shortSide else longSide,
    )
}

fun PageSettings.isLandscape(): Boolean = pageWidthPt > pageHeightPt

/** معرّف القياس الحالي إن طابق أحد الجاهزة (بأي اتجاه). */
fun PageSettings.currentPresetId(): String {
    val w = minOf(pageWidthPt, pageHeightPt)
    val h = maxOf(pageWidthPt, pageHeightPt)
    return PAGE_SIZES.firstOrNull {
        kotlin.math.abs(it.widthPt - w) < 2f && kotlin.math.abs(it.heightPt - h) < 2f
    }?.id ?: "A4"
}

/** المستند الكامل: المتن المنسّق + إعدادات الصفحة + الترويسة + التذييل + الجداول. */
data class DocBundle(
    val body: AnnotatedString,
    val page: PageSettings = PageSettings(),
    val header: AnnotatedString = AnnotatedString(""),
    val footer: AnnotatedString = AnnotatedString(""),
    val tables: List<TableData> = emptyList(),
    val afterBody: AnnotatedString = AnnotatedString(""),
)

/** تسلسل المستند الكامل إلى/من JSON (صيغة التطبيق الداخلية). */
object DocSerializer {

    fun serialize(bundle: DocBundle): String {
        val bodyObj = JSONObject(annotatedToJson(bundle.body))
        val page = JSONObject()
            .put("pW", bundle.page.pageWidthPt.toDouble())
            .put("pH", bundle.page.pageHeightPt.toDouble())
            .put("mL", bundle.page.marginLeftPt.toDouble())
            .put("mR", bundle.page.marginRightPt.toDouble())
            .put("mT", bundle.page.marginTopPt.toDouble())
            .put("mB", bundle.page.marginBottomPt.toDouble())
            .put("pn", bundle.page.showPageNumber)
            .put("rtl", bundle.page.rtlPage)
        return JSONObject()
            .put("body", bodyObj)
            .put("page", page)
            .put("header", JSONObject(annotatedToJson(bundle.header)))
            .put("footer", JSONObject(annotatedToJson(bundle.footer)))
            .put("tables", TableOps.toJson(bundle.tables))
            .put("after", JSONObject(annotatedToJson(bundle.afterBody)))
            .toString()
    }

    /** يقرأ حقلاً منسّقاً (الصيغة الجديدة JSONObject) مع توافق مع الصيغة القديمة (نص عادي). */
    private fun parseRich(obj: JSONObject, key: String): AnnotatedString {
        val o = obj.optJSONObject(key)
        if (o != null) return jsonToAnnotated(o.toString())
        return AnnotatedString(obj.optString(key, ""))
    }

    fun parse(json: String): DocBundle = try {
        parseStrict(json)
    } catch (e: Exception) {
        // حماية من التلف: لا تُسقط المحرّر؛ أعِد النص الخام إن أمكن
        DocBundle(runCatching { jsonToAnnotated(json) }.getOrElse { AnnotatedString(json) })
    }

    private fun parseStrict(json: String): DocBundle {
        if (json.isBlank()) return DocBundle(AnnotatedString(""))
        val obj = JSONObject(json)
        // توافق مع الصيغة القديمة (المتن فقط)
        if (!obj.has("body")) {
            return DocBundle(jsonToAnnotated(json))
        }
        val body = jsonToAnnotated(obj.getJSONObject("body").toString())
        val p = obj.optJSONObject("page")
        val page = if (p != null) PageSettings(
            pageWidthPt = p.optDouble("pW", 595.0).toFloat(),
            pageHeightPt = p.optDouble("pH", 842.0).toFloat(),
            marginLeftPt = p.optDouble("mL", 72.0).toFloat(),
            marginRightPt = p.optDouble("mR", 72.0).toFloat(),
            marginTopPt = p.optDouble("mT", 72.0).toFloat(),
            marginBottomPt = p.optDouble("mB", 72.0).toFloat(),
            showPageNumber = p.optBoolean("pn", false),
            rtlPage = p.optBoolean("rtl", true),
        ) else PageSettings()
        return DocBundle(
            body = body,
            page = page,
            header = parseRich(obj, "header"),
            footer = parseRich(obj, "footer"),
            tables = TableOps.fromJson(obj.optJSONArray("tables")),
            afterBody = parseRich(obj, "after"),
        )
    }
}
