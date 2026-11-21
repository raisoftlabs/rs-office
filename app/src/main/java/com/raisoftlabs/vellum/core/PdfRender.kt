package com.raisoftlabs.vellum.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File

/**
 * Thin, thread-safe wrapper around Android's PdfRenderer.
 * One instance per file; render calls are serialised because the platform
 * renderer is not thread safe.
 */
class PdfRender(file: File) : Closeable {

    private val descriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

    private val renderer: PdfRenderer = PdfRenderer(descriptor)

    val pageCount: Int = renderer.pageCount

    fun pageAspect(index: Int): Float {
        if (pageCount == 0) return 1f
        synchronized(this) {
            renderer.openPage(index).use { page ->
                return if (page.height == 0) 1f else page.width.toFloat() / page.height.toFloat()
            }
        }
    }

    @Synchronized
    fun render(index: Int, targetWidth: Int): Bitmap {
        require(index in 0 until pageCount) { "page out of range" }
        renderer.openPage(index).use { page ->
            val scale = targetWidth.toFloat() / page.width.toFloat()
            val w = maxOf(1, (page.width * scale).toInt())
            val h = maxOf(1, (page.height * scale).toInt())
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    override fun close() {
        try {
            renderer.close()
        } catch (_: Exception) {
        }
        try {
            descriptor.close()
        } catch (_: Exception) {
        }
    }
}
