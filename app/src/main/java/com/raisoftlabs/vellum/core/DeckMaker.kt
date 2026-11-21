package com.raisoftlabs.vellum.core

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Turns a PDF into a real, editable PowerPoint deck.
 *
 * EXACT  – every PDF page becomes a full-bleed slide (pixel perfect).
 * SMART  – headings are detected from the document's own font sizes and
 *          re-typeset as clean title + bullet slides. Scanned pages with no
 *          text layer are read with on-device OCR.
 */
object DeckMaker {

    enum class Mode { EXACT, SMART }

    data class Options(
        val mode: Mode,
        val palette: PptxBuilder.Palette,
        val ocrScannedPages: Boolean = true
    )

    data class Result(
        val slides: Int,
        val usedOcr: Boolean,
        val fellBackToExact: Boolean = false
    )

    private const val SLIDE_W = 12_192_000L
    private const val SLIDE_H = 6_858_000L
    private const val MAX_SLIDES = 80

    suspend fun make(
        context: Context,
        pdf: File,
        options: Options,
        out: File,
        onProgress: suspend (Float, String) -> Unit
    ): Result {
        val template = context.assets.open("deck_template.pptx").use { it.readBytes() }

        return when (options.mode) {
            Mode.EXACT -> exact(template, pdf, options, out, onProgress)
            Mode.SMART -> {
                val smart = smart(template, pdf, options, out, onProgress)
                if (smart != null) smart else exact(
                    template, pdf, options, out,
                    { p, _ -> onProgress(p, "No text layer — using page images") }
                ).copy(fellBackToExact = true)
            }
        }
    }

    /* ------------------------------- EXACT -------------------------------- */

    private suspend fun exact(
        template: ByteArray,
        pdf: File,
        options: Options,
        out: File,
        onProgress: suspend (Float, String) -> Unit
    ): Result {
        PdfRender(pdf).use { render ->
            if (render.pageCount == 0) throw IllegalStateException("This PDF has no pages.")
            val aspect = render.pageAspect(0).takeIf { it > 0.1f && it < 10f } ?: (4f / 3f)
            val width = SLIDE_W
            val height = (width / aspect).toLong().coerceIn(4_000_000L, 12_000_000L)
            val total = render.pageCount.coerceAtMost(MAX_SLIDES)
            val slides = ArrayList<PptxBuilder.Slide>(total)

            for (i in 0 until total) {
                report(onProgress, i.toFloat() / total, "Rendering page ${i + 1} of $total")
                val bitmap = render.render(i, 1600)
                val bytes = ByteArrayOutputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 84, stream)
                    stream.toByteArray()
                }
                bitmap.recycle()
                slides.add(PptxBuilder.Slide.Page(bytes, "jpg"))
            }
            report(onProgress, 0.9f, "Assembling PowerPoint…")
            out.outputStream().use { PptxBuilder.build(template, slides, options.palette, width, height, it) }
            report(onProgress, 1f, "Done")
            return Result(slides.size, usedOcr = false)
        }
    }

    /* ------------------------------- SMART -------------------------------- */

    private suspend fun smart(
        template: ByteArray,
        pdf: File,
        options: Options,
        out: File,
        onProgress: suspend (Float, String) -> Unit
    ): Result {
        val pageLines = PdfEngine.pageTexts(pdf)
        if (pageLines.isEmpty()) return null

        val total = pageLines.size.coerceAtMost(MAX_SLIDES)
        var usedOcr = false
        val extracted = ArrayList<List<PdfEngine.PdfLine>>(total)

        PdfRender(pdf).use { render ->
            for (i in 0 until total) {
                report(onProgress, 0.05f + 0.6f * i / total, "Reading page ${i + 1} of $total")
                var lines = pageLines[i]
                    .filter { it.text.isNotBlank() && it.text.length > 1 && !isNoise(it.text) }

                if (lines.isEmpty() && options.ocrScannedPages && i < render.pageCount) {
                    report(onProgress, 0.05f + 0.6f * i / total, "OCR page ${i + 1} of $total")
                    val bitmap = render.render(i, 1500)
                    val text = Ocr.recognize(bitmap)
                    bitmap.recycle()
                    if (text.isNotBlank()) {
                        usedOcr = true
                        lines = text.split('\n')
                            .map { it.trim() }
                            .filter { it.length > 1 && !isNoise(it) }
                            .map { PdfEngine.PdfLine(it, 10f) }
                    }
                }
                extracted.add(lines)
            }
        }

        val sections = buildSections(extracted)
        if (sections.isEmpty()) return null

        val slides = ArrayList<PptxBuilder.Slide>()
        val docTitle = Store.prettyName(pdf).ifBlank { "Untitled document" }
        val subtitle = if (usedOcr) "Scanned document · OCR" else "Converted document"

        slides.add(PptxBuilder.Slide.Cover(docTitle, subtitle))

        var index = 2
        for (section in sections) {
            val chunks = section.bullets
                .flatMap { splitBullet(it) }
                .chunked(7)
                .ifEmpty { listOf(emptyList()) }
            chunks.forEachIndexed { chunkIndex, chunk ->
                val title = if (chunkIndex == 0) section.title else "${section.title} (cont.)"
                slides.add(
                    PptxBuilder.Slide.Text(
                        title = title,
                        bullets = chunk,
                        pageLabel = String.format("%02d", index)
                    )
                )
                index++
            }
        }

        if (slides.size == 1) {
            // Only a cover: nothing readable was found.
            return null
        }

        report(onProgress, 0.85f, "Typesetting ${slides.size} slides…")
        out.outputStream().use { PptxBuilder.build(template, slides, options.palette, SLIDE_W, SLIDE_H, it) }
        report(onProgress, 1f, "Done")
        return Result(slides.size, usedOcr)
    }

    /* ----------------------------- heuristics ----------------------------- */

    private data class Section(val title: String, val bullets: MutableList<String>)

    private fun buildSections(pages: List<List<PdfEngine.PdfLine>>): List<Section> {
        val sizes = pages.flatten().map { it.size }.filter { it > 0f }.sorted()
        val median = if (sizes.isEmpty()) 10f else sizes[sizes.size / 2]
        val headingThreshold = maxOf(median * 1.12f, median + 1.4f)

        val sections = ArrayList<Section>()

        pages.forEachIndexed { pageIndex, lines ->
            if (lines.isEmpty()) return@forEachIndexed

            val headings = lines.filter { isHeading(it, headingThreshold) }
            var current: Section? = null

            if (headings.isEmpty()) {
                val bullets = ArrayList<String>()
                lines.forEach { line ->
                    if (!isNoise(line.text)) bullets.add(line.text)
                }
                if (bullets.isNotEmpty()) {
                    sections.add(Section("Page ${pageIndex + 1}", bullets))
                }
                return@forEachIndexed
            }

            lines.forEach { line ->
                if (isHeading(line, headingThreshold)) {
                    val title = cleanHeading(line.text)
                    if (title.length >= 3) {
                        val section = Section(title, mutableListOf())
                        sections.add(section)
                        current = section
                    }
                } else if (!isNoise(line.text)) {
                    var active = current
                    if (active == null) {
                        val fallback = cleanHeading(headings.first().text)
                            .ifBlank { "Page ${pageIndex + 1}" }
                        active = Section(fallback, mutableListOf())
                        sections.add(active)
                        current = active
                    }
                    active.bullets.add(line.text)
                }
            }
        }

        // Drop empty sections and cap the deck length.
        return sections
            .filter { it.title.isNotBlank() }
            .take(MAX_SLIDES)
            .map { s ->
                Section(
                    s.title,
                    s.bullets
                        .filter { it.length > 2 }
                        .distinct()
                        .take(21)
                        .toMutableList()
                )
            }
    }

    private fun isHeading(line: PdfEngine.PdfLine, threshold: Float): Boolean {
        val t = line.text.trim()
        if (t.length < 3 || t.length > 90) return false
        if (isNoise(t)) return false
        val big = line.size >= threshold
        val caps = t.length <= 64 && t.uppercase() == t && t.count { it.isLetter() } >= 4
        val numbered = Regex("^(\\d+[.)]|[IVXLC]+[.)])\\s+\\S").containsMatchIn(t) && line.size >= threshold * 0.95f
        return big || caps || numbered
    }

    private fun cleanHeading(text: String): String =
        text.replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', ':', '-', '|')
            .take(90)

    private fun splitBullet(text: String): List<String> {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= 170) return if (clean.isEmpty()) emptyList() else listOf(clean)
        val sentences = clean.split(Regex("(?<=[.!?])\\s+"))
        val out = ArrayList<String>()
        val sb = StringBuilder()
        for (sentence in sentences) {
            if (sb.isNotEmpty() && sb.length + sentence.length > 170) {
                out.add(sb.toString().trim())
                sb.clear()
            }
            if (sentence.length > 220) {
                if (sb.isNotEmpty()) {
                    out.add(sb.toString().trim())
                    sb.clear()
                }
                sentence.chunked(200).forEach { out.add(it.trim()) }
            } else {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(sentence)
            }
        }
        if (sb.isNotEmpty()) out.add(sb.toString().trim())
        return out.filter { it.isNotBlank() }
    }

    private fun isNoise(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return true
        if (Regex("^\\d{1,3}(\\s*/\\s*\\d{1,3})?$").matches(t)) return true
        if (Regex("^[\\-–—_•·.\\s]+$").matches(t)) return true
        if (Regex("^page\\s+\\d+.*$", RegexOption.IGNORE_CASE).matches(t)) return true
        return false
    }

    private suspend fun report(callback: suspend (Float, String) -> Unit, progress: Float, message: String) {
        withContext(Dispatchers.Main) { callback(progress, message) }
    }
}
