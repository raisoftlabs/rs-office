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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(IndigoDeep, Indigo, Violet))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "RS",
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "RS Office",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Word · Excel · PowerPoint · PDF · Scan · 2.1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Panel {
                Text(
                    "The idea",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Most office apps ask you to sign in and pay before they do anything useful. " +
                            "RS Office works offline: scan a page, edit a document, or create a PDF " +
                            "right on your phone. Word, spreadsheet and presentation files are edited in place " +
                            "with everything you did not touch preserved exactly as it was.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Panel {
                FeatureRow(
                    Icons.Rounded.Description,
                    "Word, Excel, PowerPoint",
                    "Open real .docx, .xlsx and .pptx files, edit their content offline and save a copy that other office apps can open."
                )
                Spacer(Modifier.height(14.dp))
                FeatureRow(
                    Icons.Rounded.Image,
                    "Scan with page review",
                    "Camera scans become PDFs you can rotate, reorder and clean up before saving — no automatic deck guessing."
                )
                Spacer(Modifier.height(14.dp))
                FeatureRow(
                    Icons.Rounded.AutoAwesome,
                    "PDF toolkit",
                    "Merge, split, compress, export pages to images, turn typed text or photos into PDF, and read text with OCR."
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Lock,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Private by design", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "No account, no analytics, no ads. OCR runs with the model bundled in the app; " +
                            "the camera scanner is provided by Google Play services on the device. " +
                            "Your documents never leave the phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Built for speed", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Native rendering, lazy page loading and on-device processing keep everything smooth " +
                            "even on modest phones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Description,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Icons & credits", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Interface icons are Google Material Icons (Apache License 2.0). " +
                            "The RS mark and all screen artwork are original. " +
                            "No Microsoft logos or other third-party copyrighted artwork are used.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            Text(
                "RS Office 2.1 · Made by RaiSoft Labs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Row(verticalAlignment = Alignment.Top) {
        IconBadge(icon, size = 32.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
