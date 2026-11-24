package com.raisoftlabs.rsoffice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Upload
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val toolkit = listOf(
    ToolId.MERGE,
    ToolId.SPLIT,
    ToolId.COMPRESS,
    ToolId.TO_IMAGES,
    ToolId.IMAGES_TO_PDF,
    ToolId.OCR
)

@Composable
fun HomeScreen(
    recent: List<File>,
    onScan: () -> Unit,
    onImportPdf: () -> Unit,
    onOpenOffice: (ToolId) -> Unit,
    onTool: (ToolId) -> Unit,
    onOpenFile: (File) -> Unit,
    onShareFile: (File) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenCreate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 30.dp)
    ) {
        /* ------------------------------ brand ------------------------------ */
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(listOf(Indigo, Violet))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "RS",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RS Office",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Your complete offline office",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenLibrary) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = "Files",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        /* ------------------------------- hero ------------------------------ */
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(IndigoDeep, Indigo, Violet)
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .offset(x = 190.dp, y = (-90).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(x = (-40).dp, y = 150.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    GlossTag("OFFLINE · NO ADS", Icons.Rounded.AutoAwesome)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Everything office.\nOne app.",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 27.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.7).sp
                        )
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Scan pages, edit Word, Excel and PowerPoint files, and save clean PDFs — all offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White)
                                .bounceClick { onScan() }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = IndigoDeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                "Scan",
                                color = IndigoDeep,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                        Row(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .bounceClick { onImportPdf() }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Upload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                "Import PDF",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }

        /* --------------------------- quick actions ------------------------- */
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAction("Scan", Icons.Rounded.PhotoCamera, listOf(Indigo, IndigoDeep), onScan)
                QuickAction("Import", Icons.Rounded.Upload, listOf(Teal, Indigo), onImportPdf)
                QuickAction(
                    "New",
                    Icons.Rounded.Add,
                    listOf(Indigo, Violet)
                ) { onOpenCreate() }
                QuickAction("Files", Icons.Rounded.Folder, listOf(Violet, IndigoDeep), onOpenLibrary)
            }
        }

        /* ----------------------------- office ------------------------------ */
        item { SectionHeader("Office apps") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OfficeCard(
                    icon = iconFor(ToolId.DOCX),
                    colors = colorsFor(ToolId.DOCX),
                    title = "Word",
                    subtitle = "Edit .docx",
                    modifier = Modifier.weight(1f)
                ) { onOpenOffice(ToolId.DOCX) }
                OfficeCard(
                    icon = iconFor(ToolId.SHEET),
                    colors = colorsFor(ToolId.SHEET),
                    title = "Excel",
                    subtitle = "Edit .xlsx",
                    modifier = Modifier.weight(1f)
                ) { onOpenOffice(ToolId.SHEET) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OfficeCard(
                    icon = iconFor(ToolId.SLIDES),
                    colors = colorsFor(ToolId.SLIDES),
                    title = "PowerPoint",
                    subtitle = "Edit .pptx",
                    modifier = Modifier.weight(1f)
                ) { onOpenOffice(ToolId.SLIDES) }
                OfficeCard(
                    icon = Icons.Rounded.Description,
                    colors = listOf(PdfRed, Rose),
                    title = "PDF",
                    subtitle = "Open or convert",
                    modifier = Modifier.weight(1f)
                ) { onImportPdf() }
            }
        }

        /* --------------------------- pdf toolkit --------------------------- */
        item {
            SectionHeader("PDF toolkit", action = "All tools") { onOpenTools() }
        }

        items(toolkit.chunked(3).size) { rowIndex ->
            val row = toolkit.chunked(3)[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                row.forEach { id ->
                    ToolTile(
                        icon = iconFor(id),
                        title = id.title,
                        subtitle = id.subtitle,
                        colors = colorsFor(id),
                        onClick = { onTool(id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(9.dp))
        }

        /* ------------------------------ recent ----------------------------- */
        item {
            SectionHeader(
                "Recent",
                action = if (recent.isEmpty()) null else "See all",
                onAction = if (recent.isEmpty()) null else onOpenLibrary
            )
        }

        if (recent.isEmpty()) {
            item {
                Panel {
                    EmptyState(
                        icon = Icons.Rounded.Description,
                        title = "Nothing here yet",
                        body = "Scan a page or import a PDF — everything you make stays on this phone."
                    )
                }
            }
        } else {
            items(recent.take(5).size) { index ->
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
                "Word, Excel, PowerPoint and PDF — read, convert and create without an account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GradientIcon(
            icon = icon,
            colors = colors,
            size = 52.dp,
            iconSize = 23.dp,
            modifier = Modifier.bounceClick { onClick() }
        )
        Spacer(Modifier.height(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OfficeCard(
    icon: ImageVector,
    colors: List<Color>,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        radius = 22.dp,
        padding = 14.dp,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientIcon(icon = icon, colors = colors, size = 44.dp, iconSize = 21.dp)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
