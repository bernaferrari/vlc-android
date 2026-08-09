package org.videolan.vlc.compose.player

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.artwork.MediaArtwork
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
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            showFallbackContainer = false,
            fillMaxSizeArtwork = true,
            fallback = { GenerativeAudioArtwork(item, state.progress, atmospheric = true) },
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
            contentDescription = null,
            size = coverSize,
            modifier = Modifier
                .size(coverSize)
                .shadow(24.dp, MaterialTheme.shapes.extraLarge)
                .clip(MaterialTheme.shapes.extraLarge),
            contentScale = ContentScale.Crop,
            fallback = { GenerativeAudioArtwork(item, state.progress) },
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
internal fun GenerativeAudioArtwork(
    item: MediaItem,
    progress: org.videolan.vlc.model.Progress = org.videolan.vlc.model.Progress(),
    atmospheric: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val primary = VLCThemeDefaults.colors.primary
    val seed = rememberAudioIdentitySeed(item)
    val fraction = if (progress.length > 0L) {
        (progress.time.toFloat() / progress.length.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.tertiaryContainer,
                        scheme.surfaceContainerHighest,
                        primary.copy(alpha = if (atmospheric) 0.28f else 0.46f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension
            val center = center
            repeat(6) { index ->
                val sample = ((seed ushr (index * 4)) and 0xF) / 15f
                val radius = unit * (0.10f + index * 0.07f + sample * 0.035f)
                drawCircle(
                    color = if (index % 2 == 0) scheme.onTertiaryContainer else primary,
                    radius = radius,
                    center = center.copy(
                        x = center.x + (sample - 0.5f) * unit * 0.18f,
                        y = center.y + (((seed ushr (index + 2)) and 0x7) / 7f - 0.5f) * unit * 0.16f,
                    ),
                    alpha = if (atmospheric) 0.035f else 0.055f,
                )
            }
            val ringRadius = unit * if (atmospheric) 0.23f else 0.285f
            val ringWidth = unit * if (atmospheric) 0.024f else 0.032f
            drawCircle(
                color = scheme.onSurface.copy(alpha = if (atmospheric) 0.14f else 0.20f),
                radius = ringRadius,
                style = Stroke(width = ringWidth),
            )
            if (fraction > 0f) {
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - ringRadius, center.y - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = ringWidth, cap = StrokeCap.Round),
                )
            }
            val barWidth = unit * 0.032f
            val gap = unit * 0.040f
            repeat(5) { index ->
                val sample = ((seed ushr (index * 5)) and 0x1F) / 31f
                val height = unit * (0.075f + sample * 0.16f)
                drawLine(
                    color = scheme.onTertiaryContainer.copy(alpha = if (atmospheric) 0.36f else 0.88f),
                    start = androidx.compose.ui.geometry.Offset(center.x + (index - 2) * gap, center.y - height / 2f),
                    end = androidx.compose.ui.geometry.Offset(center.x + (index - 2) * gap, center.y + height / 2f),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

internal fun rememberAudioIdentitySeed(item: MediaItem): Int =
    "${item.id}|${item.uri}|${item.displayTitle}|${item.artist.orEmpty()}".hashCode().let {
        if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it)
    }

internal val FallbackPlayerSurface: PlayerSurface = { state, _ -> PlayerArtworkFallback(state) }
