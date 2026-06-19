package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocBlockTest {

    @Test fun blocks_roundTrip_preservesOrderAndContent() {
        val blocks = listOf(
            TextBlock(AnnotatedString("قبل")),
            TableBlock(TableOps.setCell(TableOps.newTable(2, 2), 0, 0, "خلية")),
            TextBlock(AnnotatedString("بعد")),
            ImageBlock(DocImage("/x/doc_images/a.jpg", 300f, 200f)),
        )
        val parsed = DocSerializer.parse(DocSerializer.serialize(DocBundle(AnnotatedString(""), blocks = blocks))).blocks

        assertEquals(4, parsed.size)
        assertEquals("قبل", (parsed[0] as TextBlock).content.text)
        assertEquals("خلية", (parsed[1] as TableBlock).table.rows[0][0].text)
        assertEquals("بعد", (parsed[2] as TextBlock).content.text)
        assertEquals("/x/doc_images/a.jpg", (parsed[3] as ImageBlock).image.path)
        assertEquals(300f, (parsed[3] as ImageBlock).image.widthPt)
    }

    @Test fun effectiveBlocks_buildsFromLegacy_whenNoBlocks() {
        val bundle = DocBundle(
            body = AnnotatedString("متن"),
            tables = listOf(TableOps.newTable(1, 1)),
            afterBody = AnnotatedString("ذيل"),
        )
        val eff = bundle.effectiveBlocks()
        assertEquals(3, eff.size)
        assertTrue(eff[0] is TextBlock)
        assertTrue(eff[1] is TableBlock)
        assertEquals("ذيل", (eff[2] as TextBlock).content.text)
    }

    @Test fun effectiveBlocks_usesStoredBlocks_whenPresent() {
        val bundle = DocBundle(AnnotatedString("متن"), blocks = listOf(TextBlock(AnnotatedString("كتلة"))))
        val eff = bundle.effectiveBlocks()
        assertEquals(1, eff.size)
        assertEquals("كتلة", (eff[0] as TextBlock).content.text)
    }
}
