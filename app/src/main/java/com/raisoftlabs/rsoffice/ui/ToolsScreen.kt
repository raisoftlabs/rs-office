package com.raisoftlabs.rsoffice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ToolsScreen(
    onTool: (ToolId) -> Unit,
    onOpenOffice: (ToolId) -> Unit,
    onScan: () -> Unit
) {
    val groups = ToolGroup.entries
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 30.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    "All tools",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${ToolId.entries.size} tools · everything runs on this phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        groups.forEach { group ->
            item {
                SectionHeader(group.title)
            }
            val tools = ToolId.entries.filter { it.group == group }
            item {
                GlassCard(padding = 6.dp) {
                    tools.forEachIndexed { index, id ->
                        ToolRow(
                            id = id,
                            onClick = {
                                when (id) {
                                    ToolId.SCAN -> onScan()
                                    else -> if (id.isOffice) onOpenOffice(id) else onTool(id)
                                }
                            }
                        )
                        if (index < tools.lastIndex) {
                            Spacer(Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }

        item {
            Text(
                "More tools are added with every update. Nothing here uploads a file or shows an ad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun ToolRow(id: ToolId, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .bounceClick { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GradientIcon(
            icon = iconFor(id),
            colors = colorsFor(id),
            size = 42.dp,
            iconSize = 20.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                id.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                id.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}
