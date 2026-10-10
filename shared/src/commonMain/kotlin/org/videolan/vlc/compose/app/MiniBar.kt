package org.videolan.vlc.compose.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.player.GenerativeAudioArtwork
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.viewmodel.PlayerUiState

@Composable
internal fun MiniBar(
    state: PlayerUiState,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = VLCThemeDefaults.colors
    val motion = LocalVLCMotion.current
    val item = state.queue.getOrNull(state.currentQueueIndex)
    val targetProgress = if (state.progress.length > 0L) {
        (state.progress.time.toFloat() / state.progress.length.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (motion.reducedMotion) snap() else tween(220, easing = VLCMotion.Emphasized),
        label = "mini-player-progress",
    )
    Surface(
        onClick = onExpand,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val progressWidth = size.width * animatedProgress
                    drawRect(
                        color = colors.primary.copy(alpha = 0.12f),
                        topLeft = Offset(0f, size.height - 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()),
                    )
                    drawRect(
                        color = colors.primary,
                        topLeft = Offset(0f, size.height - 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(progressWidth, 2.dp.toPx()),
                    )
                }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(9.dp)),
                ) {
                    if (item != null) {
                        MediaArtwork(
                            item = item,
                            contentDescription = null,
                            size = 42.dp,
                            modifier = Modifier.fillMaxSize(),
                            fillMaxSizeArtwork = true,
                            fallback = { GenerativeAudioArtwork(item, state.progress) },
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {}
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.title.ifBlank { ShellStrings.notPlaying() },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (state.subtitle.isNotBlank()) {
                        Text(
                            state.subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (kotlin.math.abs(state.rate - 1f) > 0.001f) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primary.copy(alpha = 0.14f),
                        contentColor = colors.primary,
                    ) {
                        Text(
                            text = org.videolan.vlc.compose.player.playbackRateLabel(state.rate),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                IconButton(onClick = onToggle) {
                    Crossfade(
                        targetState = state.playing,
                        animationSpec = tween(motion.durationShort, easing = VLCMotion.Emphasized),
                        label = "mini-player-icon",
                    ) { isPlaying ->
                        Icon(
                            icon = if (isPlaying) MaterialSymbols.Filled.Pause else MaterialSymbols.Filled.PlayArrow,
                            contentDescription = if (isPlaying) ShellStrings.pause() else ShellStrings.play(),
                        )
                    }
                }
            }
        }
    }
}
