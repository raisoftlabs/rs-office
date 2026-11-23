package com.raisoftlabs.rsoffice

import com.raisoftlabs.rsoffice.core.edit.EditCommand
import com.raisoftlabs.rsoffice.core.edit.UndoStack
import com.raisoftlabs.rsoffice.core.ooxml.PartStore
import com.raisoftlabs.rsoffice.core.ooxml.Xml
import com.raisoftlabs.rsoffice.core.ooxml.child
import com.raisoftlabs.rsoffice.core.ooxml.setText
import com.raisoftlabs.rsoffice.core.ooxml.textValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OfficeEngineFoundationTest {

    @Test
    fun xmlTreeKeepsUnknownAttributesAndEscapesText() {
        val source = "<?xml version=\"1.0\"?><w:p w:rsid=\"A1\"><w:r><w:t>A &amp; B</w:t></w:r></w:p>"
        val document = Xml.parse(source)
        val para = document.root.child("w:r")?.child("w:t")
        assertEquals("A & B", para?.textValue())
        para?.setText("C < D")
        val output = document.serialize()
        assertTrue(output.contains("w:rsid=\"A1\""))
        assertTrue(output.contains("C &lt; D"))
        assertFalse(output.contains("A & B"))
    }

    @Test
    fun partStoreCopiesUntouchedPartsByteForByte() {
        val original = File.createTempFile("rso-store", ".docx")
        val output = File.createTempFile("rso-out", ".docx")
        val unknown = "keep me exactly".toByteArray()
        ZipOutputStream(original.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write("<w:document><w:body/></w:document>".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("custom/unknown.bin"))
            zip.write(unknown)
            zip.closeEntry()
        }

        val store = PartStore.read(original)
        val doc = store.xml("word/document.xml")!!
        doc.root.child("w:body")?.setText("edited")
        store.putXml("word/document.xml", doc)
        store.write(output)

        val reread = PartStore.read(output)
        assertTrue(reread.bytes("custom/unknown.bin")!!.contentEquals(unknown))
        assertEquals("edited", reread.xml("word/document.xml")?.root?.textValue())
        original.delete()
        output.delete()
    }

    @Test
    fun undoStackGroupsTransactionAndRedoes() {
        val value = StringBuilder("a")
        val stack = UndoStack()
        stack.beginTransaction("type")
        stack.execute(object : EditCommand {
            override val label = "append b"
            override fun execute() { value.append("b") }
            override fun undo() { value.setLength(value.length - 1) }
        })
        stack.execute(object : EditCommand {
            override val label = "append c"
            override fun execute() { value.append("c") }
            override fun undo() { value.setLength(value.length - 1) }
        })
        stack.endTransaction()
        assertEquals("abc", value.toString())
        assertTrue(stack.undo())
        assertEquals("a", value.toString())
        assertTrue(stack.redo())
        assertEquals("abc", value.toString())
    }
}
