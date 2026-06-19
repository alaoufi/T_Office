package com.toffice.app.feature.editor.io

import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.TableOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** يكتب جدولاً إلى DOCX ثم يقرؤه ليتأكّد من اكتمال دورة استيراد/تصدير الجداول. */
class DocxTableRoundTripTest {

    private fun writeRead(tables: List<com.toffice.app.feature.editor.model.TableData>): com.toffice.app.feature.editor.model.DocBundle {
        val out = ByteArrayOutputStream()
        DocxWriter.write(out, emptyList(), PageSettings(), emptyList(), emptyList(), tables, emptyList())
        return DocxReader.read(ByteArrayInputStream(out.toByteArray()))
    }

    @Test fun table_textAndAlignment_roundTrip() {
        var t = TableOps.newTable(2, 3)
        t = TableOps.setCell(t, 0, 0, "اسم")
        t = TableOps.setCellAlign(t, 0, 0, 1) // توسيط
        t = TableOps.setCell(t, 1, 2, "قيمة")
        val bundle = writeRead(listOf(t))

        assertEquals(1, bundle.tables.size)
        val r = bundle.tables[0]
        assertEquals(2, r.rowCount)
        assertEquals(3, r.colCount)
        assertEquals("اسم", r.rows[0][0].text)
        assertEquals(1, r.rows[0][0].align)
        assertEquals("قيمة", r.rows[1][2].text)
    }

    @Test fun mergedCell_roundTrip_keepsColumnCount() {
        var t = TableOps.newTable(2, 3)
        t = TableOps.setCell(t, 0, 0, "عنوان ممتد")
        t = TableOps.mergeRight(t, 0, 0) // امتداد ×2
        val bundle = writeRead(listOf(t))

        val r = bundle.tables[0]
        assertEquals(3, r.colCount) // العدد الكلي محفوظ بخلايا بديلة
        assertEquals(2, r.rows[0][0].span)
        assertEquals("عنوان ممتد", r.rows[0][0].text)
    }

    @Test fun bodyText_excludesTableContent() {
        var t = TableOps.newTable(1, 1)
        t = TableOps.setCell(t, 0, 0, "داخل الجدول")
        val bundle = writeRead(listOf(t))
        // نص الجدول يجب ألّا يتسرّب إلى المتن
        assertTrue(!bundle.body.text.contains("داخل الجدول"))
    }
}
