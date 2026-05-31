package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

data class VLCRendererUiItem(
    val id: String,
    val displayName: String,
    val isSelected: Boolean,
    val isChromecast: Boolean
)

/**
 * Compose renderer picker content. The app module owns RendererItem mapping,
 * icon resources, and playback side effects.
 */
@Composable
fun VLCRendererPickerDialogContent(
    title: String,
    renderers: List<VLCRendererUiItem>,
    disconnectText: String,
    showDisconnect: Boolean,
    onRendererSelected: (VLCRendererUiItem) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
    rendererIcon: @Composable (VLCRendererUiItem, Color?) -> Unit
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            modifier = modifier.widthIn(min = 384.dp),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundDefault)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .padding(bottom = 16.dp)
                ) {
                    items(renderers, key = { it.id }) { item ->
                        RendererPickerRow(
                            item = item,
                            onClick = { onRendererSelected(item) },
                            icon = rendererIcon
                        )
                    }
                }

                if (showDisconnect) {
                    TextButton(
                        onClick = onDisconnect,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(disconnectText)
                    }
                }
            }
        }
    }
}

@Composable
private fun RendererPickerRow(
    item: VLCRendererUiItem,
    onClick: () -> Unit,
    icon: @Composable (VLCRendererUiItem, Color?) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val selectedTint = if (item.isSelected) colors.primary else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            icon(item, selectedTint)
        }
        Text(
            text = item.displayName,
            color = selectedTint ?: colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (item.isSelected) FontWeight.Medium else FontWeight.Normal
            ),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Preview(name = "Renderer Picker", showBackground = true)
@Composable
private fun VLCRendererPickerDialogContentPreview() {
    VLCRendererPickerDialogContent(
        title = "Displays",
        renderers = listOf(
            VLCRendererUiItem("1", "Living Room TV", true, true),
            VLCRendererUiItem("2", "Kitchen speaker", false, false)
        ),
        disconnectText = "Disconnect",
        showDisconnect = true,
        onRendererSelected = {},
        onDisconnect = {},
        rendererIcon = { item, tint ->
            PreviewRendererIcon(tint ?: if (item.isChromecast) Color(0xFF616161) else Color(0xFF9E9E9E))
        }
    )
}

@Preview(name = "Renderer Picker Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCRendererPickerDialogContentDarkPreview() {
    VLCRendererPickerDialogContent(
        title = "Displays",
        renderers = listOf(
            VLCRendererUiItem("1", "Office Chromecast", false, true),
            VLCRendererUiItem("2", "Bedroom receiver", false, false)
        ),
        disconnectText = "Disconnect",
        showDisconnect = false,
        onRendererSelected = {},
        onDisconnect = {},
        rendererIcon = { _, tint -> PreviewRendererIcon(tint ?: Color(0xFFBDBDBD)) }
    )
}

@Composable
private fun PreviewRendererIcon(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color, CircleShape)
    )
}
