package com.raisoftlabs.rsoffice.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun LibraryScreen(
    files: List<File>,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit,
    onScan: () -> Unit
) {
    var confirm by remember { mutableStateOf<File?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)
    ) {
        item {
            TopBar(
                title = "Library",
                subtitle = if (files.isEmpty()) "Nothing saved yet" else "${files.size} documents on this phone"
            )
        }

        item { Spacer(Modifier.height(8.dp)) }

        if (files.isEmpty()) {
            item {
                Panel {
                    EmptyState(
                        icon = Icons.Rounded.Folder,
                        title = "Your library is empty",
                        body = "Scans, PDFs and decks you create stay here, on your phone.",
                        actionLabel = "Scan a document",
                        onAction = onScan
                    )
                }
            }
        } else {
            items(files.size) { index ->
                val file = files[index]
                FileRow(
                    file = file,
                    onClick = { onOpen(file) },
                    onShare = { onShare(file) },
                    onDelete = { confirm = file }
                )
            }
            item {
                Text(
                    "Files live inside RS Office's private folder. Save a copy to Downloads from any result screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, start = 4.dp)
                )
            }
        }
    }

    val target = confirm
    if (target != null) {
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Delete this file?") },
            text = { Text(target.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    confirm = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Keep") }
            }
        )
    }
}
