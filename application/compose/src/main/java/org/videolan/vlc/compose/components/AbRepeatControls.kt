package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose replacement for the former shared A-B repeat controls include root.
 *
 * This collapses the legacy XML include + child XML-friendly widgets into one
 * leaf. The app module still supplies the decorative icon content because
 * drawable resources live outside :application:compose.
 */
@Composable
fun VLCAbRepeatControls(
    markerText: String,
    modifier: Modifier = Modifier,
    onAddMarkerClick: () -> Unit = {},
    iconContent: @Composable () -> Unit
) {
    VLCTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .background(
                    color = VLCThemeDefaults.colors.audioChipsColor,
                    shape = RoundedCornerShape(100.dp)
                )
                .padding(start = 8.dp, end = 8.dp)
        ) {
            VLCAbRepeatChipIcon(iconContent = iconContent)
            VLCAbRepeatAddMarkerButton(
                text = markerText,
                onClick = onAddMarkerClick
            )
        }
    }
}

@Preview(
    name = "VLCAbRepeatControls - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 240,
    heightDp = 64
)
@Composable
fun VLCAbRepeatControlsLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAbRepeatControls(
            markerText = "Set start point",
            iconContent = { PreviewAbRepeatControlsIcon() }
        )
    }
}

@Preview(
    name = "VLCAbRepeatControls - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 240,
    heightDp = 64
)
@Composable
fun VLCAbRepeatControlsDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAbRepeatControls(
            markerText = "Set end point",
            iconContent = { PreviewAbRepeatControlsIcon() }
        )
    }
}

@Composable
private fun PreviewAbRepeatControlsIcon() {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(24.dp)
            .background(VLCThemeDefaults.colors.playerIconColor)
    )
}
