package com.raisoftlabs.rsoffice.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raisoftlabs.rsoffice.core.PdfRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ResultScreen(
    file: File,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onMakeDeck: () -> Unit,
    onOcr: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    val pageCount = remember(file.path) {
        runCatching { PdfRender(file).use { it.pageCount } }.getOrDefault(0)
    }

    val preview by produceState<Bitmap?>(initialValue = null, file.path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                PdfRender(file).use { render ->
                    if (render.pageCount == 0) null else render.render(0, 720)
                }
            }.getOrNull()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)
    ) {
        item { TopBar(title = "Scan complete", subtitle = "${file.name}", onBack = onBack) }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val bmp = preview
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = 108.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "First page",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Rounded.PictureAsPdf,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (pageCount > 0) "$pageCount pages · ${file.quickMeta()}" else file.quickMeta(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.14f))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "SAVED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionLabel("Do something with it", Modifier.padding(top = 18.dp)) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ResultAction("Open", Icons.Rounded.PictureAsPdf, Modifier.weight(1f), onOpen)
                ResultAction("OCR", Icons.Rounded.TextSnippet, Modifier.weight(1f), onOcr)
            }
        }
        item { Spacer(Modifier.height(9.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ResultAction("Share", Icons.Rounded.Share, Modifier.weight(1f), onShare)
                ResultAction("Save", Icons.Rounded.Download, Modifier.weight(1f), onSave)
            }
        }

        item { Spacer(Modifier.height(18.dp)) }

        item {
            GradientButton(
                text = "Turn this into a deck",
                onClick = onMakeDeck,
                icon = Icons.Rounded.AutoAwesome
            )
        }

        item {
            Text(
                "RS Office will detect headings and rebuild the pages as editable slides.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp)
            )
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun ResultAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
