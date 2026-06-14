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
)

/** المستند الكامل: المتن المنسّق + إعدادات الصفحة + الترويسة + التذييل. */
data class DocBundle(
    val body: AnnotatedString,
    val page: PageSettings = PageSettings(),
    val header: String = "",
    val footer: String = "",
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
        return JSONObject()
            .put("body", bodyObj)
            .put("page", page)
            .put("header", bundle.header)
            .put("footer", bundle.footer)
            .toString()
    }

    fun parse(json: String): DocBundle {
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
        ) else PageSettings()
        return DocBundle(
            body = body,
            page = page,
            header = obj.optString("header", ""),
            footer = obj.optString("footer", ""),
        )
    }
}
