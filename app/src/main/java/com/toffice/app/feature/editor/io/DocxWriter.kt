package com.toffice.app.feature.editor.io

import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.ParaOut
import com.toffice.app.feature.editor.model.RunOut
import com.toffice.app.feature.editor.model.ptToTwips
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** يكتب مستند DOCX قياسياً (OOXML) من فقرات منسّقة، مع دعم العربية RTL والهوامش والترويسة/التذييل. */
object DocxWriter {

    fun write(
        out: OutputStream,
        paragraphs: List<ParaOut>,
        page: PageSettings = PageSettings(),
        header: String = "",
        footer: String = "",
    ) {
        val hasHeader = header.isNotBlank()
        val hasFooter = footer.isNotBlank() || page.showPageNumber

        ZipOutputStream(out).use { zip ->
            zip.entry("[Content_Types].xml", contentTypes(hasHeader, hasFooter))
            zip.entry("_rels/.rels", RELS)
            zip.entry("word/_rels/document.xml.rels", documentRels(hasHeader, hasFooter))
            zip.entry("word/document.xml", buildDocument(paragraphs, page, hasHeader, hasFooter))
            if (hasHeader) zip.entry("word/header1.xml", headerFooterPart("hdr", header, false))
            if (hasFooter) zip.entry("word/footer1.xml", headerFooterPart("ftr", footer, page.showPageNumber))
        }
    }

    private fun ZipOutputStream.entry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private const val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private fun contentTypes(hasHeader: Boolean, hasFooter: Boolean): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        sb.append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        sb.append("""<Default Extension="xml" ContentType="application/xml"/>""")
        sb.append("""<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>""")
        if (hasHeader) sb.append("""<Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>""")
        if (hasFooter) sb.append("""<Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>""")
        sb.append("""</Types>""")
        return sb.toString()
    }

    private fun documentRels(hasHeader: Boolean, hasFooter: Boolean): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        if (hasHeader) sb.append("""<Relationship Id="rIdH1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/>""")
        if (hasFooter) sb.append("""<Relationship Id="rIdF1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>""")
        sb.append("""</Relationships>""")
        return sb.toString()
    }

    private fun buildDocument(
        paragraphs: List<ParaOut>,
        page: PageSettings,
        hasHeader: Boolean,
        hasFooter: Boolean,
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><w:body>""")
        val paras = if (paragraphs.isEmpty()) listOf(ParaOut(0, emptyList())) else paragraphs
        for (p in paras) sb.append(paragraph(p))
        sb.append(sectPr(page, hasHeader, hasFooter))
        sb.append("""</w:body></w:document>""")
        return sb.toString()
    }

    private fun sectPr(page: PageSettings, hasHeader: Boolean, hasFooter: Boolean): String {
        val sb = StringBuilder()
        sb.append("<w:sectPr>")
        if (hasHeader) sb.append("""<w:headerReference w:type="default" r:id="rIdH1"/>""")
        if (hasFooter) sb.append("""<w:footerReference w:type="default" r:id="rIdF1"/>""")
        sb.append("<w:pgSz w:w=\"").append(page.pageWidthPt.ptToTwips())
            .append("\" w:h=\"").append(page.pageHeightPt.ptToTwips()).append("\"/>")
        sb.append("<w:pgMar")
            .append(" w:top=\"").append(page.marginTopPt.ptToTwips()).append("\"")
            .append(" w:right=\"").append(page.marginRightPt.ptToTwips()).append("\"")
            .append(" w:bottom=\"").append(page.marginBottomPt.ptToTwips()).append("\"")
            .append(" w:left=\"").append(page.marginLeftPt.ptToTwips()).append("\"")
            .append(" w:header=\"708\" w:footer=\"708\" w:gutter=\"0\"/>")
        sb.append("<w:bidi/>")
        sb.append("</w:sectPr>")
        return sb.toString()
    }

    private fun paragraph(p: ParaOut): String {
        val rtl = isRtl(p.runs.joinToString("") { it.text })
        val jc = when (p.alignCode) {
            1 -> "center"
            2 -> "left"
            3 -> "both"
            0 -> "right"
            else -> if (rtl) "right" else "left" // تلقائي حسب اللغة
        }
        val sb = StringBuilder()
        sb.append("<w:p><w:pPr>")
        if (rtl) sb.append("<w:bidi/>")
        sb.append("<w:jc w:val=\"").append(jc).append("\"/></w:pPr>")
        for (r in p.runs) sb.append(run(r, rtl))
        sb.append("</w:p>")
        return sb.toString()
    }

    /** يكتشف إن كانت الفقرة عربية/RTL من أول حرف ذي اتجاه قوي. */
    private fun isRtl(text: String): Boolean {
        for (c in text) {
            val d = Character.getDirectionality(c)
            when (d) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return true
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
            }
        }
        return true // افتراضي للعربية
    }

    private fun run(r: RunOut, rtl: Boolean): String {
        val sb = StringBuilder()
        sb.append("<w:r><w:rPr>")
        if (r.bold) sb.append("<w:b/><w:bCs/>")
        if (r.italic) sb.append("<w:i/><w:iCs/>")
        if (r.underline) sb.append("<w:u w:val=\"single\"/>")
        if (r.strike) sb.append("<w:strike/>")
        val halfPoints = (r.sizeSp * 2).coerceAtLeast(2)
        sb.append("<w:sz w:val=\"").append(halfPoints).append("\"/>")
        sb.append("<w:szCs w:val=\"").append(halfPoints).append("\"/>")
        if (r.colorArgb != 0) {
            val hex = String.format("%06X", r.colorArgb and 0xFFFFFF)
            sb.append("<w:color w:val=\"").append(hex).append("\"/>")
        }
        if (r.highlightArgb != 0) {
            val hex = String.format("%06X", r.highlightArgb and 0xFFFFFF)
            sb.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"").append(hex).append("\"/>")
        }
        if (rtl) sb.append("<w:rtl/>")
        sb.append("</w:rPr>")
        sb.append("<w:t xml:space=\"preserve\">").append(escape(r.text)).append("</w:t>")
        sb.append("</w:r>")
        return sb.toString()
    }

    private fun headerFooterPart(tag: String, text: String, pageNumber: Boolean): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("<w:").append(tag)
            .append(" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">")
        if (text.isNotBlank()) {
            for (line in text.split('\n')) {
                sb.append("<w:p><w:pPr><w:bidi/><w:jc w:val=\"right\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:rtl/></w:rPr><w:t xml:space=\"preserve\">")
                    .append(escape(line)).append("</w:t></w:r></w:p>")
            }
        }
        if (pageNumber) {
            // فقرة رقم الصفحة (حقل PAGE) في المنتصف
            sb.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr>")
            sb.append("<w:fldSimple w:instr=\" PAGE \"><w:r><w:t>1</w:t></w:r></w:fldSimple>")
            sb.append("</w:p>")
        }
        if (text.isBlank() && !pageNumber) {
            sb.append("<w:p/>")
        }
        sb.append("</w:").append(tag).append(">")
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
