package com.toffice.app.feature.editor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

/** الحجم الافتراضي للخط بالنقاط (sp). */
const val DEFAULT_FONT_SP = 16

/** لون افتراضي (0 = استخدم لون الثيم). */
const val COLOR_DEFAULT = 0

/** خصائص تنسيق حرف واحد. */
data class CharAttrs(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val sizeSp: Int = DEFAULT_FONT_SP,
    val colorArgb: Int = COLOR_DEFAULT,
) {
    fun isDefault(): Boolean =
        !bold && !italic && !underline && sizeSp == DEFAULT_FONT_SP && colorArgb == COLOR_DEFAULT

    fun toSpanStyle(): SpanStyle = SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        textDecoration = if (underline) TextDecoration.Underline else null,
        fontSize = sizeSp.sp,
        color = if (colorArgb != COLOR_DEFAULT) Color(colorArgb) else Color.Unspecified,
    )
}

// ---- محاذاة الفقرة <-> رقم ----

fun TextAlign.toCode(): Int = when (this) {
    TextAlign.Center -> 1
    TextAlign.Left -> 2
    TextAlign.Justify -> 3
    else -> 0 // Right (افتراضي للعربية)
}

fun Int.toTextAlign(): TextAlign = when (this) {
    1 -> TextAlign.Center
    2 -> TextAlign.Left
    3 -> TextAlign.Justify
    else -> TextAlign.Right
}

/** حدود الفقرات (بداية، نهاية) مقسّمة على '\n'. */
fun paragraphSpans(text: String): List<Pair<Int, Int>> {
    val spans = mutableListOf<Pair<Int, Int>>()
    var start = 0
    for (i in text.indices) {
        if (text[i] == '\n') {
            spans.add(start to i + 1)
            start = i + 1
        }
    }
    spans.add(start to text.length)
    return spans
}

/** يحوّل النص المنسّق إلى مصفوفة خصائص لكل حرف. */
fun AnnotatedString.toCharAttrs(): MutableList<CharAttrs> {
    val attrs = MutableList(text.length) { CharAttrs() }
    for (range in spanStyles) {
        val s = range.item
        val end = range.end.coerceAtMost(text.length)
        for (i in range.start until end) {
            var a = attrs[i]
            if (s.fontWeight != null && s.fontWeight!!.weight >= FontWeight.Bold.weight) a = a.copy(bold = true)
            if (s.fontStyle == FontStyle.Italic) a = a.copy(italic = true)
            if (s.textDecoration == TextDecoration.Underline) a = a.copy(underline = true)
            if (s.fontSize != TextUnit.Unspecified) a = a.copy(sizeSp = s.fontSize.value.toInt())
            if (s.color != Color.Unspecified) a = a.copy(colorArgb = s.color.toArgb())
            attrs[i] = a
        }
    }
    return attrs
}

/** يستخرج محاذاة كل فقرة. */
fun AnnotatedString.toAligns(): MutableList<TextAlign> {
    val paras = paragraphSpans(text)
    return paras.map { (s, _) ->
        paragraphStyles.firstOrNull { it.start == s }?.item?.textAlign ?: TextAlign.Right
    }.toMutableList()
}

/** يبني نصاً منسّقاً من النص + الخصائص + المحاذاة. */
fun buildAnnotated(
    text: String,
    attrs: List<CharAttrs>,
    aligns: List<TextAlign>,
): AnnotatedString = buildAnnotatedString {
    append(text)
    val paras = paragraphSpans(text)
    paras.forEachIndexed { idx, (s, e) ->
        if (e > s) {
            val al = aligns.getOrElse(idx) { TextAlign.Right }
            addStyle(ParagraphStyle(textAlign = al), s, e)
        }
    }
    var i = 0
    while (i < text.length) {
        val a = attrs.getOrElse(i) { CharAttrs() }
        var j = i + 1
        while (j < text.length && attrs.getOrElse(j) { CharAttrs() } == a) j++
        if (!a.isDefault()) addStyle(a.toSpanStyle(), i, j)
        i = j
    }
}

// ---- التسلسل JSON (صيغة التطبيق الداخلية) ----

fun annotatedToJson(a: AnnotatedString): String {
    val attrs = a.toCharAttrs()
    val aligns = a.toAligns()
    val runs = JSONArray()
    var i = 0
    while (i < a.text.length) {
        val at = attrs[i]
        var j = i + 1
        while (j < a.text.length && attrs[j] == at) j++
        if (!at.isDefault()) {
            runs.put(
                JSONObject()
                    .put("s", i).put("e", j)
                    .put("b", at.bold).put("i", at.italic).put("u", at.underline)
                    .put("sz", at.sizeSp).put("c", at.colorArgb)
            )
        }
        i = j
    }
    val alignsArr = JSONArray()
    aligns.forEach { alignsArr.put(it.toCode()) }
    return JSONObject()
        .put("text", a.text)
        .put("runs", runs)
        .put("aligns", alignsArr)
        .toString()
}

fun jsonToAnnotated(json: String): AnnotatedString {
    if (json.isBlank()) return AnnotatedString("")
    val obj = JSONObject(json)
    val text = obj.optString("text", "")
    val attrs = MutableList(text.length) { CharAttrs() }
    val runs = obj.optJSONArray("runs") ?: JSONArray()
    for (k in 0 until runs.length()) {
        val r = runs.getJSONObject(k)
        val s = r.optInt("s").coerceIn(0, text.length)
        val e = r.optInt("e").coerceIn(0, text.length)
        val a = CharAttrs(
            bold = r.optBoolean("b"),
            italic = r.optBoolean("i"),
            underline = r.optBoolean("u"),
            sizeSp = r.optInt("sz", DEFAULT_FONT_SP),
            colorArgb = r.optInt("c", COLOR_DEFAULT),
        )
        for (idx in s until e) attrs[idx] = a
    }
    val alignsArr = obj.optJSONArray("aligns") ?: JSONArray()
    val paras = paragraphSpans(text)
    val aligns = MutableList(paras.size) { TextAlign.Right }
    for (k in 0 until minOf(alignsArr.length(), aligns.size)) {
        aligns[k] = alignsArr.getInt(k).toTextAlign()
    }
    return buildAnnotated(text, attrs, aligns)
}

// ---- التحويل إلى فقرات للتصدير (DOCX / PDF) ----

data class RunOut(
    val text: String,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val sizeSp: Int,
    val colorArgb: Int,
)

data class ParaOut(val alignCode: Int, val runs: List<RunOut>)

fun AnnotatedString.toParagraphsOut(): List<ParaOut> {
    val attrs = toCharAttrs()
    val aligns = toAligns()
    val result = mutableListOf<ParaOut>()
    val paras = paragraphSpans(text)
    paras.forEachIndexed { idx, (s, eRaw) ->
        // استبعاد '\n' الفاصل من نهاية الفقرة
        val e = if (eRaw > s && text.getOrNull(eRaw - 1) == '\n') eRaw - 1 else eRaw
        val runs = mutableListOf<RunOut>()
        var i = s
        while (i < e) {
            val a = attrs[i]
            var j = i + 1
            while (j < e && attrs[j] == a) j++
            runs.add(RunOut(text.substring(i, j), a.bold, a.italic, a.underline, a.sizeSp, a.colorArgb))
            i = j
        }
        result.add(ParaOut(aligns.getOrElse(idx) { TextAlign.Right }.toCode(), runs))
    }
    return result
}
