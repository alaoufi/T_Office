package com.toffice.app.feature.editor.io

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import com.toffice.app.feature.editor.model.CharAttrs
import com.toffice.app.feature.editor.model.DEFAULT_FONT_SP
import com.toffice.app.feature.editor.model.INDENT_STEP_PT
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.paragraphSpans
import com.toffice.app.feature.editor.model.toAligns
import com.toffice.app.feature.editor.model.toCharAttrs
import com.toffice.app.feature.editor.model.toDirections
import com.toffice.app.feature.editor.model.toIndents
import com.toffice.app.feature.editor.model.toLineSpacings
import java.io.OutputStream

/** يصدّر المستند إلى PDF مع المحافظة على التنسيق والهوامش والترويسة/التذييل وترقيم الصفحات. */
object PdfExporter {

    fun export(
        out: OutputStream,
        body: AnnotatedString,
        page: PageSettings,
        header: AnnotatedString,
        footer: AnnotatedString,
    ) {
        val pageW = page.pageWidthPt.toInt().coerceAtLeast(100)
        val pageH = page.pageHeightPt.toInt().coerceAtLeast(100)
        val left = page.marginLeftPt.toInt()
        val right = page.marginRightPt.toInt()
        val top = page.marginTopPt.toInt()
        val bottom = page.marginBottomPt.toInt()
        val contentW = (pageW - left - right).coerceAtLeast(50)
        val bodyTop = top
        val bodyHeight = (pageH - top - bottom).coerceAtLeast(50)

        val paint = TextPaint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = DEFAULT_FONT_SP.toFloat()
        }
        val spannable = toSpannable(body)
        // تباعد الأسطر: نطبّق المضاعف الأكثر شيوعاً على المتن (StaticLayout عام لا يدعم تباعداً لكل فقرة قبل API 29)
        val spacingMult = body.toLineSpacings().groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: 1f
        val layout = StaticLayout.Builder
            .obtain(spannable, 0, spannable.length, paint, contentW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setLineSpacing(0f, spacingMult)
            .setIncludePad(false)
            .build()

        val doc = PdfDocument()
        val totalLines = layout.lineCount
        var startLine = 0
        var pageIndex = 0

        do {
            val sliceTop = layout.getLineTop(startLine)
            var endLine = startLine
            while (endLine < totalLines && layout.getLineBottom(endLine) - sliceTop <= bodyHeight) endLine++
            if (endLine == startLine) endLine = startLine + 1 // سطر واحد على الأقل

            val pdfPage = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex + 1).create())
            val canvas = pdfPage.canvas

            // الترويسة (بتنسيقها)
            if (header.text.isNotBlank()) {
                drawRichText(canvas, header, left.toFloat(), 14f, contentW)
            }
            // المتن (شريحة الصفحة)
            canvas.save()
            canvas.translate(left.toFloat(), bodyTop.toFloat() - sliceTop)
            canvas.clipRect(0f, sliceTop.toFloat(), contentW.toFloat(), (sliceTop + bodyHeight).toFloat())
            layout.draw(canvas)
            canvas.restore()
            // التذييل (بتنسيقه)
            if (footer.text.isNotBlank()) {
                drawRichText(canvas, footer, left.toFloat(), (pageH - bottom + 6).toFloat(), contentW)
            }
            // رقم الصفحة
            if (page.showPageNumber) {
                val np = TextPaint(paint).apply { textAlign = android.graphics.Paint.Align.CENTER; textSize = 11f }
                canvas.drawText("${pageIndex + 1}", pageW / 2f, (pageH - bottom / 2).toFloat(), np)
            }

            doc.finishPage(pdfPage)
            startLine = endLine
            pageIndex++
        } while (startLine < totalLines)

        doc.writeTo(out)
        doc.close()
    }

    private fun drawRichText(
        canvas: android.graphics.Canvas,
        annotated: AnnotatedString,
        x: Float,
        y: Float,
        width: Int,
    ) {
        val tp = TextPaint().apply { isAntiAlias = true; color = Color.BLACK; textSize = DEFAULT_FONT_SP.toFloat() }
        val sp = toSpannable(annotated)
        val l = StaticLayout.Builder.obtain(sp, 0, sp.length, tp, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, y)
        l.draw(canvas)
        canvas.restore()
    }

    private fun toSpannable(annotated: AnnotatedString): SpannableStringBuilder {
        val text = annotated.text
        val sb = SpannableStringBuilder(text)
        val attrs = annotated.toCharAttrs()

        var i = 0
        while (i < text.length) {
            val a = attrs[i]
            var j = i + 1
            while (j < text.length && attrs[j] == a) j++
            applyRunSpans(sb, a, i, j)
            i = j
        }

        val aligns = annotated.toAligns()
        val directions = annotated.toDirections()
        val indents = annotated.toIndents()
        val paras = paragraphSpans(text)
        paras.forEachIndexed { idx, (s, e) ->
            if (e > s) {
                val rtl = when (directions.getOrElse(idx) { TextDirection.Content }) {
                    TextDirection.Rtl -> true
                    TextDirection.Ltr -> false
                    else -> isRtl(text.substring(s, e))
                }
                val al = androidAlign(aligns.getOrElse(idx) { TextAlign.Start }, rtl)
                sb.setSpan(AlignmentSpan.Standard(al), s, e, Spanned.SPAN_PARAGRAPH)
                val level = indents.getOrElse(idx) { 0 }
                if (level > 0) {
                    sb.setSpan(LeadingMarginSpan.Standard(level * INDENT_STEP_PT), s, e, Spanned.SPAN_PARAGRAPH)
                }
            }
        }
        return sb
    }

    private fun applyRunSpans(sb: SpannableStringBuilder, a: CharAttrs, s: Int, e: Int) {
        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        if (a.bold && a.italic) sb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), s, e, flag)
        else if (a.bold) sb.setSpan(StyleSpan(Typeface.BOLD), s, e, flag)
        else if (a.italic) sb.setSpan(StyleSpan(Typeface.ITALIC), s, e, flag)
        if (a.underline) sb.setSpan(UnderlineSpan(), s, e, flag)
        if (a.strike) sb.setSpan(StrikethroughSpan(), s, e, flag)
        if (a.fontFamily != com.toffice.app.feature.editor.model.FONT_DEFAULT) {
            sb.setSpan(android.text.style.TypefaceSpan(com.toffice.app.feature.editor.model.fontNameOf(a.fontFamily)), s, e, flag)
        }
        sb.setSpan(AbsoluteSizeSpan(a.sizeSp, false), s, e, flag)
        if (a.colorArgb != 0) sb.setSpan(ForegroundColorSpan(a.colorArgb), s, e, flag)
        if (a.highlightArgb != 0) sb.setSpan(BackgroundColorSpan(a.highlightArgb), s, e, flag)
    }

    private fun androidAlign(align: TextAlign, rtl: Boolean): Layout.Alignment = when (align) {
        TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
        TextAlign.Right -> if (rtl) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
        TextAlign.Left -> if (rtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        else -> Layout.Alignment.ALIGN_NORMAL // Start/Justify
    }

    private fun isRtl(text: String): Boolean {
        for (c in text) {
            when (Character.getDirectionality(c)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return true
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
            }
        }
        return true
    }
}
