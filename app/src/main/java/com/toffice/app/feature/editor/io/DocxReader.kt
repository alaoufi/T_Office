package com.toffice.app.feature.editor.io

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.toffice.app.feature.editor.model.CharAttrs
import com.toffice.app.feature.editor.model.COLOR_DEFAULT
import com.toffice.app.feature.editor.model.DEFAULT_FONT_SP
import com.toffice.app.feature.editor.model.buildAnnotated
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

/** يقرأ مستند DOCX (OOXML) ويحوّله إلى نص منسّق. يدعم: غامق/مائل/تسطير/حجم/لون/محاذاة. */
object DocxReader {

    fun read(input: InputStream): AnnotatedString {
        val xml = extractDocumentXml(input) ?: return AnnotatedString("")
        return parse(xml)
    }

    private fun extractDocumentXml(input: InputStream): String? {
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun parse(xml: String): AnnotatedString {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        val text = StringBuilder()
        val attrs = mutableListOf<CharAttrs>()
        val aligns = mutableListOf<TextAlign>()

        var firstParagraph = true
        var curAlign = TextAlign.Right
        var inT = false

        // خصائص المقطع (run) الحالي
        var rb = false
        var ri = false
        var ru = false
        var rsz = DEFAULT_FONT_SP
        var rc = COLOR_DEFAULT

        fun appendText(s: String) {
            for (c in s) {
                text.append(c)
                attrs.add(CharAttrs(rb, ri, ru, rsz, rc))
            }
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (local(parser.name)) {
                    "p" -> {
                        if (!firstParagraph) {
                            text.append('\n')
                            attrs.add(CharAttrs())
                        }
                        firstParagraph = false
                        curAlign = TextAlign.Right
                    }
                    "jc" -> curAlign = mapAlign(attr(parser, "val"))
                    "r" -> { rb = false; ri = false; ru = false; rsz = DEFAULT_FONT_SP; rc = COLOR_DEFAULT }
                    "b" -> rb = boolOn(attr(parser, "val"))
                    "i" -> ri = boolOn(attr(parser, "val"))
                    "u" -> ru = (attr(parser, "val") ?: "single") != "none"
                    "sz" -> attr(parser, "val")?.toIntOrNull()?.let { rsz = (it / 2).coerceIn(8, 96) }
                    "color" -> {
                        val v = attr(parser, "val")
                        if (v != null && v != "auto") parseHex(v)?.let { rc = it }
                    }
                    "t" -> inT = true
                    "tab" -> appendText("    ")
                    "br" -> { text.append('\n'); attrs.add(CharAttrs()) }
                }
                XmlPullParser.TEXT -> if (inT) appendText(parser.text ?: "")
                XmlPullParser.END_TAG -> when (local(parser.name)) {
                    "t" -> inT = false
                    "p" -> aligns.add(curAlign)
                }
            }
            event = parser.next()
        }

        return buildAnnotated(text.toString(), attrs, aligns)
    }

    private fun local(name: String?): String = name?.substringAfter(':') ?: ""

    private fun attr(p: XmlPullParser, localName: String): String? {
        for (i in 0 until p.attributeCount) {
            val n = p.getAttributeName(i)
            if (n == localName || n.endsWith(":$localName")) return p.getAttributeValue(i)
        }
        return null
    }

    private fun boolOn(v: String?): Boolean = v == null || (v != "0" && v != "false")

    private fun mapAlign(v: String?): TextAlign = when (v) {
        "center" -> TextAlign.Center
        "left", "start" -> TextAlign.Left
        "both", "distribute" -> TextAlign.Justify
        else -> TextAlign.Right
    }

    private fun parseHex(v: String): Int? = try {
        (0xFF000000.toInt()) or (v.removePrefix("#").toInt(16) and 0xFFFFFF)
    } catch (e: Exception) {
        null
    }
}
