package com.raisoftlabs.rsoffice

import com.raisoftlabs.rsoffice.core.DocxEditor
import com.raisoftlabs.rsoffice.core.DocxXml
import com.raisoftlabs.rsoffice.core.PptxEditor
import com.raisoftlabs.rsoffice.core.PptxXml
import com.raisoftlabs.rsoffice.core.XlsxXml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the hand-written XML the editors emit. Word, Excel and PowerPoint all
 * refuse a file with malformed markup or wrong element order, so every builder
 * gets a direct assertion here instead of relying on a device test.
 */
class EditorXmlTest {

    @Test
    fun docxParagraphEscapesTextAndKeepsFormatting() {
        val para = DocxEditor.Para(
            text = "Profit & <growth> 20%",
            bold = true,
            italic = true,
            underline = true,
            sizePt = 14,
            color = "1a73e8",
            align = DocxEditor.Align.CENTER,
            heading = true
        )
        val xml = DocxXml.paragraph(para)
        assertTrue(xml.startsWith("<w:p><w:pPr>"))
        assertTrue(xml.endsWith("</w:p>"))
        assertTrue(xml.contains("<w:jc w:val=\"center\"/>"))
        assertTrue(xml.contains("<w:b/>"))
        assertTrue(xml.contains("<w:i/>"))
        assertTrue(xml.contains("<w:u w:val=\"single\"/>"))
        assertTrue(xml.contains("<w:sz w:val=\"28\"/>"))
        assertTrue(xml.contains("<w:color w:val=\"1A73E8\"/>"))
        assertTrue("ampersand escaped", xml.contains("Profit &amp; &lt;growth&gt; 20%"))
        assertTrue("no raw ampersand", !xml.contains(" & "))
    }

    @Test
    fun docxEmptyParagraphIsStillValid() {
        val xml = DocxXml.paragraph(DocxEditor.Para(text = ""))
        assertEquals("<w:p><w:pPr><w:spacing w:before=\"60\" w:after=\"120\" w:line=\"300\" w:lineRule=\"auto\"/><w:jc w:val=\"left\"/></w:pPr></w:p>", xml)
    }

    @Test
    fun xlsxCellTypes() {
        assertEquals("<c r=\"B2\"><v>12.5</v></c>", XlsxXml.cell("B2", "12.5", ""))
        assertEquals("<c r=\"B2\"><v>7</v></c>", XlsxXml.cell("B2", "7", ""))
        assertEquals(
            "<c r=\"C3\"><f>SUM(A1:A9)</f></c>",
            XlsxXml.cell("C3", "=SUM(A1:A9)", "")
        )
        assertEquals(
            "<c r=\"D4\" s=\"3\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Raisul &amp; co</t></is></c>",
            XlsxXml.cell("D4", "Raisul & co", "3")
        )
        assertEquals("<c r=\"E5\" t=\"b\"><v>1</v></c>", XlsxXml.cell("E5", "TRUE", ""))
    }

    @Test
    fun xlsxReferenceMath() {
        assertEquals("A", XlsxXml.letters(1))
        assertEquals("Z", XlsxXml.letters(26))
        assertEquals("AA", XlsxXml.letters(27))
        assertEquals("AB", XlsxXml.letters(28))
        assertEquals("A1", XlsxXml.ref(0, 1))
        assertEquals("AB10", XlsxXml.ref(27, 10))
        assertEquals(0, XlsxXml.colIndex("A"))
        assertEquals(27, XlsxXml.colIndex("AB"))
    }

    @Test
    fun pptxParagraphKeepsPropertiesAndText() {
        val original = "<a:p><a:pPr lvl=\"0\"/><a:r><a:rPr lang=\"en-US\" sz=\"2400\" b=\"1\"/><a:t>Old</a:t></a:r></a:p>"
        val para = PptxEditor.Para(text = "New & better", bold = true, sizePt = 28, color = "C62828")
        val xml = PptxXml.paragraph(original, para)
        assertTrue(xml.startsWith("<a:p><a:pPr lvl=\"0\"/>"))
        assertTrue(xml.contains("<a:t>New &amp; better</a:t>"))
        assertTrue(xml.contains("sz=\"2800\""))
        assertTrue(xml.contains("b=\"1\""))
        assertTrue(xml.contains("<a:srgbClr val=\"C62828\"/>"))
        assertTrue("single run only", xml.indexOf("<a:r>") == xml.lastIndexOf("<a:r>"))
    }
}
