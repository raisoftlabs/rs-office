package com.raisoftlabs.rsoffice.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
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
import com.raisoftlabs.rsoffice.core.PdfEngine
import com.raisoftlabs.rsoffice.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TextToPdfScreen(
    onBack: () -> Unit,
    onDone: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)
    ) {
        item { TopBar(title = "Text to PDF", subtitle = "Type once, save as a real PDF", onBack = onBack) }
        item { Spacer(Modifier.height(6.dp)) }

        item {
            Panel {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Text") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { Spacer(Modifier.height(14.dp)) }

        item {
            GradientButton(
                text = "Create PDF",
                onClick = {
                    busy = true
                    scope.launch {
                        val output = withContext(Dispatchers.IO) {
                            buildPdf(context, title, body)
                        }
                        busy = false
                        if (output != null) onDone(output)
                    }
                },
                icon = Icons.Rounded.Description,
                enabled = !busy
            )
        }

        item {
            Text(
                "Long text is wrapped onto as many A4 pages as it needs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp)
            )
        }
    }

    BusyOverlay(visible = busy, title = "Writing PDF", message = "Formatting your text…")
}

private fun buildPdf(
    context: android.content.Context,
    title: String,
    body: String
): File? = runCatching {
    val base = title.filter { it.isLetterOrDigit() || it == ' ' }.trim().ifBlank { "document" }
    val out = Store.uniqueFile(Store.root(context), base.take(40), "pdf")
    PdfEngine.textToPdf(body, out, title.trim())
    out
}.getOrNull()
