package org.videolan.vlc.compose.components

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the audio/video full-player timeline seekbars:
 *   - application/vlc-android/res/layout/audio_player.xml @id/timeline
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/timeline
 *   - application/vlc-android/res/layout/player_hud.xml @id/player_overlay_seekbar
 *
 * The XML bridge keeps the existing imperative max/progress and drag callback
 * contract while this leaf owns the visual track/thumb rendering.
 */
@Composable
fun VLCAudioTimelineSlider(
    progress: Int,
    max: Int,
    contentDescription: String?,
    onUserProgressChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onUserDragStarted: () -> Unit = {},
    onUserDragStopped: () -> Unit = {}
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        val safeMax = max.coerceAtLeast(1)
        val safeProgress = progress.coerceIn(0, safeMax)
        var tracking by remember { mutableStateOf(false) }

        Slider(
            value = safeProgress.toFloat(),
            onValueChange = { value ->
                if (!tracking) {
                    tracking = true
                    onUserDragStarted()
                }
                onUserProgressChange(value.roundToInt().coerceIn(0, safeMax))
            },
            onValueChangeFinished = {
                if (tracking) {
                    tracking = false
                    onUserDragStopped()
                }
            },
            valueRange = 0f..safeMax.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.audioSeekTrack
            ),
            modifier = modifier.semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
        )
    }
}

@Preview(
    name = "VLCAudioTimelineSlider - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 64
)
@Composable
fun VLCAudioTimelineSliderLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioTimelineSlider(
            progress = 42_000,
            max = 180_000,
            contentDescription = "42 seconds out of 3 minutes",
            onUserProgressChange = {}
        )
    }
}

@Preview(
    name = "VLCAudioTimelineSlider - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 64
)
@Composable
fun VLCAudioTimelineSliderDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioTimelineSlider(
            progress = 126_000,
            max = 180_000,
            contentDescription = "2 minutes 6 seconds out of 3 minutes",
            onUserProgressChange = {}
        )
    }
}
