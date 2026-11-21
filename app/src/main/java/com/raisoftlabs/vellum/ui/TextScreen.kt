package com.raisoftlabs.vellum.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.raisoftlabs.vellum.core.Store
import java.io.File

@Composable
fun TextScreen(
    title: String,
    body: String,
    suggestedName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    fun writeFile(): File {
        val out = Store.uniqueFile(Store.root(context), suggestedName.ifBlank { "text" }, "txt")
        out.writeText(body)
        return out
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)
    ) {
        item {
            TopBar(
                title = title,
                subtitle = "${body.length} characters",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(body)); context.toast("Copied") }) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { Store.share(context, writeFile(), "text/plain") }) {
                        Icon(
                            Icons.Rounded.Share,
                            "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Panel(padding = 18.dp) {
                SelectionContainer {
                    Text(
                        body.ifBlank { "No text found." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientButton(
                    text = "Save .txt",
                    onClick = {
                        val file = writeFile()
                        val ok = Store.saveToDownloads(context, file)
                        context.toast(if (ok) "Saved to Downloads/Vellum" else "Saved in Vellum library")
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Download
                )
                GradientButton(
                    text = "Share",
                    onClick = { Store.share(context, writeFile(), "text/plain") },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Share
                )
            }
        }
    }
}
