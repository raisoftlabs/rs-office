package com.raisoftlabs.rsoffice.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raisoftlabs.rsoffice.core.Ocr
import com.raisoftlabs.rsoffice.core.PdfEngine
import com.raisoftlabs.rsoffice.core.PdfRender
import com.raisoftlabs.rsoffice.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ToolScreen(
    tool: ToolId,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onTextReady: (title: String, body: String, suggestedName: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var picked by remember { mutableStateOf<List<File>>(emptyList()) }
    var outputs by remember { mutableStateOf<List<File>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<String?>(null) }
    var rangeStart by remember { mutableStateOf("1") }
    var rangeEnd by remember { mutableStateOf("") }
    var quality by remember { mutableIntStateOf(1) }

    fun importOne(uri: Uri, fallback: String, ext: String? = null) {
        scope.launch {
            busy = true
            status = "Importing…"
            error = null
            val file = withContext(Dispatchers.IO) { Store.importUri(context, uri, fallback, ext) }
            busy = false
            if (file != null) {
                picked = listOf(file)
                outputs = emptyList()
                summary = null
            } else {
                error = "That file could not be read."
            }
        }
    }

    fun importMany(uris: List<Uri>) {
        scope.launch {
            busy = true
            status = "Importing ${uris.size} files…"
            error = null
            val files = withContext(Dispatchers.IO) {
                uris.mapNotNull { Store.importUri(context, it, "file") }
            }
            busy = false
            if (files.isNotEmpty()) {
                picked = files
                outputs = emptyList()
                summary = null
            } else {
                error = "Those files could not be read."
            }
        }
    }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importOne(uri, "document", "pdf")
    }
    val pickManyPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) importMany(uris)
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) importMany(uris)
    }
    val pickAny = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importOne(uri, "document")
    }

    fun run(action: String, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        status = action
        error = null
        summary = null
        outputs = emptyList()
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong"
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)
    ) {
        item {
            TopBar(title = tool.title, subtitle = tool.subtitle, onBack = onBack)
        }

        item { Spacer(Modifier.height(6.dp)) }

        item {
            Panel {
                Text(
                    when (tool) {
                        ToolId.MERGE -> "Pick two or more PDFs. They are joined in the order you select them."
                        ToolId.SPLIT -> "Pull one PDF apart: every page on its own, or a single page range."
                        ToolId.COMPRESS -> "Rebuilds the PDF at a lower resolution — big wins on scans and photos."
                        ToolId.TO_IMAGES -> "Renders every PDF page to a sharp PNG you can reuse anywhere."
                        ToolId.IMAGES_TO_PDF -> "Turn photos and screenshots into a clean A4 PDF."
                        ToolId.OCR -> "Reads text from a PDF page or a photo. Copy, save or share the result."
                        ToolId.DOCX -> "Opens the text of a Word document without uploading it anywhere."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(14.dp))

                if (picked.isEmpty()) {
                    val label = when (tool) {
                        ToolId.MERGE -> "Choose PDFs"
                        ToolId.IMAGES_TO_PDF -> "Choose images"
                        ToolId.OCR -> "Choose PDF or photo"
                        ToolId.DOCX -> "Choose a .docx"
                        else -> "Choose a PDF"
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton(label, Icons.Rounded.Description, Modifier.weight(1f)) {
                            when (tool) {
                                ToolId.MERGE -> pickManyPdf.launch(arrayOf("application/pdf"))
                                ToolId.IMAGES_TO_PDF -> pickImages.launch(arrayOf("image/*"))
                                ToolId.OCR -> pickAny.launch(arrayOf("application/pdf", "image/*"))
                                ToolId.DOCX -> pickAny.launch(arrayOf("*/*"))
                                else -> pickPdf.launch(arrayOf("application/pdf"))
                            }
                        }
                        if (tool == ToolId.OCR) {
                            ActionButton("Camera", Icons.Rounded.PhotoCamera, Modifier.weight(1f), onScan)
                        }
                    }
                } else {
                    picked.take(4).forEach { file ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Description,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    file.quickMeta(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (picked.size > 4) {
                        Text(
                            "+${picked.size - 4} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Change selection",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                picked = emptyList()
                                outputs = emptyList()
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }
            }
        }

        if (tool == ToolId.SPLIT) {
            item { SectionLabel("Range", Modifier.padding(top = 16.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = rangeStart,
                        onValueChange = { v -> rangeStart = v.filter { it.isDigit() }.take(3) },
                        label = { Text("From") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rangeEnd,
                        onValueChange = { v -> rangeEnd = v.filter { it.isDigit() }.take(3) },
                        label = { Text("To") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Leave “To” empty to split every page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (tool == ToolId.COMPRESS) {
            item { SectionLabel("Strength", Modifier.padding(top = 16.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    listOf("Light", "Balanced", "Strong").forEachIndexed { index, label ->
                        SelectChip(
                            text = label,
                            selected = quality == index,
                            onClick = { quality = index },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(18.dp)) }

        item {
            val enabled = picked.isNotEmpty() && !busy &&
                    (tool != ToolId.MERGE || picked.size >= 2)
            GradientButton(
                text = when (tool) {
                    ToolId.MERGE -> "Merge ${picked.size} PDFs"
                    ToolId.SPLIT -> "Split PDF"
                    ToolId.COMPRESS -> "Compress PDF"
                    ToolId.TO_IMAGES -> "Export pages as PNG"
                    ToolId.IMAGES_TO_PDF -> "Create PDF"
                    ToolId.OCR -> "Read text"
                    ToolId.DOCX -> "Open text"
                    else -> "Run"
                },
                enabled = enabled,
                onClick = {
                    when (tool) {
                        ToolId.MERGE -> run("Merging…") {
                            val out = Store.uniqueFile(Store.root(context), "merged", "pdf")
                            withContext(Dispatchers.IO) { PdfEngine.merge(picked, out) }
                            outputs = listOf(out)
                            summary = "${picked.size} PDFs merged."
                        }

                        ToolId.SPLIT -> run("Splitting…") {
                            val src = picked.first()
                            val end = rangeEnd.toIntOrNull()
                            if (end == null) {
                                val dir = File(Store.root(context), "${src.nameWithoutExtension}_pages")
                                val files = withContext(Dispatchers.IO) {
                                    PdfEngine.splitEachPage(src, dir)
                                }
                                outputs = files
                                summary = "${files.size} separate PDFs created."
                            } else {
                                val start = rangeStart.toIntOrNull() ?: 1
                                val out = Store.uniqueFile(
                                    Store.root(context),
                                    "${src.nameWithoutExtension}_p${start}-${end}",
                                    "pdf"
                                )
                                withContext(Dispatchers.IO) {
                                    PdfEngine.extractRange(src, start, end, out)
                                }
                                outputs = listOf(out)
                                summary = "Pages $start–$end extracted."
                            }
                        }

                        ToolId.COMPRESS -> run("Compressing…") {
                            val src = picked.first()
                            val before = src.length()
                            val out = Store.uniqueFile(
                                Store.root(context),
                                "${src.nameWithoutExtension}_compressed",
                                "pdf"
                            )
                            val width = when (quality) {
                                0 -> 1500
                                1 -> 1150
                                else -> 850
                            }
                            withContext(Dispatchers.IO) {
                                PdfRender(src).use { render ->
                                    PdfEngine.rasterize(render, out, width)
                                }
                            }
                            val after = out.length()
                            val saved = if (before > 0) ((before - after) * 100 / before) else 0
                            outputs = listOf(out)
                            summary = if (saved > 0) {
                                "${Store.prettySize(before)} → ${Store.prettySize(after)}  ($saved% smaller)"
                            } else {
                                "Rebuilt at ${Store.prettySize(after)}."
                            }
                        }

                        ToolId.TO_IMAGES -> run("Rendering pages…") {
                            val src = picked.first()
                            val dir = File(
                                Store.root(context),
                                "${src.nameWithoutExtension}_images_${System.currentTimeMillis() / 1000}"
                            )
                            dir.mkdirs()
                            val files = withContext(Dispatchers.IO) {
                                val list = ArrayList<File>()
                                PdfRender(src).use { render ->
                                    for (i in 0 until render.pageCount) {
                                        val bitmap = render.render(i, 1500)
                                        val file = File(dir, "${src.nameWithoutExtension}_p${i + 1}.png")
                                        file.outputStream().use { out ->
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        bitmap.recycle()
                                        list.add(file)
                                    }
                                }
                                list
                            }
                            outputs = files
                            summary = "${files.size} page images exported."
                        }

                        ToolId.IMAGES_TO_PDF -> run("Building PDF…") {
                            val out = Store.uniqueFile(Store.root(context), "images", "pdf")
                            withContext(Dispatchers.IO) { PdfEngine.imagesToPdf(picked, out) }
                            outputs = listOf(out)
                            summary = "${picked.size} images placed in a PDF."
                        }

                        ToolId.OCR -> run("Reading text…") {
                            val src = picked.first()
                            val text = withContext(Dispatchers.IO) { ocrFile(src) }
                            if (text.isBlank()) {
                                error = "No readable text found. Try a sharper scan."
                            } else {
                                onTextReady(
                                    Store.prettyName(src),
                                    text,
                                    "${src.nameWithoutExtension}_text"
                                )
                            }
                        }

                        ToolId.DOCX -> run("Reading Word file…") {
                            val src = picked.first()
                            val text = withContext(Dispatchers.IO) { PdfEngine.docxText(src) }
                            if (text.isBlank()) {
                                error = "This .docx has no readable text."
                            } else {
                                onTextReady(
                                    Store.prettyName(src),
                                    text,
                                    "${src.nameWithoutExtension}_text"
                                )
                            }
                        }

                        else -> Unit
                    }
                }
            )
        }

        if (busy) {
            item {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    LinearProgressIndicator(
                        progress = { 0.45f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (error != null) {
            item {
                Panel(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        "That did not work",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (outputs.isNotEmpty()) {
            item {
                Panel(modifier = Modifier.padding(top = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (outputs.size == 1) "Done" else "${outputs.size} files ready",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (summary != null) {
                                Text(
                                    summary!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("Open", Icons.Rounded.OpenInNew, Modifier.weight(1f)) {
                            Store.openWith(context, outputs.first())
                        }
                        ActionButton("Share", Icons.Rounded.Share, Modifier.weight(1f)) {
                            if (outputs.size > 1) Store.shareMultiple(context, outputs)
                            else Store.share(context, outputs.first())
                        }
                        ActionButton("Save", Icons.Rounded.Download, Modifier.weight(1f)) {
                            var saved = 0
                            outputs.forEach { if (Store.saveToDownloads(context, it)) saved++ }
                            context.toast(
                                if (saved == outputs.size) "Saved to Downloads/RS Office"
                                else "Saved $saved of ${outputs.size} files"
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun ocrFile(src: File): String {
    return if (src.extension.lowercase() == "pdf") {
        val builder = StringBuilder()
        PdfRender(src).use { render ->
            val pages = render.pageCount.coerceAtMost(30)
            for (i in 0 until pages) {
                val bitmap = render.render(i, 1500)
                val text = Ocr.recognize(bitmap)
                bitmap.recycle()
                if (text.isNotBlank()) {
                    builder.append("— Page ${i + 1} —\n").append(text.trim()).append("\n\n")
                }
            }
        }
        builder.toString().trim()
    } else {
        val bitmap = PdfEngine.decodeForPdf(src)
        val text = Ocr.recognize(bitmap)
        bitmap.recycle()
        text.trim()
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
