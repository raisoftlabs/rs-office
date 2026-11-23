package com.raisoftlabs.rsoffice.core

import java.io.File

/**
 * Read/write editor for .pptx presentations.
 *
 * Slide XML is patched paragraph by paragraph: only `<a:p>` elements the user
 * edited are rebuilt, everything else — shapes, images, layouts, themes — is
 * copied through untouched.
 */
class PptxEditor private constructor(
    private val parts: LinkedHashMap<String, ByteArray>,
    private val slideNames: List<String>,
    private val originals: List<List<String>>,
    private val loaded: List<List<Para>>,
    val slides: List<Slide>
) {

    data class Slide(val number: Int, val paragraphs: List<Para>)

    data class Para(
        val text: String = "",
        val bold: Boolean = false,
        val italic: Boolean = false,
        val sizePt: Int = 0,
        val color: String = "",
        val center: Boolean = false,
        val src: Int = -1
    )

    fun withSlides(list: List<Slide>): PptxEditor =
        PptxEditor(parts, slideNames, originals, loaded, list)

    fun save(out: File): Boolean = runCatching {
        slides.forEachIndexed { index, slide ->
            val name = slideNames.getOrNull(index) ?: return@forEachIndexed
            val xml = Ooxml.text(parts, name) ?: return@forEachIndexed
            val ranges = Ooxml.ranges(xml, "a:p")
            val before = loaded.getOrNull(index) ?: emptyList()
            val origin = originals.getOrNull(index) ?: emptyList()
            val built = StringBuilder()
            var cursor = 0
            ranges.forEachIndexed { src, range ->
                built.append(xml, cursor, range.first)
                val original = origin.getOrNull(src) ?: xml.substring(range.first, range.last + 1)
                val current = slide.paragraphs.firstOrNull { it.src == src }
                when {
                    current == null -> Unit // paragraph removed
                    current == before.getOrNull(src) -> built.append(original)
                    else -> built.append(PptxXml.paragraph(original, current))
                }
                cursor = range.last + 1
            }
            built.append(xml, cursor, xml.length)
            Ooxml.putText(parts, name, built.toString())
        }
        Ooxml.write(parts, out)
        true
    }.getOrDefault(false)

    companion object {

        fun load(file: File): PptxEditor? = runCatching {
            val parts = Ooxml.read(file)
            val names = parts.keys
                .filter { it.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
                .sortedBy { it.substringAfter("slide").substringBefore('.').toIntOrNull() ?: 0 }
            if (names.isEmpty()) return null
            val slides = ArrayList<Slide>()
            val originals = ArrayList<List<String>>()
            val loaded = ArrayList<List<Para>>()
            names.take(60).forEachIndexed { index, name ->
                val xml = Ooxml.text(parts, name) ?: return@forEachIndexed
                val ranges = Ooxml.ranges(xml, "a:p")
                val xmls = ranges.map { xml.substring(it.first, it.last + 1) }
                val paras = xmls.mapIndexed { i, p -> parse(p, i) }
                originals.add(xmls)
                loaded.add(paras)
                slides.add(Slide(index + 1, paras))
            }
            PptxEditor(parts, names.take(60), originals, loaded, slides)
        }.getOrNull()

        private fun parse(xml: String, src: Int): Para {
            val text = Regex("<a:t(?:\\s[^>]*)?>(.*?)</a:t>", RegexOption.DOT_MATCHES_ALL)
                .findAll(xml)
                .joinToString("") { XmlText.unescape(it.groupValues[1]) }
            val rPr = Regex("<a:rPr([^>]*)>").find(xml)?.groupValues?.get(1) ?: ""
            val sz = Regex("\\bsz=\"(\\d+)\"").find(rPr)?.groupValues?.get(1)?.toIntOrNull()?.div(100) ?: 0
            val color = Regex("<a:srgbClr val=\"([0-9A-Fa-f]{6})\"").find(xml)?.groupValues?.get(1) ?: ""
            val align = Regex("<a:pPr[^>]*\\balgn=\"([a-z]+)\"").find(xml)?.groupValues?.get(1)
            return Para(
                text = text,
                bold = Regex("<a:b(?:\\s[^>]*)?/>").containsMatchIn(xml),
                italic = Regex("<a:i(?:\\s[^>]*)?/>").containsMatchIn(xml),
                sizePt = sz,
                color = color,
                center = align == "ctr",
                src = src
            )
        }
    }
}

/** Rebuilds a single slide paragraph, keeping its paragraph properties. */
internal object PptxXml {

    fun paragraph(original: String, p: PptxEditor.Para): String {
        val pPr = Regex("<a:pPr(?:\\s[^>]*)?(?:/>|>.*?</a:pPr>)", RegexOption.DOT_MATCHES_ALL)
            .find(original)?.value ?: ""
        val end = Regex("<a:endParaRPr(?:\\s[^>]*)?(?:/>|>.*?</a:endParaRPr>)", RegexOption.DOT_MATCHES_ALL)
            .find(original)?.value ?: ""
        val attrs = Regex("<a:rPr([^>]*)>").find(original)?.groupValues?.get(1) ?: ""
        val lang = Regex("\\blang=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1) ?: "en-US"
        val rPr = buildString {
            append("<a:rPr lang=\"$lang\"")
            append(" dirty=\"0\"")
            if (p.sizePt > 0) append(" sz=\"${p.sizePt * 100}\"")
            if (p.bold) append(" b=\"1\"")
            if (p.italic) append(" i=\"1\"")
            val color = p.color
            if (color.isNotBlank()) {
                append("><a:solidFill><a:srgbClr val=\"${color.uppercase()}\"/></a:solidFill></a:rPr>")
            } else {
                append("/>")
            }
        }
        val run = if (p.text.isEmpty()) "" else
            "<a:r>$rPr<a:t>${Ooxml.escape(p.text)}</a:t></a:r>"
        val alignFix = when {
            pPr.isBlank() && p.center -> "<a:pPr algn=\"ctr\"/>"
            p.center && !pPr.contains("algn=") -> pPr.replace("<a:pPr", "<a:pPr algn=\"ctr\"")
            else -> pPr
        }
        return "<a:p>$alignFix$run$end</a:p>"
    }
}
