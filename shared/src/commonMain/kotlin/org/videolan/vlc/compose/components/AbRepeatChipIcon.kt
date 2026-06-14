package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the former decorative A-B repeat chip icon XML.
 *
 * The shared root is used by the audio player and video HUD. The app-side root
 * view supplies the real drawable because :application:compose stays
 * resource-isolated.
 */
@Composable
fun VLCAbRepeatChipIcon(
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    VLCTheme {
        Box(
            modifier = modifier
                .size(36.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }
    }
}

@Composable
private fun PreviewAbRepeatChipIcon() {
    Box(
        Modifier
            .size(24.dp)
            .background(VLCThemeDefaults.colors.playerIconColor)
    )
}
