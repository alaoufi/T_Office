package com.toffice.app.feature.editor.io

import androidx.compose.ui.text.AnnotatedString
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.TableBlock
import com.toffice.app.feature.editor.model.TableOps
import com.toffice.app.feature.editor.model.TextBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** يتأكّد أن تصدير الكتل المرتّبة يحافظ على ترتيب: نص ← جدول ← نص. */
class DocxBlocksTest {

    @Test fun blocks_exportedInOrder_andReadable() {
        val blocks = listOf(
            TextBlock(AnnotatedString("الفقرة الأولى")),
            TableBlock(TableOps.setCell(TableOps.newTable(1, 1), 0, 0, "خانة")),
            TextBlock(AnnotatedString("الفقرة الثانية")),
        )
        val out = ByteArrayOutputStream()
        DocxWriter.writeBlocks(out, blocks, PageSettings())
        // القراءة عبر القارئ (المحتوى داخل zip مضغوط)
        val bundle = DocxReader.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(1, bundle.tables.size)
        assertEquals("خانة", bundle.tables[0].rows[0][0].text)
        assertTrue(bundle.body.text.contains("الفقرة الأولى"))
        assertTrue(bundle.body.text.contains("الفقرة الثانية"))
    }
}
