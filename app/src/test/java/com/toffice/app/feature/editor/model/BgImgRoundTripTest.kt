package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BgImgRoundTripTest {
    @Test fun bgAndImagesSurviveSerialize() {
        val bundle = DocBundle(
            body = AnnotatedString("نص"),
            page = PageSettings(bgColorArgb = 0xFFFFFFCC.toInt()),
            images = listOf(DocImage("/data/x.jpg", 200f, 150f)),
            tables = listOf(TableOps.newTable(2,2)),
        )
        val parsed = DocSerializer.parse(DocSerializer.serialize(bundle))
        assertEquals(0xFFFFFFCC.toInt(), parsed.page.bgColorArgb)
        assertEquals(1, parsed.images.size)
        assertEquals("/data/x.jpg", parsed.images[0].path)
        val eff = parsed.effectiveBlocks()
        println("=== BLOCKS: " + eff.map { it::class.simpleName } + " bg=" + Integer.toHexString(parsed.page.bgColorArgb) + " ===")
        assertTrue("يجب أن تتضمن الكتل صورة", eff.any { it is ImageBlock })
    }
}
