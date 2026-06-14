package com.toffice.app.feature.editor.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PaginationTest {

    @Test fun lessThanOnePage_isOne() {
        assertEquals(1, paginationPageCount(800f, 842f))
    }

    @Test fun exactlyOnePage_isOne() {
        assertEquals(1, paginationPageCount(842f, 842f))
    }

    @Test fun justOverOnePage_isTwo() {
        assertEquals(2, paginationPageCount(900f, 842f))
    }

    @Test fun twoFullPages_isTwo() {
        assertEquals(2, paginationPageCount(1684f, 842f))
    }

    @Test fun justOverTwoPages_isThree() {
        assertEquals(3, paginationPageCount(1700f, 842f))
    }

    @Test fun zeroContent_isOne() {
        assertEquals(1, paginationPageCount(0f, 842f))
    }

    @Test fun zeroPageHeight_isOne() {
        assertEquals(1, paginationPageCount(1000f, 0f))
    }

    @Test fun breakOffsets_internalOnly() {
        assertEquals(listOf(842f, 1684f), pageBreakOffsets(3, 842f))
        assertEquals(emptyList<Float>(), pageBreakOffsets(1, 842f))
    }
}
