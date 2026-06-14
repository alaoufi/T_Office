package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection

/** عمليات التنسيق على محتوى المحرر (تعمل على التحديد الحالي). */
object RichTextOps {

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
        mutate: (MutableList<Float>, MutableList<Int>) -> Unit,
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

    /** يضبط تباعد الأسطر للفقرات المحدّدة (مضاعف: ١٫٠، ١٫١٥، ١٫٥، ٢٫٠). */
    fun setLineSpacing(v: TextFieldValue, multiplier: Float): TextFieldValue {
        val (s, e) = bounds(v)
        return rebuildPara(v) { ls, _ ->
            forEachSelectedParagraph(v.annotatedString.text, s, e) { idx ->
                if (idx < ls.size) ls[idx] = multiplier
            }
        }
    }

    /** يزيد/ينقص مستوى المسافة البادئة للفقرات المحدّدة (delta = ‎+1/‎-1). */
    fun changeIndent(v: TextFieldValue, delta: Int): TextFieldValue {
        val (s, e) = bounds(v)
        return rebuildPara(v) { _, ind ->
            forEachSelectedParagraph(v.annotatedString.text, s, e) { idx ->
                if (idx < ind.size) ind[idx] = (ind[idx] + delta).coerceIn(0, 12)
            }
        }
    }

    /** تباعد الأسطر للفقرة الحالية. */
    fun currentLineSpacing(v: TextFieldValue): Float {
        val idx = paragraphIndexAt(v)
        return v.annotatedString.toLineSpacings().getOrElse(idx) { 1f }
    }

    /** مستوى المسافة البادئة للفقرة الحالية. */
    fun currentIndent(v: TextFieldValue): Int {
        val idx = paragraphIndexAt(v)
        return v.annotatedString.toIndents().getOrElse(idx) { 0 }
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
