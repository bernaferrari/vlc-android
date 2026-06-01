package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the audio-player A-B repeat timeline marker ImageViews:
 *   - application/vlc-android/res/layout/audio_player.xml @id/ab_repeat_marker_a
 *   - application/vlc-android/res/layout/audio_player.xml @id/ab_repeat_marker_b
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/ab_repeat_marker_a
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/ab_repeat_marker_b
 *
 * The host keeps the existing IDs and ConstraintLayout guidelines for marker
 * positioning. This leaf owns the stable 24dp marker bounds and stays
 * decorative for accessibility, matching the previous unlabelled ImageViews.
 */
@Composable
fun VLCAudioAbRepeatMarker(
    modifier: Modifier = Modifier,
    markerContent: @Composable () -> Unit
) {
    VLCTheme {
        Box(
            modifier = modifier
                .size(24.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            markerContent()
        }
    }
}

@Preview(
    name = "VLCAudioAbRepeatMarker - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 64,
    heightDp = 48
)
@Composable
fun VLCAudioAbRepeatMarkerLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioAbRepeatMarker {
            PreviewAbRepeatMarkerIcon()
        }
    }
}

@Preview(
    name = "VLCAudioAbRepeatMarker - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 64,
    heightDp = 48
)
@Composable
fun VLCAudioAbRepeatMarkerDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioAbRepeatMarker {
            PreviewAbRepeatMarkerIcon()
        }
    }
}

@Composable
private fun PreviewAbRepeatMarkerIcon() {
    Box(
        Modifier
            .size(16.dp)
            .background(VLCThemeDefaults.colors.playerIconColor)
    )
}
