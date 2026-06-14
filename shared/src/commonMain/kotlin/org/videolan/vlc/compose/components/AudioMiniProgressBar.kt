package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the collapsed audio-player mini progress bar from:
 *   - application/vlc-android/res/layout/audio_player.xml @id/progressBar
 *   - application/vlc-android/res/layout-land/audio_player.xml @id/progressBar
 *
 * The legacy host still owns the actual playback progress and slide transition.
 * This leaf owns only the 4dp track/progress drawing and keeps the right-side
 * rounded progress cap from progress_mini_player*.xml.
 */
@Composable
fun VLCAudioMiniProgressBar(
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    val fraction = progressFraction.coerceIn(0f, 1f)
    val colors = VLCThemeDefaults.colors
    VLCTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.backgroundDefaultDarker)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(colors.primary)
            )
        }
    }
}
