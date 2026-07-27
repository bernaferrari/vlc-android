package org.videolan.vlc.compose.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
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
import org.videolan.vlc.platform.RendererInfo
import org.videolan.vlc.platform.RendererType
import org.videolan.vlc.compose.components.VLCRendererPickerDialogContent
import org.videolan.vlc.compose.components.VLCRendererUiItem
import kotlin.math.roundToInt
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.*

/**
 * Platform-neutral video chrome: surface slot + auto-hiding HUD.
 *
 * Android supplies the libVLC [surface] (VLCVideoLayout / SurfaceView) via
 * AndroidView; iOS can pass a UIKitView wrapping the VLCKit drawable.
 */
@Composable
fun VideoSurfaceWithHud(
    title: String,
    subtitle: String = "",
    error: String? = null,
    playing: Boolean,
    progress: Progress,
    shuffle: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.NONE,
    rate: Float = 1f,
    queue: List<MediaItem> = emptyList(),
    currentQueueIndex: Int = 0,
    abRepeat: ABRepeat = ABRepeat(),
    abRepeatEnabled: Boolean = false,
    stopAfterCurrent: Boolean = false,
    videoScaleMode: VideoScaleMode = VideoScaleMode.BEST_FIT,
    videoCrop: PlaybackVideoCrop = PlaybackVideoCrop(),
    videoAdjust: PlaybackVideoAdjust = PlaybackVideoAdjust(),
    tracks: PlaybackTracks = PlaybackTracks(),
    delays: PlaybackDelays = PlaybackDelays(),
    sleepTimer: SleepTimerState = SleepTimerState(),
    chapters: PlaybackChapters = PlaybackChapters(),
    hasVideoOutput: Boolean = false,
    showPictureInPicture: Boolean = false,
    showRendererSelection: Boolean = false,
    renderers: List<RendererInfo> = emptyList(),
    selectedRendererId: String? = null,
    equalizer: PlaybackEqualizer = PlaybackEqualizer(),
    bookmarks: PlaybackBookmarks = PlaybackBookmarks(),
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onSetRate: (Float) -> Unit = {},
    onPlayQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveQueueItem: (Int) -> Unit = {},
    onToggleABRepeat: () -> Unit = {},
    onSetABRepeatMarker: () -> Unit = {},
    onResetABRepeat: () -> Unit = {},
    onClearABRepeat: () -> Unit = {},
    onToggleStopAfterCurrent: () -> Unit = {},
    onSetVideoScaleMode: (VideoScaleMode) -> Unit = {},
    onSetVideoCrop: (VideoCropMode) -> Unit = {},
    onSetVideoAdjustEnabled: (Boolean) -> Unit = {},
    onSetVideoAdjust: (VideoAdjustParameter, Float) -> Unit = { _, _ -> },
    onResetVideoAdjust: () -> Unit = {},
    onSelectAudioTrack: (String) -> Unit = {},
    onSelectSubtitleTrack: (String) -> Unit = {},
    onSetAudioDelay: (Long) -> Unit = {},
    onSetSubtitleDelay: (Long) -> Unit = {},
    onSetSleepTimer: (Long, Boolean) -> Unit = { _, _ -> },
    onClearSleepTimer: () -> Unit = {},
    onSelectChapter: (Int) -> Unit = {},
    showSubtitleImport: Boolean = false,
    onImportSubtitle: () -> Unit = {},
    onEnterPictureInPicture: () -> Unit = {},
    onStartRendererDiscovery: () -> Unit = {},
    onStopRendererDiscovery: () -> Unit = {},
    onRefreshRenderers: () -> Unit = {},
    onSelectRenderer: (String?) -> Unit = {},
    onSetEqualizerEnabled: (Boolean) -> Unit = {},
    onSelectEqualizerPreset: (String) -> Unit = {},
    onSetEqualizerPreamp: (Float) -> Unit = {},
    onSetEqualizerBand: (Int, Float) -> Unit = { _, _ -> },
    onAddBookmark: () -> Unit = {},
    onRemoveBookmark: (String) -> Unit = {},
    onRenameBookmark: (String, String) -> Unit = { _, _ -> },
    onSeekBookmark: (Long) -> Unit = {},
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    surface: @Composable BoxScope.(chromeVisible: Boolean) -> Unit,
) {
    var hudVisible by remember { mutableStateOf(true) }
    var optionsVisible by remember { mutableStateOf(false) }
    var rendererPickerVisible by remember { mutableStateOf(false) }
    var interfaceLocked by remember { mutableStateOf(false) }
    LaunchedEffect(title) {
        interfaceLocked = false
        hudVisible = true
    }
    LaunchedEffect(hudVisible, playing) {
        if (hudVisible && playing) {
            delay(4_000)
            hudVisible = false
        }
    }
    LaunchedEffect(rendererPickerVisible) {
        if (!rendererPickerVisible) return@LaunchedEffect
        onStartRendererDiscovery()
        while (rendererPickerVisible) {
            delay(1_000)
            onRefreshRenderers()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { if (!interfaceLocked) hudVisible = !hudVisible }
    ) {
        // Video / artwork surface
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            surface(hudVisible || optionsVisible)
        }

        if (!error.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (hudVisible && !interfaceLocked) {
            VideoHudOverlay(
                title = title,
                subtitle = subtitle,
                playing = playing,
                progress = progress,
                shuffle = shuffle,
                repeatMode = repeatMode,
                rate = rate,
                queueSize = queue.size,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onOpenOptions = { optionsVisible = true },
                showPictureInPicture = showPictureInPicture,
                onEnterPictureInPicture = onEnterPictureInPicture,
                showRendererSelection = showRendererSelection,
                onOpenRendererSelection = { rendererPickerVisible = true },
                showInterfaceLock = hasVideoOutput,
                onLockInterface = { interfaceLocked = true },
                onClose = onClose,
            )
        }

        if (interfaceLocked) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.72f),
                contentColor = Color.White,
            ) {
                IconButton(
                    onClick = {
                        interfaceLocked = false
                        hudVisible = true
                    },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        icon = MaterialSymbols.Filled.LockOpen,
                        contentDescription = stringResource(Res.string.unlock),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }

    if (optionsVisible) {
        PlaybackOptionsSheet(
            rate = rate,
            queue = queue,
            currentQueueIndex = currentQueueIndex,
            progressTime = progress.time,
            abRepeat = abRepeat,
            abRepeatEnabled = abRepeatEnabled,
            stopAfterCurrent = stopAfterCurrent,
            videoScaleMode = videoScaleMode,
            videoCrop = videoCrop,
            videoAdjust = videoAdjust,
            tracks = tracks,
            delays = delays,
            sleepTimer = sleepTimer,
            chapters = chapters,
            equalizer = equalizer,
            bookmarks = bookmarks,
            showVideoOptions = hasVideoOutput,
            onSetRate = onSetRate,
            onPlayQueueItem = {
                onPlayQueueItem(it)
                optionsVisible = false
            },
            onMoveQueueItem = onMoveQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            onToggleABRepeat = onToggleABRepeat,
            onSetABRepeatMarker = onSetABRepeatMarker,
            onResetABRepeat = onResetABRepeat,
            onClearABRepeat = onClearABRepeat,
            onToggleStopAfterCurrent = onToggleStopAfterCurrent,
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
            onRenameBookmark = onRenameBookmark,
            onSeekBookmark = onSeekBookmark,
            showSubtitleImport = showSubtitleImport,
            onImportSubtitle = onImportSubtitle,
            onDismiss = { optionsVisible = false },
        )
    }

    if (rendererPickerVisible) {
        Dialog(
            onDismissRequest = {
                rendererPickerVisible = false
                onStopRendererDiscovery()
            },
        ) {
            VLCRendererPickerDialogContent(
                title = stringResource(Res.string.renderer_list_title),
                renderers = renderers.map { renderer ->
                    VLCRendererUiItem(
                        id = renderer.id,
                        displayName = renderer.name,
                        isSelected = renderer.id == selectedRendererId,
                        isChromecast = renderer.type == RendererType.CHROMECAST,
                    )
                },
                disconnectText = stringResource(Res.string.renderers_disconnect),
                showDisconnect = selectedRendererId != null,
                onRendererSelected = { renderer ->
                    onSelectRenderer(renderer.id)
                    rendererPickerVisible = false
                    onStopRendererDiscovery()
                },
                onDisconnect = {
                    onSelectRenderer(null)
                    rendererPickerVisible = false
                    onStopRendererDiscovery()
                },
                rendererIcon = { _, tint ->
                    Icon(
                        icon = MaterialSymbols.Filled.Devices,
                        contentDescription = null,
                        tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
fun VideoHudOverlay(
    title: String,
    subtitle: String,
    playing: Boolean,
    progress: Progress,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    rate: Float,
    queueSize: Int,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenOptions: () -> Unit,
    showPictureInPicture: Boolean,
    onEnterPictureInPicture: () -> Unit,
    showRendererSelection: Boolean,
    onOpenRendererSelection: () -> Unit,
    showInterfaceLock: Boolean,
    onLockInterface: () -> Unit,
    onClose: (() -> Unit)?,
) {
    val colors = VLCThemeDefaults.colors
    Box(modifier = Modifier.fillMaxSize()) {
        // Top gradient + title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.close_player),
                            tint = Color.White,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title.ifBlank { " " },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (showPictureInPicture) {
                    IconButton(onClick = onEnterPictureInPicture) {
                        Icon(
                            icon = MaterialSymbols.Filled.PictureInPictureAlt,
                            contentDescription = stringResource(Res.string.play_pip_title),
                            tint = Color.White,
                        )
                    }
                }
                if (showRendererSelection) {
                    IconButton(onClick = onOpenRendererSelection) {
                        Icon(
                            icon = MaterialSymbols.Filled.Devices,
                            contentDescription = stringResource(Res.string.renderer_list_title),
                            tint = Color.White,
                        )
                    }
                }
                if (showInterfaceLock) {
                    IconButton(onClick = onLockInterface) {
                        Icon(
                            icon = MaterialSymbols.Filled.Lock,
                            contentDescription = stringResource(Res.string.lock),
                            tint = Color.White,
                        )
                    }
                }
                }
        }

        // Bottom gradient + transport
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Native decoders do not all tolerate a seek for every pixel of a drag. Keep the
            // preview local and issue exactly one seek when the gesture finishes; this makes the
            // Android LibVLC and iOS VLCKit surfaces feel equally direct without flooding them.
            val seekableLength = progress.length.takeIf { it > 0L }
            val length = seekableLength ?: 1L
            var scrubPosition by remember(progress.length) { mutableStateOf<Float?>(null) }
            val displayedTime = (scrubPosition?.toLong() ?: progress.time).coerceIn(0L, length)
            Slider(
                value = (scrubPosition ?: progress.time.toFloat()).coerceIn(0f, length.toFloat()),
                onValueChange = { scrubPosition = it },
                onValueChangeFinished = {
                    scrubPosition?.let { onSeek(it.toLong()) }
                    scrubPosition = null
                },
                valueRange = 0f..length.toFloat(),
                enabled = seekableLength != null,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatPlaybackTime(displayedTime), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                Text(formatPlaybackTime(progress.length), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            }
            Box(modifier = Modifier.height(8.dp))
            Surface(
                onClick = onOpenOptions,
                shape = MaterialTheme.shapes.large,
                color = Color.White.copy(alpha = 0.14f),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon = MaterialSymbols.Filled.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "${playbackRateLabel(rate)} · ${stringResource(Res.string.playlist)} ($queueSize)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Box(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        icon = MaterialSymbols.Filled.Shuffle,
                        contentDescription = stringResource(Res.string.shuffle_play),
                        tint = if (shuffle) colors.primary else Color.White,
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        icon = MaterialSymbols.Filled.SkipPrevious,
                        contentDescription = stringResource(Res.string.previous),
                        tint = Color.White,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePlay),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            icon = if (playing) MaterialSymbols.Filled.Pause else MaterialSymbols.Filled.PlayArrow,
                            contentDescription = if (playing) {
                                stringResource(Res.string.pause)
                            } else {
                                stringResource(Res.string.play)
                            },
                            tint = colors.onPrimary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(
                        icon = MaterialSymbols.Filled.SkipNext,
                        contentDescription = stringResource(Res.string.next),
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        icon = if (repeatMode == RepeatMode.ONE) MaterialSymbols.Filled.RepeatOne else MaterialSymbols.Filled.Repeat,
                        contentDescription = when (repeatMode) {
                            RepeatMode.NONE -> stringResource(Res.string.repeat_none)
                            RepeatMode.ALL -> stringResource(Res.string.repeat_all)
                            RepeatMode.ONE -> stringResource(Res.string.repeat_single)
                        },
                        tint = if (repeatMode == RepeatMode.NONE) Color.White else colors.primary,
                    )
                }
            }
        }
    }
}

internal fun formatPlaybackTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}

internal fun playbackRateLabel(rate: Float): String {
    val value = ((rate.coerceIn(0.25f, 4f) * 100).roundToInt() / 100f)
        .toString()
        .removeSuffix(".0")
    return "${value}×"
}
