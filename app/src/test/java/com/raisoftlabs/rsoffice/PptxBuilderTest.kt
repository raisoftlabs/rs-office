package com.raisoftlabs.rsoffice

import com.raisoftlabs.rsoffice.core.PptxBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the hand-written OOXML writer: the produced .pptx must be a valid zip,
 * every slide part must be well-formed XML, the presentation must reference the
 * slides, and every relationship target must actually exist in the package.
 * PowerPoint rejects decks that break any of these rules.
 */
class PptxBuilderTest {

    private val png1x1 = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    )

    private fun template(): ByteArray {
        val candidates = listOf(
            File("src/main/assets/deck_template.pptx"),
            File("app/src/main/assets/deck_template.pptx")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("deck_template.pptx not found from ${File(".").absolutePath}")
        return file.readBytes()
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                val buf = ByteArrayOutputStream()
                zin.copyTo(buf)
                assertTrue("duplicate part ${entry.name}", out.put(entry.name, buf.toByteArray()) == null)
                zin.closeEntry()
                entry = zin.nextEntry
            }
        }
        return out
    }

    private fun assertWellFormed(name: String, bytes: ByteArray) {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = try {
            factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        } catch (e: Exception) {
            throw AssertionError("$name is not well-formed XML: ${e.message}", e)
        }
        assertTrue("$name has no root element", doc.documentElement != null)
    }

    private fun buildDeck(slides: List<PptxBuilder.Slide>): Map<String, ByteArray> {
        val out = ByteArrayOutputStream()
        PptxBuilder.build(template(), slides, PptxBuilder.SLATE, out = out)
        return unzip(out.toByteArray())
    }

    private fun text(parts: Map<String, ByteArray>, name: String): String =
        String(parts[name] ?: error("missing part $name"), Charsets.UTF_8)

    @Test
    fun mixedDeckIsValid() {
        val slides = listOf(
            PptxBuilder.Slide.Cover("Quarterly review", "Generated on device"),
            PptxBuilder.Slide.Text(
                "Highlights",
                listOf("Revenue up 12%", "Churn down to 3.1%", "Ship date locked"),
                "2 / 3"
            ),
            PptxBuilder.Slide.Page(png1x1, "png", "Scanned page")
        )
        val parts = buildDeck(slides)

        val required = listOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "ppt/presentation.xml",
            "ppt/_rels/presentation.xml.rels",
            "ppt/slides/slide1.xml",
            "ppt/slides/slide2.xml",
            "ppt/slides/slide3.xml",
            "ppt/slides/_rels/slide3.xml.rels"
        )
        required.forEach { name ->
            assertTrue("missing required part: $name", parts.containsKey(name))
        }

        required.filter { it.endsWith(".xml") || it.endsWith(".rels") }.forEach { name ->
            assertWellFormed(name, parts.getValue(name))
        }

        // presentation.xml must list exactly the slides we wrote, in order.
        val pres = text(parts, "ppt/presentation.xml")
        val ids = Regex("<p:sldId ").findAll(pres).count()
        assertEquals("presentation should reference 3 slides", 3, ids)
        assertTrue(
            "slide size not written",
            pres.contains("<p:sldSz cx=\"12192000\" cy=\"6858000\"/>")
        )

        // Content types must declare every slide part we added.
        val types = text(parts, "[Content_Types].xml")
        for (i in 1..3) {
            assertTrue(
                "content types missing slide$i",
                types.contains("/ppt/slides/slide$i.xml")
            )
        }

        // The picture slide must ship its media and a matching relationship.
        val media = parts.keys.filter { it.startsWith("ppt/media/") }
        assertTrue("no media part for the image slide", media.isNotEmpty())
        val slide3Rels = text(parts, "ppt/slides/_rels/slide3.xml.rels")
        assertTrue("slide3 rels does not point at media", slide3Rels.contains("../media/"))
        assertTrue("slide3 xml has no picture", text(parts, "ppt/slides/slide3.xml").contains("<p:pic>"))

        // Every internal relationship target inside the package must resolve.
        parts.filterKeys { it.endsWith(".rels") }.forEach { (relsName, bytes) ->
            val dir = relsName.substringBeforeLast("_rels/", "")
            Regex("<Relationship[^>]*>")
                .findAll(String(bytes, Charsets.UTF_8))
                .map { it.value }
                .filterNot { it.contains("TargetMode=\"External\"") }
                .forEach { tag ->
                    val target = Regex("Target=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)
                        ?: return@forEach
                    if (target.contains("://")) return@forEach
                    val resolved =
                        if (target.startsWith("/")) target.trimStart('/') else normalize(dir + target)
                    assertTrue(
                        "$relsName points at missing part $resolved",
                        parts.containsKey(resolved)
                    )
                }
        }
    }

    @Test
    fun textOnlyDeckIsValid() {
        val parts = buildDeck(listOf(PptxBuilder.Slide.Cover("Only a cover", "")))
        assertWellFormed("ppt/slides/slide1.xml", parts.getValue("ppt/slides/slide1.xml"))
        assertTrue(
            "cover title missing from slide xml",
            text(parts, "ppt/slides/slide1.xml").contains("Only a cover")
        )
    }

    private fun normalize(path: String): String {
        val stack = ArrayDeque<String>()
        path.split('/').forEach { seg ->
            when (seg) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(seg)
            }
        }
        return stack.joinToString("/")
    }
}
