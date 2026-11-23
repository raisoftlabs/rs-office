package com.raisoftlabs.rsoffice.core.ooxml

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * OPC/ZIP package store inspired by the "edit only what you touched" approach
 * used by serious office engines. This is an original implementation.
 *
 * Parts are kept in their original order. Untouched parts are copied back
 * byte-for-byte; parsed XML is cached so a mapper can mutate it and mark it
 * dirty without rewriting the rest of the package.
 */
class PartStore private constructor(
    private val parts: LinkedHashMap<String, Part> = LinkedHashMap()
) {
    data class Part(
        var bytes: ByteArray,
        var dirty: Boolean = false,
        var xml: XmlDocument? = null
    )

    val names: Set<String> get() = parts.keys

    fun bytes(name: String): ByteArray? = parts[name]?.bytes

    fun text(name: String): String? = parts[name]?.bytes?.toString(Charsets.UTF_8)

    fun xml(name: String): XmlDocument? {
        val part = parts[name] ?: return null
        part.xml?.let { return it }
        val parsed = runCatching { Xml.parse(part.bytes.toString(Charsets.UTF_8)) }.getOrNull()
        part.xml = parsed
        return parsed
    }

    fun put(name: String, bytes: ByteArray) {
        parts[name] = Part(bytes, dirty = true)
    }

    fun putText(name: String, value: String) {
        put(name, value.toByteArray(Charsets.UTF_8))
    }

    fun putXml(name: String, document: XmlDocument) {
        val part = parts[name] ?: Part(ByteArray(0))
        part.xml = document
        part.bytes = document.serialize().toByteArray(Charsets.UTF_8)
        part.dirty = true
        parts[name] = part
    }

    fun markDirty(name: String) {
        parts[name]?.dirty = true
    }

    fun remove(name: String) {
        parts.remove(name)
    }

    fun flushXml(name: String) {
        val part = parts[name] ?: return
        val document = part.xml ?: return
        part.bytes = document.serialize().toByteArray(Charsets.UTF_8)
        part.dirty = true
    }

    fun write(out: File) {
        out.parentFile?.mkdirs()
        val temp = File(out.absoluteFile.parentFile, "${out.name}.tmp")
        ZipOutputStream(temp.outputStream().buffered()).use { zip ->
            parts.forEach { (name, part) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(part.bytes)
                zip.closeEntry()
            }
        }
        if (temp.renameTo(out)) return
        out.outputStream().use { temp.inputStream().use { input -> input.copyTo(it) } }
        temp.delete()
    }

    companion object {
        fun read(file: File): PartStore {
            val store = PartStore()
            ZipFile(file).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (entry.isDirectory || entry.size > 40_000_000L) return@forEach
                    store.parts[entry.name] = Part(zip.getInputStream(entry).use { it.readBytes() })
                }
            }
            return store
        }

        fun empty(): PartStore = PartStore()
    }
}
