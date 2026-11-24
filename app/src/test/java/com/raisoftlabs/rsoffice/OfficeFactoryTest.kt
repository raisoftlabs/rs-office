package com.raisoftlabs.rsoffice

import com.raisoftlabs.rsoffice.core.OfficeFactory
import com.raisoftlabs.rsoffice.core.ooxml.PartStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfficeFactoryTest {

    @Test
    fun createsValidWordPackage() {
        val out = File.createTempFile("factory", ".docx")
        OfficeFactory.createWord(out, "Quarterly Report", "Line one\nLine two & <more>")
        val store = PartStore.read(out)
        val document = store.text("word/document.xml")!!
        assertTrue(document.contains("Quarterly Report"))
        assertTrue(document.contains("Line two &amp; &lt;more&gt;"))
        assertTrue(document.contains("<w:sectPr>"))
        assertTrue(store.text("word/styles.xml")!!.contains("w:styleId=\"Normal\""))
        assertTrue(store.text("[Content_Types].xml")!!.contains("wordprocessingml.document.main+xml"))
        out.delete()
    }

    @Test
    fun createsValidExcelPackage() {
        val out = File.createTempFile("factory", ".xlsx")
        OfficeFactory.createExcel(
            out,
            "Budget",
            listOf(listOf("Item", "Cost"), listOf("Paper", "12.50"), listOf("Total", "=SUM(B2:B2)"))
        )
        val store = PartStore.read(out)
        val sheet = store.text("xl/worksheets/sheet1.xml")!!
        assertTrue(store.text("xl/workbook.xml")!!.contains("name=\"Budget\""))
        assertTrue(sheet.contains("r=\"A1\" t=\"inlineStr\""))
        assertTrue(sheet.contains("r=\"B3\"><f>SUM(B2:B2)</f></c>"))
        assertEquals(1, store.names.count { it.startsWith("xl/worksheets/") })
        out.delete()
    }
}
