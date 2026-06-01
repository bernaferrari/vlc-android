package org.videolan.vlc.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose replacement for the audio player restore-video snackbar affordance:
 *   - application/vlc-android/src/org/videolan/vlc/gui/audio/AudioPlayer.kt
 *   - application/vlc-android/res/layout/audio_player.xml @id/resume_video_hint
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/resume_video_hint
 *
 * The host owns when this transient hint appears. This leaf only draws the
 * player-themed pill and optional click target.
 */
@Composable
fun VLCAudioResumeVideoHint(
    message: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            color = colors.audioChipsColor,
            contentColor = colors.audioChipsTextColor,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            modifier = if (onClick != null) {
                modifier.clickable(role = Role.Button, onClick = onClick)
            } else {
                modifier
            }
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.audioChipsTextColor
                )
            }
        }
    }
}

@Preview(
    name = "VLCAudioResumeVideoHint - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun VLCAudioResumeVideoHintLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioResumeVideoHint(message = "Long tap the cover to restore the video")
    }
}

@Preview(
    name = "VLCAudioResumeVideoHint - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212
)
@Composable
fun VLCAudioResumeVideoHintDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioResumeVideoHint(message = "Long tap the cover to restore the video")
    }
}
