package com.toffice.app.feature.editor.io

import com.toffice.app.feature.editor.model.INDENT_STEP_PT
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
        header: List<ParaOut> = emptyList(),
        footer: List<ParaOut> = emptyList(),
        tables: List<com.toffice.app.feature.editor.model.TableData> = emptyList(),
        afterParagraphs: List<ParaOut> = emptyList(),
    ) {
        val hasHeader = header.any { it.runs.isNotEmpty() }
        val hasFooter = footer.any { it.runs.isNotEmpty() } || page.showPageNumber

        ZipOutputStream(out).use { zip ->
            zip.entry("[Content_Types].xml", contentTypes(hasHeader, hasFooter))
            zip.entry("_rels/.rels", RELS)
            zip.entry("word/_rels/document.xml.rels", documentRels(hasHeader, hasFooter))
            zip.entry("word/document.xml", buildDocument(paragraphs, page, hasHeader, hasFooter, tables, afterParagraphs))
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
        tables: List<com.toffice.app.feature.editor.model.TableData>,
        afterParagraphs: List<ParaOut>,
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><w:body>""")
        val paras = if (paragraphs.isEmpty()) listOf(ParaOut(0, 0, emptyList())) else paragraphs
        for (p in paras) sb.append(paragraph(p))
        for (t in tables) sb.append(table(t, page.rtlPage))
        for (p in afterParagraphs) sb.append(paragraph(p))
        sb.append(sectPr(page, hasHeader, hasFooter))
        sb.append("""</w:body></w:document>""")
        return sb.toString()
    }

    private fun table(t: com.toffice.app.feature.editor.model.TableData, rtl: Boolean): String {
        if (t.rowCount == 0 || t.colCount == 0) return ""
        val totalTwips = 9000 // عرض الجدول التقريبي
        val sumW = (0 until t.colCount).sumOf { t.weightOf(it).toDouble() }.coerceAtLeast(0.1)
        val sb = StringBuilder()
        sb.append("<w:tbl><w:tblPr>")
        sb.append("<w:tblW w:w=\"").append(totalTwips).append("\" w:type=\"dxa\"/>")
        sb.append("<w:tblLayout w:type=\"fixed\"/>")
        if (rtl) sb.append("<w:bidiVisual/>")
        sb.append("<w:tblBorders>")
        for (edge in listOf("top", "left", "bottom", "right", "insideH", "insideV")) {
            sb.append("<w:").append(edge).append(" w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"888888\"/>")
        }
        sb.append("</w:tblBorders></w:tblPr>")
        sb.append("<w:tblGrid>")
        for (i in 0 until t.colCount) {
            val w = (totalTwips * t.weightOf(i) / sumW).toInt().coerceAtLeast(200)
            sb.append("<w:gridCol w:w=\"").append(w).append("\"/>")
        }
        sb.append("</w:tblGrid>")
        for (row in t.rows) {
            sb.append("<w:tr>")
            for (cell in row) {
                sb.append("<w:tc><w:tcPr/>")
                val jc = when (cell.align) {
                    1 -> "center"
                    2 -> "left"
                    3 -> "right"
                    else -> if (rtl) "right" else "left"
                }
                sb.append("<w:p><w:pPr>")
                if (rtl) sb.append("<w:bidi/>")
                sb.append("<w:jc w:val=\"").append(jc).append("\"/></w:pPr>")
                sb.append("<w:r><w:rPr>")
                if (rtl) sb.append("<w:rtl/>")
                sb.append("</w:rPr><w:t xml:space=\"preserve\">").append(escape(cell.text)).append("</w:t></w:r></w:p>")
                sb.append("</w:tc>")
            }
            sb.append("</w:tr>")
        }
        sb.append("</w:tbl>")
        // فقرة فارغة بعد الجدول (متطلب وورد)
        sb.append("<w:p/>")
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
        if (page.rtlPage) sb.append("<w:bidi/>")
        sb.append("</w:sectPr>")
        return sb.toString()
    }

    private fun paragraph(p: ParaOut): String {
        val rtl = when (p.dirCode) {
            1 -> true
            2 -> false
            else -> isRtl(p.runs.joinToString("") { it.text }) // تلقائي
        }
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
        sb.append("<w:jc w:val=\"").append(jc).append("\"/>")
        // المسافة البادئة (من جهة بداية الفقرة، تحترم الاتجاه)
        if (p.indentLevel > 0) {
            val twips = p.indentLevel * INDENT_STEP_PT * 20
            sb.append("<w:ind w:start=\"").append(twips).append("\"/>")
        }
        // تباعد الأسطر (٢٤٠ = مفرد)
        if (p.lineSpacing != 1f) {
            val line = (240 * p.lineSpacing).toInt()
            sb.append("<w:spacing w:line=\"").append(line).append("\" w:lineRule=\"auto\"/>")
        }
        sb.append("</w:pPr>")
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
        if (r.fontFamily != com.toffice.app.feature.editor.model.FONT_DEFAULT) {
            val name = com.toffice.app.feature.editor.model.fontNameOf(r.fontFamily)
            sb.append("<w:rFonts w:ascii=\"").append(name).append("\" w:hAnsi=\"").append(name)
                .append("\" w:cs=\"").append(name).append("\"/>")
        }
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
        when (r.script) {
            1 -> sb.append("<w:vertAlign w:val=\"superscript\"/>")
            2 -> sb.append("<w:vertAlign w:val=\"subscript\"/>")
        }
        if (rtl) sb.append("<w:rtl/>")
        sb.append("</w:rPr>")
        sb.append("<w:t xml:space=\"preserve\">").append(escape(r.text)).append("</w:t>")
        sb.append("</w:r>")
        return sb.toString()
    }

    private fun headerFooterPart(tag: String, paras: List<ParaOut>, pageNumber: Boolean): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("<w:").append(tag)
            .append(" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">")
        val hasText = paras.any { it.runs.isNotEmpty() }
        // فقرات منسّقة (تنسيق الخط محفوظ كما المتن)
        for (p in paras) if (p.runs.isNotEmpty()) sb.append(paragraph(p))
        if (pageNumber) {
            // فقرة رقم الصفحة (حقل PAGE) في المنتصف
            sb.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr>")
            sb.append("<w:fldSimple w:instr=\" PAGE \"><w:r><w:t>1</w:t></w:r></w:fldSimple>")
            sb.append("</w:p>")
        }
        if (!hasText && !pageNumber) {
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
