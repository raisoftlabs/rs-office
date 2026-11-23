package com.raisoftlabs.rsoffice.core

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/** On-device OCR through ML Kit (bundled Latin model, no network). */
object Ocr {

    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun recognize(bitmap: Bitmap): String = try {
        client.process(InputImage.fromBitmap(bitmap, 0)).await().text
    } catch (_: Exception) {
        ""
    }

    /** OCRs a PDF (first 30 pages) or a single image file. */
    suspend fun fileText(file: File): String {
        return if (file.extension.equals("pdf", true)) {
            val builder = StringBuilder()
            PdfRender(file).use { render ->
                val pages = render.pageCount.coerceAtMost(30)
                for (i in 0 until pages) {
                    val bitmap = render.render(i, 1500)
                    val text = recognize(bitmap)
                    bitmap.recycle()
                    if (text.isNotBlank()) {
                        builder.append("— Page ${i + 1} —\n")
                            .append(text.trim())
                            .append("\n\n")
                    }
                }
            }
            builder.toString().trim()
        } else {
            val bitmap = PdfEngine.decodeForPdf(file)
            val text = recognize(bitmap)
            bitmap.recycle()
            text.trim()
        }
    }

    fun close() {
        try {
            client.close()
        } catch (_: Exception) {
        }
    }
}
