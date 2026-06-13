package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * Material 3 Expressive renderer picker. Devices are grouped into a single rounded tonal card; the
 * connected device morphs its icon chip to a filled accent fill. The app module owns RendererItem
 * mapping, icon resources, and playback side effects.
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
            modifier = modifier.widthIn(min = 320.dp, max = 420.dp),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                if (renderers.isEmpty()) {
                    ScanningState()
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        renderers.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = colors.defaultDivider,
                                    modifier = Modifier.padding(start = 80.dp)
                                )
                            }
                            RendererPickerRow(
                                item = item,
                                onClick = { onRendererSelected(item) },
                                icon = rendererIcon
                            )
                        }
                    }
                }

                if (showDisconnect) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(disconnectText)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun RendererPickerRow(
    item: VLCRendererUiItem,
    onClick: () -> Unit,
    icon: @Composable (VLCRendererUiItem, Color?) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val chipColor = if (item.isSelected) colors.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val iconTint = if (item.isSelected) colors.onPrimary else colors.fontDefault
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (item.isSelected) Modifier.background(colors.primary.copy(alpha = 0.08f)) else Modifier)
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(chipColor),
            contentAlignment = Alignment.Center
        ) {
            icon(item, iconTint)
        }
        Text(
            text = item.displayName,
            color = if (item.isSelected) colors.primary else colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
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
