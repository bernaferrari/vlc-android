package org.videolan.vlc.compose.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.LocalVLCMotion

@Immutable
data class VLCTvTrack(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val duration: String? = null,
)

/** Per-track action revealed when a row has focus (play next, reorder, remove...). */
@Immutable
data class VLCTvTrackAction(val id: Int, val label: String, val icon: MaterialIcon)

/**
 * Album/playlist page: a fixed column with artwork, title, metadata and collection actions on
 * the left, the track list on the right. Each row reveals its secondary actions only while it
 * (or one of those actions) has focus, so the list stays calm while browsing with the D-pad.
 */
@Composable
fun VLCTvTrackListScreen(
    title: String,
    tracks: List<VLCTvTrack>,
    actions: List<VLCTvAction>,
    onAction: (VLCTvAction) -> Unit,
    onTrackClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    metadata: String? = null,
    trackActions: List<VLCTvTrackAction> = emptyList(),
    isTrackActionEnabled: (actionId: Int, index: Int) -> Boolean = { _, _ -> true },
    onTrackAction: (actionId: Int, index: Int) -> Unit = { _, _ -> },
    onTrackFocused: ((Int) -> Unit)? = null,
    emptyTitle: String = "",
    requestInitialFocus: Boolean = true,
    backdrop: (@Composable BoxScope.() -> Unit)? = null,
    heroArtwork: (@Composable BoxScope.() -> Unit)? = null,
    trackArtwork: (@Composable BoxScope.(Int) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val initialFocus = rememberVLCTvInitialFocus()
    VLCTvScreen(modifier, backdrop = backdrop) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = VLCTvLayout.OverscanHorizontal),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Column(
                Modifier
                    .width(248.dp)
                    .fillMaxHeight()
                    .padding(top = VLCTvLayout.OverscanVertical + 8.dp, bottom = VLCTvLayout.OverscanVertical),
            ) {
                if (heroArtwork != null) {
                    val shape = RoundedCornerShape(VLCTvLayout.CardCorner)
                    Box(
                        Modifier
                            .width(168.dp)
                            .aspectRatio(1f)
                            .shadow(20.dp, shape, clip = false, ambientColor = scheme.scrim.copy(alpha = 0.45f), spotColor = scheme.scrim.copy(alpha = 0.45f))
                            .clip(shape)
                            .background(scheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                        content = heroArtwork,
                    )
                    Spacer(Modifier.height(20.dp))
                }
                eyebrow?.takeIf { it.isNotBlank() }?.let {
                    Text(it.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.primary, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                metadata?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1)
                }
                Spacer(Modifier.height(20.dp))
                // The main action is a labelled pill; secondary collection actions are a compact
                // row of round controls so the whole column fits the 540dp-high safe area.
                actions.firstOrNull()?.let { action ->
                    VLCTvButton(
                        text = action.label,
                        icon = action.icon,
                        style = VLCTvButtonStyle.Primary,
                        onClick = { onAction(action) },
                        modifier = Modifier.fillMaxWidth().then(if (requestInitialFocus) Modifier.vlcTvInitialFocus(initialFocus) else Modifier),
                        previewKey = "action-${action.id}",
                    )
                }
                if (actions.size > 1) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        actions.drop(1).forEach { action ->
                            VLCTvIconButton(
                                contentDescription = action.label,
                                onClick = { onAction(action) },
                                icon = action.icon,
                                filled = true,
                                previewKey = "action-${action.id}",
                            )
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (tracks.isEmpty()) {
                    VLCTvEmptyState(title = emptyTitle, symbol = MaterialSymbols.Filled.QueueMusic)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = VLCTvLayout.OverscanVertical + 8.dp,
                            bottom = VLCTvLayout.OverscanVertical + 48.dp,
                            end = VLCTvLayout.OverscanHorizontal,
                            start = 6.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(tracks, key = { index, track -> "$index-${track.key}" }) { index, track ->
                            VLCTvTrackRow(
                                index = index,
                                track = track,
                                actions = trackActions,
                                isActionEnabled = { isTrackActionEnabled(it, index) },
                                onClick = { onTrackClick(index) },
                                onAction = { onTrackAction(it, index) },
                                onFocused = onTrackFocused?.let { { it(index) } },
                                artwork = trackArtwork?.let { slot -> { slot(index) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VLCTvTrackRow(
    index: Int,
    track: VLCTvTrack,
    actions: List<VLCTvTrackAction>,
    isActionEnabled: (Int) -> Boolean,
    onClick: () -> Unit,
    onAction: (Int) -> Unit,
    onFocused: (() -> Unit)?,
    artwork: (@Composable BoxScope.() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalVLCMotion.current
    var rowHasFocus by remember { mutableStateOf(false) }
    val previewFocused = LocalVLCTvPreviewFocus.current == track.key
    val actionsAlpha by animateFloatAsState(
        targetValue = if (rowHasFocus || previewFocused) 1f else 0f,
        animationSpec = tween(if (motion.reducedMotion) 0 else 160),
        label = "tv-track-actions",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                rowHasFocus = it.hasFocus
                if (it.hasFocus) onFocused?.invoke()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        VLCTvSurface(
            onClick = onClick,
            modifier = Modifier.weight(1f).heightIn(min = 60.dp),
            shape = RoundedCornerShape(12.dp),
            focusedScale = 1.02f,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedContainerColor = scheme.surfaceContainerHigh,
            contentDescription = track.title,
            previewKey = track.key,
        ) { focused ->
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (focused) scheme.primary else scheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(28.dp),
                )
                if (artwork != null) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(scheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                        content = artwork,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    track.subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                track.duration?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        actions.forEach { action ->
            val enabled = isActionEnabled(action.id)
            VLCTvIconButton(
                contentDescription = action.label,
                onClick = { onAction(action.id) },
                icon = action.icon,
                size = 40.dp,
                iconSize = 20.dp,
                enabled = enabled,
                modifier = Modifier.alpha(actionsAlpha),
            )
        }
    }
}
