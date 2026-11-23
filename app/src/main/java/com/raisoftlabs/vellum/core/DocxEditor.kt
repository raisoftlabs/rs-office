package com.raisoftlabs.vellum.core

import java.io.File

/**
 * Read/write editor for .docx files.
 *
 * The document body is patched paragraph by paragraph: paragraphs the user did
 * not touch are written back byte-for-byte (so tables, images, fields and text
 * boxes survive), while edited paragraphs are re-emitted with explicit run
 * properties. The document root, namespaces and section properties are reused
 * verbatim, which is what lets Word open the result without a repair prompt.
 */
class DocxEditor private constructor(
    private val parts: LinkedHashMap<String, ByteArray>,
    private val prefix: String,
    private val bodyInner: String,
    private val suffix: String,
    private val ranges: List<IntRange>,
    private val originals: List<String>,
    private val loaded: List<Para>,
    val paragraphs: List<Para>
) {

    enum class Align { LEFT, CENTER, RIGHT, JUSTIFY }

    data class Para(
        val text: String = "",
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val sizePt: Int = 0,
        val color: String = "",
        val align: Align = Align.LEFT,
        val heading: Boolean = false,
        val src: Int = -1,
        val locked: Boolean = false
    )

    fun withParagraphs(list: List<Para>): DocxEditor =
        DocxEditor(parts, prefix, bodyInner, suffix, ranges, originals, loaded, list)

    fun save(out: File): Boolean = runCatching {
        val built = StringBuilder()
        var cursor = 0
        ranges.forEachIndexed { src, range ->
            built.append(bodyInner, cursor, range.first)
            val para = paragraphs.firstOrNull { it.src == src }
            if (para != null) {
                val untouched = para == loaded[src] || para.locked
                built.append(if (untouched) originals[src] else DocxXml.paragraph(para))
            }
            cursor = range.last + 1
        }
        built.append(bodyInner, cursor, bodyInner.length)

        val appended = paragraphs.filter { it.src < 0 }
        if (appended.isNotEmpty()) {
            val extra = appended.joinToString("") { DocxXml.paragraph(it) }
            val current = built.toString()
            val sect = current.lastIndexOf("<w:sectPr")
            built.setLength(0)
            if (sect > 0) {
                built.append(current, 0, sect).append(extra).append(current, sect, current.length)
            } else {
                built.append(current).append(extra)
            }
        }

        Ooxml.putText(parts, "word/document.xml", prefix + built + suffix)
        Ooxml.write(parts, out)
        true
    }.getOrDefault(false)

    companion object {

        fun load(file: File): DocxEditor? = runCatching {
            val parts = Ooxml.read(file)
            val document = Ooxml.text(parts, "word/document.xml") ?: return null
            val bodyOpen = Regex("<w:body(\\s[^>]*)?>").find(document) ?: return null
            val bodyEnd = document.lastIndexOf("</w:body>")
            if (bodyEnd <= bodyOpen.range.last) return null
            val prefix = document.substring(0, bodyOpen.range.last + 1)
            val suffix = document.substring(bodyEnd)
            val inner = document.substring(bodyOpen.range.last + 1, bodyEnd)
            val ranges = Ooxml.ranges(inner, "w:p")
            val originals = ranges.map { inner.substring(it.first, it.last + 1) }
            val loaded = originals.mapIndexed { index, xml -> parse(xml, index) }
            DocxEditor(parts, prefix, inner, suffix, ranges, originals, loaded, loaded)
        }.getOrNull()

        private fun parse(xml: String, src: Int): Para {
            val text = Regex("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                .findAll(xml)
                .joinToString("") { XmlText.unescape(it.groupValues[1]) }
                .replace('\u00A0', ' ')
            val style = Regex("<w:pStyle w:val=\"([^\"]+)\"").find(xml)?.groupValues?.get(1) ?: ""
            val heading = style.startsWith("Heading", true) || style.equals("Title", true)
            val bold = heading ||
                Regex("<w:b(?:\\s[^>]*)?/>").containsMatchIn(xml) ||
                Regex("<w:b w:val=\"(?:1|true)\"").containsMatchIn(xml)
            val italic = Regex("<w:i(?:\\s[^>]*)?/>").containsMatchIn(xml) ||
                Regex("<w:i w:val=\"(?:1|true)\"").containsMatchIn(xml)
            val underline = Regex("<w:u w:val=\"(?:single|double|thick)\"").containsMatchIn(xml)
            val size = Regex("<w:sz w:val=\"(\\d+)\"").findAll(xml)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .maxOrNull()?.div(2) ?: 0
            val color = Regex("<w:color w:val=\"([0-9A-Fa-f]{6})\"").find(xml)?.groupValues?.get(1) ?: ""
            val jc = Regex("<w:jc w:val=\"(left|center|right|both|justify)\"").find(xml)?.groupValues?.get(1)
            val align = when (jc) {
                "center" -> Align.CENTER
                "right" -> Align.RIGHT
                "both", "justify" -> Align.JUSTIFY
                else -> Align.LEFT
            }
            val locked = xml.contains("<w:txbxContent") || xml.contains("<w:drawing") ||
                xml.contains("<w:object") || xml.contains("<mc:AlternateContent")
            return Para(
                text = text,
                bold = bold,
                italic = italic,
                underline = underline,
                sizePt = size,
                color = color,
                align = align,
                heading = heading,
                src = src,
                locked = locked
            )
        }
    }
}

/** Emits a single `<w:p>` from the editor model. */
internal object DocxXml {

    fun paragraph(p: DocxEditor.Para): String {
        val jc = when (p.align) {
            DocxEditor.Align.CENTER -> "center"
            DocxEditor.Align.RIGHT -> "right"
            DocxEditor.Align.JUSTIFY -> "both"
            DocxEditor.Align.LEFT -> "left"
        }
        val before = if (p.heading) 240 else 60
        val pPr = buildString {
            if (p.heading) append("<w:pStyle w:val=\"Heading1\"/>")
            append("<w:spacing w:before=\"$before\" w:after=\"120\" w:line=\"300\" w:lineRule=\"auto\"/>")
            append("<w:jc w:val=\"$jc\"/>")
        }
        val size = when {
            p.sizePt > 0 -> p.sizePt * 2
            p.heading -> 32
            else -> 0
        }
        val rPr = buildString {
            if (p.bold || p.heading) append("<w:b/><w:bCs/>")
            if (p.italic) append("<w:i/><w:iCs/>")
            if (p.underline) append("<w:u w:val=\"single\"/>")
            if (p.color.isNotBlank()) append("<w:color w:val=\"${p.color.uppercase()}\"/>")
            if (size > 0) append("<w:sz w:val=\"$size\"/><w:szCs w:val=\"$size\"/>")
        }
        val run = if (p.text.isEmpty()) "" else
            "<w:r><w:rPr>$rPr</w:rPr><w:t xml:space=\"preserve\">${Ooxml.escape(p.text)}</w:t></w:r>"
        return "<w:p><w:pPr>$pPr</w:pPr>$run</w:p>"
    }
}
