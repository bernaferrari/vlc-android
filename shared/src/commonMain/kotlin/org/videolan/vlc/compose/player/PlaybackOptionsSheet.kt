@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.videolan.vlc.compose.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import org.videolan.vlc.compose.components.VLCRenameItemDialog
import org.videolan.vlc.compose.components.VLCModalHeader
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.VerticalSlider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCExpandableContent
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.player.PlaybackBookmarks
import org.videolan.vlc.player.PlaybackChapters
import org.videolan.vlc.player.PlaybackDelays
import org.videolan.vlc.player.PlaybackEqualizer
import org.videolan.vlc.player.PlaybackRate
import org.videolan.vlc.player.PlaybackTracks
import org.videolan.vlc.player.PlaybackVideoAdjust
import org.videolan.vlc.player.PlaybackVideoCrop
import org.videolan.vlc.player.SleepTimerState
import org.videolan.vlc.player.VideoAdjustParameter
import org.videolan.vlc.player.VideoCropMode
import org.videolan.vlc.player.VideoScaleMode
import org.videolan.vlc.util.VlcTextUtils
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.*

enum class PlaybackSheetDestination {
    SPEED,
    QUEUE,
    TOOLS,
}

private enum class PlaybackToolSection {
    AB_REPEAT,
    VIDEO,
    TRACKS,
    DELAYS,
    EQUALIZER,
    SLEEP,
    CHAPTERS,
    BOOKMARKS,
}

private val PlaybackRatePresets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 4f, 8f)

/**
 * A focused player control center. Speed and queue are first-class destinations; infrequent
 * decoder tools remain available without forcing every control into one scrolling wall.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PlaybackOptionsSheet(
    initialDestination: PlaybackSheetDestination,
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
    onSeekTo: (Long) -> Unit,
    onSavePlaylist: (String) -> Unit,
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
    var destination by remember(initialDestination) { mutableStateOf(initialDestination) }
    var previewRate by remember(rate) { mutableFloatStateOf(PlaybackRate.normalize(rate)) }
    var expandedTool by remember {
        mutableStateOf<PlaybackToolSection?>(
            PlaybackToolSection.EQUALIZER.takeIf { equalizer.enabled }
        )
    }
    var jumpToTimeVisible by remember { mutableStateOf(false) }
    var savePlaylistVisible by remember { mutableStateOf(false) }
    var bookmarkToRename by remember { mutableStateOf<org.videolan.vlc.player.PlaybackBookmark?>(null) }
    val motion = LocalVLCMotion.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .animateContentSize(
                    animationSpec = if (motion.reducedMotion) snap() else spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .padding(bottom = VLCLayout.SheetBottomPadding),
        ) {
            SheetHandle()
            PlayerSheetHeader(destination = destination, onDismiss = onDismiss)
            PlayerDestinationBar(
                selected = destination,
                onSelect = { destination = it },
                modifier = Modifier.padding(horizontal = VLCLayout.SheetHorizontalPadding),
            )
            Spacer(Modifier.height(12.dp))
            Crossfade(
                targetState = destination,
                animationSpec = if (motion.reducedMotion) snap() else tween(
                    durationMillis = motion.durationShort,
                    easing = VLCMotion.Emphasized,
                ),
                label = "player-sheet-destination",
            ) { page ->
                when (page) {
                    PlaybackSheetDestination.SPEED -> SpeedPage(
                        rate = previewRate,
                        onPreviewRate = { previewRate = it },
                        onCommitRate = onSetRate,
                    )

                    PlaybackSheetDestination.QUEUE -> QueuePage(
                        queue = queue,
                        currentQueueIndex = currentQueueIndex,
                        stopAfterCurrent = stopAfterCurrent,
                        onToggleStopAfterCurrent = onToggleStopAfterCurrent,
                        onSavePlaylist = { savePlaylistVisible = true },
                        onPlayQueueItem = onPlayQueueItem,
                        onMoveQueueItem = onMoveQueueItem,
                        onRemoveQueueItem = onRemoveQueueItem,
                    )

                    PlaybackSheetDestination.TOOLS -> ToolsPage(
                        expanded = expandedTool,
                        onExpandedChange = { expandedTool = if (expandedTool == it) null else it },
                        progressTime = progressTime,
                        abRepeat = abRepeat,
                        abRepeatEnabled = abRepeatEnabled,
                        videoScaleMode = videoScaleMode,
                        videoCrop = videoCrop,
                        videoAdjust = videoAdjust,
                        tracks = tracks,
                        delays = delays,
                        sleepTimer = sleepTimer,
                        chapters = chapters,
                        equalizer = equalizer,
                        bookmarks = bookmarks,
                        showVideoOptions = showVideoOptions,
                        showSubtitleImport = showSubtitleImport,
                        onJumpToTime = { jumpToTimeVisible = true },
                        onToggleABRepeat = onToggleABRepeat,
                        onSetABRepeatMarker = onSetABRepeatMarker,
                        onResetABRepeat = onResetABRepeat,
                        onClearABRepeat = onClearABRepeat,
                        onSetVideoScaleMode = onSetVideoScaleMode,
                        onSetVideoCrop = onSetVideoCrop,
                        onSetVideoAdjustEnabled = onSetVideoAdjustEnabled,
                        onSetVideoAdjust = onSetVideoAdjust,
                        onResetVideoAdjust = onResetVideoAdjust,
                        onSelectAudioTrack = onSelectAudioTrack,
                        onSelectSubtitleTrack = onSelectSubtitleTrack,
                        onSetAudioDelay = onSetAudioDelay,
                        onSetSubtitleDelay = onSetSubtitleDelay,
                        onSetSleepTimer = onSetSleepTimer,
                        onClearSleepTimer = onClearSleepTimer,
                        onSelectChapter = onSelectChapter,
                        onSetEqualizerEnabled = onSetEqualizerEnabled,
                        onSelectEqualizerPreset = onSelectEqualizerPreset,
                        onSetEqualizerPreamp = onSetEqualizerPreamp,
                        onSetEqualizerBand = onSetEqualizerBand,
                        onAddBookmark = onAddBookmark,
                        onRemoveBookmark = onRemoveBookmark,
                        onRenameBookmark = { bookmark ->
                            bookmarkToRename = bookmark
                        },
                        onSeekBookmark = onSeekBookmark,
                        onPreviousBookmark = onPreviousBookmark,
                        onNextBookmark = onNextBookmark,
                        onImportSubtitle = onImportSubtitle,
                    )
                }
            }
        }
    }

    if (jumpToTimeVisible) {
        VLCRenameItemDialog(
            title = stringResource(Res.string.jump_to_time),
            initialValue = "",
            fieldLabel = "HH:MM:SS",
            supportingText = stringResource(Res.string.seek_examples),
            isValid = { parsePlaybackTimestamp(it) != null },
            confirmLabel = stringResource(Res.string.done),
            cancelLabel = stringResource(Res.string.cancel),
            onConfirm = { value ->
                parsePlaybackTimestamp(value)?.let(onSeekTo)
                jumpToTimeVisible = false
            },
            onDismiss = { jumpToTimeVisible = false },
        )
    }

    if (savePlaylistVisible) {
        VLCRenameItemDialog(
            title = stringResource(Res.string.playlist_save),
            initialValue = "",
            fieldLabel = stringResource(Res.string.playlist_name_hint),
            confirmLabel = stringResource(Res.string.save),
            cancelLabel = stringResource(Res.string.cancel),
            onConfirm = { name ->
                onSavePlaylist(name)
                savePlaylistVisible = false
            },
            onDismiss = { savePlaylistVisible = false },
        )
    }

    bookmarkToRename?.let { bookmark ->
        VLCRenameItemDialog(
            title = stringResource(Res.string.rename),
            initialValue = bookmark.title,
            confirmLabel = stringResource(Res.string.save),
            cancelLabel = stringResource(Res.string.cancel),
            onConfirm = { name ->
                onRenameBookmark(bookmark.id, name)
                bookmarkToRename = null
            },
            onDismiss = { bookmarkToRename = null },
        )
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(width = 36.dp, height = 4.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f),
        ) {}
    }
}

@Composable
private fun PlayerSheetHeader(
    destination: PlaybackSheetDestination,
    onDismiss: () -> Unit,
) {
    val title = when (destination) {
        PlaybackSheetDestination.SPEED -> stringResource(Res.string.playback_speed)
        PlaybackSheetDestination.QUEUE -> stringResource(Res.string.up_next)
        PlaybackSheetDestination.TOOLS -> stringResource(Res.string.player_controls)
    }
    VLCModalHeader(
        title = title,
        onDismiss = onDismiss,
        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 14.dp),
    )
}

@Composable
private fun PlayerDestinationBar(
    selected: PlaybackSheetDestination,
    onSelect: (PlaybackSheetDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonGroup(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        PlaybackSheetDestination.entries.forEachIndexed { index, destination ->
            val interactionSource = remember { MutableInteractionSource() }
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                PlaybackSheetDestination.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = selected == destination,
                onCheckedChange = { onSelect(destination) },
                modifier = Modifier
                    .weight(1f)
                    .animateWidth(interactionSource)
                    .height(52.dp),
                shapes = shapes,
                colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                interactionSource = interactionSource,
            ) {
                Text(
                    text = when (destination) {
                        PlaybackSheetDestination.SPEED -> stringResource(Res.string.speed)
                        PlaybackSheetDestination.QUEUE -> stringResource(Res.string.queue)
                        PlaybackSheetDestination.TOOLS -> stringResource(Res.string.tools)
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SpeedPage(
    rate: Float,
    onPreviewRate: (Float) -> Unit,
    onCommitRate: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VLCLayout.SheetHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = playbackRateLabel(rate),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(
                if (rate == 1f) Res.string.normal_speed
                else if (rate < 1f) Res.string.slower_playback
                else Res.string.faster_playback,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))
        Slider(
            value = PlaybackRate.normalize(rate),
            onValueChange = { value ->
                onPreviewRate(snapPlaybackRate(value))
            },
            onValueChangeFinished = { onCommitRate(rate) },
            valueRange = PlaybackRate.MIN..PlaybackRate.MAX,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("¼×", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text("8×", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(20.dp))
        val presetRows = PlaybackRatePresets.chunked(4)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            presetRows.forEachIndexed { rowIndex, presets ->
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    presets.forEachIndexed { columnIndex, preset ->
                        val interactionSource = remember { MutableInteractionSource() }
                        FilledTonalButton(
                            onClick = {
                                onPreviewRate(preset)
                                onCommitRate(preset)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .animateWidth(interactionSource)
                                .height(52.dp)
                                .semantics { selected = rate == preset },
                            shapes = androidx.compose.material3.ButtonDefaults.shapes(
                                shape = playbackRateGridShape(
                                    rowIndex = rowIndex,
                                    columnIndex = columnIndex,
                                    rowCount = presetRows.size,
                                    columnCount = presets.size,
                                ),
                                pressedShape = playbackRateGridShape(
                                    rowIndex = rowIndex,
                                    columnIndex = columnIndex,
                                    rowCount = presetRows.size,
                                    columnCount = presets.size,
                                    pressed = true,
                                ),
                            ),
                            interactionSource = interactionSource,
                        ) {
                            Text(
                                playbackRateLabel(preset),
                                fontWeight = FontWeight.SemiBold,
                                color = if (rate == preset) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.fine_tune_pace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QueuePage(
    queue: List<MediaItem>,
    currentQueueIndex: Int,
    stopAfterCurrent: Boolean,
    onToggleStopAfterCurrent: () -> Unit,
    onSavePlaylist: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = VLCLayout.SheetHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "queue-header") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pluralStringResource(Res.plurals.items_count, queue.size, queue.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.tap_queue_item),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = onSavePlaylist, enabled = queue.isNotEmpty()) {
                    Text(stringResource(Res.string.save))
                }
            }
        }
        item(key = "stop-after-current") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = stopAfterCurrent, role = Role.Switch, onValueChange = { onToggleStopAfterCurrent() })
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.stop_after_this), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(Res.string.stop_after_current_summary),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = stopAfterCurrent, onCheckedChange = null)
                }
            }
        }
        if (queue.isEmpty()) {
            item(key = "empty-queue") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(MaterialSymbols.Outlined.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                    Text(stringResource(Res.string.queue_empty), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.queue_empty_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            itemsIndexed(items = queue, key = { index, item -> "${item.id}:${item.uri}:$index" }) { index, item ->
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
    var menuVisible by remember { mutableStateOf(false) }
    val itemShape = when {
        isFirst && isLast -> RoundedCornerShape(18.dp)
        isFirst -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
        isLast -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        else -> RoundedCornerShape(10.dp)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = itemShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !selected, onClick = onPlay)
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selected) {
                        Icon(MaterialSymbols.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(25.dp))
                    } else {
                        Text("${index + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.displayTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                val metadata = if (selected) {
                    stringResource(Res.string.now_playing)
                } else {
                    VlcTextUtils.separatedString(arrayOf(item.artist, item.album))
                }
                if (metadata.isNotBlank()) {
                    Text(
                        metadata,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuVisible = true }) {
                    Icon(MaterialSymbols.Filled.MoreVert, contentDescription = stringResource(Res.string.more_options))
                }
                DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.move_up)) },
                        leadingIcon = { Icon(MaterialSymbols.Filled.ArrowUpward, contentDescription = null) },
                        enabled = !isFirst,
                        onClick = { menuVisible = false; onMoveUp() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.move_down)) },
                        leadingIcon = { Icon(MaterialSymbols.Filled.ArrowDownward, contentDescription = null) },
                        enabled = !isLast,
                        onClick = { menuVisible = false; onMoveDown() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.remove_from_playlist)) },
                        leadingIcon = { Icon(MaterialSymbols.Filled.Delete, contentDescription = null) },
                        onClick = { menuVisible = false; onRemove() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolsPage(
    expanded: PlaybackToolSection?,
    onExpandedChange: (PlaybackToolSection) -> Unit,
    progressTime: Long,
    abRepeat: ABRepeat,
    abRepeatEnabled: Boolean,
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
    showSubtitleImport: Boolean,
    onJumpToTime: () -> Unit,
    onToggleABRepeat: () -> Unit,
    onSetABRepeatMarker: () -> Unit,
    onResetABRepeat: () -> Unit,
    onClearABRepeat: () -> Unit,
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
    onRenameBookmark: (org.videolan.vlc.player.PlaybackBookmark) -> Unit,
    onSeekBookmark: (Long) -> Unit,
    onPreviousBookmark: () -> Unit,
    onNextBookmark: () -> Unit,
    onImportSubtitle: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 540.dp)
            .padding(horizontal = VLCLayout.SheetHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            QuickToolRow(
                title = stringResource(Res.string.jump_to_time),
                summary = stringResource(Res.string.seek_exact_timestamp),
                icon = MaterialSymbols.Filled.History,
                onClick = onJumpToTime,
            )
        }
        item {
            ToolCard(
                title = stringResource(Res.string.ab_repeat),
                icon = MaterialSymbols.Filled.Repeat,
                summary = when {
                    !abRepeatEnabled -> stringResource(Res.string.loop_precise_section)
                    abRepeat.start < 0L -> stringResource(Res.string.choose_start_point)
                    abRepeat.stop < 0L -> stringResource(
                        Res.string.choose_end_point,
                        formatPlaybackTime(abRepeat.start),
                    )
                    else -> stringResource(
                        Res.string.ab_points,
                        formatPlaybackTime(abRepeat.start),
                        formatPlaybackTime(abRepeat.stop),
                    )
                },
                expanded = expanded == PlaybackToolSection.AB_REPEAT,
                onClick = { onExpandedChange(PlaybackToolSection.AB_REPEAT) },
            ) {
                ABRepeatContent(
                    progressTime = progressTime,
                    abRepeat = abRepeat,
                    enabled = abRepeatEnabled,
                    onToggle = onToggleABRepeat,
                    onSetMarker = onSetABRepeatMarker,
                    onReset = onResetABRepeat,
                    onClear = onClearABRepeat,
                )
            }
        }
        if (showVideoOptions) {
            item {
                ToolCard(
                    title = stringResource(Res.string.video_fit_crop),
                    summary = "${videoScaleMode.label} · ${videoCrop.mode.label}",
                    icon = MaterialSymbols.Filled.VideoLibrary,
                    expanded = expanded == PlaybackToolSection.VIDEO,
                    onClick = { onExpandedChange(PlaybackToolSection.VIDEO) },
                ) {
                    VideoContent(
                        scaleMode = videoScaleMode,
                        crop = videoCrop,
                        adjust = videoAdjust,
                        onSetScaleMode = onSetVideoScaleMode,
                        onSetCrop = onSetVideoCrop,
                        onSetAdjustEnabled = onSetVideoAdjustEnabled,
                        onSetAdjust = onSetVideoAdjust,
                        onResetAdjust = onResetVideoAdjust,
                    )
                }
            }
        }
        if (tracks.hasSelectableTracks || showSubtitleImport) {
            item {
                ToolCard(
                    title = stringResource(Res.string.tracks),
                    summary = stringResource(Res.string.tracks_summary),
                    icon = MaterialSymbols.Filled.MusicNote,
                    expanded = expanded == PlaybackToolSection.TRACKS,
                    onClick = { onExpandedChange(PlaybackToolSection.TRACKS) },
                ) {
                    TracksContent(
                        tracks = tracks,
                        showSubtitleImport = showSubtitleImport,
                        onSelectAudioTrack = onSelectAudioTrack,
                        onSelectSubtitleTrack = onSelectSubtitleTrack,
                        onImportSubtitle = onImportSubtitle,
                    )
                }
            }
        }
        if (delays.supported) {
            item {
                ToolCard(
                    title = stringResource(Res.string.synchronization),
                    summary = stringResource(
                        Res.string.synchronization_summary,
                        delays.audioUs / 1_000L,
                        delays.subtitleUs / 1_000L,
                    ),
                    icon = MaterialSymbols.Filled.Tune,
                    expanded = expanded == PlaybackToolSection.DELAYS,
                    onClick = { onExpandedChange(PlaybackToolSection.DELAYS) },
                ) {
                    DelayChoices(stringResource(Res.string.audio_delay), delays.audioUs, onSetAudioDelay)
                    DelayChoices(stringResource(Res.string.spu_delay), delays.subtitleUs, onSetSubtitleDelay)
                }
            }
        }
        if (equalizer.supported) {
            item {
                val equalizerExpanded = expanded == PlaybackToolSection.EQUALIZER
                val selectedPreset = equalizer.presets.firstOrNull { it.id == equalizer.selectedPresetId }?.label
                ToolCard(
                    title = stringResource(Res.string.equalizer),
                    summary = when {
                        !equalizer.enabled -> stringResource(Res.string.equalizer_off)
                        selectedPreset != null -> selectedPreset
                        else -> stringResource(Res.string.custom_sound)
                    },
                    icon = MaterialSymbols.Filled.Tune,
                    expanded = equalizer.enabled && equalizerExpanded,
                    onClick = {
                        if (equalizer.enabled) {
                            onExpandedChange(PlaybackToolSection.EQUALIZER)
                        } else {
                            onSetEqualizerEnabled(true)
                            if (!equalizerExpanded) onExpandedChange(PlaybackToolSection.EQUALIZER)
                        }
                    },
                    trailingAction = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(if (equalizer.enabled) Res.string.on else Res.string.off),
                                color = if (equalizer.enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(24.dp),
                            )
                            Switch(
                                checked = equalizer.enabled,
                                onCheckedChange = { enabled ->
                                    onSetEqualizerEnabled(enabled)
                                    if (enabled && !equalizerExpanded) {
                                        onExpandedChange(PlaybackToolSection.EQUALIZER)
                                    } else if (!enabled && equalizerExpanded) {
                                        onExpandedChange(PlaybackToolSection.EQUALIZER)
                                    }
                                },
                            )
                        }
                    },
                ) {
                    EqualizerContent(
                        equalizer = equalizer,
                        onSelectPreset = onSelectEqualizerPreset,
                        onSetPreamp = onSetEqualizerPreamp,
                        onSetBand = onSetEqualizerBand,
                    )
                }
            }
        }
        item {
            ToolCard(
                title = stringResource(Res.string.sleep_title),
                icon = MaterialSymbols.Filled.History,
                summary = when {
                    sleepTimer.awaitingCurrentItemEnd -> stringResource(Res.string.after_this_item)
                    sleepTimer.isActive -> formatPlaybackTime(sleepTimer.remainingMillis)
                    else -> stringResource(Res.string.off)
                },
                expanded = expanded == PlaybackToolSection.SLEEP,
                onClick = { onExpandedChange(PlaybackToolSection.SLEEP) },
            ) {
                SleepTimerChoices(sleepTimer, onSetSleepTimer, onClearSleepTimer)
            }
        }
        if (chapters.entries.isNotEmpty()) {
            item {
                ToolCard(
                    title = stringResource(Res.string.go_to_chapter),
                    summary = chapters.entries.firstOrNull { it.selected }?.title ?: pluralStringResource(
                        Res.plurals.chapters_count,
                        chapters.entries.size,
                        chapters.entries.size,
                    ),
                    icon = MaterialSymbols.Filled.ViewList,
                    expanded = expanded == PlaybackToolSection.CHAPTERS,
                    onClick = { onExpandedChange(PlaybackToolSection.CHAPTERS) },
                ) {
                    ChoiceFlow {
                        chapters.entries.forEach { chapter ->
                            FilterChip(
                                selected = chapter.selected,
                                onClick = { onSelectChapter(chapter.index) },
                                label = { Text(chapter.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
            }
        }
        if (bookmarks.supported) {
            item {
                ToolCard(
                    title = stringResource(Res.string.bookmarks),
                    summary = if (bookmarks.entries.isEmpty()) {
                        stringResource(Res.string.no_bookmarks)
                    } else {
                        pluralStringResource(
                            Res.plurals.saved_bookmarks_quantity,
                            bookmarks.entries.size,
                            bookmarks.entries.size,
                        )
                    },
                    icon = MaterialSymbols.Filled.Star,
                    expanded = expanded == PlaybackToolSection.BOOKMARKS,
                    onClick = { onExpandedChange(PlaybackToolSection.BOOKMARKS) },
                ) {
                    BookmarksContent(
                        progressTime = progressTime,
                        bookmarks = bookmarks,
                        onAdd = onAddBookmark,
                        onRemove = onRemoveBookmark,
                        onRename = onRenameBookmark,
                        onSeek = onSeekBookmark,
                        onPrevious = onPreviousBookmark,
                        onNext = onNextBookmark,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(2.dp)) }
    }
}

@Composable
private fun QuickToolRow(
    title: String,
    summary: String,
    icon: MaterialIcon,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(MaterialSymbols.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    summary: String,
    icon: MaterialIcon,
    expanded: Boolean,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = LocalVLCMotion.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = if (motion.reducedMotion) snap() else tween(
            durationMillis = motion.durationShort,
            easing = VLCMotion.Emphasized,
        ),
        label = "player-tool-chevron",
    )
    val expansionState = stringResource(if (expanded) Res.string.expanded else Res.string.collapsed)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { stateDescription = expansionState }
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(25.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (trailingAction != null) {
                    trailingAction()
                } else {
                    Icon(
                        MaterialSymbols.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                    )
                }
            }
            VLCExpandableContent(visible = expanded) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f),
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun ABRepeatContent(
    progressTime: Long,
    abRepeat: ABRepeat,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSetMarker: () -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
) {
    if (!enabled) {
        FilledTonalButton(onClick = onToggle) { Text(stringResource(Res.string.start_ab_repeat)) }
        return
    }
    Text(
        stringResource(Res.string.current_position_value, formatPlaybackTime(progressTime)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onSetMarker) {
            Text(
                stringResource(
                    if (abRepeat.start < 0L) Res.string.abrepeat_add_first_marker
                    else Res.string.abrepeat_add_second_marker
                )
            )
        }
        TextButton(onClick = onReset, enabled = abRepeat.start >= 0L) { Text(stringResource(Res.string.reset)) }
        TextButton(onClick = onClear) { Text(stringResource(Res.string.ab_repeat_stop)) }
    }
}

@Composable
private fun VideoContent(
    scaleMode: VideoScaleMode,
    crop: PlaybackVideoCrop,
    adjust: PlaybackVideoAdjust,
    onSetScaleMode: (VideoScaleMode) -> Unit,
    onSetCrop: (VideoCropMode) -> Unit,
    onSetAdjustEnabled: (Boolean) -> Unit,
    onSetAdjust: (VideoAdjustParameter, Float) -> Unit,
    onResetAdjust: () -> Unit,
) {
    ChoiceFlow {
        VideoScaleMode.entries.forEach { mode ->
            FilterChip(selected = mode == scaleMode, onClick = { onSetScaleMode(mode) }, label = { Text(mode.label) })
        }
    }
    if (crop.supported) {
        Text(stringResource(Res.string.video_crop), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ChoiceFlow {
            VideoCropMode.entries.forEach { mode ->
                FilterChip(selected = mode == crop.mode, onClick = { onSetCrop(mode) }, label = { Text(mode.label) })
            }
        }
    }
    if (adjust.supported) {
        ToggleRow(stringResource(Res.string.image_adjustments), adjust.enabled, onSetAdjustEnabled)
        if (adjust.enabled) {
            VideoAdjustParameter.entries.forEach { parameter ->
                LabeledSlider(
                    label = parameter.label,
                    valueLabel = formatAdjustValue(adjust.value(parameter)),
                    value = adjust.value(parameter).coerceIn(parameter.minimum, parameter.maximum),
                    valueRange = parameter.minimum..parameter.maximum,
                    onValueChange = { onSetAdjust(parameter, it) },
                )
            }
            TextButton(onClick = onResetAdjust) {
                Text(stringResource(Res.string.reset))
            }
        }
    }
}

@Composable
private fun TracksContent(
    tracks: PlaybackTracks,
    showSubtitleImport: Boolean,
    onSelectAudioTrack: (String) -> Unit,
    onSelectSubtitleTrack: (String) -> Unit,
    onImportSubtitle: () -> Unit,
) {
    if (tracks.audio.size > 1) {
        Text(stringResource(Res.string.audio), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        TrackChoices(tracks.audio, onSelectAudioTrack)
    }
    if (tracks.subtitles.isNotEmpty()) {
        Text(stringResource(Res.string.subtitles), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        TrackChoices(tracks.subtitles, onSelectSubtitleTrack)
    }
    if (showSubtitleImport) {
        FilledTonalButton(onClick = onImportSubtitle) { Text(stringResource(Res.string.subtitle_select)) }
    }
}

@Composable
private fun EqualizerContent(
    equalizer: PlaybackEqualizer,
    onSelectPreset: (String) -> Unit,
    onSetPreamp: (Float) -> Unit,
    onSetBand: (Int, Float) -> Unit,
) {
    if (!equalizer.enabled) return

    if (equalizer.presets.isNotEmpty()) {
        val selectedPreset = equalizer.presets.firstOrNull { it.id == equalizer.selectedPresetId }?.label ?: stringResource(Res.string.custom_sound)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.sound_profile),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                selectedPreset,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            gridItemsIndexed(
                items = equalizer.presets,
                key = { _, preset -> preset.id },
            ) { index, preset ->
                val rowIndex = index % 3
                val columnStart = (index / 3) * 3
                val rowCount = minOf(3, equalizer.presets.size - columnStart)
                val shape = equalizerPresetGridShape(rowIndex, rowCount)
                ToggleButton(
                    checked = preset.id == equalizer.selectedPresetId,
                    onCheckedChange = { onSelectPreset(preset.id) },
                    modifier = Modifier
                        .width(116.dp)
                        .height(48.dp),
                    shapes = ToggleButtonDefaults.shapes(
                        shape = shape,
                        pressedShape = equalizerPresetGridShape(rowIndex, rowCount, pressed = true),
                        checkedShape = shape,
                    ),
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                ) {
                    Text(
                        preset.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.fine_tune), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(stringResource(Res.string.all_frequency_bands), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EqualizerBandControl(
                label = stringResource(Res.string.preamp),
                value = equalizer.preampDb,
                emphasized = true,
                onCommit = onSetPreamp,
            )
            equalizer.bands.forEach { band ->
                EqualizerBandControl(
                    label = band.label,
                    value = band.amplificationDb,
                    onCommit = { onSetBand(band.index, it) },
                )
            }
        }
    }
}

@Composable
private fun BookmarksContent(
    progressTime: Long,
    bookmarks: PlaybackBookmarks,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onRename: (org.videolan.vlc.player.PlaybackBookmark) -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = onPrevious,
            enabled = bookmarks.entries.any { it.timeMs < progressTime },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(Res.string.previous_bookmark)) }
        FilledTonalButton(
            onClick = onNext,
            enabled = bookmarks.entries.any { it.timeMs > progressTime },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(Res.string.next_bookmark)) }
    }
    bookmarks.entries.forEach { bookmark ->
        var menuVisible by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSeek(bookmark.timeMs) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatPlaybackTime(bookmark.timeMs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box {
                IconButton(onClick = { menuVisible = true }) {
                    Icon(MaterialSymbols.Filled.MoreVert, contentDescription = stringResource(Res.string.more_options))
                }
                DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.rename)) },
                        leadingIcon = { Icon(MaterialSymbols.Filled.Edit, contentDescription = null) },
                        onClick = { menuVisible = false; onRename(bookmark) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.delete)) },
                        leadingIcon = { Icon(MaterialSymbols.Filled.Delete, contentDescription = null) },
                        onClick = { menuVisible = false; onRemove(bookmark.id) },
                    )
                }
            }
        }
    }
    FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.add_bookmark))
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(valueLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
    Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label })
}

@Composable
private fun EqualizerBandControl(
    label: String,
    value: Float,
    emphasized: Boolean = false,
    onCommit: (Float) -> Unit,
) {
    var preview by remember(value) { mutableFloatStateOf(value.coerceIn(-20f, 20f)) }
    val sliderState = rememberSliderState(
        value = preview,
        valueRange = -20f..20f,
    )
    sliderState.onValueChange = { updated ->
        sliderState.value = updated
        preview = updated
        onCommit(updated)
    }
    LaunchedEffect(value) {
        val updated = value.coerceIn(-20f, 20f)
        if (!sliderState.isDragging && sliderState.value != updated) {
            preview = updated
            sliderState.value = updated
        }
    }
    Surface(
        modifier = Modifier.width(66.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatEqualizerDb(preview),
                color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            VerticalSlider(
                state = sliderState,
                modifier = Modifier
                    .height(116.dp)
                    .semantics { contentDescription = label },
                reverseDirection = true,
                track = { state ->
                    SliderDefaults.CenteredTrack(
                        sliderState = state,
                        modifier = Modifier.width(28.dp),
                        trackCornerSize = 10.dp,
                    )
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ChoiceFlow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun TrackChoices(
    tracks: List<org.videolan.vlc.player.PlaybackTrack>,
    onSelect: (String) -> Unit,
) {
    ChoiceFlow {
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
private fun DelayChoices(title: String, delayUs: Long, onSetDelay: (Long) -> Unit) {
    val delayMs = delayUs / 1_000L
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("$delayMs ms", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        ButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            listOf("−500", stringResource(Res.string.reset), "+500").forEachIndexed { index, label ->
                val interactionSource = remember { MutableInteractionSource() }
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                    2 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(8.dp)
                }
                FilledTonalButton(
                    onClick = {
                        when (index) {
                            0 -> onSetDelay(delayUs - 500_000L)
                            1 -> onSetDelay(0L)
                            else -> onSetDelay(delayUs + 500_000L)
                        }
                    },
                    shapes = androidx.compose.material3.ButtonDefaults.shapes(
                        shape = shape,
                        pressedShape = RoundedCornerShape(16.dp),
                    ),
                    enabled = index != 1 || delayUs != 0L,
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(interactionSource)
                        .height(50.dp),
                    interactionSource = interactionSource,
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SleepTimerChoices(
    state: SleepTimerState,
    onSetTimer: (Long, Boolean) -> Unit,
    onClear: () -> Unit,
) {
    var waitForCurrentItem by remember(state.isActive) { mutableStateOf(state.waitForCurrentItem) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15L, 30L, 60L).forEach { minutes ->
            FilledTonalButton(
                onClick = { onSetTimer(minutes * 60_000L, waitForCurrentItem) },
                modifier = Modifier.weight(1f),
            ) { Text(pluralStringResource(Res.plurals.minutes_count, minutes.toInt(), minutes)) }
        }
    }
    ToggleRow(stringResource(Res.string.wait_before_sleep), waitForCurrentItem) {
        waitForCurrentItem = it
        if (state.isActive && state.durationMillis > 0L) onSetTimer(state.durationMillis, it)
    }
    if (state.isActive) {
        TextButton(onClick = onClear) {
            Text(stringResource(Res.string.cancel))
        }
    }
}

private fun formatAdjustValue(value: Float): String =
    ((value * 100f).roundToInt() / 100f).toString().removeSuffix(".0")

private fun playbackRateGridShape(
    rowIndex: Int,
    columnIndex: Int,
    rowCount: Int,
    columnCount: Int,
    pressed: Boolean = false,
): RoundedCornerShape {
    val outerCorner = if (pressed) 20.dp else 24.dp
    val innerCorner = if (pressed) 12.dp else 8.dp
    return RoundedCornerShape(
        topStart = if (rowIndex == 0 && columnIndex == 0) outerCorner else innerCorner,
        topEnd = if (rowIndex == 0 && columnIndex == columnCount - 1) outerCorner else innerCorner,
        bottomStart = if (rowIndex == rowCount - 1 && columnIndex == 0) outerCorner else innerCorner,
        bottomEnd = if (rowIndex == rowCount - 1 && columnIndex == columnCount - 1) outerCorner else innerCorner,
    )
}

private fun equalizerPresetGridShape(
    rowIndex: Int,
    rowCount: Int,
    pressed: Boolean = false,
): RoundedCornerShape {
    val outerCorner = if (pressed) 16.dp else 18.dp
    val innerCorner = if (pressed) 10.dp else 6.dp
    return RoundedCornerShape(
        topStart = if (rowIndex == 0) outerCorner else innerCorner,
        topEnd = if (rowIndex == 0) outerCorner else innerCorner,
        bottomStart = if (rowIndex == rowCount - 1) outerCorner else innerCorner,
        bottomEnd = if (rowIndex == rowCount - 1) outerCorner else innerCorner,
    )
}

private fun formatEqualizerDb(value: Float): String {
    val rounded = value.roundToInt()
    return when {
        rounded > 0 -> "+$rounded dB"
        rounded < 0 -> "−${-rounded} dB"
        else -> "0 dB"
    }
}

internal fun snapPlaybackRate(value: Float): Float =
    PlaybackRate.normalize((value * 20f).roundToInt() / 20f)

/** Parses VLC's compact seek notation: seconds, MM:SS, or HH:MM:SS. */
internal fun parsePlaybackTimestamp(input: String): Long? {
    val values = input.trim().split(':').takeIf { it.size in 1..3 } ?: return null
    if (values.any { it.isEmpty() || it.any { char -> !char.isDigit() } }) return null
    val numbers = values.map { it.toLongOrNull() ?: return null }
    val seconds = numbers.last()
    if (values.size > 1 && seconds >= 60) return null
    val minutes = numbers.getOrNull(numbers.lastIndex - 1) ?: 0L
    if (values.size > 2 && minutes >= 60) return null
    val hours = numbers.getOrNull(numbers.lastIndex - 2) ?: 0L
    val minuteSeconds = minutes.safeMultiplyAdd(60, seconds) ?: return null
    val totalSeconds = hours.safeMultiplyAdd(3_600, minuteSeconds) ?: return null
    return totalSeconds.safeMultiplyAdd(1_000, 0)
}

private fun Long.safeMultiplyAdd(multiplier: Long, addend: Long): Long? =
    if (this > (Long.MAX_VALUE - addend) / multiplier) null else this * multiplier + addend
