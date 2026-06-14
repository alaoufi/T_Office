package com.toffice.app.feature.editor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** الحجم الافتراضي للخط بالنقاط (sp). */
const val DEFAULT_FONT_SP = 16

/** لون افتراضي (0 = استخدم لون الثيم). */
const val COLOR_DEFAULT = 0

/** مقدار المسافة البادئة لكل مستوى (نقطة ≈ ٠٫٥ بوصة كما في وورد). */
const val INDENT_STEP_PT = 36

/** عائلات الخطوط المتاحة (0 = الافتراضي). */
const val FONT_DEFAULT = 0
const val FONT_SERIF = 1
const val FONT_SANS = 2
const val FONT_MONO = 3

val FONT_FAMILY_NAMES = mapOf(
    FONT_DEFAULT to "افتراضي",
    FONT_SERIF to "نسخ (Serif)",
    FONT_SANS to "Sans",
    FONT_MONO to "أحادي",
)

fun fontFamilyOf(code: Int): FontFamily? = when (code) {
    FONT_SERIF -> FontFamily.Serif
    FONT_SANS -> FontFamily.SansSerif
    FONT_MONO -> FontFamily.Monospace
    else -> null
}

fun codeOfFontFamily(ff: FontFamily?): Int = when (ff) {
    FontFamily.Serif -> FONT_SERIF
    FontFamily.SansSerif -> FONT_SANS
    FontFamily.Monospace -> FONT_MONO
    else -> FONT_DEFAULT
}

/** اسم الخط المقابل في DOCX/Android لكل عائلة. */
fun fontNameOf(code: Int): String = when (code) {
    FONT_SERIF -> "serif"
    FONT_SANS -> "sans-serif"
    FONT_MONO -> "monospace"
    else -> "sans-serif"
}

/** خصائص تنسيق حرف واحد. */
data class CharAttrs(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val sizeSp: Int = DEFAULT_FONT_SP,
    val colorArgb: Int = COLOR_DEFAULT,
    val highlightArgb: Int = COLOR_DEFAULT,
    val fontFamily: Int = FONT_DEFAULT,
) {
    fun isDefault(): Boolean =
        !bold && !italic && !underline && !strike &&
            sizeSp == DEFAULT_FONT_SP && colorArgb == COLOR_DEFAULT && highlightArgb == COLOR_DEFAULT &&
            fontFamily == FONT_DEFAULT

    fun toSpanStyle(): SpanStyle {
        val decos = mutableListOf<TextDecoration>()
        if (underline) decos.add(TextDecoration.Underline)
        if (strike) decos.add(TextDecoration.LineThrough)
        return SpanStyle(
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            fontFamily = fontFamilyOf(fontFamily),
            textDecoration = if (decos.isEmpty()) null else TextDecoration.combine(decos),
            fontSize = sizeSp.sp,
            color = if (colorArgb != COLOR_DEFAULT) Color(colorArgb) else Color.Unspecified,
            background = if (highlightArgb != COLOR_DEFAULT) Color(highlightArgb) else Color.Unspecified,
        )
    }
}

// ---- محاذاة الفقرة <-> رقم ----
// 4 = تلقائي (يتبع لغة الفقرة: عربي→يمين، لاتيني→يسار) وهو الافتراضي

fun TextAlign.toCode(): Int = when (this) {
    TextAlign.Center -> 1
    TextAlign.Left -> 2
    TextAlign.Justify -> 3
    TextAlign.Right -> 0
    else -> 4 // Start = تلقائي
}

fun Int.toTextAlign(): TextAlign = when (this) {
    0 -> TextAlign.Right
    1 -> TextAlign.Center
    2 -> TextAlign.Left
    3 -> TextAlign.Justify
    else -> TextAlign.Start // تلقائي
}

// ---- اتجاه الفقرة (للأسطر فقط، منفصل عن اتجاه الصفحة) ----
// 0 = تلقائي (حسب اللغة)، 1 = RTL، 2 = LTR

fun Int.toTextDirection(): TextDirection = when (this) {
    1 -> TextDirection.Rtl
    2 -> TextDirection.Ltr
    else -> TextDirection.Content // تلقائي
}

fun TextDirection.toDirCode(): Int = when (this) {
    TextDirection.Rtl -> 1
    TextDirection.Ltr -> 2
    else -> 0
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
            s.textDecoration?.let { d ->
                if (d.contains(TextDecoration.Underline)) a = a.copy(underline = true)
                if (d.contains(TextDecoration.LineThrough)) a = a.copy(strike = true)
            }
            if (s.fontSize != TextUnit.Unspecified) a = a.copy(sizeSp = s.fontSize.value.toInt())
            if (s.color != Color.Unspecified) a = a.copy(colorArgb = s.color.toArgb())
            if (s.background != Color.Unspecified) a = a.copy(highlightArgb = s.background.toArgb())
            if (s.fontFamily != null) a = a.copy(fontFamily = codeOfFontFamily(s.fontFamily))
            attrs[i] = a
        }
    }
    return attrs
}

/** يستخرج محاذاة كل فقرة (الافتراضي تلقائي = Start). */
fun AnnotatedString.toAligns(): MutableList<TextAlign> {
    val paras = paragraphSpans(text)
    return paras.map { (s, _) ->
        paragraphStyles.firstOrNull { it.start == s }?.item?.textAlign ?: TextAlign.Start
    }.toMutableList()
}

/** يستخرج اتجاه كل فقرة (الافتراضي تلقائي = Content). */
fun AnnotatedString.toDirections(): MutableList<TextDirection> {
    val paras = paragraphSpans(text)
    return paras.map { (s, _) ->
        paragraphStyles.firstOrNull { it.start == s }?.item?.textDirection ?: TextDirection.Content
    }.toMutableList()
}

/** يستخرج تباعد الأسطر لكل فقرة (مضاعف: ١٫٠ مفرد، ١٫٥ … الافتراضي ١٫٠). */
fun AnnotatedString.toLineSpacings(): MutableList<Float> {
    val paras = paragraphSpans(text)
    return paras.map { (s, _) ->
        val lh = paragraphStyles.firstOrNull { it.start == s }?.item?.lineHeight
        if (lh != null && lh != TextUnit.Unspecified) lh.value else 1f
    }.toMutableList()
}

/** يستخرج مستوى المسافة البادئة لكل فقرة (٠ = بلا، الافتراضي ٠). */
fun AnnotatedString.toIndents(): MutableList<Int> {
    val paras = paragraphSpans(text)
    return paras.map { (s, _) ->
        val ti = paragraphStyles.firstOrNull { it.start == s }?.item?.textIndent
        if (ti != null && ti != TextIndent.None && ti.firstLine != TextUnit.Unspecified)
            (ti.firstLine.value / INDENT_STEP_PT).roundToInt() else 0
    }.toMutableList()
}

/** يبني نصاً منسّقاً من النص + الخصائص + المحاذاة + اتجاه الفقرات + التباعد + المسافة البادئة. */
fun buildAnnotated(
    text: String,
    attrs: List<CharAttrs>,
    aligns: List<TextAlign>,
    directions: List<TextDirection> = emptyList(),
    lineSpacings: List<Float> = emptyList(),
    indents: List<Int> = emptyList(),
): AnnotatedString = buildAnnotatedString {
    append(text)
    val paras = paragraphSpans(text)
    paras.forEachIndexed { idx, (s, e) ->
        if (e > s) {
            val al = aligns.getOrElse(idx) { TextAlign.Start }
            val dir = directions.getOrElse(idx) { TextDirection.Content }
            val ls = lineSpacings.getOrElse(idx) { 1f }
            val ind = indents.getOrElse(idx) { 0 }
            addStyle(
                ParagraphStyle(
                    textAlign = al,
                    textDirection = dir,
                    lineHeight = if (ls != 1f) ls.em else TextUnit.Unspecified,
                    textIndent = if (ind > 0) {
                        val v = (ind * INDENT_STEP_PT).sp
                        TextIndent(firstLine = v, restLine = v)
                    } else TextIndent.None,
                ),
                s, e,
            )
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
                    .put("b", at.bold).put("i", at.italic).put("u", at.underline).put("st", at.strike)
                    .put("sz", at.sizeSp).put("c", at.colorArgb).put("hl", at.highlightArgb)
                    .put("ff", at.fontFamily)
            )
        }
        i = j
    }
    val alignsArr = JSONArray()
    aligns.forEach { alignsArr.put(it.toCode()) }
    val dirsArr = JSONArray()
    a.toDirections().forEach { dirsArr.put(it.toDirCode()) }
    val lsArr = JSONArray()
    a.toLineSpacings().forEach { lsArr.put(it.toDouble()) }
    val indArr = JSONArray()
    a.toIndents().forEach { indArr.put(it) }
    return JSONObject()
        .put("text", a.text)
        .put("runs", runs)
        .put("aligns", alignsArr)
        .put("dirs", dirsArr)
        .put("ls", lsArr)
        .put("ind", indArr)
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
            strike = r.optBoolean("st"),
            sizeSp = r.optInt("sz", DEFAULT_FONT_SP),
            colorArgb = r.optInt("c", COLOR_DEFAULT),
            highlightArgb = r.optInt("hl", COLOR_DEFAULT),
            fontFamily = r.optInt("ff", FONT_DEFAULT),
        )
        for (idx in s until e) attrs[idx] = a
    }
    val alignsArr = obj.optJSONArray("aligns") ?: JSONArray()
    val paras = paragraphSpans(text)
    val aligns = MutableList(paras.size) { TextAlign.Start }
    for (k in 0 until minOf(alignsArr.length(), aligns.size)) {
        aligns[k] = alignsArr.getInt(k).toTextAlign()
    }
    val dirsArr = obj.optJSONArray("dirs") ?: JSONArray()
    val directions = MutableList(paras.size) { TextDirection.Content }
    for (k in 0 until minOf(dirsArr.length(), directions.size)) {
        directions[k] = dirsArr.getInt(k).toTextDirection()
    }
    val lsArr = obj.optJSONArray("ls") ?: JSONArray()
    val lineSpacings = MutableList(paras.size) { 1f }
    for (k in 0 until minOf(lsArr.length(), lineSpacings.size)) {
        lineSpacings[k] = lsArr.getDouble(k).toFloat()
    }
    val indArr = obj.optJSONArray("ind") ?: JSONArray()
    val indents = MutableList(paras.size) { 0 }
    for (k in 0 until minOf(indArr.length(), indents.size)) {
        indents[k] = indArr.getInt(k)
    }
    return buildAnnotated(text, attrs, aligns, directions, lineSpacings, indents)
}

// ---- التحويل إلى فقرات للتصدير (DOCX / PDF) ----

data class RunOut(
    val text: String,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val strike: Boolean,
    val sizeSp: Int,
    val colorArgb: Int,
    val highlightArgb: Int,
    val fontFamily: Int = FONT_DEFAULT,
)

data class ParaOut(
    val alignCode: Int,
    val dirCode: Int,
    val runs: List<RunOut>,
    val lineSpacing: Float = 1f,
    val indentLevel: Int = 0,
)

fun AnnotatedString.toParagraphsOut(): List<ParaOut> {
    val attrs = toCharAttrs()
    val aligns = toAligns()
    val directions = toDirections()
    val lineSpacings = toLineSpacings()
    val indents = toIndents()
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
            runs.add(RunOut(text.substring(i, j), a.bold, a.italic, a.underline, a.strike, a.sizeSp, a.colorArgb, a.highlightArgb, a.fontFamily))
            i = j
        }
        result.add(
            ParaOut(
                aligns.getOrElse(idx) { TextAlign.Start }.toCode(),
                directions.getOrElse(idx) { TextDirection.Content }.toDirCode(),
                runs,
                lineSpacings.getOrElse(idx) { 1f },
                indents.getOrElse(idx) { 0 },
            )
        )
    }
    return result
}
