package com.raisoftlabs.rsoffice.core

import java.io.File
import java.util.zip.ZipFile

/** Reads the text of every slide out of a .pptx (zip of XML parts). */
object PptxReader {

    data class Slide(val number: Int, val lines: List<String>)

    fun read(file: File, maxSlides: Int = 80): List<Slide> {
        val slides = ArrayList<Slide>()
        try {
            ZipFile(file).use { zip ->
                val names = zip.entries().asSequence()
                    .map { it.name }
                    .filter { it.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
                    .sortedBy { it.substringAfter("slide").substringBefore('.').toIntOrNull() ?: 0 }
                    .take(maxSlides)
                    .toList()

                names.forEachIndexed { index, name ->
                    val entry = zip.getEntry(name) ?: return@forEachIndexed
                    val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                    val lines = ArrayList<String>()
                    Regex("<a:p>(.*?)</a:p>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { p ->
                        val text = Regex("<a:t[^>]*>(.*?)</a:t>", RegexOption.DOT_MATCHES_ALL)
                            .findAll(p.groupValues[1])
                            .joinToString("") { XmlText.unescape(it.groupValues[1]) }
                            .trim()
                        if (text.isNotBlank()) lines.add(text)
                    }
                    slides.add(Slide(index + 1, lines))
                }
            }
        } catch (_: Exception) {
        }
        return slides
    }

    fun plainText(slides: List<Slide>): String = slides.joinToString("\n\n") { slide ->
        "Slide ${slide.number}\n" + slide.lines.joinToString("\n")
    }
}
