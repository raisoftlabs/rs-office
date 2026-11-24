package com.raisoftlabs.rsoffice.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raisoftlabs.rsoffice.core.Store
import java.io.File

@Composable
fun PdfOpenScreen(
    onBack: () -> Unit,
    onPicked: (File) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val file = Store.importUri(context, uri, "Imported", forceExt = "pdf")
        if (file != null) onPicked(file)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TopBar(title = "Edit PDF", subtitle = "Choose a file to edit", onBack = onBack)
        Spacer(Modifier.height(14.dp))
        Text(
            "Pick a PDF from your device. RS Office works on a private copy; the original stays untouched.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        GradientButton(
            text = "Choose PDF",
            icon = iconFor(ToolId.PDF_EDITOR),
            onClick = { picker.launch(arrayOf("application/pdf")) }
        )
        Spacer(Modifier.height(18.dp))
    }
}
