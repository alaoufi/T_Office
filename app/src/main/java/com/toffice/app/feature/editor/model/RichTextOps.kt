package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import kotlin.math.roundToInt

/** عمليات التنسيق على محتوى المحرر (تعمل على التحديد الحالي). */
object RichTextOps {

    const val BULLET = "• "
    // كشف عام لعلامة قائمة (عشري/روماني/حروف لاتينية/عربية) للإزالة والاستبدال
    private val NUM_RE = Regex("^(\\()?([0-9٠-٩]{1,4}|[A-Za-z]{1,6}|[ء-ي]{1,2})([.)\\-:：])( +)")
    // كشف العلامة العشرية فقط (للمتابعة التلقائية الآمنة عند Enter)
    private val NUM_DECIMAL_RE = Regex("^(\\()?([0-9٠-٩]+)([.)\\-:：])( +)")
    private val BULLET_RE = Regex("^([•◦▪◆✤➢✔✧‣*\\-])( +)")
    private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"
    private const val AR_ALPHA = "أبتثجحخدذرزسشصضطظعغفقكلمنهوي"

    /** نوع الترقيم. */
    enum class NumType { DECIMAL, UPPER_ROMAN, LOWER_ROMAN, UPPER_ALPHA, LOWER_ALPHA, ARABIC_ALPHA }

    /** مواصفة نمط القائمة (مرقّمة أو نقطية) مع النوع والفاصل والمسافة. */
    data class ListSpec(
        val numbered: Boolean,
        val numType: NumType = NumType.DECIMAL,
        val sep: String = ".",     // الفاصل بعد الرقم: . أو - أو ) أو :
        val wrap: Boolean = false, // (١) بقوسين
        val glyph: String = "•",   // رمز النقطة
        val spaces: Int = 1,       // المسافة بين العلامة والكلمة
    )

    /** يصوغ رقماً بأرقام عربية أو لاتينية حسب السياق. */
    private fun formatNumber(n: Int, arabic: Boolean): String =
        if (arabic) n.toString().map { ARABIC_DIGITS[it - '0'] }.joinToString("") else n.toString()

    /** يحوّل سلسلة أرقام (عربية/لاتينية) إلى عدد. */
    private fun parseNumber(s: String): Int {
        val sb = StringBuilder()
        for (c in s) {
            val idx = ARABIC_DIGITS.indexOf(c)
            sb.append(if (idx >= 0) ('0' + idx) else c)
        }
        return sb.toString().toIntOrNull() ?: 0
    }

    private fun usesArabicDigits(s: String): Boolean = s.any { ARABIC_DIGITS.indexOf(it) >= 0 }

    /** هل سياق النص عربي (RTL) من أول حرف قوي الاتجاه؟ */
    private fun isArabicContext(text: String): Boolean {
        for (c in text) {
            when (Character.getDirectionality(c)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return true
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
            }
        }
        return true // الافتراضي عربي
    }

    private fun bounds(v: TextFieldValue): Pair<Int, Int> {
        val s = minOf(v.selection.start, v.selection.end)
        val e = maxOf(v.selection.start, v.selection.end)
        return s to e
    }

    private fun rebuild(
        v: TextFieldValue,
        mutate: (MutableList<CharAttrs>, MutableList<TextAlign>, MutableList<TextDirection>) -> Unit,
    ): TextFieldValue {
        val a = v.annotatedString
        val attrs = a.toCharAttrs()
        val aligns = a.toAligns()
        val dirs = a.toDirections()
        val ls = a.toLineSpacings()
        val ind = a.toIndents()
        mutate(attrs, aligns, dirs)
        return v.copy(annotatedString = buildAnnotated(a.text, attrs, aligns, dirs, ls, ind))
    }

    /** نسخة تتيح تعديل تباعد الأسطر والمسافة البادئة للفقرات. */
    private fun rebuildPara(
        v: TextFieldValue,
        mutate: (MutableList<Float>, MutableList<ParaIndent>) -> Unit,
    ): TextFieldValue {
        val a = v.annotatedString
        val attrs = a.toCharAttrs()
        val aligns = a.toAligns()
        val dirs = a.toDirections()
        val ls = a.toLineSpacings()
        val ind = a.toIndents()
        mutate(ls, ind)
        return v.copy(annotatedString = buildAnnotated(a.text, attrs, aligns, dirs, ls, ind))
    }

    fun toggleBold(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            val all = (s until e).all { attrs[it].bold }
            for (i in s until e) attrs[i] = attrs[i].copy(bold = !all)
        }
    }

    fun toggleItalic(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            val all = (s until e).all { attrs[it].italic }
            for (i in s until e) attrs[i] = attrs[i].copy(italic = !all)
        }
    }

    fun toggleUnderline(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            val all = (s until e).all { attrs[it].underline }
            for (i in s until e) attrs[i] = attrs[i].copy(underline = !all)
        }
    }

    fun toggleStrike(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            val all = (s until e).all { attrs[it].strike }
            for (i in s until e) attrs[i] = attrs[i].copy(strike = !all)
        }
    }

    fun setHighlight(v: TextFieldValue, colorArgb: Int): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            for (i in s until e) attrs[i] = attrs[i].copy(highlightArgb = colorArgb)
        }
    }

    fun setSize(v: TextFieldValue, sizeSp: Int): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            for (i in s until e) attrs[i] = attrs[i].copy(sizeSp = sizeSp.coerceIn(8, 96))
        }
    }

    fun setColor(v: TextFieldValue, colorArgb: Int): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            for (i in s until e) attrs[i] = attrs[i].copy(colorArgb = colorArgb)
        }
    }

    /** يمسح كل تنسيق الحروف على التحديد (غامق/مائل/لون/حجم/خط…) ويعيده للافتراضي. */
    fun clearFormatting(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            for (i in s until e) attrs[i] = CharAttrs()
        }
    }

    // ---- البحث والاستبدال ----

    /** مواضع كل تطابقات النص (غير متداخلة). */
    fun findRanges(text: String, query: String, caseSensitive: Boolean = false): List<IntRange> {
        if (query.isEmpty()) return emptyList()
        val result = mutableListOf<IntRange>()
        var from = 0
        while (true) {
            val i = text.indexOf(query, from, ignoreCase = !caseSensitive)
            if (i < 0) break
            result.add(i until (i + query.length))
            from = i + query.length
        }
        return result
    }

    /** ينقل التحديد إلى التطابق التالي بعد الموضع الحالي (مع الالتفاف). */
    fun findNext(v: TextFieldValue, query: String, caseSensitive: Boolean = false): TextFieldValue {
        val ranges = findRanges(v.annotatedString.text, query, caseSensitive)
        if (ranges.isEmpty()) return v
        val from = maxOf(v.selection.start, v.selection.end)
        val next = ranges.firstOrNull { it.first >= from } ?: ranges.first()
        return v.copy(selection = TextRange(next.first, next.last + 1))
    }

    private fun replaceRange(v: TextFieldValue, s: Int, e: Int, replacement: String): TextFieldValue {
        val a = v.annotatedString
        val attrs = a.toCharAttrs()
        val aligns = a.toAligns()
        val dirs = a.toDirections()
        val ls = a.toLineSpacings()
        val ind = a.toIndents()
        val attrAt = attrs.getOrElse(s) { CharAttrs() }
        val sb = StringBuilder(a.text)
        sb.replace(s, e, replacement)
        repeat(e - s) { if (s < attrs.size) attrs.removeAt(s) }
        repeat(replacement.length) { attrs.add(s, attrAt) }
        val txt = sb.toString()
        return v.copy(
            annotatedString = buildAnnotated(txt, attrs, aligns, dirs, ls, ind),
            selection = TextRange((s + replacement.length).coerceIn(0, txt.length)),
        )
    }

    /** يستبدل التطابق المحدّد حالياً (إن كان محدّداً) ثم ينتقل للتالي. */
    fun replaceCurrent(v: TextFieldValue, query: String, replacement: String, caseSensitive: Boolean = false): TextFieldValue {
        if (query.isEmpty()) return v
        val s = minOf(v.selection.start, v.selection.end)
        val e = maxOf(v.selection.start, v.selection.end)
        val selText = if (e > s) v.annotatedString.text.substring(s, e) else ""
        return if (e > s && selText.equals(query, ignoreCase = !caseSensitive)) {
            val replaced = replaceRange(v, s, e, replacement)
            findNext(replaced, query, caseSensitive)
        } else {
            findNext(v, query, caseSensitive)
        }
    }

    /** يستبدل كل التطابقات. يعيد القيمة الجديدة وعدد الاستبدالات. */
    fun replaceAll(v: TextFieldValue, query: String, replacement: String, caseSensitive: Boolean = false): Pair<TextFieldValue, Int> {
        if (query.isEmpty()) return v to 0
        val a = v.annotatedString
        val ranges = findRanges(a.text, query, caseSensitive)
        if (ranges.isEmpty()) return v to 0
        val attrs = a.toCharAttrs()
        val aligns = a.toAligns()
        val dirs = a.toDirections()
        val ls = a.toLineSpacings()
        val ind = a.toIndents()
        val sb = StringBuilder(a.text)
        for (r in ranges.reversed()) {
            val attrAt = attrs.getOrElse(r.first) { CharAttrs() }
            sb.replace(r.first, r.last + 1, replacement)
            repeat(r.last - r.first + 1) { if (r.first < attrs.size) attrs.removeAt(r.first) }
            repeat(replacement.length) { attrs.add(r.first, attrAt) }
        }
        val txt = sb.toString()
        return v.copy(
            annotatedString = buildAnnotated(txt, attrs, aligns, dirs, ls, ind),
            selection = TextRange(0),
        ) to ranges.size
    }

    /** يستبدل التحديد الحالي (أو يُدرج عند المؤشر) بنصّ عادي — لأوامر القص/اللصق. */
    fun replaceSelection(v: TextFieldValue, replacement: String): TextFieldValue {
        val s = minOf(v.selection.start, v.selection.end)
        val e = maxOf(v.selection.start, v.selection.end)
        return replaceRange(v, s, e, replacement)
    }

    fun setFontFamily(v: TextFieldValue, code: Int): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            for (i in s until e) attrs[i] = attrs[i].copy(fontFamily = code)
        }
    }

    /** يبدّل الكتابة المرتفعة (١) أو المنخفضة (٢) على التحديد. */
    fun toggleScript(v: TextFieldValue, code: Int): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _, _ ->
            val all = (s until e).all { attrs[it].script == code }
            for (i in s until e) attrs[i] = attrs[i].copy(script = if (all) 0 else code)
        }
    }

    fun setAlign(v: TextFieldValue, align: TextAlign): TextFieldValue {
        val (s, e) = bounds(v)
        return rebuild(v) { _, aligns, _ ->
            forEachSelectedParagraph(v.annotatedString.text, s, e) { idx ->
                if (idx < aligns.size) aligns[idx] = align
            }
        }
    }

    /** يضبط اتجاه الفقرة (للأسطر فقط): RTL / LTR / تلقائي. */
    fun setDirection(v: TextFieldValue, direction: TextDirection): TextFieldValue {
        val (s, e) = bounds(v)
        return rebuild(v) { _, _, dirs ->
            forEachSelectedParagraph(v.annotatedString.text, s, e) { idx ->
                if (idx < dirs.size) dirs[idx] = direction
            }
        }
    }

    // ---- القوائم النقطية/المرقّمة (العلامة نص فعلي) ----

    /** طول علامة القائمة في بداية السطر (٠ إن لا توجد). */
    private fun markerLength(text: String, lineStart: Int): Int {
        var end = lineStart
        while (end < text.length && text[end] != '\n') end++
        val seg = text.substring(lineStart, end)
        NUM_RE.find(seg)?.let { return it.value.length }
        BULLET_RE.find(seg)?.let { return it.value.length }
        return 0
    }

    private fun buildMarker(spec: ListSpec, n: Int, arabic: Boolean): String {
        val gap = " ".repeat(spec.spaces.coerceAtLeast(1))
        return if (spec.numbered) {
            val body = formatNum(n, spec.numType, arabic)
            if (spec.wrap) "($body)$gap" else "$body${spec.sep}$gap"
        } else spec.glyph + gap
    }

    /** علامة المعاينة للعنصر n (سياق عربي) — لعرضها في مربّع الاختيار. */
    fun previewMarker(spec: ListSpec, n: Int): String = buildMarker(spec, n, arabic = true)

    private fun formatNum(n: Int, type: NumType, arabic: Boolean): String = when (type) {
        NumType.DECIMAL -> formatNumber(n, arabic)
        NumType.UPPER_ROMAN -> toRoman(n)
        NumType.LOWER_ROMAN -> toRoman(n).lowercase()
        NumType.UPPER_ALPHA -> toAlpha(n)
        NumType.LOWER_ALPHA -> toAlpha(n).lowercase()
        NumType.ARABIC_ALPHA -> toArabicAlpha(n)
    }

    private fun toRoman(n: Int): String {
        if (n <= 0 || n >= 4000) return n.toString()
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val syms = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        var x = n
        val sb = StringBuilder()
        for (i in values.indices) while (x >= values[i]) { sb.append(syms[i]); x -= values[i] }
        return sb.toString()
    }

    private fun toAlpha(n: Int): String {
        if (n <= 0) return n.toString()
        var x = n
        val sb = StringBuilder()
        while (x > 0) { x--; sb.insert(0, ('A' + (x % 26))); x /= 26 }
        return sb.toString()
    }

    private fun toArabicAlpha(n: Int): String {
        if (n <= 0) return n.toString()
        var x = n
        val sb = StringBuilder()
        while (x > 0) { x--; sb.insert(0, AR_ALPHA[x % AR_ALPHA.length]); x /= AR_ALPHA.length }
        return sb.toString()
    }

    /** يطبّق نمط قائمة على الفقرات المحدّدة (spec=null لإزالة القائمة). */
    fun applyList(v: TextFieldValue, spec: ListSpec?): TextFieldValue {
        val a = v.annotatedString
        val origText = a.text
        val attrs = a.toCharAttrs()
        val aligns = a.toAligns()
        val dirs = a.toDirections()
        val ls = a.toLineSpacings()
        val ind = a.toIndents()
        val paras = paragraphSpans(origText)
        val (s, e) = bounds(v)
        val selIdx = paras.indices.filter { i ->
            val (ps, pe) = paras[i]
            if (s == e) ps <= s && s <= pe else ps <= e && pe >= s
        }
        if (selIdx.isEmpty()) return v

        val sb = StringBuilder(origText)
        var offset = 0
        var caret = v.selection.end
        var n = 1
        for (i in selIdx) {
            val ps = paras[i].first + offset
            val curLen = markerLength(sb.toString(), ps)
            if (curLen > 0) {
                sb.delete(ps, ps + curLen)
                repeat(curLen) { if (ps < attrs.size) attrs.removeAt(ps) }
                if (caret > ps) caret -= minOf(curLen, caret - ps)
                offset -= curLen
            }
            if (spec != null) {
                var lineEnd = ps
                while (lineEnd < sb.length && sb[lineEnd] != '\n') lineEnd++
                val arabic = isArabicContext(sb.substring(ps, lineEnd))
                val marker = buildMarker(spec, n, arabic)
                sb.insert(ps, marker)
                repeat(marker.length) { attrs.add(ps, CharAttrs()) }
                if (caret >= ps) caret += marker.length
                offset += marker.length
                n++
            }
        }
        val newText = sb.toString()
        return v.copy(
            annotatedString = buildAnnotated(newText, attrs, aligns, dirs, ls, ind),
            selection = TextRange(caret.coerceIn(0, newText.length)),
        )
    }

    /** متابعة القائمة تلقائياً عند Enter (وإنهاؤها عند عنصر فارغ). تُستدعى من معالج التغيير. */
    fun maybeContinueList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        try {
            if (new.text.length != old.text.length + 1) return new
            val caret = new.selection.end
            if (caret < 1 || caret > new.text.length || new.text[caret - 1] != '\n') return new
            val prevStart = run {
                var i = caret - 2
                while (i >= 0 && new.text[i] != '\n') i--
                i + 1
            }
            val prevLine = new.text.substring(prevStart, caret - 1)
            val numMatch = NUM_DECIMAL_RE.find(prevLine)
            val bulMatch = BULLET_RE.find(prevLine)
            val attrs = new.annotatedString.toCharAttrs()
            val aligns = new.annotatedString.toAligns()
            val dirs = new.annotatedString.toDirections()
            val ls = new.annotatedString.toLineSpacings()
            val ind = new.annotatedString.toIndents()
            val marker = when {
                numMatch != null -> {
                    val wrap = numMatch.groupValues[1].isNotEmpty()
                    val digits = numMatch.groupValues[2]
                    val sep = numMatch.groupValues[3]
                    val gap = numMatch.groupValues[4]
                    val num = formatNumber(parseNumber(digits) + 1, usesArabicDigits(digits))
                    if (wrap) "($num)$gap" else "$num$sep$gap"
                }
                bulMatch != null -> bulMatch.value
                else -> null
            }
            // ليست قائمة: ورّث المحاذاة والاتجاه من الفقرة السابقة لتبقى مستقرّة
            if (marker == null) {
                val paras = paragraphSpans(new.text)
                val prevIdx = paras.indexOfFirst { (ps, pe) -> prevStart in ps until (pe + 1) && ps == prevStart }
                val newIdx = prevIdx + 1
                if (prevIdx < 0 || newIdx >= aligns.size || newIdx >= dirs.size) return new
                val pAlign = aligns[prevIdx]
                val pDir = dirs[prevIdx]
                if (pAlign == TextAlign.Start && pDir == TextDirection.Content) return new
                aligns[newIdx] = pAlign
                dirs[newIdx] = pDir
                return new.copy(
                    annotatedString = buildAnnotated(new.text, attrs, aligns, dirs, ls, ind),
                    selection = new.selection,
                )
            }
            // عنصر فارغ (علامة فقط) ثم Enter => إنهاء القائمة بحذف العلامة
            val prevMarkerLen = markerLength(new.text, prevStart)
            if (prevLine.length == prevMarkerLen) {
                val sb = StringBuilder(new.text)
                sb.delete(prevStart, prevStart + prevMarkerLen)
                repeat(prevMarkerLen) { if (prevStart < attrs.size) attrs.removeAt(prevStart) }
                val txt = sb.toString()
                val newCaret = (caret - prevMarkerLen).coerceIn(0, txt.length)
                return new.copy(
                    annotatedString = buildAnnotated(txt, attrs, aligns, dirs, ls, ind),
                    selection = TextRange(newCaret),
                )
            }
            // أدرج علامة في السطر الجديد
            val sb = StringBuilder(new.text)
            sb.insert(caret, marker)
            repeat(marker.length) { attrs.add(caret, CharAttrs()) }
            val txt = sb.toString()
            return new.copy(
                annotatedString = buildAnnotated(txt, attrs, aligns, dirs, ls, ind),
                selection = TextRange((caret + marker.length).coerceIn(0, txt.length)),
            )
        } catch (_: Exception) {
            return new
        }
    }

    /** يضبط تباعد الأسطر للفقرات المحدّدة (مضاعف: ١٫٠، ١٫١٥، ١٫٥، ٢٫٠). */
    fun setLineSpacing(v: TextFieldValue, multiplier: Float): TextFieldValue {
        val (s, e) = bounds(v)
        return rebuildPara(v) { ls, _ ->
            forEachSelectedParagraph(v.annotatedString.text, s, e) { idx ->
                if (idx < ls.size) ls[idx] = multiplier
            }
        }
    }

    /** يزيد/ينقص المسافة البادئة (السطر الأول + البقية معاً) للفقرات المحدّدة بخطوة ثابتة. */
    fun changeIndent(v: TextFieldValue, delta: Int): TextFieldValue {
        val step = delta * INDENT_STEP_PT.toFloat()
        val (s, e) = bounds(v)
        return rebuildPara(v) { _, ind ->
            forEachSelectedParagraph(v.annotatedString.text, s, e) { idx ->
                if (idx < ind.size) {
                    val cur = ind[idx]
                    val max = 12f * INDENT_STEP_PT
                    ind[idx] = ParaIndent(
                        (cur.firstPt + step).coerceIn(0f, max),
                        (cur.leftPt + step).coerceIn(0f, max),
                    )
                }
            }
        }
    }

    /** يضبط المسافة البادئة (سطر أول/بقية بالنقاط) للفقرة عند المؤشر فقط (من المسطرة). */
    fun setParagraphIndent(v: TextFieldValue, firstPt: Float, leftPt: Float): TextFieldValue {
        val idx = paragraphIndexAt(v)
        return rebuildPara(v) { _, ind ->
            if (idx in ind.indices) ind[idx] = ParaIndent(firstPt.coerceAtLeast(0f), leftPt.coerceAtLeast(0f))
        }
    }

    /** المسافة البادئة (سطر أول/بقية) للفقرة الحالية بالنقاط. */
    fun currentParaIndent(v: TextFieldValue): ParaIndent {
        val idx = paragraphIndexAt(v)
        return v.annotatedString.toIndents().getOrElse(idx) { ParaIndent() }
    }

    /** تباعد الأسطر للفقرة الحالية. */
    fun currentLineSpacing(v: TextFieldValue): Float {
        val idx = paragraphIndexAt(v)
        return v.annotatedString.toLineSpacings().getOrElse(idx) { 1f }
    }

    /** مستوى المسافة البادئة التقريبي للفقرة الحالية (لإبراز أزرار الشريط). */
    fun currentIndent(v: TextFieldValue): Int {
        val left = currentParaIndent(v).leftPt
        return (left / INDENT_STEP_PT).roundToInt()
    }

    private inline fun forEachSelectedParagraph(text: String, s: Int, e: Int, action: (Int) -> Unit) {
        val paras = paragraphSpans(text)
        paras.forEachIndexed { idx, (ps, pe) ->
            val intersects = if (s == e) (ps <= s && s <= pe) else (ps <= e && pe >= s)
            if (intersects) action(idx)
        }
    }

    /** يقرأ حالة تنسيق الحرف عند التحديد لإبراز الأزرار. */
    fun currentAttrs(v: TextFieldValue): CharAttrs {
        val (s, e) = bounds(v)
        val attrs = v.annotatedString.toCharAttrs()
        if (attrs.isEmpty()) return CharAttrs()
        val idx = (if (s >= e) (s - 1).coerceAtLeast(0) else s).coerceIn(0, attrs.size - 1)
        return attrs[idx]
    }

    /** محاذاة الفقرة الحالية. */
    fun currentAlign(v: TextFieldValue): TextAlign {
        val idx = paragraphIndexAt(v)
        return v.annotatedString.toAligns().getOrElse(idx) { TextAlign.Start }
    }

    /** اتجاه الفقرة الحالية. */
    fun currentDirection(v: TextFieldValue): TextDirection {
        val idx = paragraphIndexAt(v)
        return v.annotatedString.toDirections().getOrElse(idx) { TextDirection.Content }
    }

    private fun paragraphIndexAt(v: TextFieldValue): Int {
        val pos = minOf(v.selection.start, v.selection.end)
        val paras = paragraphSpans(v.annotatedString.text)
        paras.forEachIndexed { idx, (ps, pe) -> if (ps <= pos && pos <= pe) return idx }
        return 0
    }
}
