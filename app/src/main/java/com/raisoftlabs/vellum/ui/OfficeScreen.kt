package com.raisoftlabs.vellum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raisoftlabs.vellum.core.DocxReader
import com.raisoftlabs.vellum.core.PptxReader
import com.raisoftlabs.vellum.core.Store
import com.raisoftlabs.vellum.core.XlsxEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class OfficeKind { WORD, SHEET, SLIDES, OTHER }

private fun kindOf(file: File): OfficeKind = when (file.extension.lowercase()) {
    "docx" -> OfficeKind.WORD
    "xlsx", "xls" -> OfficeKind.SHEET
    "pptx" -> OfficeKind.SLIDES
    else -> OfficeKind.OTHER
}

/**
 * Universal viewer for Word, Excel and PowerPoint files. Everything is parsed
 * from the file itself on this phone — no servers, no conversions uploaded.
 */
@Composable
fun OfficeScreen(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kind = remember(file.path) { kindOf(file) }

    var loading by remember(file.path) { mutableStateOf(true) }
    var error by remember(file.path) { mutableStateOf<String?>(null) }
    var exporting by remember(file.path) { mutableStateOf(false) }
    var blocks by remember(file.path) { mutableStateOf<List<DocxReader.Block>>(emptyList()) }
    var sheets by remember(file.path) { mutableStateOf<List<XlsxEngine.Sheet>>(emptyList()) }
    var slides by remember(file.path) { mutableStateOf<List<PptxReader.Slide>>(emptyList()) }
    var sheetIndex by remember(file.path) { mutableIntStateOf(0) }

    LaunchedEffect(file.path) {
        loading = true
        error = null
        withContext(Dispatchers.IO) {
            try {
                when (kind) {
                    OfficeKind.WORD -> blocks = DocxReader.read(file)
                    OfficeKind.SHEET -> sheets = XlsxEngine.read(file)
                    OfficeKind.SLIDES -> slides = PptxReader.read(file)
                    OfficeKind.OTHER -> error = "This file type cannot be read yet."
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not read this file."
            }
        }
        if (error == null) {
            when (kind) {
                OfficeKind.WORD -> if (blocks.isEmpty()) error = "This document has no readable text."
                OfficeKind.SHEET -> if (sheets.isEmpty()) error = "This spreadsheet has no readable cells."
                OfficeKind.SLIDES -> if (slides.isEmpty()) error = "This presentation has no readable slides."
                OfficeKind.OTHER -> Unit
            }
        }
        loading = false
    }

    fun exportSheetAsPdf() {
        if (sheets.isEmpty() || exporting) return
        exporting = true
        scope.launch {
            val out = Store.uniqueFile(Store.root(context), file.nameWithoutExtension, "pdf")
            val ok = withContext(Dispatchers.IO) {
                runCatching { XlsxEngine.toPdf(sheets, out) }.isSuccess
            }
            exporting = false
            if (ok) {
                context.toast("PDF saved · ${out.name}")
                Store.share(context, out)
            } else {
                context.toast("Could not export this sheet.")
            }
        }
    }

    val subtitle = when (kind) {
        OfficeKind.WORD -> "${blocks.size} paragraphs"
        OfficeKind.SHEET -> {
            val sheet = sheets.getOrNull(sheetIndex)
            if (sheet == null) "Spreadsheet" else "${sheet.name} · ${sheet.rows.size} rows"
        }

        OfficeKind.SLIDES -> "${slides.size} slides"
        OfficeKind.OTHER -> "Document"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = file.name,
            subtitle = subtitle,
            onBack = onBack,
            actions = {
                if (kind == OfficeKind.SHEET && sheets.isNotEmpty()) {
                    IconButton(onClick = { exportSheetAsPdf() }) {
                        Icon(
                            Icons.Rounded.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { Store.share(context, file) }) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    val ok = Store.saveToDownloads(context, file)
                    context.toast(if (ok) "Saved to Downloads/Vellum" else "Could not save")
                }) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            }

            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                GlassCard(radius = 22.dp, padding = 20.dp) {
                    Text(
                        "Cannot open this file",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "You can still share the original file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            kind == OfficeKind.WORD -> WordBody(blocks)
            kind == OfficeKind.SHEET -> SheetBody(sheets, sheetIndex) { sheetIndex = it }
            kind == OfficeKind.SLIDES -> SlideBody(slides)
        }
    }
}

@Composable
private fun WordBody(blocks: List<DocxReader.Block>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp)
    ) {
        items(blocks.size) { index ->
            val block = blocks[index]
            if (block.heading) {
                Text(
                    block.text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                )
            } else {
                Text(
                    block.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
        item {
            Text(
                "Read-only view · ${blocks.size} paragraphs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SheetBody(sheets: List<XlsxEngine.Sheet>, index: Int, onPick: (Int) -> Unit) {
    val sheet = sheets.getOrNull(index.coerceIn(0, (sheets.size - 1).coerceAtLeast(0))) ?: return
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        if (sheets.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sheets.size) { i ->
                    SelectChip(text = sheets[i].name, selected = i == index, onClick = { onPick(i) })
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .verticalScroll(vertical)
        ) {
            Column(modifier = Modifier.horizontalScroll(horizontal).padding(12.dp)) {
                sheet.rows.take(160).forEachIndexed { rowIndex, row ->
                    TableRow(cells = row, header = rowIndex == 0)
                }
                if (sheet.rows.size > 160) {
                    Text(
                        "Showing 160 of ${sheet.rows.size} rows",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        cells.forEachIndexed { column, cell ->
            Text(
                cell,
                modifier = Modifier
                    .width(if (column == 0) 128.dp else 112.dp)
                    .background(
                        if (header) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 7.dp, vertical = 7.dp),
                style = if (header) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                color = if (header) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SlideBody(slides: List<PptxReader.Slide>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(slides.size) { index ->
            val slide = slides[index]
            GlassCard(radius = 20.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(PptOrange, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${slide.number}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Slide ${slide.number}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (slide.lines.isEmpty()) {
                    Text(
                        "Image-only slide",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    slide.lines.take(10).forEachIndexed { i, line ->
                        Text(
                            if (i == 0) line else "•  $line",
                            style = if (i == 0) MaterialTheme.typography.titleSmall
                            else MaterialTheme.typography.bodySmall,
                            color = if (i == 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Read-only view · open the file in PowerPoint to edit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
