package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import kotlin.math.roundToInt

/**
 * Compose equivalent of the A-B repeat timeline marker strip formerly built
 * with ConstraintLayout guidelines in:
 *   - application/vlc-android/res/layout/audio_player.xml
 *   - application/vlc-android/res/layout-land/audio_player.xml
 *   - application/vlc-android/src/org/videolan/vlc/gui/view/VideoHudOverlayView.kt
 *
 * Hosts push normalized start/stop fractions into AbRepeatMarkerContainerView;
 * this leaf owns both marker placement and the stable 24dp decorative bounds.
 */
@Composable
fun VLCAudioAbRepeatMarkers(
    startFraction: Float,
    stopFraction: Float,
    modifier: Modifier = Modifier,
    markerContent: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .clearAndSetSemantics { }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val markerSizePx = with(density) { 24.dp.toPx() }

        fun Modifier.markerOffset(fraction: Float) = offset {
            IntOffset(
                x = (widthPx * fraction.coerceIn(0f, 1f) - markerSizePx / 2f).roundToInt(),
                y = 0
            )
        }

        if (startFraction >= 0f) {
            VLCAudioAbRepeatMarker(
                modifier = Modifier.markerOffset(startFraction),
                markerContent = markerContent
            )
        }
        if (stopFraction >= 0f) {
            VLCAudioAbRepeatMarker(
                modifier = Modifier.markerOffset(stopFraction),
                markerContent = markerContent
            )
        }
    }
}

/**
 * Single decorative marker used by [VLCAudioAbRepeatMarkers] and previews.
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

@Preview(
    name = "VLCAudioAbRepeatMarkers - Strip",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 320,
    heightDp = 48
)
@Composable
fun VLCAudioAbRepeatMarkersStripPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioAbRepeatMarkers(
            startFraction = 0.22f,
            stopFraction = 0.78f,
            modifier = Modifier.width(280.dp)
        ) {
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
