package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign

/** عمليات التنسيق على محتوى المحرر (تعمل على التحديد الحالي). */
object RichTextOps {

    private fun bounds(v: TextFieldValue): Pair<Int, Int> {
        val s = minOf(v.selection.start, v.selection.end)
        val e = maxOf(v.selection.start, v.selection.end)
        return s to e
    }

    private fun rebuild(v: TextFieldValue, mutate: (MutableList<CharAttrs>, MutableList<TextAlign>) -> Unit): TextFieldValue {
        val a = v.annotatedString
        val attrs = a.toCharAttrs()
        val aligns = a.toAligns()
        mutate(attrs, aligns)
        return v.copy(annotatedString = buildAnnotated(a.text, attrs, aligns))
    }

    fun toggleBold(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _ ->
            val all = (s until e).all { attrs[it].bold }
            for (i in s until e) attrs[i] = attrs[i].copy(bold = !all)
        }
    }

    fun toggleItalic(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _ ->
            val all = (s until e).all { attrs[it].italic }
            for (i in s until e) attrs[i] = attrs[i].copy(italic = !all)
        }
    }

    fun toggleUnderline(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _ ->
            val all = (s until e).all { attrs[it].underline }
            for (i in s until e) attrs[i] = attrs[i].copy(underline = !all)
        }
    }

    fun toggleStrike(v: TextFieldValue): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _ ->
            val all = (s until e).all { attrs[it].strike }
            for (i in s until e) attrs[i] = attrs[i].copy(strike = !all)
        }
    }

    fun setHighlight(v: TextFieldValue, colorArgb: Int): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _ ->
            for (i in s until e) attrs[i] = attrs[i].copy(highlightArgb = colorArgb)
        }
    }

    fun setSize(v: TextFieldValue, sizeSp: Int): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _ ->
            for (i in s until e) attrs[i] = attrs[i].copy(sizeSp = sizeSp.coerceIn(8, 96))
        }
    }

    fun setColor(v: TextFieldValue, colorArgb: Int): TextFieldValue {
        val (s, e) = bounds(v)
        if (s >= e) return v
        return rebuild(v) { attrs, _ ->
            for (i in s until e) attrs[i] = attrs[i].copy(colorArgb = colorArgb)
        }
    }

    fun setAlign(v: TextFieldValue, align: TextAlign): TextFieldValue {
        val (s, e) = bounds(v)
        return rebuild(v) { _, aligns ->
            val paras = paragraphSpans(v.annotatedString.text)
            paras.forEachIndexed { idx, (ps, pe) ->
                val intersects = if (s == e) (ps <= s && s <= pe) else (ps <= e && pe >= s)
                if (intersects && idx < aligns.size) aligns[idx] = align
            }
        }
    }

    /** يقرأ حالة التنسيق عند التحديد لإبراز أزرار شريط الأدوات. */
    fun currentAttrs(v: TextFieldValue): CharAttrs {
        val (s, e) = bounds(v)
        val attrs = v.annotatedString.toCharAttrs()
        if (attrs.isEmpty()) return CharAttrs()
        val idx = (if (s >= e) (s - 1).coerceAtLeast(0) else s).coerceIn(0, attrs.size - 1)
        return attrs[idx]
    }
}
