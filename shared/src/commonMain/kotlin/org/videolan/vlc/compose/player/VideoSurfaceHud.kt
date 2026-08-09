package org.videolan.vlc.compose.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
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
import org.videolan.vlc.player.PlaybackRate
import org.videolan.vlc.platform.RendererInfo
import org.videolan.vlc.platform.RendererType
import org.videolan.vlc.compose.components.VLCRendererPickerDialogContent
import org.videolan.vlc.compose.components.VLCRendererUiItem
import org.videolan.vlc.compose.components.VLCBookmarkMarkers
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
    hudTimeoutSeconds: Int = 4,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onSetRate: (Float) -> Unit = {},
    onSavePlaylist: (String) -> Unit = {},
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
    onPreviousBookmark: () -> Unit = {},
    onNextBookmark: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    surface: @Composable BoxScope.(chromeVisible: Boolean) -> Unit,
) {
    var hudVisible by remember { mutableStateOf(true) }
    var activeSheet by remember { mutableStateOf<PlaybackSheetDestination?>(null) }
    var rendererPickerVisible by remember { mutableStateOf(false) }
    var interfaceLocked by remember { mutableStateOf(false) }
    var interactionActive by remember { mutableStateOf(false) }
    var interactionEpoch by remember { mutableStateOf(0) }
    var audioDismissOffsetPx by remember { mutableFloatStateOf(0f) }
    val playerMotion = LocalVLCMotion.current
    val audioDismissThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() }
    val audioDismissDragState = rememberDraggableState { delta ->
        audioDismissOffsetPx = (audioDismissOffsetPx + delta).coerceAtLeast(0f)
    }
    LaunchedEffect(title) {
        interfaceLocked = false
        hudVisible = true
    }
    LaunchedEffect(
        hudVisible,
        hasVideoOutput,
        playing,
        hudTimeoutSeconds,
        interactionEpoch,
        interactionActive,
        activeSheet,
        rendererPickerVisible,
    ) {
        if (
            hasVideoOutput && hudVisible && playing && !interactionActive &&
            activeSheet == null && !rendererPickerVisible
        ) {
            delay(hudTimeoutSeconds.coerceIn(1, 10) * 1_000L)
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
            .graphicsLayer {
                translationY = audioDismissOffsetPx
                val progress = (audioDismissOffsetPx / (audioDismissThresholdPx * 4f)).coerceIn(0f, 1f)
                scaleX = 1f - progress * 0.018f
                scaleY = 1f - progress * 0.018f
            }
            .draggable(
                state = audioDismissDragState,
                orientation = Orientation.Vertical,
                enabled = !hasVideoOutput && onClose != null && activeSheet == null,
                onDragStopped = { velocity ->
                    if (audioDismissOffsetPx >= audioDismissThresholdPx || velocity > 1_200f) {
                        audioDismissOffsetPx = 0f
                        onClose?.invoke()
                    } else {
                        animate(
                            initialValue = audioDismissOffsetPx,
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = if (playerMotion.reducedMotion) 0 else 180,
                                easing = VLCMotion.EmphasizedDecelerate,
                            ),
                        ) { value, _ -> audioDismissOffsetPx = value }
                    }
                },
            )
            .background(Color.Black)
    ) {
        // Video / artwork surface
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            surface(!hasVideoOutput || hudVisible || activeSheet != null)
        }

        // Keep the full-surface tap affordance behind the HUD. Putting click handling on the
        // parent also observes taps consumed by child buttons on some Compose targets, which can
        // hide the chrome at the exact moment a speed or overflow action opens.
        Box(
            modifier = if (hasVideoOutput) {
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (!interfaceLocked) hudVisible = !hudVisible }
            } else {
                Modifier.fillMaxSize()
            },
        )

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

        VideoHudOverlay(
            visible = (!hasVideoOutput || hudVisible) && !interfaceLocked,
            title = title,
            subtitle = subtitle,
            playing = playing,
            progress = progress,
            shuffle = shuffle,
            repeatMode = repeatMode,
            rate = rate,
            queueSize = queue.size,
            hasVideoOutput = hasVideoOutput,
            isLiveStream = queue.getOrNull(currentQueueIndex)?.isStream == true,
            bookmarks = bookmarks,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onNext = onNext,
            onPrevious = onPrevious,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onOpenOptions = { activeSheet = it },
            onUserInteraction = {
                hudVisible = true
                interactionEpoch++
            },
            onInteractionActiveChanged = { active ->
                interactionActive = active
                if (!active) interactionEpoch++
            },
            showPictureInPicture = showPictureInPicture,
            onEnterPictureInPicture = onEnterPictureInPicture,
            showRendererSelection = showRendererSelection,
            onOpenRendererSelection = { rendererPickerVisible = true },
            showInterfaceLock = hasVideoOutput,
            onLockInterface = { interfaceLocked = true },
            onClose = onClose,
        )

        AnimatedVisibility(
            visible = interfaceLocked,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(LocalVLCMotion.current.durationShort)) +
                slideInVertically(
                    animationSpec = tween(LocalVLCMotion.current.durationShort, easing = VLCMotion.EmphasizedDecelerate),
                    initialOffsetY = { it / 4 },
                ),
            exit = fadeOut(tween(LocalVLCMotion.current.durationShort)) +
                slideOutVertically(
                    animationSpec = tween(LocalVLCMotion.current.durationShort, easing = VLCMotion.EmphasizedAccelerate),
                    targetOffsetY = { it / 5 },
                ),
        ) {
            Surface(
                modifier = Modifier.clip(CircleShape),
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

    activeSheet?.let { initialDestination ->
        PlaybackOptionsSheet(
            initialDestination = initialDestination,
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
            onSeekTo = onSeek,
            onSavePlaylist = onSavePlaylist,
            onPlayQueueItem = onPlayQueueItem,
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
            onPreviousBookmark = onPreviousBookmark,
            onNextBookmark = onNextBookmark,
            showSubtitleImport = showSubtitleImport,
            onImportSubtitle = onImportSubtitle,
            onDismiss = {
                activeSheet = null
                interactionEpoch++
            },
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
    visible: Boolean = true,
    title: String,
    subtitle: String,
    playing: Boolean,
    progress: Progress,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    rate: Float,
    queueSize: Int,
    hasVideoOutput: Boolean,
    isLiveStream: Boolean,
    bookmarks: PlaybackBookmarks = PlaybackBookmarks(),
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenOptions: (PlaybackSheetDestination) -> Unit,
    onUserInteraction: () -> Unit,
    onInteractionActiveChanged: (Boolean) -> Unit,
    showPictureInPicture: Boolean,
    onEnterPictureInPicture: () -> Unit,
    showRendererSelection: Boolean,
    onOpenRendererSelection: () -> Unit,
    showInterfaceLock: Boolean,
    onLockInterface: () -> Unit,
    onClose: (() -> Unit)?,
) {
    val colors = VLCThemeDefaults.colors
    val motion = LocalVLCMotion.current
    val density = LocalDensity.current
    val enterDuration = if (motion.reducedMotion) 0 else 180
    val exitDuration = if (motion.reducedMotion) 0 else 120
    val chromeAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) enterDuration else exitDuration,
            easing = if (visible) VLCMotion.EmphasizedDecelerate else VLCMotion.EmphasizedAccelerate,
        ),
        label = "player-chrome-scrim",
    )
    val shuffleTint by animateColorAsState(
        targetValue = if (shuffle) colors.primary else Color.White,
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Emphasized),
        label = "shuffle-tint",
    )
    val repeatTint by animateColorAsState(
        targetValue = if (repeatMode == RepeatMode.NONE) Color.White else colors.primary,
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Emphasized),
        label = "repeat-tint",
    )
    val onStateLabel = stringResource(Res.string.on)
    val offStateLabel = stringResource(Res.string.off)
    val repeatStateLabel = when (repeatMode) {
        RepeatMode.NONE -> stringResource(Res.string.repeat_none)
        RepeatMode.ALL -> stringResource(Res.string.repeat_all)
        RepeatMode.ONE -> stringResource(Res.string.repeat_single)
    }
    val bottomScrim = colors.primary.copy(alpha = 0.16f).compositeOver(Color.Black)
    Box(modifier = Modifier.fillMaxSize()) {
        // Gradients stay anchored to the viewport and only change opacity. Foreground controls
        // travel a fixed 10dp so toggling chrome feels connected without dragging in whole rows.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(128.dp)
                .graphicsLayer { alpha = chromeAlpha }
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.76f), Color.Transparent)
                    )
                )
        )
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(enterDuration, easing = VLCMotion.EmphasizedDecelerate)) + slideInVertically(
                animationSpec = tween(enterDuration, easing = VLCMotion.EmphasizedDecelerate),
                initialOffsetY = { with(density) { -10.dp.roundToPx() } },
            ),
            exit = fadeOut(tween(exitDuration, easing = VLCMotion.EmphasizedAccelerate)) + slideOutVertically(
                animationSpec = tween(exitDuration, easing = VLCMotion.EmphasizedAccelerate),
                targetOffsetY = { with(density) { -8.dp.roundToPx() } },
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Preserve the edge-to-edge artwork while keeping close/title actions below
                    // status bars and display cutouts on every shared target.
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
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
                TextButton(
                    onClick = {
                        onUserInteraction()
                        onOpenOptions(PlaybackSheetDestination.SPEED)
                    },
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = playbackRateLabel(rate),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                var overflowVisible by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = {
                        overflowVisible = true
                        onInteractionActiveChanged(true)
                    }) {
                        Icon(
                            icon = MaterialSymbols.Filled.MoreVert,
                            contentDescription = stringResource(Res.string.more_options),
                            tint = Color.White,
                        )
                    }
                    DropdownMenu(
                        expanded = overflowVisible,
                        onDismissRequest = {
                            overflowVisible = false
                            onInteractionActiveChanged(false)
                        },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(Res.string.up_next_count, queueSize))
                            },
                            leadingIcon = {
                                Icon(MaterialSymbols.Filled.QueueMusic, contentDescription = null)
                            },
                            onClick = {
                                overflowVisible = false
                                onInteractionActiveChanged(false)
                                onOpenOptions(PlaybackSheetDestination.QUEUE)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.player_controls)) },
                            leadingIcon = {
                                Icon(MaterialSymbols.Filled.Tune, contentDescription = null)
                            },
                            onClick = {
                                overflowVisible = false
                                onInteractionActiveChanged(false)
                                onOpenOptions(PlaybackSheetDestination.TOOLS)
                            },
                        )
                        if (showPictureInPicture) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.play_pip_title)) },
                                leadingIcon = {
                                    Icon(MaterialSymbols.Filled.PictureInPictureAlt, contentDescription = null)
                                },
                                onClick = {
                                    overflowVisible = false
                                    onInteractionActiveChanged(false)
                                    onEnterPictureInPicture()
                                },
                            )
                        }
                        if (showRendererSelection) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.renderer_list_title)) },
                                leadingIcon = {
                                    Icon(MaterialSymbols.Filled.Devices, contentDescription = null)
                                },
                                onClick = {
                                    overflowVisible = false
                                    onInteractionActiveChanged(false)
                                    onOpenRendererSelection()
                                },
                            )
                        }
                        if (showInterfaceLock) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.lock)) },
                                leadingIcon = {
                                    Icon(MaterialSymbols.Filled.Lock, contentDescription = null)
                                },
                                onClick = {
                                    overflowVisible = false
                                    onInteractionActiveChanged(false)
                                    onLockInterface()
                                },
                            )
                        }
                    }
                }
            }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(244.dp)
                .graphicsLayer { alpha = chromeAlpha }
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, bottomScrim)
                    )
                )
        )
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(enterDuration, easing = VLCMotion.EmphasizedDecelerate)) + slideInVertically(
                animationSpec = tween(enterDuration, easing = VLCMotion.EmphasizedDecelerate),
                initialOffsetY = { with(density) { 10.dp.roundToPx() } },
            ),
            exit = fadeOut(tween(exitDuration, easing = VLCMotion.EmphasizedAccelerate)) + slideOutVertically(
                animationSpec = tween(exitDuration, easing = VLCMotion.EmphasizedAccelerate),
                targetOffsetY = { with(density) { 8.dp.roundToPx() } },
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // The player is intentionally edge-to-edge, but its transport controls are not.
                    // Keep the primary play action clear of gesture and three-button navigation.
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
            // Native decoders do not all tolerate a seek for every pixel of a drag. Keep the
            // preview local and issue exactly one seek when the gesture finishes; this makes the
            // Android LibVLC and iOS VLCKit surfaces feel equally direct without flooding them.
            val seekableLength = progress.length.takeIf { it > 0L }
            var scrubPosition by remember(progress.length) { mutableStateOf<Float?>(null) }
            val displayedTime = (scrubPosition?.toLong() ?: progress.time).coerceAtLeast(0L).let {
                if (seekableLength != null) it.coerceAtMost(seekableLength) else it
            }
            if (seekableLength != null && bookmarks.entries.isNotEmpty()) {
                VLCBookmarkMarkers(
                    markerFractions = bookmarks.entries.map { bookmark ->
                        bookmark.timeMs.toFloat() / seekableLength.toFloat()
                    },
                    markerColor = Color.White.copy(alpha = 0.9f),
                )
            }
            if (seekableLength != null) {
                val playbackPositionDescription = stringResource(
                    Res.string.playback_position_value,
                    formatPlaybackTime(displayedTime),
                    formatPlaybackTime(seekableLength),
                )
                Slider(
                    value = (scrubPosition ?: progress.time.toFloat()).coerceIn(0f, seekableLength.toFloat()),
                    onValueChange = {
                        scrubPosition = it
                        onInteractionActiveChanged(true)
                    },
                    onValueChangeFinished = {
                        scrubPosition?.let { onSeek(it.toLong()) }
                        scrubPosition = null
                        onInteractionActiveChanged(false)
                    },
                    valueRange = 0f..seekableLength.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = playbackPositionDescription
                        },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatPlaybackTime(displayedTime),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                )
                if (seekableLength != null) {
                    Text(
                        text = "−${formatPlaybackTime((seekableLength - displayedTime).coerceAtLeast(0L))}",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        maxLines = 1,
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White,
                    ) {
                        Text(
                            text = if (isLiveStream) {
                                stringResource(Res.string.live_stream)
                            } else {
                                stringResource(Res.string.duration_unavailable)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Box(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        onUserInteraction()
                        onToggleShuffle()
                    },
                    modifier = Modifier.semantics {
                        selected = shuffle
                        stateDescription = if (shuffle) onStateLabel else offStateLabel
                    },
                ) {
                    Icon(
                        icon = MaterialSymbols.Filled.Shuffle,
                        contentDescription = if (shuffle) {
                            stringResource(Res.string.shuffle_on)
                        } else {
                            stringResource(Res.string.shuffle)
                        },
                        tint = shuffleTint,
                    )
                }
                IconButton(onClick = {
                    onUserInteraction()
                    onPrevious()
                }) {
                    Icon(
                        icon = MaterialSymbols.Filled.SkipPrevious,
                        contentDescription = stringResource(Res.string.previous),
                        tint = Color.White,
                    )
                }
                val playInteractionSource = remember { MutableInteractionSource() }
                val playPressed by playInteractionSource.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (playPressed) 0.96f else 1f,
                    animationSpec = tween(motion.durationShort, easing = VLCMotion.Emphasized),
                    label = "play-button-scale",
                )
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = playInteractionSource,
                            indication = ripple(bounded = true),
                            onClick = {
                                onUserInteraction()
                                onTogglePlay()
                            },
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Crossfade(
                            targetState = playing,
                            animationSpec = tween(motion.durationShort, easing = VLCMotion.Emphasized),
                            label = "playback-icon",
                        ) { isPlaying ->
                            Icon(
                                icon = if (isPlaying) MaterialSymbols.Filled.Pause else MaterialSymbols.Filled.PlayArrow,
                                contentDescription = if (isPlaying) {
                                    stringResource(Res.string.pause)
                                } else {
                                    stringResource(Res.string.play)
                                },
                                tint = colors.onPrimary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = {
                    onUserInteraction()
                    onNext()
                }) {
                    Icon(
                        icon = MaterialSymbols.Filled.SkipNext,
                        contentDescription = stringResource(Res.string.next),
                        tint = Color.White,
                    )
                }
                IconButton(
                    onClick = {
                        onUserInteraction()
                        onCycleRepeat()
                    },
                    modifier = Modifier.semantics {
                        selected = repeatMode != RepeatMode.NONE
                        stateDescription = repeatStateLabel
                    },
                ) {
                    Icon(
                        icon = if (repeatMode == RepeatMode.ONE) MaterialSymbols.Filled.RepeatOne else MaterialSymbols.Filled.Repeat,
                        contentDescription = when (repeatMode) {
                            RepeatMode.NONE -> stringResource(Res.string.repeat_none)
                            RepeatMode.ALL -> stringResource(Res.string.repeat_all)
                            RepeatMode.ONE -> stringResource(Res.string.repeat_single)
                        },
                        tint = repeatTint,
                    )
                }
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
    val value = ((PlaybackRate.normalize(rate) * 100).roundToInt() / 100f)
        .toString()
        .removeSuffix(".0")
    return "${value}×"
}
