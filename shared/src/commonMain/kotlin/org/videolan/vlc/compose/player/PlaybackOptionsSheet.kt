package org.videolan.vlc.compose.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.player.VideoScaleMode
import org.videolan.vlc.player.PlaybackTracks
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.done
import vlc_android.shared.generated.resources.ab_repeat
import vlc_android.shared.generated.resources.aspect_ratio
import vlc_android.shared.generated.resources.audio
import vlc_android.shared.generated.resources.subtitles
import vlc_android.shared.generated.resources.ab_repeat_reset
import vlc_android.shared.generated.resources.ab_repeat_stop
import vlc_android.shared.generated.resources.abrepeat_add_first_marker
import vlc_android.shared.generated.resources.abrepeat_add_second_marker
import vlc_android.shared.generated.resources.move_down
import vlc_android.shared.generated.resources.move_up
import vlc_android.shared.generated.resources.now_playing
import vlc_android.shared.generated.resources.playback_speed
import vlc_android.shared.generated.resources.playlist
import vlc_android.shared.generated.resources.remove_from_playlist
import vlc_android.shared.generated.resources.reset
import vlc_android.shared.generated.resources.stop_after_this

private val PlaybackRatePresets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * Shared advanced playback entry point.
 *
 * Both VLC Android and VLC iOS expose rate and queue management from the player. Keeping this
 * sheet in commonMain gives every host the same interaction while queue mutations remain native.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
internal fun PlaybackOptionsSheet(
    rate: Float,
    queue: List<MediaItem>,
    currentQueueIndex: Int,
    progressTime: Long,
    abRepeat: ABRepeat,
    abRepeatEnabled: Boolean,
    stopAfterCurrent: Boolean,
    videoScaleMode: VideoScaleMode,
    tracks: PlaybackTracks,
    showVideoOptions: Boolean,
    onSetRate: (Float) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onToggleABRepeat: () -> Unit,
    onSetABRepeatMarker: () -> Unit,
    onResetABRepeat: () -> Unit,
    onClearABRepeat: () -> Unit,
    onToggleStopAfterCurrent: () -> Unit,
    onSetVideoScaleMode: (VideoScaleMode) -> Unit,
    onSelectAudioTrack: (String) -> Unit,
    onSelectSubtitleTrack: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var previewRate by remember(rate) { mutableFloatStateOf(rate) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        stringResource(Res.string.playback_speed),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        playbackRateLabel(previewRate),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                TextButton(
                    onClick = {
                        previewRate = 1f
                        onSetRate(1f)
                    },
                    enabled = previewRate != 1f,
                ) {
                    Text(stringResource(Res.string.reset))
                }
            }

            Slider(
                value = previewRate.coerceIn(0.25f, 4f),
                onValueChange = { previewRate = it },
                onValueChangeFinished = { onSetRate(previewRate) },
                valueRange = 0.25f..4f,
                steps = 14,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaybackRatePresets.forEach { preset ->
                    FilterChip(
                        selected = previewRate == preset,
                        onClick = {
                            previewRate = preset
                            onSetRate(preset)
                        },
                        label = { Text(playbackRateLabel(preset)) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        stringResource(Res.string.ab_repeat),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (abRepeat.start >= 0L) {
                        Text(
                            buildString {
                                append("A  ")
                                append(formatPlaybackTime(abRepeat.start))
                                if (abRepeat.stop >= 0L) {
                                    append("   ·   B  ")
                                    append(formatPlaybackTime(abRepeat.stop))
                                }
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (!abRepeatEnabled) {
                    FilledTonalButton(onClick = onToggleABRepeat) {
                        Text(stringResource(Res.string.ab_repeat))
                    }
                }
            }
            if (abRepeatEnabled) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = onSetABRepeatMarker) {
                        Text(
                            stringResource(
                                if (abRepeat.start < 0L) {
                                    Res.string.abrepeat_add_first_marker
                                } else {
                                    Res.string.abrepeat_add_second_marker
                                }
                            )
                        )
                    }
                    TextButton(onClick = onResetABRepeat, enabled = abRepeat.start >= 0L) {
                        Text(stringResource(Res.string.ab_repeat_reset))
                    }
                    TextButton(onClick = onClearABRepeat) {
                        Text(stringResource(Res.string.ab_repeat_stop))
                    }
                }
                Text(
                    formatPlaybackTime(progressTime),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            if (showVideoOptions) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Text(
                    stringResource(Res.string.aspect_ratio),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VideoScaleMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == videoScaleMode,
                            onClick = { onSetVideoScaleMode(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }

            if (tracks.hasSelectableTracks) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                if (tracks.audio.size > 1) {
                    Text(
                        stringResource(Res.string.audio),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TrackChoices(tracks.audio, onSelectAudioTrack)
                }
                if (tracks.subtitles.isNotEmpty()) {
                    Text(
                        stringResource(Res.string.subtitles),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TrackChoices(tracks.subtitles, onSelectSubtitleTrack)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Text(
                "${stringResource(Res.string.playlist)} · ${queue.size}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            FilterChip(
                selected = stopAfterCurrent,
                onClick = onToggleStopAfterCurrent,
                label = { Text(stringResource(Res.string.stop_after_this)) },
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
            ) {
                itemsIndexed(
                    items = queue,
                    key = { index, item -> "${item.id}:${item.uri}:$index" },
                ) { index, item ->
                    QueueItem(
                        item = item,
                        index = index,
                        selected = index == currentQueueIndex,
                        isFirst = index == 0,
                        isLast = index == queue.lastIndex,
                        onPlay = { onPlayQueueItem(index) },
                        onMoveUp = { onMoveQueueItem(index, index - 1) },
                        onMoveDown = { onMoveQueueItem(index, index + 1) },
                        onRemove = { onRemoveQueueItem(index) },
                    )
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.done))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TrackChoices(
    tracks: List<org.videolan.vlc.player.PlaybackTrack>,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tracks.forEach { track ->
            FilterChip(
                selected = track.selected,
                onClick = { onSelect(track.id) },
                label = { Text(track.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun QueueItem(
    item: MediaItem,
    index: Int,
    selected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = !selected, onClick = onPlay)
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (selected) "•" else "${index + 1}",
                modifier = Modifier.padding(end = 12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.displayTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge,
                )
                val supportingText = if (selected) {
                    stringResource(Res.string.now_playing)
                } else {
                    listOfNotNull(item.artist, item.album).joinToString(" · ")
                }
                if (supportingText.isNotBlank()) {
                    Text(
                        supportingText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(
                    MaterialSymbols.Filled.ArrowUpward,
                    contentDescription = stringResource(Res.string.move_up),
                )
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(
                    MaterialSymbols.Filled.ArrowDownward,
                    contentDescription = stringResource(Res.string.move_down),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    MaterialSymbols.Filled.Delete,
                    contentDescription = stringResource(Res.string.remove_from_playlist),
                )
            }
        }
    }
}
