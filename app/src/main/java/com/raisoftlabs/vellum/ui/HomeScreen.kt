package com.raisoftlabs.vellum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun HomeScreen(
    recent: List<File>,
    onScan: () -> Unit,
    onImportPdf: () -> Unit,
    onTool: (ToolId) -> Unit,
    onOpenFile: (File) -> Unit,
    onShareFile: (File) -> Unit,
    onOpenLibrary: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp
        )
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(Indigo, Violet))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "V",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Vellum",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Documents, decks, done.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenLibrary) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = "Library",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Indigo, Violet)))
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .offset(x = 210.dp, y = (-70).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.09f))
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "ON-DEVICE · NO ADS",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(Modifier.height(13.dp))
                    Text(
                        "Turn any document\ninto a deck.",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 26.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.6).sp
                        )
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Scan a page or pick a PDF — Vellum typesets a real, editable PowerPoint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color.White)
                            .clickable(onClick = onScan)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            tint = Indigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Scan document",
                            color = IndigoDeep,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = IndigoDeep,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("Import PDF", Icons.Rounded.Description, Modifier.weight(1f), onImportPdf)
                QuickAction("Deck studio", Icons.Rounded.AutoAwesome, Modifier.weight(1f)) {
                    onTool(ToolId.DECK)
                }
            }
        }

        item { SectionLabel("Studio", Modifier.padding(top = 20.dp)) }

        items(ToolId.entries.chunked(3).size) { rowIndex ->
            val row = ToolId.entries.chunked(3)[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                row.forEach { id ->
                    ToolTile(
                        icon = iconFor(id),
                        title = id.title,
                        subtitle = id.subtitle,
                        onClick = { onTool(id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(9.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Recent", Modifier.weight(1f))
                Text(
                    "See all",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenLibrary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (recent.isEmpty()) {
            item {
                Panel(modifier = Modifier.padding(top = 2.dp)) {
                    EmptyState(
                        icon = Icons.Rounded.Description,
                        title = "Nothing here yet",
                        body = "Scan a document or import a PDF — everything you make shows up here."
                    )
                }
            }
        } else {
            items(recent.take(4).size) { index ->
                val file = recent[index]
                FileRow(
                    file = file,
                    onClick = { onOpenFile(file) },
                    onShare = { onShareFile(file) }
                )
            }
        }

        item {
            Text(
                "Everything runs on your phone. No account, no upload, no ads.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun QuickAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
