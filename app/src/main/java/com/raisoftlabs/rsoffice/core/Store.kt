package com.raisoftlabs.rsoffice.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** All on-device file handling: import, list, share, export. */
object Store {

    fun root(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "RS Office").apply { if (!exists()) mkdirs() }
    }

    fun uniqueFile(dir: File, base: String, ext: String): File {
        if (!dir.exists()) dir.mkdirs()
        var f = File(dir, "$base.$ext")
        var i = 2
        while (f.exists()) {
            f = File(dir, "${base}_$i.$ext")
            i++
        }
        return f
    }

    fun queryName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    } catch (_: Exception) {
        null
    }

    /** Copies a SAF uri into the app library so every engine works on a real file. */
    fun importUri(context: Context, uri: Uri, fallback: String, forceExt: String? = null): File? {
        return try {
            val rawName = queryName(context, uri) ?: "$fallback-${System.currentTimeMillis()}"
            val cleaned = rawName.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(64)
            val ext = forceExt ?: cleaned.substringAfterLast('.', "").ifEmpty { "bin" }
            val base = cleaned.substringBeforeLast('.').ifEmpty { fallback }
            val dest = uniqueFile(root(context), base, ext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out, 64 * 1024) }
            } ?: return null
            dest
        } catch (_: Exception) {
            null
        }
    }

    fun list(context: Context): List<File> {
        val dir = root(context)
        return (dir.listFiles() ?: emptyArray())
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
    }

    fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "txt" -> "text/plain"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> "application/octet-stream"
    }

    fun share(context: Context, file: File, mime: String = mimeOf(file.name)) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
        } catch (_: Exception) {
        }
    }

    fun shareMultiple(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        try {
            val uris = ArrayList<android.net.Uri>()
            files.forEach { f ->
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeOf(files.first().name)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${files.size} files"))
        } catch (_: Exception) {
        }
    }

    fun openWith(context: Context, file: File, mime: String = mimeOf(file.name)) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open ${file.name}"))
        } catch (_: Exception) {
            share(context, file, mime)
        }
    }

    /** Exports a copy into the public Downloads folder (Android 10+). */
    fun saveToDownloads(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return try {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "RS Office"
                )
                if (!dir.exists()) dir.mkdirs()
                file.copyTo(File(dir, file.name), overwrite = true)
                true
            } catch (_: Exception) {
                false
            }
        }
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeOf(file.name))
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/RS Office")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out, 64 * 1024) }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun prettySize(bytes: Long): String = when {
        bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000f)
        bytes >= 1000 -> String.format(Locale.US, "%d KB", bytes / 1000)
        else -> "$bytes B"
    }

    fun prettyDate(millis: Long): String =
        SimpleDateFormat("d MMM · HH:mm", Locale.US).format(Date(millis))

    fun prettyName(file: File): String =
        file.name.substringBeforeLast('.').replace('_', ' ').trim()

    fun extension(file: File): String = file.extension.lowercase()
}
