package org.videolan.vlc.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the collapsed audio-player header time label from:
 *   - application/vlc-android/res/layout/audio_player.xml
 *   - application/vlc-android/res/layout-land/audio_player.xml
 *
 * The host owns time formatting and toggling remaining-time mode. This leaf
 * only renders the tappable header chrome using the existing VLC font token.
 */
@Composable
fun VLCAudioHeaderTimeLabel(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    VLCTheme {
        Text(
            text = text,
            color = VLCThemeDefaults.colors.fontDefault,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}
