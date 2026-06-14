package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
}
