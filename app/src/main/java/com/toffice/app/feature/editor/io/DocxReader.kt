package com.toffice.app.feature.editor.io

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import com.toffice.app.feature.editor.model.CharAttrs
import com.toffice.app.feature.editor.model.COLOR_DEFAULT
import com.toffice.app.feature.editor.model.DEFAULT_FONT_SP
import com.toffice.app.feature.editor.model.DocBundle
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.buildAnnotated
import com.toffice.app.feature.editor.model.twipsToPt
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

/** يقرأ مستند DOCX (OOXML): المتن + التنسيق + الهوامش + الترويسة + التذييل. */
object DocxReader {

    fun read(input: InputStream): DocBundle {
        val parts = readAllParts(input)
        val docXml = parts["word/document.xml"] ?: return DocBundle(AnnotatedString(""))

        val parsed = parseDocument(docXml)

        val headerXml = parts.entries.firstOrNull { it.key.matches(Regex("word/header\\d*\\.xml")) }?.value
        val footerXml = parts.entries.firstOrNull { it.key.matches(Regex("word/footer\\d*\\.xml")) }?.value

        val showPageNumber = footerXml?.contains("PAGE") == true
        var footerText = footerXml?.let { plainText(it) } ?: ""
        if (showPageNumber) {
            // إزالة رقم الصفحة المخزّن مؤقتاً من نص التذييل
            footerText = footerText.lines().filterNot { it.trim().matches(Regex("\\d+")) }.joinToString("\n").trim()
        }

        // اتجاه الصفحة من قسم sectPr (bidi)
        val sectPr = Regex("<w:sectPr[\\s\\S]*?</w:sectPr>").find(docXml)?.value
        val rtlPage = if (sectPr != null) sectPr.contains("<w:bidi") else true

        return DocBundle(
            body = parsed.first,
            page = parsed.second.copy(showPageNumber = showPageNumber, rtlPage = rtlPage),
            header = AnnotatedString(headerXml?.let { plainText(it) } ?: ""),
            footer = AnnotatedString(footerText),
        )
    }

    private fun readAllParts(input: InputStream): Map<String, String> {
        val map = mutableMapOf<String, String>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.endsWith(".xml")) {
                    map[name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                entry = zip.nextEntry
            }
        }
        return map
    }

    private fun parseDocument(xml: String): Pair<AnnotatedString, PageSettings> {
        val parser = newParser(xml)

        val text = StringBuilder()
        val attrs = mutableListOf<CharAttrs>()
        val aligns = mutableListOf<TextAlign>()
        val directions = mutableListOf<TextDirection>()
        var page = PageSettings()

        var firstParagraph = true
        var curAlign = TextAlign.Start
        var curDir = TextDirection.Content
        var inT = false

        var rb = false; var ri = false; var ru = false; var rst = false
        var rsz = DEFAULT_FONT_SP; var rc = COLOR_DEFAULT; var rhl = COLOR_DEFAULT

        fun appendText(s: String) {
            for (c in s) {
                text.append(c)
                attrs.add(CharAttrs(rb, ri, ru, rst, rsz, rc, rhl))
            }
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (local(parser.name)) {
                    "p" -> {
                        if (!firstParagraph) { text.append('\n'); attrs.add(CharAttrs()) }
                        firstParagraph = false
                        curAlign = TextAlign.Start
                        curDir = TextDirection.Content
                    }
                    "bidi" -> curDir = TextDirection.Rtl
                    "jc" -> curAlign = mapAlign(attr(parser, "val"))
                    "r" -> { rb = false; ri = false; ru = false; rst = false; rsz = DEFAULT_FONT_SP; rc = COLOR_DEFAULT; rhl = COLOR_DEFAULT }
                    "b" -> rb = boolOn(attr(parser, "val"))
                    "i" -> ri = boolOn(attr(parser, "val"))
                    "u" -> ru = (attr(parser, "val") ?: "single") != "none"
                    "strike" -> rst = boolOn(attr(parser, "val"))
                    "sz" -> attr(parser, "val")?.toIntOrNull()?.let { rsz = (it / 2).coerceIn(8, 96) }
                    "color" -> {
                        val v = attr(parser, "val")
                        if (v != null && v != "auto") parseHex(v)?.let { rc = it }
                    }
                    "shd" -> {
                        val v = attr(parser, "fill")
                        if (v != null && v != "auto") parseHex(v)?.let { rhl = it }
                    }
                    "highlight" -> namedColor(attr(parser, "val"))?.let { rhl = it }
                    "pgSz" -> {
                        attr(parser, "w")?.toIntOrNull()?.let { page = page.copy(pageWidthPt = it.twipsToPt()) }
                        attr(parser, "h")?.toIntOrNull()?.let { page = page.copy(pageHeightPt = it.twipsToPt()) }
                    }
                    "pgMar" -> {
                        attr(parser, "top")?.toIntOrNull()?.let { page = page.copy(marginTopPt = it.twipsToPt()) }
                        attr(parser, "right")?.toIntOrNull()?.let { page = page.copy(marginRightPt = it.twipsToPt()) }
                        attr(parser, "bottom")?.toIntOrNull()?.let { page = page.copy(marginBottomPt = it.twipsToPt()) }
                        attr(parser, "left")?.toIntOrNull()?.let { page = page.copy(marginLeftPt = it.twipsToPt()) }
                    }
                    "t" -> inT = true
                    "tab" -> appendText("    ")
                    "br" -> { text.append('\n'); attrs.add(CharAttrs()) }
                }
                XmlPullParser.TEXT -> if (inT) appendText(parser.text ?: "")
                XmlPullParser.END_TAG -> when (local(parser.name)) {
                    "t" -> inT = false
                    "p" -> { aligns.add(curAlign); directions.add(curDir) }
                }
            }
            event = parser.next()
        }

        return buildAnnotated(text.toString(), attrs, aligns, directions) to page
    }

    /** استخراج نص بسيط من ترويسة/تذييل (الفقرات مفصولة بسطر جديد). */
    private fun plainText(xml: String): String {
        val parser = newParser(xml)
        val sb = StringBuilder()
        var inT = false
        var firstP = true
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (local(parser.name)) {
                    "p" -> { if (!firstP) sb.append('\n'); firstP = false }
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inT) sb.append(parser.text ?: "")
                XmlPullParser.END_TAG -> if (local(parser.name) == "t") inT = false
            }
            event = parser.next()
        }
        return sb.toString().trim()
    }

    private fun newParser(xml: String): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        return factory.newPullParser().apply { setInput(StringReader(xml)) }
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

    /** ألوان التظليل المسماة في Word -> ARGB. */
    private fun namedColor(v: String?): Int? = when (v) {
        "yellow" -> 0xFFFFFF00.toInt()
        "green" -> 0xFF00FF00.toInt()
        "cyan" -> 0xFF00FFFF.toInt()
        "magenta" -> 0xFFFF00FF.toInt()
        "red" -> 0xFFFF0000.toInt()
        "blue" -> 0xFF0000FF.toInt()
        "darkGray", "lightGray" -> 0xFFC0C0C0.toInt()
        else -> null
    }
}
