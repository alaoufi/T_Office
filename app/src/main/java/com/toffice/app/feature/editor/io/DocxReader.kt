package com.toffice.app.feature.editor.io

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import com.toffice.app.feature.editor.model.CharAttrs
import com.toffice.app.feature.editor.model.COLOR_DEFAULT
import com.toffice.app.feature.editor.model.DEFAULT_FONT_SP
import com.toffice.app.feature.editor.model.DocBundle
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.TableCell
import com.toffice.app.feature.editor.model.TableData
import com.toffice.app.feature.editor.model.buildAnnotated
import com.toffice.app.feature.editor.model.twipsToPt
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

/** يقرأ مستند DOCX (OOXML): المتن + التنسيق + الهوامش + الترويسة + التذييل. */
object DocxReader {

    fun read(input: InputStream): DocBundle = read(input.readBytes())

    fun read(bytes: ByteArray): DocBundle {
        val parts = readAllParts(bytes)
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
            tables = runCatching { parseTables(docXml) }.getOrDefault(emptyList()),
        )
    }

    /**
     * قراءة أجزاء ZIP عبر الفهرس المركزي (Central Directory) بدل ZipInputStream —
     * يتحمّل ملفات docx المكتوبة بنمط التدفّق (data descriptor / CRC صفري) التي
     * ترفضها ZipInputStream بخطأ "invalid entry CRC". يقرأ ملفات .xml فقط.
     */
    private fun readAllParts(data: ByteArray): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        val eocd = findEocd(data)
        if (eocd < 0) return readAllPartsStreaming(data)
        val cdOffset = i32le(data, eocd + 16)
        val cdCount = u16le(data, eocd + 10)
        var p = cdOffset
        var i = 0
        while (i < cdCount && p + 46 <= data.size && i32le(data, p) == 0x02014b50) {
            val method = u16le(data, p + 10)
            val compSize = i32le(data, p + 20)
            val fnLen = u16le(data, p + 28)
            val extraLen = u16le(data, p + 30)
            val commentLen = u16le(data, p + 32)
            val localOff = i32le(data, p + 42)
            val name = if (p + 46 + fnLen <= data.size) String(data, p + 46, fnLen, Charsets.UTF_8) else ""
            if (name.endsWith(".xml")) {
                runCatching { inflateEntry(data, localOff, method, compSize) }.getOrNull()?.let { map[name] = it }
            }
            p += 46 + fnLen + extraLen + commentLen
            i++
        }
        return if (map.isEmpty()) readAllPartsStreaming(data) else map
    }

    private fun findEocd(data: ByteArray): Int {
        var i = data.size - 22
        val min = maxOf(0, data.size - 22 - 65536)
        while (i >= min) {
            if (i + 4 <= data.size && i32le(data, i) == 0x06054b50) return i
            i--
        }
        return -1
    }

    private fun inflateEntry(data: ByteArray, localOff: Int, method: Int, compSize: Int): String? {
        if (localOff < 0 || localOff + 30 > data.size || i32le(data, localOff) != 0x04034b50) return null
        val fnLen = u16le(data, localOff + 26)
        val extraLen = u16le(data, localOff + 28)
        val start = localOff + 30 + fnLen + extraLen
        if (compSize < 0 || start + compSize > data.size) return null
        if (method == 0) return String(data, start, compSize, Charsets.UTF_8)
        val inf = java.util.zip.Inflater(true)
        inf.setInput(data, start, compSize)
        val out = java.io.ByteArrayOutputStream(compSize * 3)
        val buf = ByteArray(16384)
        try {
            while (!inf.finished()) {
                val n = inf.inflate(buf)
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
                out.write(buf, 0, n)
            }
        } finally {
            inf.end()
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    /** مسار احتياطي: ZipInputStream يتجاهل أخطاء CRC لكل مدخل. */
    private fun readAllPartsStreaming(data: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        runCatching {
            ZipInputStream(java.io.ByteArrayInputStream(data)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.endsWith(".xml")) {
                        runCatching { map[name] = zip.readBytes().toString(Charsets.UTF_8) }
                    }
                    entry = runCatching { zip.nextEntry }.getOrNull()
                }
            }
        }
        return map
    }

    private fun u16le(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun i32le(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)

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
        var tableDepth = 0 // محتوى الجداول يُحلَّل منفصلاً ولا يُدرج في المتن

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
                XmlPullParser.START_TAG -> if (local(parser.name) == "tbl") {
                    tableDepth++
                } else if (tableDepth == 0) when (local(parser.name)) {
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
                XmlPullParser.TEXT -> if (tableDepth == 0 && inT) appendText(parser.text ?: "")
                XmlPullParser.END_TAG -> if (local(parser.name) == "tbl") {
                    if (tableDepth > 0) tableDepth--
                } else if (tableDepth == 0) when (local(parser.name)) {
                    "t" -> inT = false
                    "p" -> { aligns.add(curAlign); directions.add(curDir) }
                }
            }
            event = parser.next()
        }

        return buildAnnotated(text.toString(), attrs, aligns, directions) to page
    }

    /** يحلّل جداول المستند (w:tbl) إلى نموذجنا، مع gridSpan والمحاذاة وتنسيق بسيط للخلية. */
    private fun parseTables(xml: String): List<TableData> {
        val parser = newParser(xml)
        val tables = mutableListOf<TableData>()

        var rows = mutableListOf<List<TableCell>>()
        var row = mutableListOf<TableCell>()
        var cellText = StringBuilder()
        var cellAlign = 0
        var cellSpan = 1
        var cellBold = false
        var cellColor = 0L
        var cellSize = 0
        var inT = false
        var depth = 0 // عمق w:tbl (لتفادي الجداول المتداخلة بشكل آمن)

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val name = local(parser.name)
            when (event) {
                XmlPullParser.START_TAG -> when (name) {
                    "tbl" -> { depth++; if (depth == 1) rows = mutableListOf() }
                    "tr" -> if (depth == 1) row = mutableListOf()
                    "tc" -> if (depth == 1) {
                        cellText = StringBuilder(); cellAlign = 0; cellSpan = 1
                        cellBold = false; cellColor = 0L; cellSize = 0
                    }
                    "gridSpan" -> if (depth == 1) cellSpan = (attr(parser, "val")?.toIntOrNull() ?: 1).coerceIn(1, 50)
                    "jc" -> if (depth == 1) cellAlign = mapCellAlign(attr(parser, "val"))
                    "b" -> if (depth == 1) cellBold = boolOn(attr(parser, "val"))
                    "color" -> if (depth == 1) {
                        val v = attr(parser, "val")
                        if (v != null && v != "auto") parseHex(v)?.let { cellColor = it.toLong() and 0xFFFFFF }
                    }
                    "sz" -> if (depth == 1) attr(parser, "val")?.toIntOrNull()?.let { cellSize = (it / 2).coerceIn(8, 72) }
                    "t" -> if (depth == 1) inT = true
                    "tab" -> if (depth == 1 && inT) cellText.append("    ")
                }
                XmlPullParser.TEXT -> if (depth == 1 && inT) cellText.append(parser.text ?: "")
                XmlPullParser.END_TAG -> when (name) {
                    "t" -> if (depth == 1) inT = false
                    "tc" -> if (depth == 1) {
                        row.add(TableCell(cellText.toString().trim(), cellAlign, cellSpan, cellBold, cellColor, cellSize))
                        // خلايا بديلة لتغطية الأعمدة المدموجة (ليتوافق مع نموذجنا)
                        repeat(cellSpan - 1) { row.add(TableCell()) }
                    }
                    "tr" -> if (depth == 1 && row.isNotEmpty()) rows.add(row.toList())
                    "tbl" -> {
                        if (depth == 1 && rows.isNotEmpty()) {
                            // توحيد عدد الأعمدة لأكبر صف (لتفادي صفوف ناقصة)
                            val cols = rows.maxOf { it.size }
                            val norm = rows.map { r -> if (r.size < cols) r + List(cols - r.size) { TableCell() } else r }
                            tables.add(TableData(norm))
                        }
                        if (depth > 0) depth--
                    }
                }
            }
            event = parser.next()
        }
        return tables
    }

    private fun mapCellAlign(v: String?): Int = when (v) {
        "center" -> 1
        "left" -> 2
        "right", "end" -> 3
        else -> 0
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
