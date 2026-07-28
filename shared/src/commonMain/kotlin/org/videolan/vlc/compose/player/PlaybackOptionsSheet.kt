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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import org.videolan.vlc.player.PlaybackVideoCrop
import org.videolan.vlc.player.VideoCropMode
import org.videolan.vlc.player.PlaybackVideoAdjust
import org.videolan.vlc.player.VideoAdjustParameter
import org.videolan.vlc.player.PlaybackTracks
import org.videolan.vlc.player.PlaybackDelays
import org.videolan.vlc.player.SleepTimerState
import org.videolan.vlc.player.PlaybackChapters
import org.videolan.vlc.player.PlaybackEqualizer
import org.videolan.vlc.player.PlaybackBookmarks
import org.videolan.vlc.player.PlaybackBookmark
import org.videolan.vlc.player.PlaybackRate
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.done
import vlc_android.shared.generated.resources.ab_repeat
import vlc_android.shared.generated.resources.aspect_ratio
import vlc_android.shared.generated.resources.audio
import vlc_android.shared.generated.resources.subtitles
import vlc_android.shared.generated.resources.subtitle_select
import vlc_android.shared.generated.resources.audio_delay
import vlc_android.shared.generated.resources.spu_delay
import vlc_android.shared.generated.resources.sleep_title
import vlc_android.shared.generated.resources.wait_before_sleep
import vlc_android.shared.generated.resources.cancel
import vlc_android.shared.generated.resources.go_to_chapter
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
import vlc_android.shared.generated.resources.equalizer
import vlc_android.shared.generated.resources.enable_equalizer
import vlc_android.shared.generated.resources.preamp
import vlc_android.shared.generated.resources.video_crop
import vlc_android.shared.generated.resources.video_adjust
import vlc_android.shared.generated.resources.bookmarks
import vlc_android.shared.generated.resources.add_bookmark
import vlc_android.shared.generated.resources.delete
import vlc_android.shared.generated.resources.rename
import vlc_android.shared.generated.resources.save
import vlc_android.shared.generated.resources.previous_bookmark
import vlc_android.shared.generated.resources.next_bookmark

private val PlaybackRatePresets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 4f, 8f)

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
    videoCrop: PlaybackVideoCrop,
    videoAdjust: PlaybackVideoAdjust,
    tracks: PlaybackTracks,
    delays: PlaybackDelays,
    sleepTimer: SleepTimerState,
    chapters: PlaybackChapters,
    equalizer: PlaybackEqualizer,
    bookmarks: PlaybackBookmarks,
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
    onSetVideoCrop: (VideoCropMode) -> Unit,
    onSetVideoAdjustEnabled: (Boolean) -> Unit,
    onSetVideoAdjust: (VideoAdjustParameter, Float) -> Unit,
    onResetVideoAdjust: () -> Unit,
    onSelectAudioTrack: (String) -> Unit,
    onSelectSubtitleTrack: (String) -> Unit,
    onSetAudioDelay: (Long) -> Unit,
    onSetSubtitleDelay: (Long) -> Unit,
    onSetSleepTimer: (Long, Boolean) -> Unit,
    onClearSleepTimer: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSelectEqualizerPreset: (String) -> Unit,
    onSetEqualizerPreamp: (Float) -> Unit,
    onSetEqualizerBand: (Int, Float) -> Unit,
    onAddBookmark: () -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onRenameBookmark: (String, String) -> Unit,
    onSeekBookmark: (Long) -> Unit,
    onPreviousBookmark: () -> Unit,
    onNextBookmark: () -> Unit,
    showSubtitleImport: Boolean,
    onImportSubtitle: () -> Unit,
    onDismiss: () -> Unit,
) {
    var previewRate by remember(rate) { mutableFloatStateOf(rate) }
    var bookmarkToRename by remember { mutableStateOf<PlaybackBookmark?>(null) }
    var bookmarkName by remember { mutableStateOf("") }
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
                value = PlaybackRate.normalize(previewRate),
                onValueChange = { previewRate = it },
                onValueChangeFinished = { onSetRate(previewRate) },
                valueRange = PlaybackRate.MIN..PlaybackRate.MAX,
                steps = 30,
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
                if (videoCrop.supported) {
                    Text(
                        stringResource(Res.string.video_crop),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        VideoCropMode.entries.forEach { mode ->
                            FilterChip(
                                selected = mode == videoCrop.mode,
                                onClick = { onSetVideoCrop(mode) },
                                label = { Text(mode.label) },
                            )
                        }
                    }
                }
                if (videoAdjust.supported) {
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(Res.string.video_adjust),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Switch(checked = videoAdjust.enabled, onCheckedChange = onSetVideoAdjustEnabled)
                    }
                    if (videoAdjust.enabled) {
                        VideoAdjustParameter.entries.forEach { parameter ->
                            val value = videoAdjust.value(parameter)
                            Text(
                                "${parameter.label}  ${value.roundToInt()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = value.coerceIn(parameter.minimum, parameter.maximum),
                                onValueChange = { onSetVideoAdjust(parameter, it) },
                                valueRange = parameter.minimum..parameter.maximum,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        TextButton(onClick = onResetVideoAdjust, modifier = Modifier.align(Alignment.End)) {
                            Text(stringResource(Res.string.reset))
                        }
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
            if (showSubtitleImport) {
                TextButton(onClick = onImportSubtitle, modifier = Modifier.align(Alignment.Start)) {
                    Text(stringResource(Res.string.subtitle_select))
                }
            }

            if (delays.supported) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                DelayChoices(
                    title = stringResource(Res.string.audio_delay),
                    delayUs = delays.audioUs,
                    onSetDelay = onSetAudioDelay,
                )
                DelayChoices(
                    title = stringResource(Res.string.spu_delay),
                    delayUs = delays.subtitleUs,
                    onSetDelay = onSetSubtitleDelay,
                )
            }

            if (equalizer.supported) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            stringResource(Res.string.equalizer),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(Res.string.enable_equalizer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = equalizer.enabled, onCheckedChange = onSetEqualizerEnabled)
                }
                if (equalizer.enabled) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        equalizer.presets.forEach { preset ->
                            FilterChip(
                                selected = preset.id == equalizer.selectedPresetId,
                                onClick = { onSelectEqualizerPreset(preset.id) },
                                label = { Text(preset.label) },
                            )
                        }
                    }
                    EqualizerSlider(
                        label = stringResource(Res.string.preamp),
                        value = equalizer.preampDb,
                        onValueChangeFinished = onSetEqualizerPreamp,
                    )
                    equalizer.bands.forEach { band ->
                        EqualizerSlider(
                            label = band.label,
                            value = band.amplificationDb,
                            onValueChangeFinished = { onSetEqualizerBand(band.index, it) },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            SleepTimerChoices(
                state = sleepTimer,
                onSetTimer = onSetSleepTimer,
                onClear = onClearSleepTimer,
            )

            if (chapters.entries.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Text(stringResource(Res.string.go_to_chapter), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chapters.entries.forEach { chapter ->
                        FilterChip(
                            selected = chapter.selected,
                            onClick = { onSelectChapter(chapter.index) },
                            label = { Text(chapter.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }

            if (bookmarks.supported) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(Res.string.bookmarks), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onAddBookmark) { Text(stringResource(Res.string.add_bookmark)) }
                }
                if (bookmarks.entries.isNotEmpty()) {
                    val hasPreviousBookmark = bookmarks.entries.any { it.timeMs < progressTime }
                    val hasNextBookmark = bookmarks.entries.any { it.timeMs > progressTime }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onPreviousBookmark,
                            enabled = hasPreviousBookmark,
                        ) {
                            Text(stringResource(Res.string.previous_bookmark))
                        }
                        FilledTonalButton(
                            onClick = onNextBookmark,
                            enabled = hasNextBookmark,
                        ) {
                            Text(stringResource(Res.string.next_bookmark))
                        }
                    }
                }
                bookmarks.entries.forEach { bookmark ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { onSeekBookmark(bookmark.timeMs) }, modifier = Modifier.weight(1f)) {
                            Text(
                                "${bookmark.title} · ${formatPlaybackTime(bookmark.timeMs)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { onRemoveBookmark(bookmark.id) }) {
                            Text(stringResource(Res.string.delete))
                        }
                        TextButton(onClick = {
                            bookmarkToRename = bookmark
                            bookmarkName = bookmark.title
                        }) {
                            Text(stringResource(Res.string.rename))
                        }
                    }
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
    bookmarkToRename?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { bookmarkToRename = null },
            title = { Text(stringResource(Res.string.rename)) },
            text = {
                OutlinedTextField(
                    value = bookmarkName,
                    onValueChange = { bookmarkName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameBookmark(bookmark.id, bookmarkName)
                        bookmarkToRename = null
                    },
                    enabled = bookmarkName.isNotBlank(),
                ) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToRename = null }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

@Composable
private fun EqualizerSlider(
    label: String,
    value: Float,
    onValueChangeFinished: (Float) -> Unit,
) {
    var preview by remember(value) { mutableFloatStateOf(value.coerceIn(-20f, 20f)) }
    Text(
        "$label  ${preview.roundToInt()} dB",
        style = MaterialTheme.typography.labelLarge,
    )
    Slider(
        value = preview,
        onValueChange = { preview = it },
        onValueChangeFinished = { onValueChangeFinished(preview) },
        valueRange = -20f..20f,
        modifier = Modifier.fillMaxWidth(),
    )
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
@OptIn(ExperimentalLayoutApi::class)
private fun DelayChoices(title: String, delayUs: Long, onSetDelay: (Long) -> Unit) {
    val delayMs = delayUs / 1_000L
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("${delayMs} ms", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = false, onClick = { onSetDelay(delayUs - 500_000L) }, label = { Text("−500 ms") })
            FilterChip(selected = delayUs == 0L, onClick = { onSetDelay(0L) }, label = { Text(stringResource(Res.string.reset)) })
            FilterChip(selected = false, onClick = { onSetDelay(delayUs + 500_000L) }, label = { Text("+500 ms") })
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SleepTimerChoices(
    state: SleepTimerState,
    onSetTimer: (Long, Boolean) -> Unit,
    onClear: () -> Unit,
) {
    var waitForCurrentItem by remember(state.isActive) { mutableStateOf(state.waitForCurrentItem) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.sleep_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (state.isActive) {
                Text(
                    if (state.awaitingCurrentItemEnd) "After this item" else formatPlaybackTime(state.remainingMillis),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15L, 30L, 60L).forEach { minutes ->
                FilterChip(
                    selected = state.isActive && state.durationMillis == minutes * 60_000L,
                    onClick = { onSetTimer(minutes * 60_000L, waitForCurrentItem) },
                    label = { Text("$minutes min") },
                )
            }
            if (state.isActive) {
                FilterChip(selected = false, onClick = onClear, label = { Text(stringResource(Res.string.cancel)) })
            }
        }
        FilterChip(
            selected = waitForCurrentItem,
            onClick = { waitForCurrentItem = !waitForCurrentItem },
            label = { Text(stringResource(Res.string.wait_before_sleep)) },
        )
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
