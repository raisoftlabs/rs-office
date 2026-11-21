package com.raisoftlabs.vellum.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.exifinterface.media.ExifInterface
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.File
import java.util.zip.ZipFile

/**
 * PDF and Office-document operations built on pdfbox-android plus Android's
 * own PdfDocument writer. Everything stays on the device.
 */
object PdfEngine {

    data class PdfLine(val text: String, val size: Float)

    private const val A4_W = 595
    private const val A4_H = 842

    /* --------------------------- text extraction --------------------------- */

    private class FontSizeStripper : PDFTextStripper() {
        val lines = ArrayList<PdfLine>()

        override fun writeString(text: String, textPositions: MutableList<TextPosition>?) {
            val clean = text.replace('\u00A0', ' ').replace(Regex("[ \\t]+"), " ").trim()
            if (clean.isEmpty()) return
            var max = 0f
            textPositions?.forEach { p ->
                if (p.fontSizeInPt > max) max = p.fontSizeInPt
            }
            lines.add(PdfLine(clean, max))
        }
    }

    /** Every page's text lines together with the largest font size on the line. */
    fun pageTexts(file: File): List<List<PdfLine>> {
        val result = ArrayList<List<PdfLine>>()
        PDDocument.load(file).use { doc ->
            for (page in 1..doc.numberOfPages) {
                val stripper = FontSizeStripper()
                stripper.sortByPosition = true
                stripper.startPage = page
                stripper.endPage = page
                try {
                    stripper.getText(doc)
                } catch (_: Exception) {
                }
                result.add(stripper.lines)
            }
        }
        return result
    }

    fun plainText(file: File): String =
        pageTexts(file).joinToString("\n\n") { page ->
            page.joinToString("\n") { it.text }
        }

    /* ------------------------------- merging ------------------------------- */

    fun merge(inputs: List<File>, out: File) {
        require(inputs.isNotEmpty()) { "no input files" }
        val merger = PDFMergerUtility()
        inputs.forEach { merger.addSource(it) }
        merger.destinationFileName = out.absolutePath
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
    }

    /* ------------------------------- splitting ----------------------------- */

    fun splitEachPage(file: File, outDir: File): List<File> {
        if (!outDir.exists()) outDir.mkdirs()
        val outputs = ArrayList<File>()
        PDDocument.load(file).use { doc ->
            val splitter = Splitter()
            splitter.split(doc).forEachIndexed { index, page ->
                PDDocument().use { single ->
                    single.addPage(page)
                    val out = File(outDir, "${file.nameWithoutExtension}_p${index + 1}.pdf")
                    single.save(out)
                    outputs.add(out)
                }
            }
        }
        return outputs
    }

    fun extractRange(file: File, from: Int, to: Int, out: File) {
        PDDocument.load(file).use { src ->
            val start = from.coerceIn(1, src.numberOfPages)
            val end = to.coerceIn(start, src.numberOfPages)
            PDDocument().use { dst ->
                for (i in start..end) dst.importPage(src.getPage(i - 1))
                dst.save(out)
            }
        }
    }

    /* ---------------------------- images <-> pdf --------------------------- */

    fun imagesToPdf(images: List<File>, out: File) {
        require(images.isNotEmpty()) { "no images" }
        val doc = PdfDocument()
        images.forEachIndexed { index, image ->
            val bitmap = decodeForPdf(image)
            val pageInfo = PdfDocument.PageInfo.Builder(A4_W, A4_H, index + 1).create()
            val page = doc.startPage(pageInfo)
            val scale = minOf(A4_W.toFloat() / bitmap.width, A4_H.toFloat() / bitmap.height)
            val w = bitmap.width * scale
            val h = bitmap.height * scale
            val left = (A4_W - w) / 2f
            val top = (A4_H - h) / 2f
            page.canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), null)
            doc.finishPage(page)
            bitmap.recycle()
        }
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    /** Rebuilds a PDF from rendered pages at a lower resolution (real size win). */
    fun rasterize(render: PdfRender, out: File, targetWidth: Int) {
        val doc = PdfDocument()
        for (i in 0 until render.pageCount) {
            val bitmap = render.render(i, targetWidth)
            val pageInfo = PdfDocument.PageInfo.Builder(A4_W, A4_H, i + 1).create()
            val page = doc.startPage(pageInfo)
            val scale = minOf(A4_W.toFloat() / bitmap.width, A4_H.toFloat() / bitmap.height)
            val w = bitmap.width * scale
            val h = bitmap.height * scale
            val left = (A4_W - w) / 2f
            val top = (A4_H - h) / 2f
            page.canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), null)
            doc.finishPage(page)
            bitmap.recycle()
        }
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    fun decodeForPdf(file: File, maxWidth: Int = 2400): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth > 0 && bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: throw IllegalStateException("Cannot read ${file.name}")
        val rotation = try {
            when (ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }
        if (rotation != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            bitmap = rotated
        }
        return bitmap
    }

    /* -------------------------------- docx -------------------------------- */

    fun docxText(file: File): String {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml") ?: return ""
            val raw = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            var text = raw
                .replace(Regex("<w:tab[^>]*/>"), "\t")
                .replace(Regex("<w:br[^>]*/>"), "\n")
                .replace(Regex("</w:p>"), "\n")
                .replace(Regex("<[^>]+>"), "")
            text = text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace(Regex("\n{3,}"), "\n\n")
            return text.trim()
        }
    }
}
