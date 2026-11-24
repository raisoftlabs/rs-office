package com.raisoftlabs.rsoffice.core

import android.content.Context
import com.raisoftlabs.rsoffice.core.ooxml.PartStore
import java.io.File

/**
 * Creates brand-new Office documents with our own OOXML parts.
 *
 * Design goal: small, valid, editable files that open in Word / Excel /
 * PowerPoint and can immediately be re-opened in RS Office editors. Existing
 * files are still handled by the read-modify-write editors.
 */
object OfficeFactory {

    private const val DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val OFFICE_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    fun createWord(out: File, title: String, body: String) {
        val store = PartStore.empty()
        store.putText("[Content_Types].xml", DECL + """
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
              <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
            </Types>
        """.trimIndent())

        store.putText("_rels/.rels", DECL + """
            <Relationships xmlns="$REL_NS">
              <Relationship Id="rId1" Type="$OFFICE_REL/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent())

        store.putText("word/_rels/document.xml.rels", DECL + """
            <Relationships xmlns="$REL_NS">
              <Relationship Id="rId1" Type="$OFFICE_REL/styles" Target="styles.xml"/>
            </Relationships>
        """.trimIndent())

        val paragraphs = buildList {
            if (title.isNotBlank()) {
                add(
                    "<w:p><w:pPr><w:pStyle w:val=\"Title\"/></w:pPr>" +
                            "<w:r><w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr>" +
                            "<w:t>${Ooxml.escape(title)}</w:t></w:r></w:p>"
                )
            }
            val lines = body.replace("\r\n", "\n").lines()
            if (lines.isEmpty() || lines.all { it.isBlank() }) {
                add("<w:p><w:r><w:t>Start writing here.</w:t></w:r></w:p>")
            } else {
                lines.forEach { line ->
                    add(
                        "<w:p><w:r><w:t xml:space=\"preserve\">" +
                                Ooxml.escape(line) + "</w:t></w:r></w:p>"
                    )
                }
            }
        }.joinToString("")

        store.putText(
            "word/document.xml",
            DECL + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                    "<w:body>$paragraphs" +
                    "<w:sectPr><w:pgSz w:w=\"12240\" w:h=\"15840\"/>" +
                    "<w:pgMar w:top=\"1440\" w:right=\"1800\" w:bottom=\"1440\" w:left=\"1800\" " +
                    "w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/>" +
                    "<w:cols w:space=\"720\"/><w:docGrid w:linePitch=\"360\"/></w:sectPr>" +
                    "</w:body></w:document>"
        )

        store.putText(
            "word/styles.xml",
            DECL + "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                    "<w:docDefaults><w:rPrDefault><w:rPr>" +
                    "<w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\"/><w:sz w:val=\"22\"/>" +
                    "</w:rPr></w:rPrDefault><w:pPrDefault/></w:docDefaults>" +
                    "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\">" +
                    "<w:name w:val=\"Normal\"/><w:qFormat/></w:style>" +
                    "<w:style w:type=\"paragraph\" w:styleId=\"Title\">" +
                    "<w:name w:val=\"Title\"/><w:basedOn w:val=\"Normal\"/>" +
                    "<w:qFormat/><w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr></w:style>" +
                    "</w:styles>"
        )
        store.write(out)
    }

    fun createExcel(out: File, sheetName: String, rows: List<List<String>>) {
        val store = PartStore.empty()
        store.putText("[Content_Types].xml", DECL + """
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent())

        store.putText("_rels/.rels", DECL + """
            <Relationships xmlns="$REL_NS">
              <Relationship Id="rId1" Type="$OFFICE_REL/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent())

        store.putText("xl/_rels/workbook.xml.rels", DECL + """
            <Relationships xmlns="$REL_NS">
              <Relationship Id="rId1" Type="$OFFICE_REL/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent())

        val safeSheet = sheetName.ifBlank { "Sheet1" }.take(31)
            .filter { it.isLetterOrDigit() || it == ' ' || it == '_' }
            .ifBlank { "Sheet1" }

        store.putText(
            "xl/workbook.xml",
            DECL + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                    "xmlns:r=\"$OFFICE_REL\">" +
                    "<bookViews><workbookView/></bookViews>" +
                    "<sheets><sheet name=\"${Ooxml.escape(safeSheet)}\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
                    "<calcPr calcId=\"0\"/></workbook>"
        )

        val sheetRows = buildString {
            append("<dimension ref=\"A1\"/>")
            append("<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>")
            append("<sheetFormatPr defaultRowHeight=\"15\"/>")
            append("<sheetData>")
            rows.forEachIndexed { rowIndex, row ->
                append("<row r=\"${rowIndex + 1}\">")
                row.forEachIndexed { colIndex, value ->
                    val ref = "${columnName(colIndex)}${rowIndex + 1}"
                    val clean = value.trim()
                    if (clean.startsWith("=") && clean.length > 1) {
                        append("<c r=\"$ref\"><f>${Ooxml.escape(clean.removePrefix("="))}</f></c>")
                    } else if (clean.toDoubleOrNull() != null) {
                        append("<c r=\"$ref\"><v>$clean</v></c>")
                    } else {
                        append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">" +
                                Ooxml.escape(value) + "</t></is></c>")
                    }
                }
                append("</row>")
            }
            if (rows.isEmpty()) append("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Start here</t></is></c></row>")
            append("</sheetData>")
        }

        store.putText(
            "xl/worksheets/sheet1.xml",
            DECL + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">$sheetRows</worksheet>"
        )
        store.write(out)
    }

    fun createPowerPoint(context: Context, out: File, title: String, body: String) {
        val template = context.assets.open("deck_template.pptx").use { it.readBytes() }
        val bullets = body.replace("\r\n", "\n").lines().map { it.trim() }.filter { it.isNotBlank() }
            .ifEmpty { listOf("Double-click to add your content") }
        val slides = listOf(
            PptxBuilder.Slide.Cover(title.ifBlank { "Untitled presentation" }, "Made in RS Office"),
            PptxBuilder.Slide.Text("Overview", bullets.chunked(6).firstOrNull().orEmpty(), "2")
        )
        out.outputStream().use { stream ->
            PptxBuilder.build(template, slides, PptxBuilder.SLATE, out = stream)
        }
    }

    private fun columnName(index: Int): String {
        var value = index
        val result = StringBuilder()
        while (value >= 0) {
            result.insert(0, ('A'.code + value % 26).toChar())
            value = value / 26 - 1
        }
        return result.toString()
    }
}
