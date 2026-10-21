package org.videolan.vlc.compose.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.videolan.vlc.compose.artwork.MediaArtwork
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.tv.LocalVLCTvPreviewFocus
import org.videolan.vlc.compose.tv.VLCTvLayout
import org.videolan.vlc.compose.tv.VLCTvSurface
import org.videolan.vlc.compose.icons.ExtraSymbols
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Progress
import org.videolan.vlc.player.PlaybackBookmarks
import org.videolan.vlc.viewmodel.PlayerUiState
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.*

/** Preview keys for snapshot review of the TV chrome's focused states. */
object TvPlayerFocusKeys {
    const val PLAY = "tv-player-play"
    const val PREVIOUS = "tv-player-previous"
    const val NEXT = "tv-player-next"
    const val TIMELINE = "tv-player-timeline"
    const val TRACKS = "tv-player-tracks"
    const val QUEUE = "tv-player-queue"
    const val SPEED = "tv-player-speed"
    const val TOOLS = "tv-player-tools"
}

/**
 * The sheet a TV chrome button asks for. [TRACKS] is the tools page opened on audio and subtitle
 * tracks: on a TV the subtitle switch is the most used tool, so it gets its own button.
 */
internal enum class TvPlayerSheet(val destination: PlaybackSheetDestination) {
    SPEED(PlaybackSheetDestination.SPEED),
    QUEUE(PlaybackSheetDestination.QUEUE),
    TOOLS(PlaybackSheetDestination.TOOLS),
    TRACKS(PlaybackSheetDestination.TOOLS),
}

/**
 * Ten-foot chrome for the shared video player.
 *
 * Same visual language as the phone HUD (scrims over the picture, the accent play control, the
 * slim timeline) re-proportioned for a remote: overscan-safe margins, larger glyphs and type, a
 * focusable timeline that scrubs with the D-pad, and exactly one focused element at a time, which
 * always reads as the accent. Touch gestures do not exist here; every action is reachable by focus.
 */
@Composable
internal fun TvVideoHudOverlay(
    visible: Boolean,
    title: String,
    subtitle: String,
    playing: Boolean,
    progress: Progress,
    rate: Float,
    queueSize: Int,
    hasVideoOutput: Boolean,
    isLiveStream: Boolean,
    bookmarks: PlaybackBookmarks,
    seekStepMs: Long,
    playFocusRequester: FocusRequester,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSheet: (TvPlayerSheet) -> Unit,
    onUserInteraction: () -> Unit,
    onInteractionActiveChanged: (Boolean) -> Unit,
) {
    // The picture is dark whatever the app theme: chrome over video is always light-on-dark.
    VLCTheme(darkTheme = true) {
        val motion = LocalVLCMotion.current
        val density = LocalDensity.current
        val enterDuration = if (motion.reducedMotion) 0 else 200
        val exitDuration = if (motion.reducedMotion) 0 else 140
        val chromeAlpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (visible) enterDuration else exitDuration,
                easing = if (visible) VLCMotion.EmphasizedDecelerate else VLCMotion.EmphasizedAccelerate,
            ),
            label = "tv-player-scrim",
        )
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(168.dp)
                    .graphicsLayer { alpha = chromeAlpha }
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent))),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .graphicsLayer { alpha = chromeAlpha }
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))),
            )
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.TopStart),
                enter = fadeIn(tween(enterDuration, easing = VLCMotion.EmphasizedDecelerate)) + slideInVertically(
                    animationSpec = tween(enterDuration, easing = VLCMotion.EmphasizedDecelerate),
                    initialOffsetY = { with(density) { -12.dp.roundToPx() } },
                ),
                exit = fadeOut(tween(exitDuration, easing = VLCMotion.EmphasizedAccelerate)) + slideOutVertically(
                    animationSpec = tween(exitDuration, easing = VLCMotion.EmphasizedAccelerate),
                    targetOffsetY = { with(density) { -8.dp.roundToPx() } },
                ),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VLCTvLayout.OverscanHorizontal, vertical = VLCTvLayout.OverscanVertical)
                        .widthIn(max = 720.dp),
                ) {
                    Text(
                        text = title.ifBlank { " " },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(enterDuration, easing = VLCMotion.EmphasizedDecelerate)) + slideInVertically(
                    animationSpec = tween(enterDuration, easing = VLCMotion.EmphasizedDecelerate),
                    initialOffsetY = { with(density) { 12.dp.roundToPx() } },
                ),
                exit = fadeOut(tween(exitDuration, easing = VLCMotion.EmphasizedAccelerate)) + slideOutVertically(
                    animationSpec = tween(exitDuration, easing = VLCMotion.EmphasizedAccelerate),
                    targetOffsetY = { with(density) { 8.dp.roundToPx() } },
                ),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VLCTvLayout.OverscanHorizontal)
                        .padding(bottom = VLCTvLayout.OverscanVertical),
                ) {
                    TvPlayerTimeline(
                        progress = progress,
                        isLiveStream = isLiveStream,
                        bookmarks = bookmarks,
                        seekStepMs = seekStepMs,
                        onSeek = onSeek,
                        onUserInteraction = onUserInteraction,
                        onInteractionActiveChanged = onInteractionActiveChanged,
                    )
                    Spacer(Modifier.height(20.dp))
                    TvTransportRow(
                        playing = playing,
                        rate = rate,
                        queueSize = queueSize,
                        hasVideoOutput = hasVideoOutput,
                        playFocusRequester = playFocusRequester,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onOpenSheet = onOpenSheet,
                        onUserInteraction = onUserInteraction,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvTransportRow(
    playing: Boolean,
    rate: Float,
    queueSize: Int,
    hasVideoOutput: Boolean,
    playFocusRequester: FocusRequester,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSheet: (TvPlayerSheet) -> Unit,
    onUserInteraction: () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasVideoOutput) {
                TvChromeIconButton(
                    icon = ExtraSymbols.Subtitles,
                    contentDescription = stringResource(Res.string.tracks),
                    previewKey = TvPlayerFocusKeys.TRACKS,
                    onClick = { onUserInteraction(); onOpenSheet(TvPlayerSheet.TRACKS) },
                )
            }
            TvChromeIconButton(
                icon = MaterialSymbols.Filled.QueueMusic,
                contentDescription = stringResource(Res.string.up_next_count, queueSize),
                previewKey = TvPlayerFocusKeys.QUEUE,
                onClick = { onUserInteraction(); onOpenSheet(TvPlayerSheet.QUEUE) },
            )
        }
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvChromeIconButton(
                icon = MaterialSymbols.Filled.SkipPrevious,
                contentDescription = stringResource(Res.string.previous),
                size = 56.dp,
                iconSize = 30.dp,
                previewKey = TvPlayerFocusKeys.PREVIOUS,
                onClick = { onUserInteraction(); onPrevious() },
            )
            TvPlayButton(
                playing = playing,
                focusRequester = playFocusRequester,
                onClick = { onUserInteraction(); onTogglePlay() },
            )
            TvChromeIconButton(
                icon = MaterialSymbols.Filled.SkipNext,
                contentDescription = stringResource(Res.string.next),
                size = 56.dp,
                iconSize = 30.dp,
                previewKey = TvPlayerFocusKeys.NEXT,
                onClick = { onUserInteraction(); onNext() },
            )
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvRateButton(
                rate = rate,
                onClick = { onUserInteraction(); onOpenSheet(TvPlayerSheet.SPEED) },
            )
            TvChromeIconButton(
                icon = MaterialSymbols.Filled.Tune,
                contentDescription = stringResource(Res.string.player_controls),
                previewKey = TvPlayerFocusKeys.TOOLS,
                onClick = { onUserInteraction(); onOpenSheet(TvPlayerSheet.TOOLS) },
            )
        }
    }
}

/** Secondary control: a light glyph on a faint disc that turns solid accent when focused. */
@Composable
private fun TvChromeIconButton(
    icon: MaterialIcon,
    contentDescription: String,
    previewKey: Any,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = VLCTvLayout.IconButtonSize,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
    val scheme = MaterialTheme.colorScheme
    VLCTvSurface(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        focusedScale = 1.12f,
        containerColor = Color.White.copy(alpha = 0.12f),
        focusedContainerColor = scheme.primary,
        contentColor = Color.White,
        focusedContentColor = scheme.onPrimary,
        outline = false,
        contentDescription = contentDescription,
        previewKey = previewKey,
    ) {
        Icon(icon = icon, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(iconSize))
    }
}

/**
 * The primary action. At rest it is a frosted disc like its neighbours so the accent never doubles
 * as a focus cue; focused (the default when the chrome appears) it becomes the accent pill the
 * phone player uses, so the first thing the eye lands on is the familiar VLC play button.
 */
@Composable
private fun TvPlayButton(playing: Boolean, focusRequester: FocusRequester, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalVLCMotion.current
    VLCTvSurface(
        onClick = onClick,
        modifier = Modifier.focusRequester(focusRequester).size(width = 88.dp, height = 72.dp),
        shape = RoundedCornerShape(36.dp),
        focusedScale = 1.08f,
        containerColor = Color.White.copy(alpha = 0.18f),
        focusedContainerColor = scheme.primary,
        contentColor = Color.White,
        focusedContentColor = scheme.onPrimary,
        outline = false,
        contentDescription = stringResource(if (playing) Res.string.pause else Res.string.play),
        previewKey = TvPlayerFocusKeys.PLAY,
    ) {
        Crossfade(
            targetState = playing,
            animationSpec = tween(if (motion.reducedMotion) 0 else motion.durationShort, easing = VLCMotion.Emphasized),
            modifier = Modifier.align(Alignment.Center),
            label = "tv-play-icon",
        ) { isPlaying ->
            Icon(
                icon = if (isPlaying) MaterialSymbols.Filled.Pause else MaterialSymbols.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun TvRateButton(rate: Float, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(Res.string.playback_speed)
    VLCTvSurface(
        onClick = onClick,
        modifier = Modifier.height(VLCTvLayout.IconButtonSize).widthIn(min = 72.dp),
        shape = CircleShape,
        focusedScale = 1.08f,
        containerColor = Color.White.copy(alpha = 0.12f),
        focusedContainerColor = scheme.primary,
        contentColor = Color.White,
        focusedContentColor = scheme.onPrimary,
        outline = false,
        contentDescription = label,
        previewKey = TvPlayerFocusKeys.SPEED,
    ) {
        Text(
            text = playbackRateLabel(rate),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * D-pad timeline. Left/right move a preview position (repeats accelerate from the key-seek step to
 * a minute), and the seek is issued once the remote rests or on OK, so a decoder never receives a
 * burst of seeks. The thumb and a time bubble only appear while it holds focus.
 */
@Composable
private fun TvPlayerTimeline(
    progress: Progress,
    isLiveStream: Boolean,
    bookmarks: PlaybackBookmarks,
    seekStepMs: Long,
    onSeek: (Long) -> Unit,
    onUserInteraction: () -> Unit,
    onInteractionActiveChanged: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalVLCMotion.current
    val length = progress.length.takeIf { it > 0L }
    val interactionSource = remember { MutableInteractionSource() }
    val realFocus by interactionSource.collectIsFocusedAsState()
    val focused = realFocus || LocalVLCTvPreviewFocus.current == TvPlayerFocusKeys.TIMELINE
    var scrubTarget by remember(length) { mutableStateOf<Long?>(null) }
    var repeatCount by remember { mutableIntStateOf(0) }
    var scrubEpoch by remember { mutableIntStateOf(0) }
    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestActive by rememberUpdatedState(onInteractionActiveChanged)
    LaunchedEffect(scrubEpoch) {
        val target = scrubTarget ?: return@LaunchedEffect
        delay(TvScrubCommitDelayMs)
        latestOnSeek(target)
        scrubTarget = null
        repeatCount = 0
        latestActive(false)
    }
    val displayed = (scrubTarget ?: progress.time).coerceAtLeast(0L).let { if (length != null) it.coerceAtMost(length) else it }
    val trackHeight by animateDpAsState(
        targetValue = if (focused) 8.dp else 4.dp,
        animationSpec = tween(if (motion.reducedMotion) 0 else motion.durationShort, easing = VLCMotion.Emphasized),
        label = "tv-timeline-track",
    )
    val positionDescription = length?.let {
        stringResource(Res.string.playback_position_value, formatPlaybackTime(displayed), formatPlaybackTime(it))
    }
    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .semantics {
                    positionDescription?.let { contentDescription = it }
                    if (length == null) stateDescription = "live"
                }
                .onKeyEvent { event ->
                    if (length == null) return@onKeyEvent false
                    val direction = when (event.key) {
                        Key.DirectionLeft -> -1
                        Key.DirectionRight -> 1
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> 0
                        else -> return@onKeyEvent false
                    }
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent true
                    onUserInteraction()
                    if (direction == 0) {
                        val target = scrubTarget ?: return@onKeyEvent false
                        latestOnSeek(target)
                        scrubTarget = null
                        repeatCount = 0
                        latestActive(false)
                        return@onKeyEvent true
                    }
                    repeatCount = if (scrubTarget != null) repeatCount + 1 else 0
                    val step = tvScrubStepMs(seekStepMs, repeatCount)
                    scrubTarget = ((scrubTarget ?: progress.time) + direction * step).coerceIn(0L, length)
                    latestActive(true)
                    scrubEpoch++
                    true
                }
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.CenterStart,
        ) {
            val fraction = if (length != null) (displayed.toFloat() / length.toFloat()).coerceIn(0f, 1f) else 0f
            val bufferedColor = Color.White.copy(alpha = if (focused) 0.32f else 0.24f)
            val accent = scheme.primary
            val markerFractions = if (length != null) bookmarks.entries.map { (it.timeMs.toFloat() / length).coerceIn(0f, 1f) } else emptyList()
            Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
                val radius = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(color = bufferedColor, cornerRadius = radius)
                if (length != null) {
                    drawRoundRect(color = accent, size = Size(size.width * fraction, size.height), cornerRadius = radius)
                }
                markerFractions.forEach { marker ->
                    val x = size.width * marker
                    drawCircle(Color.White, radius = size.height * 0.6f, center = Offset(x, size.height / 2f))
                }
            }
            if (length != null && focused) {
                val thumb = 20.dp
                val x = (maxWidth - thumb) * fraction
                Box(
                    Modifier
                        .offset(x = x)
                        .size(thumb)
                        .shadow(10.dp, CircleShape, ambientColor = accent, spotColor = accent)
                        .clip(CircleShape)
                        .background(Color.White),
                )
                if (scrubTarget != null) {
                    val bubbleWidth = 96.dp
                    val bubbleX = (x + thumb / 2 - bubbleWidth / 2).coerceIn(0.dp, maxWidth - bubbleWidth)
                    Surface(
                        modifier = Modifier.offset(x = bubbleX, y = (-36).dp).width(bubbleWidth),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        contentColor = Color.Black,
                    ) {
                        Text(
                            text = formatPlaybackTime(displayed),
                            modifier = Modifier.padding(vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val timeStyle = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum", fontSize = 15.sp)
            Text(formatPlaybackTime(displayed), color = Color.White.copy(alpha = 0.86f), style = timeStyle, maxLines = 1)
            if (length != null) {
                Text(
                    text = "−${formatPlaybackTime((length - displayed).coerceAtLeast(0L))}",
                    color = Color.White.copy(alpha = 0.66f),
                    style = timeStyle,
                    maxLines = 1,
                )
            } else {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f), contentColor = Color.White) {
                    Text(
                        text = stringResource(if (isLiveStream) Res.string.live_stream else Res.string.duration_unavailable),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** A scrub rests this long before the single seek is issued. */
internal const val TvScrubCommitDelayMs = 900L

/** Held or repeated presses travel further: the key-seek step, then 30 s, then a minute. */
internal fun tvScrubStepMs(baseStepMs: Long, repeatCount: Int): Long = when {
    repeatCount >= 12 -> maxOf(baseStepMs, 60_000L)
    repeatCount >= 4 -> maxOf(baseStepMs, 30_000L)
    else -> baseStepMs.coerceAtLeast(1_000L)
}

/**
 * Audio on the TV player (rare: music normally opens the TV audio player). The cover sits above the
 * chrome, which already shows title and artist, so nothing is repeated.
 */
@Composable
fun TvPlayerArtworkStage(state: PlayerUiState, modifier: Modifier = Modifier) {
    val item = state.queue.getOrNull(state.currentQueueIndex) ?: MediaItem(
        id = 0L,
        title = state.title,
        uri = state.uri,
        type = MediaType.AUDIO,
        artworkUri = state.artworkUri,
    )
    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        // Clear of the title above and the timeline below (about 170dp of bottom chrome).
        val cover = minOf(maxHeight - 360.dp, 220.dp).coerceAtLeast(0.dp)
        MediaArtwork(
            item = item,
            contentDescription = null,
            size = cover,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
                .size(cover)
                .clip(RoundedCornerShape(VLCTvLayout.CardCorner)),
            contentScale = ContentScale.Crop,
            fallback = { GenerativeAudioArtwork(item, state.progress) },
        )
    }
}

/** What a remote key does on the TV player. Pure, so the policy is unit-tested. */
internal enum class TvPlayerKeyAction {
    TOGGLE_PLAY,
    /** OK on a bare picture: toggle playback and bring the chrome up on the play button. */
    TOGGLE_PLAY_AND_SHOW,
    PLAY,
    PAUSE,
    SEEK_BACK,
    SEEK_FORWARD,
    SHOW_HUD,
    HIDE_HUD,
    CLOSE,
    OPEN_TOOLS,
    OPEN_TRACKS,
    NEXT,
    PREVIOUS,
}

/**
 * TV remote policy.
 *
 * Media keys and Menu always act. Back first hides visible chrome, then leaves. While the chrome is
 * hidden the D-pad drives playback directly (left/right seek, OK plays/pauses and reveals the
 * chrome, up/down reveal it); while it is shown the D-pad belongs to focus navigation.
 */
internal fun tvPlayerKeyAction(key: Key, chromeVisible: Boolean, chromeHideable: Boolean): TvPlayerKeyAction? =
    when (key) {
        Key.MediaPlayPause, Key.Spacebar -> TvPlayerKeyAction.TOGGLE_PLAY
        Key.MediaPlay -> TvPlayerKeyAction.PLAY
        Key.MediaPause -> TvPlayerKeyAction.PAUSE
        Key.MediaNext -> TvPlayerKeyAction.NEXT
        Key.MediaPrevious -> TvPlayerKeyAction.PREVIOUS
        Key.MediaFastForward -> TvPlayerKeyAction.SEEK_FORWARD
        Key.MediaRewind -> TvPlayerKeyAction.SEEK_BACK
        Key.MediaStop -> TvPlayerKeyAction.CLOSE
        Key.Menu -> TvPlayerKeyAction.OPEN_TOOLS
        Key.MediaAudioTrack -> TvPlayerKeyAction.OPEN_TRACKS
        Key.Back, Key.Escape -> if (chromeVisible && chromeHideable) TvPlayerKeyAction.HIDE_HUD else TvPlayerKeyAction.CLOSE
        Key.DirectionLeft -> if (chromeVisible) null else TvPlayerKeyAction.SEEK_BACK
        Key.DirectionRight -> if (chromeVisible) null else TvPlayerKeyAction.SEEK_FORWARD
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> if (chromeVisible) null else TvPlayerKeyAction.TOGGLE_PLAY_AND_SHOW
        Key.DirectionUp, Key.DirectionDown -> if (chromeVisible) null else TvPlayerKeyAction.SHOW_HUD
        else -> null
    }
