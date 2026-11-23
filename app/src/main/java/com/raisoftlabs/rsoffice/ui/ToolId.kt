package com.raisoftlabs.rsoffice.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolGroup(val title: String) {
    CREATE("Create"),
    ORGANIZE("Organize"),
    CONVERT("Convert"),
    READ("Read"),
    EDIT("Edit")
}

enum class ToolId(val title: String, val subtitle: String, val group: ToolGroup) {
    SCAN("Scan", "Camera to PDF", ToolGroup.CREATE),
    DECK("Deck studio", "PDF to PowerPoint", ToolGroup.CREATE),
    IMAGES_TO_PDF("Images to PDF", "Photos to A4", ToolGroup.CREATE),
    MERGE("Merge", "Join PDFs", ToolGroup.ORGANIZE),
    SPLIT("Split", "Separate pages", ToolGroup.ORGANIZE),
    COMPRESS("Compress", "Shrink size", ToolGroup.ORGANIZE),
    TO_IMAGES("To images", "Pages to PNG", ToolGroup.CONVERT),
    OCR("OCR", "Read text", ToolGroup.READ),
    DOCX("Word", "Edit .docx", ToolGroup.EDIT),
    SHEET("Excel", "Edit .xlsx", ToolGroup.EDIT),
    SLIDES("PowerPoint", "Edit .pptx", ToolGroup.EDIT);

    val isOffice: Boolean get() = this == DOCX || this == SHEET || this == SLIDES
}

fun iconFor(id: ToolId): ImageVector = when (id) {
    ToolId.SCAN -> Icons.Rounded.PhotoCamera
    ToolId.DECK -> Icons.Rounded.AutoAwesome
    ToolId.IMAGES_TO_PDF -> Icons.Rounded.PictureAsPdf
    ToolId.MERGE -> Icons.Rounded.MergeType
    ToolId.SPLIT -> Icons.Rounded.ContentCut
    ToolId.COMPRESS -> Icons.Rounded.Compress
    ToolId.TO_IMAGES -> Icons.Rounded.Image
    ToolId.OCR -> Icons.Rounded.TextSnippet
    ToolId.DOCX -> Icons.Rounded.Description
    ToolId.SHEET -> Icons.Rounded.TableChart
    ToolId.SLIDES -> Icons.Rounded.Slideshow
}

fun colorsFor(id: ToolId): List<Color> = when (id) {
    ToolId.DOCX -> listOf(Color(0xFF3B6FC4), WordBlue)
    ToolId.SHEET -> listOf(Color(0xFF2E9B66), ExcelGreen)
    ToolId.SLIDES -> listOf(Color(0xFFE9693F), PptOrange)
    ToolId.DECK -> listOf(Amber, Color(0xFFFF6B00))
    ToolId.SCAN -> listOf(Indigo, Color(0xFF0060DF))
    ToolId.COMPRESS -> listOf(Color(0xFF00C7BE), Color(0xFF0A8F88))
    ToolId.MERGE -> listOf(Violet, Color(0xFF4B48C9))
    ToolId.SPLIT -> listOf(Teal, Color(0xFF0A84C9))
    ToolId.OCR -> listOf(Color(0xFFFF9F0A), Color(0xFFE06300))
    ToolId.TO_IMAGES -> listOf(Mint, Color(0xFF16904B))
    ToolId.IMAGES_TO_PDF -> listOf(Pink, Color(0xFFC0103F))
}

/** MIME types used by the system pickers. */
fun mimeFor(id: ToolId): String = when (id) {
    ToolId.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ToolId.SHEET -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ToolId.SLIDES -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    else -> "application/pdf"
}
