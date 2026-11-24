package com.raisoftlabs.rsoffice.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raisoftlabs.rsoffice.core.OfficeFactory
import com.raisoftlabs.rsoffice.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CreatorScreen(
    kind: ToolId,
    onBack: () -> Unit,
    onCreated: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isSheet = kind == ToolId.NEW_EXCEL
    val titleLabel = if (isSheet) "Sheet name" else "Title"
    val contentLabel = when {
        isSheet -> "Rows (one per line, comma or tab separated)"
        kind == ToolId.NEW_POWERPOINT -> "Bullets (one per line)"
        else -> "Content"
    }
    val ext = when (kind) {
        ToolId.NEW_WORD -> "docx"
        ToolId.NEW_EXCEL -> "xlsx"
        ToolId.NEW_POWERPOINT -> "pptx"
        else -> "pdf"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TopBar(
            title = kind.title,
            subtitle = "Saved to your RS Office library",
            onBack = onBack
        )
        Spacer(Modifier.height(8.dp))
        Panel {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(titleLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(contentLabel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSheet) 190.dp else 230.dp)
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        GradientButton(
            text = "Create ${ext.uppercase()}",
            icon = iconFor(kind),
            onClick = {
                if (busy) return@GradientButton
                busy = true
                error = null
                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        runCatching {
                            val base = title.ifBlank { "Untitled" }
                                .replace(Regex("[^A-Za-z0-9._ -]"), "_")
                                .take(48)
                                .ifBlank { "Untitled" }
                            val out = Store.uniqueFile(Store.root(context), base, ext)
                            when (kind) {
                                ToolId.NEW_WORD -> OfficeFactory.createWord(out, title, content)
                                ToolId.NEW_EXCEL -> OfficeFactory.createExcel(
                                    out,
                                    title.ifBlank { "Sheet1" },
                                    parseRows(content)
                                )
                                ToolId.NEW_POWERPOINT -> OfficeFactory.createPowerPoint(
                                    context,
                                    out,
                                    title.ifBlank { "Untitled presentation" },
                                    content
                                )
                                else -> error("Unsupported creator")
                            }
                            out
                        }.getOrElse {
                            error = it.message ?: "Could not create the file"
                            null
                        }
                    }
                    busy = false
                    if (file != null) onCreated(file)
                }
            }
        )
        Spacer(Modifier.height(18.dp))
    }
    BusyOverlay(busy, "Creating file", "Writing original Office parts offline…")
}

private fun parseRows(text: String): List<List<String>> {
    if (text.isBlank()) return emptyList()
    return text.replace("\r\n", "\n").lines().map { line ->
        val separator = if (line.contains('\t')) "\t" else ","
        line.split(separator).map { it.trim() }
    }
}
