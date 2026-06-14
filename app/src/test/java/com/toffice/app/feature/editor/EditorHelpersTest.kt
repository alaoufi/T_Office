package com.toffice.app.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorHelpersTest {

    @Test fun arabicDigits_converts() {
        assertEquals("١٢٣", arabicDigits(123))
        assertEquals("٥", arabicDigits(5))
        assertEquals("١٠", arabicDigits(10))
    }

    @Test fun wordCount_counts() {
        assertEquals(0, wordCount(""))
        assertEquals(3, wordCount("بسم الله الرحمن".trim()))
        assertEquals(2, wordCount("  كلمة   أخرى  "))
    }
}
