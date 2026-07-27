package org.videolan.vlc.compose.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.viewmodel.PlayerUiState

/**
 * Platform-owned decoder content for the otherwise shared player route.
 *
 * The common shell owns navigation, playback state and [VideoSurfaceWithHud].
 * A host only needs to provide a native rendering view when the current item
 * has visual output; audio remains fully Compose.
 */
typealias PlayerSurface = @Composable BoxScope.(state: PlayerUiState, chromeVisible: Boolean) -> Unit

/** Default artwork used on hosts without a native video decoder (web/JVM). */
@Composable
fun PlayerArtworkFallback(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = MaterialSymbols.Filled.MusicNote,
            contentDescription = null,
            tint = VLCThemeDefaults.colors.primary,
            modifier = Modifier.size(80.dp),
        )
    }
}

internal val FallbackPlayerSurface: PlayerSurface = { _, _ -> PlayerArtworkFallback() }
