package com.toffice.app.feature.editor.io

import com.toffice.app.feature.editor.model.ParaOut
import com.toffice.app.feature.editor.model.RunOut
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** يكتب مستند DOCX قياسياً (OOXML) من فقرات منسّقة، مع دعم العربية RTL. */
object DocxWriter {

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    private const val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    fun write(out: OutputStream, paragraphs: List<ParaOut>) {
        ZipOutputStream(out).use { zip ->
            zip.entry("[Content_Types].xml", CONTENT_TYPES)
            zip.entry("_rels/.rels", RELS)
            zip.entry("word/document.xml", buildDocument(paragraphs))
        }
    }

    private fun ZipOutputStream.entry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun buildDocument(paragraphs: List<ParaOut>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")
        val paras = if (paragraphs.isEmpty()) listOf(ParaOut(0, emptyList())) else paragraphs
        for (p in paras) sb.append(paragraph(p))
        // إعدادات الصفحة A4 مع اتجاه RTL
        sb.append("""<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:bidi/></w:sectPr>""")
        sb.append("""</w:body></w:document>""")
        return sb.toString()
    }

    private fun paragraph(p: ParaOut): String {
        val jc = when (p.alignCode) {
            1 -> "center"
            2 -> "left"
            3 -> "both"
            else -> "right"
        }
        val sb = StringBuilder()
        sb.append("<w:p><w:pPr><w:bidi/><w:jc w:val=\"").append(jc).append("\"/></w:pPr>")
        for (r in p.runs) sb.append(run(r))
        sb.append("</w:p>")
        return sb.toString()
    }

    private fun run(r: RunOut): String {
        val sb = StringBuilder()
        sb.append("<w:r><w:rPr>")
        if (r.bold) sb.append("<w:b/><w:bCs/>")
        if (r.italic) sb.append("<w:i/><w:iCs/>")
        if (r.underline) sb.append("<w:u w:val=\"single\"/>")
        val halfPoints = (r.sizeSp * 2).coerceAtLeast(2)
        sb.append("<w:sz w:val=\"").append(halfPoints).append("\"/>")
        sb.append("<w:szCs w:val=\"").append(halfPoints).append("\"/>")
        if (r.colorArgb != 0) {
            val hex = String.format("%06X", r.colorArgb and 0xFFFFFF)
            sb.append("<w:color w:val=\"").append(hex).append("\"/>")
        }
        sb.append("<w:rtl/>")
        sb.append("</w:rPr>")
        sb.append("<w:t xml:space=\"preserve\">").append(escape(r.text)).append("</w:t>")
        sb.append("</w:r>")
        return sb.toString()
    }

    private fun escape(s: String): String = buildString {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            '\t' -> append("    ")
            else -> append(c)
        }
    }
}
