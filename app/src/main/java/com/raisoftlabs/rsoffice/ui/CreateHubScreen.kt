package com.raisoftlabs.rsoffice.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp

@Composable
fun CreateHubScreen(onTool: (ToolId) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TopBar(
            title = "Create",
            subtitle = "Blank files, made on your device"
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose what you want to make",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(12.dp))
        val tools = listOf(
            ToolId.NEW_WORD,
            ToolId.NEW_EXCEL,
            ToolId.NEW_POWERPOINT,
            ToolId.TEXT_TO_PDF,
            ToolId.SCAN,
            ToolId.IMAGES_TO_PDF
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tools.chunked(2).forEach { row ->
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
