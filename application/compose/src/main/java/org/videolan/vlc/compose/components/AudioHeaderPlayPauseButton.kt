package org.videolan.vlc.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose equivalent of the collapsed audio-player header play/pause button:
 *   - application/vlc-android/res/layout/audio_player.xml @id/header_play_pause
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/header_play_pause
 *
 * The host supplies icon content so app drawables and playback state remain in
 * :application:vlc-android. Long-click is preserved for the existing stop action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCAudioHeaderPlayPauseButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    iconContent: @Composable () -> Unit
) {
    VLCTheme {
        Box(
            modifier = modifier
                .size(38.dp)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }
    }
}

@Preview(
    name = "VLCAudioHeaderPlayPauseButton - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 80,
    heightDp = 64
)
@Composable
fun VLCAudioHeaderPlayPauseButtonLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioHeaderPlayPauseButton(contentDescription = "Play") {
            PreviewHeaderPlayPauseIcon()
        }
    }
}

@Preview(
    name = "VLCAudioHeaderPlayPauseButton - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 80,
    heightDp = 64
)
@Composable
fun VLCAudioHeaderPlayPauseButtonDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioHeaderPlayPauseButton(contentDescription = "Pause") {
            PreviewHeaderPlayPauseIcon()
        }
    }
}

@Composable
private fun PreviewHeaderPlayPauseIcon() {
    Box(Modifier.size(32.dp))
}
