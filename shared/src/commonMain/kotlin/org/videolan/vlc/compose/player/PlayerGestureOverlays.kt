package org.videolan.vlc.compose.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.player.PlaybackResumeOffer
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.*
import kotlin.math.abs
import kotlin.math.roundToInt

/** The video chrome's translucent material: legible over any frame, never a solid card. */
private val OverlayScrim = Color.Black.copy(alpha = 0.6f)
private val OverlayTrack = Color.White.copy(alpha = 0.18f)
private val OverlayMuted = Color.White.copy(alpha = 0.72f)

/**
 * Every transient gesture overlay of the shared video player. The overlays never take pointer
 * input, so gestures keep flowing to the surface underneath them.
 */
@Composable
internal fun BoxScope.PlayerGestureOverlays(state: PlayerGestureFeedbackState) {
    val motion = LocalVLCMotion.current
    // Expire released feedback after a short beat; pinned (design review) feedback stays.
    LaunchedEffect(state.levelActive, state.level) {
        if (!state.pinned && !state.levelActive && state.level != null) {
            delay(650)
            state.level = null
        }
    }
    LaunchedEffect(state.seekActive, state.seek) {
        if (!state.pinned && !state.seekActive && state.seek != null) {
            delay(500)
            state.seek = null
        }
    }
    LaunchedEffect(state.tapSeek) {
        if (!state.pinned && state.tapSeek != null) {
            delay(SeekBurstWindowMs + 250)
            state.tapSeek = null
        }
    }
    LaunchedEffect(state.notice) {
        if (!state.pinned && state.notice != null) {
            delay(900)
            state.notice = null
        }
    }

    val level = state.level
    TransientOverlay(
        visible = level != null,
        modifier = Modifier.align(
            if (level?.target == PlayerSwipeTarget.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd,
        ).padding(horizontal = 28.dp),
    ) {
        RememberLast(level) { LevelPill(it) }
    }
    TransientOverlay(
        visible = level?.heldAtUnity == true,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 20.dp),
    ) {
        BoostHintChip()
    }

    val seek = state.seek
    TransientOverlay(visible = seek != null, modifier = Modifier.align(Alignment.Center)) {
        RememberLast(seek) { SeekPill(it) }
    }

    TapSeekOverlay(state.tapSeek, reducedMotion = motion.reducedMotion)

    val fastPlay = state.fastPlay
    TransientOverlay(
        visible = fastPlay != null,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 20.dp),
    ) {
        RememberLast(fastPlay) { FastPlayChip(it.rate) }
    }

    val notice = state.notice
    TransientOverlay(visible = notice != null, modifier = Modifier.align(Alignment.Center)) {
        RememberLast(notice) { NoticePill(it.kind) }
    }
}

/** Keeps the last non-null value so exit animations do not render an empty shell. */
@Composable
private fun <T : Any> RememberLast(value: T?, content: @Composable (T) -> Unit) {
    val holder = remember { arrayOfNulls<Any>(1) }
    if (value != null) holder[0] = value
    @Suppress("UNCHECKED_CAST")
    (holder[0] as? T)?.let { content(it) }
}

@Composable
private fun TransientOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motion = LocalVLCMotion.current
    val enter = if (motion.reducedMotion) 0 else 140
    val exit = if (motion.reducedMotion) 0 else 180
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(enter, easing = VLCMotion.EmphasizedDecelerate)) +
            scaleIn(tween(enter, easing = VLCMotion.EmphasizedDecelerate), initialScale = 0.92f),
        exit = fadeOut(tween(exit, easing = VLCMotion.EmphasizedAccelerate)) +
            scaleOut(tween(exit, easing = VLCMotion.EmphasizedAccelerate), targetScale = 0.96f),
    ) {
        content()
    }
}

/**
 * Brightness/volume: a slim vertical pill on the side being swiped. The pill itself is the track,
 * so the level reads at a glance without numbers; the percentage is a quiet caption.
 */
@Composable
private fun LevelPill(feedback: PlayerGestureFeedback.Level) {
    val colors = VLCThemeDefaults.colors
    val max = feedback.max.coerceAtLeast(1f)
    val fraction = (feedback.level / max).coerceIn(0f, 1f)
    val boosted = feedback.level > 1f + 0.005f
    val percent = (feedback.level * 100f).roundToInt()
    val label = stringResource(
        if (feedback.target == PlayerSwipeTarget.BRIGHTNESS) Res.string.brightness else Res.string.volume,
    )
    val icon = when {
        feedback.target == PlayerSwipeTarget.BRIGHTNESS -> MaterialSymbols.Filled.Brightness6
        feedback.level <= 0.001f -> MaterialSymbols.Filled.VolumeOff
        else -> MaterialSymbols.Filled.VolumeUp
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$label $percent%"
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        Box(
            Modifier
                .background(OverlayScrim, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "$percent%",
                color = if (boosted) colors.primary else Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .size(width = 48.dp, height = 168.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(OverlayScrim),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val fill = if (boosted) colors.primary else Color.White
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val radius = CornerRadius(size.width / 2f)
                drawRoundRect(color = OverlayTrack, cornerRadius = radius)
                val filled = size.height * fraction
                if (filled > 0f) {
                    val height = filled.coerceAtLeast(size.width)
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(0f, size.height - height),
                        size = Size(size.width, height),
                        cornerRadius = radius,
                    )
                }
                if (max > 1f) {
                    // A hairline marks 100%: everything above it is amplification.
                    val y = size.height - size.height / max
                    drawLine(
                        color = if (fraction >= 1f / max) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.5f),
                        start = Offset(size.width * 0.28f, y),
                        end = Offset(size.width * 0.72f, y),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
            }
            // The icon sits in the lowest segment; it inverts once the fill reaches it.
            val iconOnFill = fraction > 0.16f
            Icon(
                icon = icon,
                contentDescription = null,
                tint = if (iconOnFill) Color.Black.copy(alpha = 0.78f) else Color.White,
                modifier = Modifier.padding(bottom = 14.dp).size(22.dp),
            )
        }
    }
}

/** Explains the 100% detent; shown apart from the pill so the bar never moves under the finger. */
@Composable
private fun BoostHintChip() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(OverlayScrim)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon = MaterialSymbols.Filled.ArrowUpward,
            contentDescription = null,
            tint = VLCThemeDefaults.colors.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(Res.string.audio_boost_warning),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Horizontal scrub: the jump first, then where it lands, over a slim position bar. */
@Composable
private fun SeekPill(feedback: PlayerGestureFeedback.Seek) {
    val colors = VLCThemeDefaults.colors
    val sign = if (feedback.deltaMs < 0L) "−" else "+"
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(OverlayScrim)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .width(168.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = sign + formatPlaybackTime(abs(feedback.deltaMs)),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${formatPlaybackTime(feedback.targetMs)} / ${formatPlaybackTime(feedback.lengthMs)}",
            color = OverlayMuted,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
        Spacer(Modifier.height(12.dp))
        val fraction = if (feedback.lengthMs > 0L) {
            (feedback.targetMs.toFloat() / feedback.lengthMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        Canvas(Modifier.fillMaxWidth().height(4.dp)) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = OverlayTrack, cornerRadius = radius)
            drawRoundRect(
                color = colors.primary,
                size = Size((size.width * fraction).coerceAtLeast(size.height), size.height),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * Double-tap seek: the tapped third of the picture softly lights up with an arc-shaped wash, a
 * ripple grows from the finger, and the accumulated jump is labelled.
 */
@Composable
private fun BoxScope.TapSeekOverlay(feedback: PlayerGestureFeedback.TapSeek?, reducedMotion: Boolean) {
    val visibleFor = remember { arrayOfNulls<PlayerGestureFeedback.TapSeek>(1) }
    if (feedback != null) visibleFor[0] = feedback
    val shown = visibleFor[0]
    val alpha by animateFloatAsState(
        targetValue = if (feedback != null) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else if (feedback != null) 120 else 240, easing = VLCMotion.Emphasized),
        label = "tap-seek-alpha",
    )
    if (shown == null || alpha <= 0f) return
    val forward = shown.burst.forward
    BoxWithConstraints(
        Modifier
            .align(if (forward) Alignment.CenterEnd else Alignment.CenterStart)
            .fillMaxHeight()
            .fillMaxWidth(TapSeekRegionFraction)
            .graphicsLayer { this.alpha = alpha }
            .clip(if (forward) ForwardArcShape else RewindArcShape)
            .background(Color.White.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        val ripple = remember { Animatable(0f) }
        LaunchedEffect(shown.burst.taps, shown.burst.lastTapAtMs) {
            if (reducedMotion) {
                ripple.snapTo(1f)
            } else {
                ripple.snapTo(0f)
                ripple.animateTo(1f, tween(420, easing = VLCMotion.EmphasizedDecelerate))
            }
        }
        val regionWidth = constraints.maxWidth.toFloat()
        val regionHeight = constraints.maxHeight.toFloat()
        Canvas(Modifier.fillMaxSize()) {
            if (reducedMotion) return@Canvas
            // The ripple starts where the finger landed, translated into this side region.
            val regionStart = if (forward) regionWidth / TapSeekRegionFraction - regionWidth else 0f
            val origin = shown.touch.takeIf { it.isSpecified() }
                ?.let { Offset((it.x - regionStart).coerceIn(0f, regionWidth), it.y.coerceIn(0f, regionHeight)) }
                ?: Offset(regionWidth / 2f, regionHeight / 2f)
            val progress = ripple.value
            drawCircle(
                color = Color.White.copy(alpha = 0.16f * (1f - progress)),
                radius = maxOf(regionWidth, regionHeight) * (0.15f + progress * 0.85f),
                center = origin,
            )
        }
        val pulse = if (reducedMotion) 1f else 0.9f + 0.1f * ripple.value
        val seconds = (shown.burst.totalMs / 1000L).toInt()
        val label = (if (forward) "+" else "−") + if (seconds < 60) {
            stringResource(Res.string.player_seconds_short, seconds)
        } else {
            formatPlaybackTime(shown.burst.totalMs)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = label },
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .background(OverlayScrim, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon = if (forward) MaterialSymbols.Filled.FastForward else MaterialSymbols.Filled.FastRewind,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun Offset.isSpecified(): Boolean = this != Offset.Unspecified && !x.isNaN() && !y.isNaN()

/**
 * The lit side is bounded by one long, gentle elliptical curve rather than a rounded slab, so it
 * reads as light spilling in from the edge in both portrait and landscape.
 */
private val RewindArcShape = GenericShape { size, _ ->
    addOval(Rect(-size.width, -size.height * 0.25f, size.width, size.height * 1.25f))
}
private val ForwardArcShape = GenericShape { size, _ ->
    addOval(Rect(0f, -size.height * 0.25f, size.width * 2f, size.height * 1.25f))
}

/** Share of the picture lit by a double-tap seek, matching the tap zones. */
private const val TapSeekRegionFraction = 0.4f

@Composable
private fun FastPlayChip(rate: Float) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(OverlayScrim)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon = MaterialSymbols.Filled.FastForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = playbackRateLabel(rate),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NoticePill(kind: PlayerNoticeKind) {
    val icon: MaterialIcon = when (kind) {
        PlayerNoticeKind.PLAY -> MaterialSymbols.Filled.PlayArrow
        PlayerNoticeKind.PAUSE -> MaterialSymbols.Filled.Pause
        PlayerNoticeKind.ZOOM_FILL -> MaterialSymbols.Filled.ZoomOutMap
        PlayerNoticeKind.ZOOM_FIT -> MaterialSymbols.Filled.FitScreen
        PlayerNoticeKind.SCREENSHOT_SAVED -> MaterialSymbols.Filled.PhotoCamera
        PlayerNoticeKind.SCREENSHOT_FAILED -> MaterialSymbols.Filled.Warning
        PlayerNoticeKind.ROTATION_LOCKED -> MaterialSymbols.Filled.ScreenLockRotation
        PlayerNoticeKind.ROTATION_UNLOCKED -> MaterialSymbols.Filled.ScreenRotation
        PlayerNoticeKind.UNSEEKABLE -> MaterialSymbols.Filled.Block
    }
    val text: String? = when (kind) {
        PlayerNoticeKind.PLAY, PlayerNoticeKind.PAUSE -> null
        PlayerNoticeKind.ZOOM_FILL -> stringResource(Res.string.player_zoom_fill)
        PlayerNoticeKind.ZOOM_FIT -> stringResource(Res.string.player_zoom_fit)
        PlayerNoticeKind.SCREENSHOT_SAVED -> stringResource(Res.string.player_screenshot_saved)
        PlayerNoticeKind.SCREENSHOT_FAILED -> stringResource(Res.string.screenshot_error)
        PlayerNoticeKind.ROTATION_LOCKED -> stringResource(Res.string.player_rotation_locked)
        PlayerNoticeKind.ROTATION_UNLOCKED -> stringResource(Res.string.player_rotation_unlocked)
        PlayerNoticeKind.UNSEEKABLE -> stringResource(Res.string.unseekable_stream)
    }
    if (text == null) {
        // Play/pause is self-explanatory; a single glyph keeps the picture uncluttered.
        Box(
            Modifier.size(72.dp).background(OverlayScrim, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        return
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(OverlayScrim)
            .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * "Resume from 12:34" — a non-blocking alternative to the old modal question. Playback has already
 * started from the beginning; one tap jumps to the saved position, and the chip leaves by itself.
 */
@Composable
internal fun BoxScope.ResumeOfferChip(
    offer: PlaybackResumeOffer?,
    hudVisible: Boolean,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    pinned: Boolean = false,
) {
    LaunchedEffect(offer) {
        if (offer != null && !pinned) {
            delay(8_000)
            onDismiss()
        }
    }
    val motion = LocalVLCMotion.current
    val topPadding by animateFloatAsState(
        targetValue = if (hudVisible) 72f else 20f,
        animationSpec = tween(if (motion.reducedMotion) 0 else 180, easing = VLCMotion.Emphasized),
        label = "resume-chip-offset",
    )
    TransientOverlay(
        visible = offer != null,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = topPadding.dp),
    ) {
        RememberLast(offer) { shown ->
            val label = stringResource(Res.string.player_resume_from, formatPlaybackTime(shown.positionMs))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.72f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = label, onClick = onResume)
                        .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon = MaterialSymbols.Filled.History,
                        contentDescription = null,
                        tint = VLCThemeDefaults.colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Box(
                    Modifier
                        .padding(vertical = 8.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.22f)),
                )
                val closeLabel = stringResource(Res.string.close)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = onDismiss)
                        .padding(10.dp)
                        .semantics { contentDescription = closeLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon = MaterialSymbols.Filled.Close,
                        contentDescription = null,
                        tint = OverlayMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
