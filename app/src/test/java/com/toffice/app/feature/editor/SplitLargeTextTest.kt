package com.toffice.app.feature.editor

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitLargeTextTest {

    @Test fun smallText_returnsSinglePart() {
        val a = AnnotatedString("مستند صغير\nسطر ثانٍ")
        val parts = splitLargeText(a)
        assertEquals(1, parts.size)
        assertEquals(a.text, parts[0].text)
    }

    @Test fun largeText_splits_andPreservesAllContent() {
        // نص كبير مؤلّف من فقرات
        val sb = StringBuilder()
        repeat(2000) { sb.append("سطر رقم ").append(it).append('\n') }
        val a = AnnotatedString(sb.toString())
        val parts = splitLargeText(a)

        assertTrue("يجب أن يُقسَّم إلى أكثر من جزء", parts.size > 1)
        // لا يُفقد أي محتوى: الأجزاء مجتمعةً = الأصل
        assertEquals(a.text, parts.joinToString("") { it.text })
        // كل جزء ضمن السقف الصارم (ضعف الحد)
        parts.forEach { assertTrue(it.length <= MAX_FIELD_CHARS * 2) }
    }

    @Test fun singleHugeParagraph_stillBounded() {
        // فقرة واحدة بلا أسطر جديدة أطول من الحد
        val a = AnnotatedString("ا".repeat(MAX_FIELD_CHARS * 3))
        val parts = splitLargeText(a)
        assertTrue(parts.size >= 2)
        assertEquals(a.text, parts.joinToString("") { it.text })
        parts.forEach { assertTrue(it.length <= MAX_FIELD_CHARS * 2) }
    }
}
