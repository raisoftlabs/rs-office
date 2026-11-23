package com.raisoftlabs.rsoffice.core

import java.io.File
import java.util.zip.ZipFile

/** Reads a .docx into styled paragraphs (headings detected from Word styles). */
object DocxReader {

    data class Block(val text: String, val heading: Boolean)

    fun read(file: File, maxBlocks: Int = 800): List<Block> {
        try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("word/document.xml") ?: return emptyList()
                val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                val blocks = ArrayList<Block>()
                Regex("<w:p(?: [^>]*)?>(.*?)</w:p>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { m ->
                    if (blocks.size >= maxBlocks) return@forEach
                    val body = m.groupValues[1]
                    val text = Regex("<w:t[^>]*>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                        .findAll(body)
                        .joinToString("") { XmlText.unescape(it.groupValues[1]) }
                        .replace('\u00A0', ' ')
                        .trim()
                    if (text.isBlank()) return@forEach
                    val heading = body.contains("w:val=\"Heading") || body.contains("w:val=\"Title\"")
                    blocks.add(Block(text, heading))
                }
                return blocks
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun plainText(blocks: List<Block>): String = blocks.joinToString("\n\n") { it.text }
}
