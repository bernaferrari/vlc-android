package org.videolan.vlc.compose.player

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
 * A neutral canvas keeps artwork and track identity in focus. The artwork resolves through
 * the same platform-aware pipeline as the library.
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
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Compact windows show track identity in the HUD, beside its close and menu actions.
        // Do not reserve a portrait artwork stage where it would crowd out that metadata.
        if (usesCompactAudioLayout(maxHeight)) return@BoxWithConstraints
        val supportingText = item.artist?.takeIf(String::isNotBlank)
            ?: state.subtitle.takeIf(String::isNotBlank)
        if (usesSplitAudioLayout(maxWidth, maxHeight)) {
            // Wide windows: artwork owns the left half, identity sits just above the transport in
            // the right half (the HUD places its controls directly below the vertical centre).
            val cover = minOf(maxWidth / 2 - 96.dp, maxHeight - 176.dp, 560.dp).coerceAtLeast(0.dp)
            Box(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(top = 64.dp, bottom = 24.dp),
            ) {
                Box(Modifier.fillMaxWidth(0.5f).fillMaxSize().align(Alignment.CenterStart), contentAlignment = Alignment.Center) {
                    MediaArtwork(
                        item = item,
                        contentDescription = null,
                        size = cover,
                        modifier = Modifier
                            .size(cover)
                            .shadow(20.dp, RoundedCornerShape(28.dp), ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f), spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(28.dp)),
                        contentScale = ContentScale.Crop,
                        fallback = { GenerativeAudioArtwork(item, state.progress) },
                    )
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                Column(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = 24.dp, end = 64.dp)
                        .widthIn(max = SplitAudioControlsMaxWidth)
                        .offset(y = (-SplitAudioTitleLift)),
                ) {
                    Text(
                        text = item.displayTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (supportingText != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = supportingText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            return@BoxWithConstraints
        }
        // Artwork and identity form one group, centred in the space the HUD leaves free. The
        // cover takes every available dp up to a comfortable cap so tall phones never show an
        // empty band above it.
        val stageHeight = maxHeight - 64.dp - 236.dp
        val coverSize = minOf(maxWidth - 56.dp, stageHeight - 96.dp, 440.dp).coerceAtLeast(0.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp + 56.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 64.dp, bottom = 236.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The cover is centred in the free stage; identity stays anchored just above the
            // transport so the eye travels title → timeline → play without a gap.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                MediaArtwork(
                    item = item,
                    contentDescription = null,
                    size = coverSize,
                    modifier = Modifier
                        .size(coverSize)
                        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f), spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f))
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                    fallback = { GenerativeAudioArtwork(item, state.progress) },
                )
            }
            Text(
                text = item.displayTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = coverSize).fillMaxWidth(),
            )
            if (supportingText != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = coverSize).fillMaxWidth(),
                )
            }
        }

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

/** Wide, tall-enough windows split audio into artwork and controls side by side. */
internal fun usesSplitAudioLayout(width: Dp, height: Dp): Boolean = width >= 840.dp && height >= 480.dp

internal val SplitAudioControlsMaxWidth = 480.dp

/** The title block sits this far above the vertical centre; transport starts just below it. */
internal val SplitAudioTitleLift = 56.dp

/** Shared breakpoint for the audio artwork stage and its independently composed HUD. */
internal fun usesCompactAudioLayout(height: Dp): Boolean = height < 520.dp
