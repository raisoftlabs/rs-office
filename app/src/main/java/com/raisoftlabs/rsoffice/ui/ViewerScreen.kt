package com.raisoftlabs.rsoffice.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.raisoftlabs.rsoffice.core.PdfRender
import com.raisoftlabs.rsoffice.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ViewerScreen(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val render = remember(file.path) {
        runCatching { PdfRender(file) }.getOrNull()
    }
    DisposableEffect(render) {
        onDispose { render?.close() }
    }

    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val widthPx = remember(screenWidthDp) {
        with(density) { screenWidthDp.dp.roundToPx() }.coerceIn(600, 1080)
    }

    var zoomed by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = file.name,
            subtitle = if (render == null) "Unreadable file" else "${render.pageCount} pages",
            onBack = onBack,
            actions = {
                IconButton(onClick = { Store.openWith(context, file) }) {
                    Icon(Icons.Rounded.OpenInNew, "Open with", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { Store.share(context, file) }) {
                    Icon(Icons.Rounded.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )

        if (render == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "This PDF could not be opened.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(render.pageCount) { index ->
                    PdfPage(
                        render = render,
                        index = index,
                        widthPx = widthPx,
                        onClick = { zoomed = index }
                    )
                }
            }
        }
    }

    val openPage = zoomed
    if (render != null && openPage != null) {
        ZoomDialog(
            render = render,
            index = openPage,
            onClose = { zoomed = null }
        )
    }
}

@Composable
private fun PdfPage(render: PdfRender, index: Int, widthPx: Int, onClick: () -> Unit) {
    val bitmap by produceState<Bitmap?>(initialValue = null, index) {
        value = withContext(Dispatchers.IO) {
            runCatching { render.render(index, widthPx) }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
        }
    } else {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            )
        }
    }
}

@Composable
private fun ZoomDialog(render: PdfRender, index: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    val bitmap by produceState<Bitmap?>(initialValue = null, index) {
        value = withContext(Dispatchers.IO) {
            runCatching { render.render(index, 2200) }.getOrNull()
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08090C))
        ) {
            val bmp = bitmap
            if (bmp == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .transformable(transformState)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Page ${index + 1} · pinch to zoom",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, "Close", tint = Color.White)
                }
            }
        }
    }
}
