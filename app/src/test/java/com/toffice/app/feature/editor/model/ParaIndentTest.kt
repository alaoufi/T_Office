package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class ParaIndentTest {

    @Test fun indent_serialization_roundTrip_firstAndLeft() {
        val a = buildAnnotated(
            text = "فقرة",
            attrs = List(4) { CharAttrs() },
            aligns = listOf(androidx.compose.ui.text.style.TextAlign.Start),
            indents = listOf(ParaIndent(firstPt = 36f, leftPt = 18f)),
        )
        val parsed = jsonToAnnotated(annotatedToJson(a))
        val ind = parsed.toIndents().first()
        assertEquals(36f, ind.firstPt)
        assertEquals(18f, ind.leftPt)
    }

    @Test fun legacy_ind_levels_mapToPoints() {
        // الصيغة القديمة: "ind" = مستوى صحيح ×٣٦ نقطة على السطر الأول والبقية
        val legacy = """{"text":"س","ind":[2]}"""
        val ind = jsonToAnnotated(legacy).toIndents().first()
        assertEquals(72f, ind.firstPt)
        assertEquals(72f, ind.leftPt)
    }

    @Test fun setParagraphIndent_appliesToCursorParagraph() {
        val v = TextFieldValue(buildAnnotated("أ\nبب", List(4) { CharAttrs() }, listOf()))
        // المؤشر داخل الفقرة الثانية (الحرف الثاني من "بب")
        val v2 = v.copy(selection = androidx.compose.ui.text.TextRange(3))
        val out = RichTextOps.setParagraphIndent(v2, firstPt = 40f, leftPt = 10f)
        val indents = out.annotatedString.toIndents()
        assertEquals(ParaIndent(0f, 0f), indents[0])
        assertEquals(40f, indents[1].firstPt)
        assertEquals(10f, indents[1].leftPt)
    }
}
