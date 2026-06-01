package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the audio queue progress pill from:
 *   - application/vlc-android/res/layout/audio_player.xml
 *   - application/vlc-android/res/layout-land/audio_player.xml
 *
 * This is the small tappable HUD chip above the timeline that cycles between
 * queue elapsed/remaining/finish-time text. Visibility and progress calculation
 * remain owned by the host while this leaf owns the rendered chrome.
 */
data class VLCAudioQueueProgressPillState(
    val text: String = "",
    val contentDescription: String = ""
)

@Composable
fun VLCAudioQueueProgressPill(
    state: VLCAudioQueueProgressPillState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    VLCTheme {
        if (state.text.isNotBlank()) {
            val colors = VLCThemeDefaults.colors
            Box(
                modifier = modifier
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.audioChipBackground)
                    .clickable(onClick = onClick)
                    .semantics {
                        contentDescription = state.contentDescription.ifBlank { state.text }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = state.text,
                    color = colors.audioChipTextColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(
    name = "VLCAudioQueueProgressPill - Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 96
)
@Composable
fun VLCAudioQueueProgressPillLightPreview() {
    VLCTheme(darkTheme = false) {
        VLCAudioQueueProgressPill(
            state = VLCAudioQueueProgressPillState(
                text = "Track 3 / 12  •  10:42 / 48:12",
                contentDescription = "Track 3 of 12. 10 minutes 42 seconds out of 48 minutes 12 seconds."
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    name = "VLCAudioQueueProgressPill - Dark",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 96
)
@Composable
fun VLCAudioQueueProgressPillDarkPreview() {
    VLCTheme(darkTheme = true) {
        VLCAudioQueueProgressPill(
            state = VLCAudioQueueProgressPillState(
                text = "Track 8 / 24  •  Ends at 12:55 AM",
                contentDescription = "Track 8 of 24. Ends at 12:55 AM."
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
