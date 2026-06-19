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
    fun directionPreservedAcrossBold() {
        val rtl = RichTextOps.setDirection(tfv("سطر"), TextDirection.Rtl)
        val bolded = RichTextOps.toggleBold(rtl.copy(selection = TextRange(0, 3)))
        assertEquals(TextDirection.Rtl, bolded.annotatedString.toDirections()[0])
    }

    // ---- البحث والاستبدال ----

    @Test
    fun findRanges_findsAll() {
        val r = RichTextOps.findRanges("اب اب اب", "اب")
        assertEquals(3, r.size)
        assertEquals(0, r[0].first)
    }

    @Test
    fun replaceAll_replacesAndCounts() {
        val (nv, count) = RichTextOps.replaceAll(tfv("قطة قطة"), "قطة", "كلب")
        assertEquals("كلب كلب", nv.annotatedString.text)
        assertEquals(2, count)
    }

    @Test
    fun replaceAll_noMatch_zero() {
        val (nv, count) = RichTextOps.replaceAll(tfv("نص"), "xyz", "abc")
        assertEquals("نص", nv.annotatedString.text)
        assertEquals(0, count)
    }

    @Test
    fun findNext_movesSelection() {
        val v = tfv("اب جد اب", selStart = 0, selEnd = 0)
        val r = RichTextOps.findNext(v, "اب")
        assertEquals(0, r.selection.start)
        assertEquals(2, r.selection.end)
        val r2 = RichTextOps.findNext(r, "اب")
        assertEquals(6, r2.selection.start)
    }

    @Test
    fun replaceCurrent_replacesSelectedMatch() {
        // التطابق الأول محدّد
        val v = TextFieldValue(AnnotatedString("اب اب"), selection = TextRange(0, 2))
        val r = RichTextOps.replaceCurrent(v, "اب", "جد")
        assertEquals("جد اب", r.annotatedString.text)
    }

    @Test
    fun splitParagraphInheritsDirection() {
        // Enter في منتصف فقرة RTL ⇒ النصف الثاني (فقرة غير فارغة) يرث RTL
        val old = TextFieldValue(AnnotatedString("سطرنص"), selection = TextRange(3))
        val newAnn = buildAnnotated(
            "سطر\nنص",
            List(6) { CharAttrs() },
            listOf(TextAlign.Start, TextAlign.Start),
            listOf(TextDirection.Rtl, TextDirection.Content),
        )
        val new = TextFieldValue(newAnn, selection = TextRange(4))
        val r = RichTextOps.maybeContinueList(old, new)
        assertEquals(TextDirection.Rtl, r.annotatedString.toDirections()[1])
    }

    // ---- استقرار التحرير ----

    @Test
    fun continueList_noopOnDeletion() {
        val old = tfv("• عنصر", selStart = 6, selEnd = 6)
        val new = TextFieldValue(AnnotatedString("• عنص"), selection = TextRange(5))
        assertEquals("• عنص", RichTextOps.maybeContinueList(old, new).annotatedString.text)
    }

    @Test
    fun continueList_noopOnPaste() {
        val old = tfv("اب", selStart = 2, selEnd = 2)
        val new = TextFieldValue(AnnotatedString("اب نص كثير"), selection = TextRange(10))
        assertEquals("اب نص كثير", RichTextOps.maybeContinueList(old, new).annotatedString.text)
    }

    @Test
    fun formatting_preservesTextAndSelection() {
        val v = TextFieldValue(AnnotatedString("نص مهم"), selection = TextRange(0, 6))
        val r = RichTextOps.toggleBold(v)
        assertEquals("نص مهم", r.annotatedString.text)
        assertEquals(TextRange(0, 6), r.selection)
    }

    @Test
    fun setSize_preservesText() {
        val v = TextFieldValue(AnnotatedString("حجم"), selection = TextRange(0, 3))
        val r = RichTextOps.setSize(v, 24)
        assertEquals("حجم", r.annotatedString.text)
    }

    @Test
    fun replaceSelection_cut() {
        val v = TextFieldValue(AnnotatedString("نص محذوف هنا"), selection = TextRange(3, 9))
        val r = RichTextOps.replaceSelection(v, "")
        assertEquals("نص هنا", r.annotatedString.text)
    }

    @Test
    fun fontFamily_roundTrip() {
        val v = TextFieldValue(AnnotatedString("نص"), selection = TextRange(0, 2))
        val amiri = RichTextOps.setFontFamily(v, 5)
        assertEquals(5, amiri.annotatedString.toCharAttrs()[0].fontFamily)
        val sans = RichTextOps.setFontFamily(v, 2)
        assertEquals(2, sans.annotatedString.toCharAttrs()[0].fontFamily)
    }

    @Test
    fun fontName_exportName() {
        assertEquals("Amiri", fontNameOf(5))
        assertEquals("sans-serif", fontNameOf(0))
    }

    @Test
    fun replaceSelection_pasteAtCursor() {
        val v = TextFieldValue(AnnotatedString("اب"), selection = TextRange(2, 2))
        val r = RichTextOps.replaceSelection(v, " جديد")
        assertEquals("اب جديد", r.annotatedString.text)
        assertEquals(7, r.selection.start)
    }
}
