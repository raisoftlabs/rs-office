package com.raisoftlabs.vellum.core

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Zip-level helpers for OOXML packages (.docx / .xlsx / .pptx).
 *
 * Every editor reads the whole package into memory, patches only the parts it
 * understands and writes every other part back byte-for-byte. That is what
 * keeps Word/Excel/PowerPoint happy when the saved file is reopened: styles,
 * themes, media and relationships are never regenerated.
 */
internal object Ooxml {

    fun read(file: File): LinkedHashMap<String, ByteArray> {
        val parts = LinkedHashMap<String, ByteArray>()
        ZipFile(file).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.isDirectory) return@forEach
                if (entry.size > 40_000_000L) return@forEach
                parts[entry.name] = zip.getInputStream(entry).use { it.readBytes() }
            }
        }
        return parts
    }

    fun write(parts: Map<String, ByteArray>, out: File) {
        out.parentFile?.mkdirs()
        ZipOutputStream(out.outputStream().buffered()).use { zip ->
            parts.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    fun text(parts: Map<String, ByteArray>, name: String): String? =
        parts[name]?.toString(Charsets.UTF_8)

    fun putText(parts: MutableMap<String, ByteArray>, name: String, xml: String) {
        parts[name] = xml.toByteArray(Charsets.UTF_8)
    }

    fun escape(raw: String): String = buildString(raw.length + 16) {
        raw.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    /** Replaces the content of the first tag matched by [regex] inside [xml]. */
    fun replaceInner(xml: String, regex: Regex, transform: (String) -> String): String {
        val match = regex.find(xml) ?: return xml
        val group = match.groups[1] ?: return xml
        return xml.substring(0, group.range.first) + transform(group.value) + xml.substring(group.range.last + 1)
    }

    /**
     * Top-level ranges of an element (default `<w:p`) inside [xml], skipping
     * elements nested inside a same-named element (text boxes inside runs).
     */
    fun ranges(xml: String, tag: String): List<IntRange> {
        val open = Regex("<$tag(?=[\\s/>])")
        val close = "</$tag>"
        val out = ArrayList<IntRange>()
        var index = 0
        while (index < xml.length) {
            val m = open.find(xml, index) ?: break
            val start = m.range.first
            val tagEnd = xml.indexOf('>', start)
            if (tagEnd < 0) break
            if (xml[tagEnd - 1] == '/') {
                out.add(start..tagEnd)
                index = tagEnd + 1
                continue
            }
            var depth = 1
            var cursor = tagEnd + 1
            var end = -1
            while (cursor < xml.length) {
                val nextOpen = xml.indexOf("<$tag", cursor)
                val nextClose = xml.indexOf(close, cursor)
                if (nextClose < 0) break
                if (nextOpen in 0 until nextClose &&
                    nextOpen + tag.length + 1 <= xml.length &&
                    isBoundary(xml, nextOpen + tag.length + 1)
                ) {
                    depth++
                    cursor = nextOpen + tag.length + 1
                } else {
                    depth--
                    cursor = nextClose + close.length
                    if (depth == 0) {
                        end = nextClose + close.length - 1
                        break
                    }
                }
            }
            if (end < 0) break
            out.add(start..end)
            index = end + 1
        }
        return out
    }

    private fun isBoundary(xml: String, i: Int): Boolean =
        i < xml.length && (xml[i].isWhitespace() || xml[i] == '>' || xml[i] == '/')
}
