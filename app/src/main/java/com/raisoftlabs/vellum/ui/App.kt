package com.raisoftlabs.vellum.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.raisoftlabs.vellum.core.Ocr
import com.raisoftlabs.vellum.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Route {
    data class Result(val file: File) : Route
    data class Deck(val source: File?) : Route
    data class Tool(val id: ToolId) : Route
    data class Viewer(val file: File) : Route
    data class Text(val title: String, val body: String, val name: String) : Route
}

@Composable
fun VellumApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableStateOf(0) }
    var route by remember { mutableStateOf<Route?>(null) }
    var libraryKey by remember { mutableIntStateOf(0) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var tabLibraryKey by remember { mutableIntStateOf(0) }

    fun refreshLibrary() {
        libraryKey++
        tabLibraryKey++
    }

    val openPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busyMessage = "Importing PDF…"
                val file = withContext(Dispatchers.IO) {
                    Store.importUri(context, uri, "document", "pdf")
                }
                busyMessage = null
                if (file != null) {
                    refreshLibrary()
                    route = Route.Result(file)
                } else {
                    context.toast("That PDF could not be read.")
                }
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uri = scan?.pdf?.uri
            if (uri != null) {
                scope.launch {
                    busyMessage = "Saving scan…"
                    val file = withContext(Dispatchers.IO) {
                        Store.importUri(context, uri, "scan", "pdf")
                    }
                    busyMessage = null
                    if (file != null) {
                        refreshLibrary()
                        route = Route.Result(file)
                    } else {
                        context.toast("The scan could not be saved.")
                    }
                }
            }
        }
    }

    fun startScan() {
        val activity = context.findActivity()
        if (activity == null) {
            openPdf.launch(arrayOf("application/pdf"))
            return
        }
        try {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(60)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
            GmsDocumentScanning.getClient(options)
                .getStartScanIntent(activity)
                .addOnSuccessListener { sender ->
                    scanLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
                .addOnFailureListener {
                    context.toast("Scanner not available — choose a PDF instead.")
                    openPdf.launch(arrayOf("application/pdf"))
                }
        } catch (_: Exception) {
            openPdf.launch(arrayOf("application/pdf"))
        }
    }

    fun runOcr(file: File) {
        scope.launch {
            busyMessage = "Reading text…"
            val text = withContext(Dispatchers.IO) { Ocr.fileText(file) }
            busyMessage = null
            if (text.isBlank()) {
                context.toast("No readable text found in this file.")
            } else {
                route = Route.Text(
                    title = Store.prettyName(file).ifBlank { "Extracted text" },
                    body = text,
                    name = "${file.nameWithoutExtension}_text"
                )
            }
        }
    }

    fun openFile(file: File) {
        if (file.extension.equals("pdf", true)) {
            route = Route.Viewer(file)
        } else {
            Store.openWith(context, file)
        }
    }

    val files = remember(libraryKey, tab, route) { Store.list(context) }

    BackHandler(enabled = route != null) { route = null }
    BackHandler(enabled = route == null && tab != 0) { tab = 0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Box(modifier = Modifier.weight(1f)) {
                val current = route
                if (current == null) {
                    when (tab) {
                        0 -> HomeScreen(
                            recent = files,
                            onScan = { startScan() },
                            onImportPdf = { openPdf.launch(arrayOf("application/pdf")) },
                            onTool = { id ->
                                if (id == ToolId.DECK) route = Route.Deck(null)
                                else route = Route.Tool(id)
                            },
                            onOpenFile = { openFile(it) },
                            onShareFile = { Store.share(context, it) },
                            onOpenLibrary = { tab = 2 }
                        )

                        1 -> DeckScreen(
                            initialSource = null,
                            onBack = null,
                            onScan = { startScan() },
                            onOpenFile = { openFile(it) }
                        )

                        2 -> LibraryScreen(
                            files = files,
                            onOpen = { openFile(it) },
                            onShare = { Store.share(context, it) },
                            onDelete = { file ->
                                val deleted = file.delete()
                                if (deleted) refreshLibrary() else context.toast("Could not delete that file.")
                            },
                            onScan = { startScan() }
                        )

                        else -> AboutScreen()
                    }
                } else {
                    when (current) {
                        is Route.Result -> ResultScreen(
                            file = current.file,
                            onBack = { route = null },
                            onOpen = { route = Route.Viewer(current.file) },
                            onMakeDeck = { route = Route.Deck(current.file) },
                            onOcr = { runOcr(current.file) },
                            onShare = { Store.share(context, current.file) },
                            onSave = {
                                val ok = Store.saveToDownloads(context, current.file)
                                context.toast(
                                    if (ok) "Saved to Downloads/Vellum" else "Could not save to Downloads"
                                )
                            }
                        )

                        is Route.Deck -> DeckScreen(
                            initialSource = current.source,
                            onBack = { route = null },
                            onScan = { startScan() },
                            onOpenFile = { file ->
                                refreshLibrary()
                                openFile(file)
                            }
                        )

                        is Route.Tool -> ToolScreen(
                            tool = current.id,
                            onBack = { route = null },
                            onScan = { startScan() },
                            onTextReady = { title, body, name ->
                                route = Route.Text(title, body, name)
                            }
                        )

                        is Route.Viewer -> ViewerScreen(
                            file = current.file,
                            onBack = { route = null }
                        )

                        is Route.Text -> TextScreen(
                            title = current.title,
                            body = current.body,
                            suggestedName = current.name,
                            onBack = { route = null }
                        )
                    }
                }
            }

            if (route == null) {
                BottomBar(tab = tab, onTab = { tab = it })
            }
        }

        BusyOverlay(
            visible = busyMessage != null,
            title = "On it",
            message = busyMessage ?: ""
        )
    }
}

@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 14.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(0, "Home", Icons.Rounded.Home, tab, onTab, Modifier.weight(1f))
            TabItem(1, "Studio", Icons.Rounded.AutoAwesome, tab, onTab, Modifier.weight(1f))
            TabItem(2, "Files", Icons.Rounded.Folder, tab, onTab, Modifier.weight(1f))
            TabItem(3, "About", Icons.Rounded.Info, tab, onTab, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(
    index: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Int,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = selected == index
    Column(
        modifier = modifier
            .clickable { onTab(index) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}
