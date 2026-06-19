package com.toffice.app.feature.editor.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TableOpsTest {

    @Test fun newTable_dimensions() {
        val t = TableOps.newTable(3, 4)
        assertEquals(3, t.rowCount)
        assertEquals(4, t.colCount)
    }

    @Test fun setCell_updatesOnlyTarget() {
        val t = TableOps.setCell(TableOps.newTable(2, 2), 0, 1, "س")
        assertEquals("س", t.rows[0][1].text)
        assertEquals("", t.rows[0][0].text)
    }

    @Test fun setCellAlign_updatesAlign() {
        val t = TableOps.setCellAlign(TableOps.newTable(2, 2), 1, 0, 1)
        assertEquals(1, t.rows[1][0].align)
        assertEquals(0, t.rows[0][0].align)
    }

    @Test fun addAndDeleteRowColumn() {
        var t = TableOps.newTable(2, 2)
        t = TableOps.addRow(t); assertEquals(3, t.rowCount)
        t = TableOps.addColumn(t); assertEquals(3, t.colCount)
        t = TableOps.deleteRow(t, 0); assertEquals(2, t.rowCount)
        t = TableOps.deleteColumn(t, 0); assertEquals(2, t.colCount)
    }

    @Test fun mergeAndSplitCell() {
        var t = TableOps.newTable(2, 3)
        t = TableOps.mergeRight(t, 0, 0)
        assertEquals(2, t.rows[0][0].span)
        t = TableOps.mergeRight(t, 0, 0)
        assertEquals(3, t.rows[0][0].span)
        // لا تتجاوز حدود الجدول
        t = TableOps.mergeRight(t, 0, 0)
        assertEquals(3, t.rows[0][0].span)
        t = TableOps.splitCell(t, 0, 0)
        assertEquals(1, t.rows[0][0].span)
    }

    @Test fun colWeight_changes() {
        var t = TableOps.newTable(1, 2)
        t = TableOps.widenColumn(t, 0, 0.5f)
        assertEquals(1.5f, t.weightOf(0))
    }

    @Test fun cannotDeleteLastRowOrColumn() {
        val t = TableOps.newTable(1, 1)
        assertEquals(1, TableOps.deleteRow(t, 0).rowCount)
        assertEquals(1, TableOps.deleteColumn(t, 0).colCount)
    }
}
