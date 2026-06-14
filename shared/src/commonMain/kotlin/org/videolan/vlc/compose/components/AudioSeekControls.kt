package org.videolan.vlc.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalents of the cover-mode audio-player seek/bookmark HUD controls:
 *   - application/vlc-android/res/layout/audio_player.xml @id/audio_rewind_bookmark
 *   - application/vlc-android/res/layout/audio_player.xml @id/audio_rewind_10
 *   - application/vlc-android/res/layout/audio_player.xml @id/audio_rewind_text
 *   - application/vlc-android/res/layout/audio_player.xml @id/audio_forward_10
 *   - application/vlc-android/res/layout/audio_player.xml @id/audio_forward_text
 *   - application/vlc-android/res/layout/audio_player.xml @id/audio_forward_bookmark
 *
 * Hosts keep the existing IDs for animator visibility and layout constraints
 * while this leaf owns touch target sizing, semantics, and delay label styling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VLCAudioSeekHudButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    iconContent: @Composable () -> Unit
) {
    VLCTheme {
        Box(
            modifier = modifier
                .size(48.dp)
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

@Composable
fun VLCAudioSeekDelayLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    VLCTheme {
        Box(
            modifier = modifier
                .size(48.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = VLCThemeDefaults.colors.playerIconColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PreviewSeekIcon() {
    Box(Modifier.size(32.dp))
}
