package com.toffice.app.feature.editor.model

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/** يثبت أن الحفظ ثم الفتح يحافظان على كل التنسيق حرفياً (مطابقة تامة للمستندات الداخلية). */
class DocFidelityTest {

    @Test fun fullFormatting_survivesSaveThenOpen() {
        val text = "عنوان\nفقرة ملوّنة\nسطر ثالث"
        // خصائص حرفية متنوعة على أجزاء مختلفة
        val attrs = MutableList(text.length) { CharAttrs() }
        // "عنوان" غامق + حجم 22
        for (i in 0..4) attrs[i] = CharAttrs(bold = true, sizeSp = 22)
        // "فقرة ملوّنة" مائل + تسطير + لون + تظليل + خط
        for (i in 6..16) attrs[i] = CharAttrs(
            italic = true, underline = true, strike = true,
            colorArgb = 0xFFCC0000.toInt(), highlightArgb = 0xFFFFFF00.toInt(),
            fontFamily = 2, script = 1,
        )

        val aligns = listOf(TextAlign.Center, TextAlign.Right, TextAlign.Justify)
        val dirs = listOf(TextDirection.Rtl, TextDirection.Ltr, TextDirection.Content)
        val ls = listOf(1.0f, 1.5f, 2.0f)
        val indents = listOf(ParaIndent(0f, 0f), ParaIndent(36f, 18f), ParaIndent(0f, 72f))

        val original = buildAnnotated(text, attrs, aligns, dirs, ls, indents)

        // حفظ ← فتح
        val reopened = jsonToAnnotated(annotatedToJson(original))

        assertEquals(text, reopened.text)
        // كل خاصية حرفية
        assertEquals(original.toCharAttrs(), reopened.toCharAttrs())
        // كل خصائص الفقرات
        assertEquals(original.toAligns(), reopened.toAligns())
        assertEquals(original.toDirections(), reopened.toDirections())
        assertEquals(original.toLineSpacings(), reopened.toLineSpacings())
        assertEquals(original.toIndents(), reopened.toIndents())
    }

    @Test fun doubleRoundTrip_isStable() {
        val text = "أ\nب"
        val attrs = MutableList(text.length) { CharAttrs(bold = true, colorArgb = 0xFF00AA00.toInt()) }
        val doc = buildAnnotated(text, attrs, listOf(TextAlign.End, TextAlign.Center),
            listOf(TextDirection.Rtl, TextDirection.Ltr), listOf(1.5f, 1f),
            listOf(ParaIndent(24f, 12f), ParaIndent()))
        val once = annotatedToJson(doc)
        val twice = annotatedToJson(jsonToAnnotated(once))
        // نفس JSON بعد دورتين => لا انحراف تدريجي في التنسيق
        assertEquals(once, twice)
    }
}
