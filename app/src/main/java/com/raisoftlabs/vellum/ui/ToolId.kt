package com.raisoftlabs.vellum.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolId(val title: String, val subtitle: String) {
    DECK("Deck studio", "PDF to .pptx"),
    SCAN("Scan", "Camera to PDF"),
    MERGE("Merge", "Join PDFs"),
    SPLIT("Split", "Separate pages"),
    COMPRESS("Compress", "Shrink size"),
    OCR("OCR", "Read text"),
    TO_IMAGES("To images", "Pages to PNG"),
    IMAGES_TO_PDF("Images to PDF", "Photos to a page"),
    DOCX("Word reader", "Open .docx")
}

fun iconFor(id: ToolId): ImageVector = when (id) {
    ToolId.DECK -> Icons.Rounded.Slideshow
    ToolId.SCAN -> Icons.Rounded.PhotoCamera
    ToolId.MERGE -> Icons.Rounded.MergeType
    ToolId.SPLIT -> Icons.Rounded.ContentCut
    ToolId.COMPRESS -> Icons.Rounded.Compress
    ToolId.OCR -> Icons.Rounded.TextSnippet
    ToolId.TO_IMAGES -> Icons.Rounded.Image
    ToolId.IMAGES_TO_PDF -> Icons.Rounded.PictureAsPdf
    ToolId.DOCX -> Icons.Rounded.Description
}
