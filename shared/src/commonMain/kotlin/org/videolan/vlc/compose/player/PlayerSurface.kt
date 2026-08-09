package org.videolan.vlc.compose.player

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.icons.VlcCone
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.viewmodel.PlayerUiState

/**
 * Platform-owned decoder content for the otherwise shared player route.
 *
 * The common shell owns navigation, playback state and [VideoSurfaceWithHud].
 * A host only needs to provide a native rendering view when the current item
 * has visual output; audio remains fully Compose.
 */
typealias PlayerSurface = @Composable BoxScope.(state: PlayerUiState, chromeVisible: Boolean) -> Unit

/**
 * Shared audio artwork stage used by every host when there is no native video output.
 * A quiet full-bleed cover establishes atmosphere while the crisp foreground cover remains the
 * focal point; both resolve through the same platform-aware artwork pipeline as the library.
 */
@Composable
fun PlayerArtworkFallback(
    state: PlayerUiState = PlayerUiState(),
    modifier: Modifier = Modifier,
) {
    val item = state.queue.getOrNull(state.currentQueueIndex) ?: MediaItem(
        id = 0L,
        title = state.title,
        uri = state.uri,
        type = MediaType.AUDIO,
        artist = state.subtitle.takeIf(String::isNotBlank),
        artworkUri = state.artworkUri,
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        MediaArtwork(
            item = item,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            showFallbackContainer = false,
            fillMaxSizeArtwork = true,
            fallback = { PlayerCoverFallback(background = true) },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.70f),
                        0.48f to MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                        1f to Color.Black.copy(alpha = 0.90f),
                    )
                )
        )
        val coverSize = minOf(maxWidth - 56.dp, maxHeight * 0.48f, 420.dp).coerceAtLeast(160.dp)
        MediaArtwork(
            item = item,
            size = coverSize,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(24.dp, MaterialTheme.shapes.extraLarge)
                .clip(MaterialTheme.shapes.extraLarge),
            contentScale = ContentScale.Crop,
            fallback = { PlayerCoverFallback(background = false) },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1f to VLCThemeDefaults.colors.primary.copy(alpha = 0.12f),
                    )
                )
        )
    }
}

@Composable
private fun PlayerCoverFallback(background: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainer,
                        VLCThemeDefaults.colors.primary.copy(alpha = if (background) 0.20f else 0.34f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = VlcCone,
            contentDescription = null,
            modifier = Modifier
                .size(if (background) 156.dp else 104.dp)
                .alpha(if (background) 0.22f else 1f),
        )
    }
}

internal val FallbackPlayerSurface: PlayerSurface = { state, _ -> PlayerArtworkFallback(state) }
