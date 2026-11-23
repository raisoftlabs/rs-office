package com.raisoftlabs.rsoffice.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun File.quickMeta(): String {
    val size = when {
        length() >= 1_000_000 -> String.format(Locale.US, "%.1f MB", length() / 1_000_000f)
        length() >= 1000 -> "${length() / 1000} KB"
        else -> "${length()} B"
    }
    val date = SimpleDateFormat("d MMM, HH:mm", Locale.US).format(Date(lastModified()))
    return "$size · $date"
}

fun typeLabel(file: File): String = when (file.extension.lowercase()) {
    "pdf" -> "PDF"
    "pptx" -> "PPT"
    "docx" -> "DOC"
    "jpg", "jpeg" -> "JPG"
    "png" -> "PNG"
    "txt" -> "TXT"
    "zip" -> "ZIP"
    else -> file.extension.uppercase().take(4).ifBlank { "FILE" }
}
