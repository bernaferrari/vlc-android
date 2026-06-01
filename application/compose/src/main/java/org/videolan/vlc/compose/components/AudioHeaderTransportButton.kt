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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose equivalent of the audio-player transport ImageViews:
 *   - application/vlc-android/res/layout/audio_player.xml @id/shuffle
 *   - application/vlc-android/res/layout/audio_player.xml @id/previous
 *   - application/vlc-android/res/layout/audio_player.xml @id/play_pause
 *   - application/vlc-android/res/layout/audio_player.xml @id/next
 *   - application/vlc-android/res/layout/audio_player.xml @id/repeat
 *   - application/vlc-android/res/layout/audio_player.xml @id/header_shuffle
 *   - application/vlc-android/res/layout/audio_player.xml @id/header_previous
 *   - application/vlc-android/res/layout/audio_player.xml @id/header_large_play_pause
 *   - application/vlc-android/res/layout/audio_player.xml @id/header_next
 *   - application/vlc-android/res/layout/audio_player.xml @id/header_repeat
 *
 * The host owns drawable resources, playback state, and click callbacks. This
 * leaf owns the stable touch target and semantics while ConstraintLayout keeps
 * the existing IDs for tablet header animations, hinge constraints, and
 * previous/next long-seek listeners.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCAudioHeaderTransportButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    iconContent: @Composable () -> Unit
) {
    VLCTheme {
        Box(
            modifier = modifier
                .size(size)
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
    name = "VLCAudioHeaderTransportButton - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 96,
    heightDp = 64
)
@Composable
fun VLCAudioHeaderTransportButtonLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioHeaderTransportButton(contentDescription = "Previous") {
            PreviewHeaderTransportIcon()
        }
    }
}

@Preview(
    name = "VLCAudioHeaderTransportButton - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 96,
    heightDp = 64
)
@Composable
fun VLCAudioHeaderTransportButtonDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioHeaderTransportButton(
            contentDescription = "Pause",
            size = 56.dp
        ) {
            PreviewHeaderLargePlayPauseIcon()
        }
    }
}

@Composable
private fun PreviewHeaderTransportIcon() {
    Box(Modifier.size(32.dp))
}

@Composable
private fun PreviewHeaderLargePlayPauseIcon() {
    Box(Modifier.size(48.dp))
}
