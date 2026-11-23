package com.raisoftlabs.vellum.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    data class Office(val file: File) : Route
    data class Text(val title: String, val body: String, val name: String) : Route
}

private fun extFor(kind: ToolId): String = when (kind) {
    ToolId.DOCX -> "docx"
    ToolId.SHEET -> "xlsx"
    ToolId.SLIDES -> "pptx"
    else -> "pdf"
}

@Composable
fun VellumApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableStateOf(0) }
    var route by remember { mutableStateOf<Route?>(null) }
    var libraryVersion by remember { mutableIntStateOf(0) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(libraryVersion) {
        files = withContext(Dispatchers.IO) { Store.list(context) }
    }

    fun refreshLibrary() {
        libraryVersion++
    }

    LaunchedEffect(route) {
        if (route == null) libraryVersion++
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

    var officeKind by remember { mutableStateOf(ToolId.DOCX) }

    val openOfficeFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val kind = officeKind
            scope.launch {
                busyMessage = "Opening ${kind.title} file…"
                val imported = withContext(Dispatchers.IO) { Store.importUri(context, uri, kind.title.lowercase()) }
                busyMessage = null
                if (imported == null) {
                    context.toast("Could not open that file.")
                    return@launch
                }
                val known = setOf("docx", "xlsx", "pptx", "doc", "xls")
                val file = if (imported.extension.lowercase() in known) {
                    imported
                } else {
                    val renamed = File(imported.parentFile, "${imported.nameWithoutExtension}.${extFor(kind)}")
                    if (imported.renameTo(renamed)) renamed else imported
                }
                refreshLibrary()
                route = Route.Office(file)
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

    fun openOffice(kind: ToolId) {
        officeKind = kind
        openOfficeFile.launch(arrayOf(mimeFor(kind), "application/octet-stream"))
    }

    fun handleTool(id: ToolId) {
        when {
            id == ToolId.SCAN -> startScan()
            id == ToolId.DECK -> route = Route.Deck(null)
            id.isOffice -> openOffice(id)
            else -> route = Route.Tool(id)
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
        when (file.extension.lowercase()) {
            "pdf" -> route = Route.Viewer(file)
            "docx", "xlsx", "pptx" -> route = Route.Office(file)
            else -> Store.openWith(context, file)
        }
    }

    BackHandler(enabled = route != null) { route = null }
    BackHandler(enabled = route == null && tab != 0) { tab = 0 }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        (
                                fadeIn(animationSpec = tween(220)) +
                                        slideInHorizontally(animationSpec = tween(260)) { it / 10 }
                                ).togetherWith(fadeOut(animationSpec = tween(140)))
                    },
                    label = "route"
                ) { current ->
                    if (current == null) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(190))
                                    .togetherWith(fadeOut(animationSpec = tween(120)))
                            },
                            label = "tab"
                        ) { selectedTab ->
                            when (selectedTab) {
                                0 -> HomeScreen(
                                    recent = files,
                                    onScan = { startScan() },
                                    onImportPdf = { openPdf.launch(arrayOf("application/pdf")) },
                                    onOpenOffice = { openOffice(it) },
                                    onTool = { handleTool(it) },
                                    onOpenFile = { openFile(it) },
                                    onShareFile = { Store.share(context, it) },
                                    onOpenLibrary = { tab = 2 },
                                    onOpenTools = { tab = 1 }
                                )

                                1 -> ToolsScreen(
                                    onTool = { handleTool(it) },
                                    onOpenOffice = { openOffice(it) },
                                    onScan = { startScan() }
                                )

                                2 -> LibraryScreen(
                                    files = files,
                                    onOpen = { openFile(it) },
                                    onShare = { Store.share(context, it) },
                                    onDelete = { file ->
                                        if (file.delete()) refreshLibrary()
                                        else context.toast("Could not delete that file.")
                                    },
                                    onScan = { startScan() }
                                )

                                else -> AboutScreen()
                            }
                        }
                    } else {
                        when (current) {
                            is Route.Result -> ResultScreen(
                                file = current.file,
                                onBack = { route = null },
                                onOpen = { openFile(current.file) },
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

                            is Route.Office -> OfficeScreen(
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
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(0, "Home", Icons.Rounded.Home, tab, onTab, Modifier.weight(1f))
            TabItem(1, "Tools", Icons.Rounded.Apps, tab, onTab, Modifier.weight(1f))
            TabItem(2, "Files", Icons.Rounded.Folder, tab, onTab, Modifier.weight(1f))
            TabItem(3, "About", Icons.Rounded.Info, tab, onTab, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(
    index: Int,
    label: String,
    icon: ImageVector,
    selected: Int,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = selected == index
    val pillWidth by animateDpAsState(
        targetValue = if (active) 46.dp else 0.dp,
        animationSpec = tween(240),
        label = "pill"
    )
    val tint by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "tint"
    )
    Column(
        modifier = modifier
            .bounceClick { onTab(index) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(pillWidth)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else Color.Transparent
                    )
            )
            Icon(
                icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/** Unused-but-kept helper: spring preset shared by press animations. */
internal val BouncySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)
