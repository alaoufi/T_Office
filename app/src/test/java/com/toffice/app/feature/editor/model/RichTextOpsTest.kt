package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات منطق القوائم (الترقيم/النقاط) — نص غير منسّق فلا يلمس Android أو org.json.
 */
class RichTextOpsTest {

    private fun tfv(text: String, selStart: Int = 0, selEnd: Int = text.length) =
        TextFieldValue(AnnotatedString(text), selection = TextRange(selStart, selEnd))

    @Test
    fun numberedList_arabicText_usesArabicDigits() {
        val v = tfv("مرحبا")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, sep = "."))
        assertEquals("١. مرحبا", r.annotatedString.text)
    }

    @Test
    fun numberedList_latinText_usesLatinDigits() {
        val v = tfv("Hello")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, sep = "."))
        assertEquals("1. Hello", r.annotatedString.text)
    }

    @Test
    fun numberedList_dashSeparator() {
        val v = tfv("بند")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, sep = "-"))
        assertEquals("١- بند", r.annotatedString.text)
    }

    @Test
    fun numberedList_parenWrap() {
        val v = tfv("بند")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, sep = ")", wrap = true))
        assertEquals("(١) بند", r.annotatedString.text)
    }

    @Test
    fun numberedList_wideSpacing() {
        val v = tfv("بند")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, sep = ".", spaces = 3))
        assertEquals("١.   بند", r.annotatedString.text)
    }

    @Test
    fun bulletList_glyph() {
        val v = tfv("عنصر")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = false, glyph = "•"))
        assertEquals("• عنصر", r.annotatedString.text)
    }

    @Test
    fun multiParagraph_numbersSequentially() {
        val v = tfv("أ\nب\nج")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, sep = "."))
        assertEquals("١. أ\n٢. ب\n٣. ج", r.annotatedString.text)
    }

    @Test
    fun applyNull_removesMarker() {
        val v = tfv("١. مرحبا")
        val r = RichTextOps.applyList(v, null)
        assertEquals("مرحبا", r.annotatedString.text)
    }

    @Test
    fun switchStyle_replacesMarker() {
        val numbered = RichTextOps.applyList(tfv("بند"), RichTextOps.ListSpec(numbered = true, sep = "."))
        val toBullet = RichTextOps.applyList(
            numbered.copy(selection = TextRange(0, numbered.annotatedString.text.length)),
            RichTextOps.ListSpec(numbered = false, glyph = "•"),
        )
        assertEquals("• بند", toBullet.annotatedString.text)
    }

    @Test
    fun continueList_incrementsArabicNumber() {
        val old = tfv("١. أ", selStart = 4, selEnd = 4)
        val new = TextFieldValue(AnnotatedString("١. أ\n"), selection = TextRange(5))
        val r = RichTextOps.maybeContinueList(old, new)
        assertEquals("١. أ\n٢. ", r.annotatedString.text)
        assertEquals(8, r.selection.start)
    }

    @Test
    fun continueList_bulletContinues() {
        val old = tfv("• عنصر", selStart = 6, selEnd = 6)
        val new = TextFieldValue(AnnotatedString("• عنصر\n"), selection = TextRange(7))
        val r = RichTextOps.maybeContinueList(old, new)
        assertEquals("• عنصر\n• ", r.annotatedString.text)
    }

    @Test
    fun continueList_emptyItemEndsList() {
        // عنصر فارغ (علامة فقط) ثم Enter => تُحذف العلامة وتنتهي القائمة
        val old = tfv("٢. ", selStart = 3, selEnd = 3)
        val new = TextFieldValue(AnnotatedString("٢. \n"), selection = TextRange(4))
        val r = RichTextOps.maybeContinueList(old, new)
        assertEquals("\n", r.annotatedString.text)
    }

    @Test
    fun continueList_nonListUnchanged() {
        val old = tfv("نص عادي", selStart = 7, selEnd = 7)
        val new = TextFieldValue(AnnotatedString("نص عادي\n"), selection = TextRange(8))
        val r = RichTextOps.maybeContinueList(old, new)
        assertEquals("نص عادي\n", r.annotatedString.text)
    }

    // ---- أنواع الترقيم الموسّعة ----

    @Test
    fun upperRoman_sequence() {
        val v = tfv("A\nB\nC")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.UPPER_ROMAN, sep = "."))
        assertEquals("I. A\nII. B\nIII. C", r.annotatedString.text)
    }

    @Test
    fun lowerRoman_sequence() {
        val v = tfv("A\nB\nC\nD")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.LOWER_ROMAN, sep = "."))
        assertEquals("i. A\nii. B\niii. C\niv. D", r.annotatedString.text)
    }

    @Test
    fun upperAlpha_sequence() {
        val v = tfv("x\ny\nz")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.UPPER_ALPHA, sep = "."))
        assertEquals("A. x\nB. y\nC. z", r.annotatedString.text)
    }

    @Test
    fun lowerAlpha_parenSequence() {
        val v = tfv("x\ny\nz")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.LOWER_ALPHA, sep = ")"))
        assertEquals("a) x\nb) y\nc) z", r.annotatedString.text)
    }

    @Test
    fun arabicAlpha_sequence() {
        val v = tfv("س١\nس٢\nس٣")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.ARABIC_ALPHA, sep = "."))
        assertEquals("أ. س١\nب. س٢\nت. س٣", r.annotatedString.text)
    }

    @Test
    fun diamondBullet_sequence() {
        val v = tfv("a\nb")
        val r = RichTextOps.applyList(v, RichTextOps.ListSpec(numbered = false, glyph = "◆"))
        assertEquals("◆ a\n◆ b", r.annotatedString.text)
    }

    @Test
    fun removeRomanMarker() {
        val v = tfv("II. بند")
        val r = RichTextOps.applyList(v, null)
        assertEquals("بند", r.annotatedString.text)
    }

    @Test
    fun removeAlphaMarker() {
        val v = tfv("A. بند")
        val r = RichTextOps.applyList(v, null)
        assertEquals("بند", r.annotatedString.text)
    }

    // ---- استقرار الاتجاه/المحاذاة ----

    @Test
    fun emptyTrailingParagraph_keepsDirection() {
        val ann = buildAnnotated(
            "سطر\n",
            List(4) { CharAttrs() },
            listOf(TextAlign.Start, TextAlign.Start),
            listOf(TextDirection.Rtl, TextDirection.Rtl),
        )
        assertEquals(TextDirection.Rtl, ann.toDirections()[1])
    }

    @Test
    fun directionPreservedAcrossBold() {
        val rtl = RichTextOps.setDirection(tfv("سطر"), TextDirection.Rtl)
        val bolded = RichTextOps.toggleBold(rtl.copy(selection = TextRange(0, 3)))
        assertEquals(TextDirection.Rtl, bolded.annotatedString.toDirections()[0])
    }

    @Test
    fun newParagraphInheritsDirection() {
        // الفقرة الأولى RTL، ثم Enter ⇒ الفقرة الجديدة ترث RTL
        val rtl = RichTextOps.setDirection(tfv("سطر"), TextDirection.Rtl)
        val old = rtl.copy(selection = TextRange(3))
        // محاكاة قيمة ما بعد Enter مع الحفاظ على نمط الفقرة الأولى
        val newAnn = buildAnnotated(
            "سطر\n",
            List(4) { CharAttrs() },
            listOf(TextAlign.Start, TextAlign.Start),
            listOf(TextDirection.Rtl, TextDirection.Content),
        )
        val new = TextFieldValue(newAnn, selection = TextRange(4))
        val r = RichTextOps.maybeContinueList(old, new)
        assertEquals(TextDirection.Rtl, r.annotatedString.toDirections()[1])
    }
}
