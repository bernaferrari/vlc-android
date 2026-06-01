package org.videolan.vlc.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the audio-player timeline time TextViews:
 *   - application/vlc-android/res/layout/audio_player.xml @id/time
 *   - application/vlc-android/res/layout/audio_player.xml @id/length
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/time
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/length
 *
 * The host keeps the existing IDs for SeekBar constraints, bookmark progress
 * height anchoring, and timeline positioning. This leaf owns text styling and
 * click handling for toggling elapsed/remaining time.
 */
@Composable
fun VLCAudioTimelineTimeLabel(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    VLCTheme {
        Text(
            text = text,
            color = VLCThemeDefaults.colors.fontDefault,
            fontSize = 12.sp,
            modifier = modifier
                .clickable(onClick = onClick)
                .clearAndSetSemantics { }
        )
    }
}

@Preview(
    name = "VLCAudioTimelineTimeLabel - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 96,
    heightDp = 48
)
@Composable
fun VLCAudioTimelineTimeLabelLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioTimelineTimeLabel(text = "3:42")
    }
}

@Preview(
    name = "VLCAudioTimelineTimeLabel - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 96,
    heightDp = 48
)
@Composable
fun VLCAudioTimelineTimeLabelDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioTimelineTimeLabel(text = "-12:08")
    }
}
