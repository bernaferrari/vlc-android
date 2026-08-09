@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.videolan.vlc.compose.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
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
    var jumpToTimeText by remember { mutableStateOf("") }
    var savePlaylistVisible by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var bookmarkToRename by remember { mutableStateOf<org.videolan.vlc.player.PlaybackBookmark?>(null) }
    var bookmarkName by remember { mutableStateOf("") }
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
                            bookmarkName = bookmark.title
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
        val targetTime = parsePlaybackTimestamp(jumpToTimeText)
        AlertDialog(
            onDismissRequest = { jumpToTimeVisible = false },
            title = { Text(stringResource(Res.string.jump_to_time)) },
            text = {
                OutlinedTextField(
                    value = jumpToTimeText,
                    onValueChange = { jumpToTimeText = it },
                    label = { Text("HH:MM:SS") },
                    supportingText = { Text("Examples: 90, 12:30, or 1:04:15") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = targetTime != null,
                    onClick = {
                        targetTime?.let(onSeekTo)
                        jumpToTimeVisible = false
                    },
                ) { Text(stringResource(Res.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { jumpToTimeVisible = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (savePlaylistVisible) {
        AlertDialog(
            onDismissRequest = { savePlaylistVisible = false },
            title = { Text(stringResource(Res.string.playlist_save)) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text(stringResource(Res.string.playlist_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = playlistName.isNotBlank(),
                    onClick = {
                        onSavePlaylist(playlistName.trim())
                        savePlaylistVisible = false
                    },
                ) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { savePlaylistVisible = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
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
                    enabled = bookmarkName.isNotBlank(),
                    onClick = {
                        onRenameBookmark(bookmark.id, bookmarkName.trim())
                        bookmarkToRename = null
                    },
                ) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToRename = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
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
        PlaybackSheetDestination.QUEUE -> "Up next"
        PlaybackSheetDestination.TOOLS -> stringResource(Res.string.player_controls)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PLAYER",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        FilledTonalIconButton(
            onClick = onDismiss,
            shapes = IconButtonShapes(
                shape = RoundedCornerShape(22.dp),
                pressedShape = RoundedCornerShape(14.dp),
            ),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                MaterialSymbols.Filled.Close,
                contentDescription = stringResource(Res.string.close),
                modifier = Modifier.size(22.dp),
            )
        }
    }
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
                        PlaybackSheetDestination.SPEED -> "Speed"
                        PlaybackSheetDestination.QUEUE -> "Queue"
                        PlaybackSheetDestination.TOOLS -> "Tools"
                    },
                    fontWeight = if (selected == destination) FontWeight.Bold else FontWeight.SemiBold,
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
            .padding(horizontal = VLCLayout.SheetHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = playbackRateLabel(rate),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (rate == 1f) "Normal speed" else if (rate < 1f) "Slower playback" else "Faster playback",
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
        PlaybackRatePresets.chunked(4).forEachIndexed { rowIndex, presets ->
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                presets.forEachIndexed { index, preset ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val restingShape = when (index) {
                        0 -> RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                        presets.lastIndex -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(8.dp)
                    }
                    FilledTonalButton(
                        onClick = {
                            onPreviewRate(preset)
                            onCommitRate(preset)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(interactionSource)
                            .height(52.dp),
                        shapes = androidx.compose.material3.ButtonDefaults.shapes(
                            shape = restingShape,
                            pressedShape = RoundedCornerShape(16.dp),
                        ),
                        interactionSource = interactionSource,
                    ) {
                        Text(
                            playbackRateLabel(preset),
                            fontWeight = if (rate == preset) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (rate == preset) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            if (rowIndex == 0) Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Fine-tune the pace for this item.",
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VLCLayout.SheetHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (queue.size == 1) "1 item" else "${queue.size} items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Tap any item to play it next",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onSavePlaylist, enabled = queue.isNotEmpty()) {
                Text(stringResource(Res.string.save))
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VLCLayout.SheetHorizontalPadding, vertical = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleStopAfterCurrent)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.stop_after_this), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Finish the current item, then pause the queue",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = stopAfterCurrent, onCheckedChange = { onToggleStopAfterCurrent() })
            }
        }

        if (queue.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    MaterialSymbols.Outlined.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("Nothing else is queued", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Add media from the library to keep listening.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
        isFirst && isLast -> RoundedCornerShape(28.dp)
        isFirst -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
        isLast -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
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
                shape = if (selected) MaterialShapes.Cookie6Sided.toShape() else RoundedCornerShape(16.dp),
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
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
                summary = "Seek to an exact timestamp",
                icon = MaterialSymbols.Filled.History,
                onClick = onJumpToTime,
            )
        }
        item {
            ToolCard(
                title = stringResource(Res.string.ab_repeat),
                icon = MaterialSymbols.Filled.Repeat,
                summary = when {
                    !abRepeatEnabled -> "Loop a precise section"
                    abRepeat.start < 0L -> "Choose the start point"
                    abRepeat.stop < 0L -> "A · ${formatPlaybackTime(abRepeat.start)} — choose the end"
                    else -> "A · ${formatPlaybackTime(abRepeat.start)}   B · ${formatPlaybackTime(abRepeat.stop)}"
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
                    title = "Video fit & crop",
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
                    title = "Tracks",
                    summary = "Audio, subtitles, and imports",
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
                    title = "Synchronization",
                    summary = "Audio ${delays.audioUs / 1_000L} ms · Subtitles ${delays.subtitleUs / 1_000L} ms",
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
                ToolCard(
                    title = stringResource(Res.string.equalizer),
                    summary = if (equalizer.enabled) "On" else "Off",
                    icon = MaterialSymbols.Filled.Settings,
                    expanded = expanded == PlaybackToolSection.EQUALIZER,
                    onClick = {
                        if (!equalizer.enabled) onSetEqualizerEnabled(true)
                        onExpandedChange(PlaybackToolSection.EQUALIZER)
                    },
                    trailingAction = {
                        Switch(
                            checked = equalizer.enabled,
                            onCheckedChange = { enabled ->
                                onSetEqualizerEnabled(enabled)
                                val isExpanded = expanded == PlaybackToolSection.EQUALIZER
                                if (enabled != isExpanded) {
                                    onExpandedChange(PlaybackToolSection.EQUALIZER)
                                }
                            },
                        )
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
                    sleepTimer.awaitingCurrentItemEnd -> "After this item"
                    sleepTimer.isActive -> formatPlaybackTime(sleepTimer.remainingMillis)
                    else -> "Off"
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
                    summary = chapters.entries.firstOrNull { it.selected }?.title ?: "${chapters.entries.size} chapters",
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
                    summary = if (bookmarks.entries.isEmpty()) "No bookmarks" else "${bookmarks.entries.size} saved",
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
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialShapes.Cookie6Sided.toShape(),
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = if (expanded) MaterialShapes.Cookie6Sided.toShape() else RoundedCornerShape(18.dp),
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
        FilledTonalButton(onClick = onToggle) { Text("Start A–B repeat") }
        return
    }
    Text(
        "Current position · ${formatPlaybackTime(progressTime)}",
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
        ToggleRow("Image adjustments", adjust.enabled, onSetAdjustEnabled)
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
    ChoiceFlow {
        equalizer.presets.forEach { preset ->
            FilterChip(
                selected = preset.id == equalizer.selectedPresetId,
                onClick = { onSelectPreset(preset.id) },
                label = { Text(preset.label) },
            )
        }
    }
    EqualizerSlider(stringResource(Res.string.preamp), equalizer.preampDb, onSetPreamp)
    equalizer.bands.forEach { band ->
        EqualizerSlider(band.label, band.amplificationDb) { onSetBand(band.index, it) }
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
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
    Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun EqualizerSlider(label: String, value: Float, onCommit: (Float) -> Unit) {
    var preview by remember(value) { mutableFloatStateOf(value.coerceIn(-20f, 20f)) }
    LabeledSlider(
        label = label,
        valueLabel = "${preview.roundToInt()} dB",
        value = preview,
        valueRange = -20f..20f,
        onValueChange = { preview = it; onCommit(it) },
    )
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
            ) { Text("$minutes min") }
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
