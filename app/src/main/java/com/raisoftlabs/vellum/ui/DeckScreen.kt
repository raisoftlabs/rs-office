package com.raisoftlabs.vellum.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.raisoftlabs.vellum.core.DeckMaker
import com.raisoftlabs.vellum.core.PptxBuilder
import com.raisoftlabs.vellum.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class DeckThemeOption(
    val label: String,
    val palette: PptxBuilder.Palette,
    val dot: Color
)

private val deckThemes = listOf(
    DeckThemeOption("Slate", PptxBuilder.SLATE, Color(0xFF1E293B)),
    DeckThemeOption("Paper", PptxBuilder.PAPER, Color(0xFFF0E8D8)),
    DeckThemeOption("Neon", PptxBuilder.NEON, Color(0xFF0E1014))
)

@Composable
fun DeckScreen(
    initialSource: File?,
    onBack: (() -> Unit)? = null,
    onScan: () -> Unit,
    onOpenFile: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var source by remember { mutableStateOf(initialSource) }
    var smart by remember { mutableStateOf(true) }
    var themeIndex by remember { mutableStateOf(0) }
    var ocr by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<File?>(null) }
    var slideCount by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            scope.launch {
                busy = true
                error = null
                status = "Importing…"
                val file = withContext(Dispatchers.IO) { Store.importUri(context, uri, "deck", "pdf") }
                busy = false
                if (file != null) {
                    source = file
                    output = null
                } else {
                    error = "That PDF could not be read."
                }
            }
        }
    }

    LaunchedEffect(initialSource) {
        if (initialSource != null) {
            source = initialSource
            output = null
            error = null
        }
    }

    fun generate() {
        val src = source ?: return
        busy = true
        error = null
        output = null
        progress = 0f
        status = "Preparing…"
        scope.launch {
            try {
                val base = src.nameWithoutExtension.take(40).ifBlank { "deck" }
                val out = Store.uniqueFile(Store.root(context), "${base}_deck", "pptx")
                val options = DeckMaker.Options(
                    mode = if (smart) DeckMaker.Mode.SMART else DeckMaker.Mode.EXACT,
                    palette = deckThemes[themeIndex].palette,
                    ocrScannedPages = ocr
                )
                val result = withContext(Dispatchers.IO) {
                    DeckMaker.make(context, src, options, out) { p, message ->
                        progress = p
                        status = message
                    }
                }
                output = out
                slideCount = result.slides
                note = when {
                    result.fellBackToExact -> "No readable text layer — pages were used as slides."
                    result.usedOcr -> "Scanned pages were read with on-device OCR."
                    else -> null
                }
            } catch (e: Exception) {
                error = e.message ?: "Conversion failed"
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp
        )
    ) {
        item {
            TopBar(
                title = "Deck studio",
                subtitle = "PDF in · editable .pptx out",
                onBack = onBack
            )
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Panel {
                if (source == null) {
                    Text(
                        "Choose a document",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "A PDF, a scanned handout, a lecture script — Vellum reads the structure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallAction("Pick PDF", Icons.Rounded.Description, Modifier.weight(1f)) {
                            pickPdf.launch(arrayOf("application/pdf"))
                        }
                        SmallAction("Scan", Icons.Rounded.PhotoCamera, Modifier.weight(1f), onScan)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "PDF",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                source!!.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            Text(
                                source!!.quickMeta(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { pickPdf.launch(arrayOf("application/pdf")) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item { SectionLabel("Layout", Modifier.padding(top = 18.dp)) }

        item {
            ModeCard(
                title = "Smart slides",
                body = "Detects headings and rebuilds them as title + bullets. Fully editable.",
                selected = smart,
                onClick = { smart = true }
            )
        }
        item { Spacer(Modifier.height(9.dp)) }
        item {
            ModeCard(
                title = "Exact pages",
                body = "Every page becomes a full-bleed slide. Pixel-perfect, not editable.",
                selected = !smart,
                onClick = { smart = false }
            )
        }

        item { SectionLabel("Theme", Modifier.padding(top = 18.dp)) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                deckThemes.forEachIndexed { index, option ->
                    ThemeChip(
                        option = option,
                        selected = themeIndex == index,
                        modifier = Modifier.weight(1f)
                    ) { themeIndex = index }
                }
            }
        }

        if (smart) {
            item { Spacer(Modifier.height(14.dp)) }
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Read scanned pages",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Runs OCR on pages that have no text layer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = ocr, onCheckedChange = { ocr = it })
                    }
                }
            }
        }

        item { Spacer(Modifier.height(18.dp)) }

        item {
            GradientButton(
                text = if (busy) "Building deck…" else "Generate presentation",
                onClick = { if (!busy && source != null) generate() },
                enabled = !busy && source != null,
                icon = Icons.Rounded.AutoAwesome
            )
        }

        if (busy) {
            item {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
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
                        "Could not build the deck",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }

        val done = output
        if (done != null) {
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
                            Text("✓", color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "$slideCount slides ready",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                done.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    if (note != null) {
                        Text(
                            note!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallAction("Open", Icons.Rounded.OpenInNew, Modifier.weight(1f)) {
                            onOpenFile(done)
                        }
                        SmallAction("Share", Icons.Rounded.Share, Modifier.weight(1f)) {
                            Store.share(context, done)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    option: DeckThemeOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(option.dot)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                option.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmallAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
