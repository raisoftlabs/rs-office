package com.raisoftlabs.rsoffice.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raisoftlabs.rsoffice.core.PptxBuilder
import com.raisoftlabs.rsoffice.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NewDeckScreen(
    onBack: () -> Unit,
    onDone: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var bullets by remember { mutableStateOf("") }
    var palette by remember { mutableStateOf(PptxBuilder.SLATE) }
    var busy by remember { mutableStateOf(false) }

    val palettes = listOf(
        "Slate" to PptxBuilder.SLATE,
        "Paper" to PptxBuilder.PAPER,
        "Neon" to PptxBuilder.NEON
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp
        )
    ) {
        item { TopBar(title = "New deck", subtitle = "Type it once, edit it anywhere", onBack = onBack) }
        item { Spacer(Modifier.height(6.dp)) }

        item {
            Panel {
                Text("Deck details", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = bullets,
                    onValueChange = { bullets = it },
                    label = { Text("Bullets (one per line)") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Panel {
                Text("Style", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    palettes.forEach { (label, option) ->
                        SelectChip(
                            text = label,
                            selected = palette == option,
                            onClick = { palette = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(14.dp)) }

        item {
            GradientButton(
                text = "Create PowerPoint",
                onClick = {
                    busy = true
                    scope.launch {
                        val output = withContext(Dispatchers.IO) {
                            buildDeck(context, title, subtitle, bullets, palette)
                        }
                        busy = false
                        if (output != null) onDone(output)
                    }
                },
                icon = Icons.Rounded.Add,
                enabled = !busy
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                "Saved as a real .pptx in RS Office. Every slide stays editable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }

    BusyOverlay(visible = busy, title = "Building deck", message = "Writing editable slides…")
}

private fun buildDeck(
    context: android.content.Context,
    title: String,
    subtitle: String,
    bullets: String,
    palette: PptxBuilder.Palette
): File? = runCatching {
    val template = context.assets.open("deck_template.pptx").use { it.readBytes() }
    val deckTitle = title.ifBlank { "Untitled deck" }
    val lines = bullets.lines().map { it.trim() }.filter { it.isNotBlank() }
        .ifEmpty { listOf("Add your first point") }
    val slides = mutableListOf<PptxBuilder.Slide>()
    slides += PptxBuilder.Slide.Cover(deckTitle, subtitle.trim())
    lines.chunked(5).forEachIndexed { index, chunk ->
        slides += PptxBuilder.Slide.Text(
            title = if (index == 0) "Agenda" else "More points",
            bullets = chunk,
            pageLabel = "${index + 2}"
        )
    }
    val base = deckTitle.filter { it.isLetterOrDigit() || it == ' ' }.trim().ifBlank { "deck" }
    val out = Store.uniqueFile(Store.root(context), base.take(40), "pptx")
    out.outputStream().use { PptxBuilder.build(template, slides, palette, out = it) }
    out
}.getOrNull()
