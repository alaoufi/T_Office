package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocSerializerTest {

    @Test fun roundTrip_preservesBodyAndTables() {
        val bundle = DocBundle(
            body = AnnotatedString("مرحبا"),
            tables = listOf(TableOps.setCell(TableOps.newTable(2, 2), 0, 0, "خلية")),
            afterBody = AnnotatedString("تذييل"),
        )
        val parsed = DocSerializer.parse(DocSerializer.serialize(bundle))
        assertEquals("مرحبا", parsed.body.text)
        assertEquals("خلية", parsed.tables[0].rows[0][0].text)
        assertEquals("تذييل", parsed.afterBody.text)
    }

    @Test fun parse_corruptJson_doesNotCrash() {
        // لا يجوز أن يرمي استثناءً مهما كان المدخل
        val corrupt = "{ this is : not json ]]"
        val bundle = DocSerializer.parse(corrupt)
        assertTrue(bundle.body.text.isNotEmpty())
    }

    @Test fun parse_emptyJson_returnsEmpty() {
        assertEquals("", DocSerializer.parse("").body.text)
    }

    @Test fun parse_plainOldFormat_works() {
        // صيغة قديمة: نص خام بلا غلاف
        val bundle = DocSerializer.parse(DocSerializer.serialize(DocBundle(AnnotatedString("قديم"))))
        assertEquals("قديم", bundle.body.text)
    }
}
