package com.raisoftlabs.rsoffice.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raisoftlabs.rsoffice.core.PdfEngine
import com.raisoftlabs.rsoffice.core.PdfRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class EditorPage(val sourceIndex: Int, val bitmap: Bitmap, val rotation: Int = 0)

/**
 * General PDF editor. Structural edits preserve the original page streams —
 * no rasterising, no quality loss. Sticky notes are standard PDF annotations.
 */
@Composable
fun PdfEditorScreen(
    file: File,
    onBack: () -> Unit,
    onDone: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember(file.path) { mutableStateOf(file) }
    var pages by remember(file.path) { mutableStateOf<List<EditorPage>>(emptyList()) }
    var selected by remember(file.path) { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var noteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    LaunchedEffect(working.path) {
        pages = withContext(Dispatchers.IO) {
            PdfRender(working).use { render ->
                (0 until render.pageCount).map { index ->
                    EditorPage(sourceIndex = index, bitmap = render.render(index, 360))
                }
            }
        }
        selected = 0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = "Edit PDF",
            subtitle = "${pages.size} page${if (pages.size == 1) "" else "s"} · rotate, reorder, note",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { noteDialog = true },
                    enabled = pages.isNotEmpty() && !busy
                ) {
                    Icon(Icons.Rounded.Comment, contentDescription = "Add note", tint = IndigoDeep)
                }
                IconButton(
                    onClick = {
                        if (pages.isEmpty() || busy) return@IconButton
                        busy = true
                        scope.launch {
                            val output = withContext(Dispatchers.IO) {
                                runCatching {
                                    val out = File(
                                        working.parentFile,
                                        "${working.nameWithoutExtension}_edited.pdf"
                                    )
                                    PdfEngine.applyPageOperations(
                                        working,
                                        out,
                                        order = pages.map { it.sourceIndex },
                                        rotations = pages.associate { it.sourceIndex to it.rotation }
                                    )
                                    out
                                }.getOrNull()
                            }
                            busy = false
                            if (output != null) onDone(output)
                        }
                    },
                    enabled = pages.isNotEmpty() && !busy
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Save PDF", tint = IndigoDeep)
                }
            }
        )

        if (pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val currentIndex = selected.coerceIn(0, pages.lastIndex)
            val current = pages[currentIndex]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = current.bitmap.asImageBitmap(),
                    contentDescription = "Page ${currentIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .graphicsLayer(rotationZ = current.rotation.toFloat())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorControl(Icons.Rounded.RotateLeft, "Rotate left") {
                    pages = pages.update(currentIndex) { it.copy(rotation = (it.rotation - 90).mod(360)) }
                }
                EditorControl(Icons.Rounded.RotateRight, "Rotate right") {
                    pages = pages.update(currentIndex) { it.copy(rotation = (it.rotation + 90).mod(360)) }
                }
                EditorControl(Icons.Rounded.ArrowBack, "Move earlier", enabled = currentIndex > 0) {
                    pages = pages.swap(currentIndex, currentIndex - 1)
                    selected = currentIndex - 1
                }
                EditorControl(
                    Icons.Rounded.ArrowForward,
                    "Move later",
                    enabled = currentIndex < pages.lastIndex
                ) {
                    pages = pages.swap(currentIndex, currentIndex + 1)
                    selected = currentIndex + 1
                }
                EditorControl(
                    Icons.Rounded.Delete,
                    "Delete page",
                    enabled = pages.size > 1,
                    tint = PdfRed
                ) {
                    pages = pages.filterIndexed { index, _ -> index != currentIndex }
                    selected = (currentIndex - 1).coerceAtLeast(0)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(pages) { index, page ->
                    val active = index == currentIndex
                    Surface(
                        modifier = Modifier.bounceClick { selected = index },
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) Indigo.copy(alpha = 0.12f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            if (active) 2.dp else 1.dp,
                            if (active) Indigo else Color(0xFFE8ECF6)
                        )
                    ) {
                        Box(contentAlignment = Alignment.BottomCenter) {
                            Image(
                                bitmap = page.bitmap.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(width = 62.dp, height = 82.dp)
                            )
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) IndigoDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.85f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (noteDialog) {
        AlertDialog(
            onDismissRequest = { noteDialog = false },
            title = { Text("Add sticky note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val text = noteText.trim()
                        if (text.isBlank() || pages.isEmpty()) {
                            noteDialog = false
                            return@TextButton
                        }
                        noteDialog = false
                        noteText = ""
                        busy = true
                        val pageIndex = pages[selected.coerceIn(0, pages.lastIndex)].sourceIndex
                        scope.launch {
                            val next = withContext(Dispatchers.IO) {
                                runCatching {
                                    val out = File(
                                        context.cacheDir,
                                        "note-${System.currentTimeMillis()}.pdf"
                                    )
                                    PdfEngine.addStickyNote(working, out, pageIndex, 48f, 48f, text)
                                    out
                                }.getOrNull()
                            }
                            busy = false
                            if (next != null) working = next
                        }
                    },
                    enabled = noteText.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { noteDialog = false }) { Text("Cancel") } }
        )
    }

    BusyOverlay(busy, "Saving PDF", "Writing your page edits…")
}

@Composable
private fun EditorControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color = IndigoDeep,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = label, tint = if (enabled) tint else Color(0xFFB9C0D4))
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFB9C0D4)
        )
    }
}

private fun <T> List<T>.update(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { i, item -> if (i == index) transform(item) else item }

private fun <T> List<T>.swap(first: Int, second: Int): List<T> {
    if (first !in indices || second !in indices) return this
    return toMutableList().apply {
        val item = removeAt(first)
        add(second, item)
    }
}
