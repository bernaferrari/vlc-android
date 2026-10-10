package org.videolan.vlc.compose.player

import org.videolan.vlc.player.VideoScaleMode
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/** The platform level a vertical swipe adjusts. */
enum class PlayerSwipeTarget { BRIGHTNESS, VOLUME }

/**
 * VLC's long-standing convention: the right part of the picture controls volume, the left part
 * brightness, and the middle seventh is a dead zone. When only one level is available it takes
 * both sides.
 */
internal fun verticalSwipeTarget(
    x: Float,
    width: Float,
    brightnessAvailable: Boolean,
    volumeAvailable: Boolean,
): PlayerSwipeTarget? {
    if (width <= 0f) return null
    val right = x > width * 4f / 7f
    val left = x < width * 3f / 7f
    return when {
        right && volumeAvailable -> PlayerSwipeTarget.VOLUME
        right && brightnessAvailable -> PlayerSwipeTarget.BRIGHTNESS
        left && brightnessAvailable -> PlayerSwipeTarget.BRIGHTNESS
        left && volumeAvailable -> PlayerSwipeTarget.VOLUME
        else -> null
    }
}

/**
 * Level after a vertical drag of [dragY] pixels (negative is up). A full-height drag moves the
 * level by 125%, matching the Android player's sensitivity.
 *
 * Amplification is never entered by accident: a gesture that started at or below 100% stops at
 * 100%, and the user must lift and swipe again to boost.
 */
internal fun levelAfterVerticalDrag(
    startLevel: Float,
    dragY: Float,
    height: Float,
    max: Float,
): Float {
    if (height <= 0f) return startLevel.coerceIn(0f, max)
    val upper = if (startLevel < 1f - LEVEL_EPSILON) minOf(1f, max) else max
    return (startLevel - dragY / height * 1.25f).coerceIn(0f, upper)
}

/** True when a drag is being held at 100% because amplification needs a second swipe. */
internal fun isHoldingAtUnity(startLevel: Float, level: Float, max: Float): Boolean =
    max > 1f && startLevel < 1f - LEVEL_EPSILON && level >= 1f - LEVEL_EPSILON

private const val LEVEL_EPSILON = 0.001f

/**
 * Seek offset for a horizontal swipe of [dragDp] density-independent pixels.
 *
 * This keeps VLC's bi-cubic progression: precise at a few centimetres, up to ten minutes for an
 * eight-centimetre gesture. Gestures shorter than one centimetre do not seek. The result is
 * clamped so the target stays inside `0..lengthMs`.
 */
internal fun swipeSeekOffsetMs(dragDp: Float, timeMs: Long, lengthMs: Long): Long {
    if (lengthMs <= 0L) return 0L
    val centimetres = dragDp / DP_PER_INCH * CM_PER_INCH
    if (abs(centimetres) < 1f) return 0L
    val jump = (sign(centimetres) * (600_000.0 * (abs(centimetres) / 8.0).pow(4.0) + 3_000.0)).toLong()
    val time = timeMs.coerceIn(0L, lengthMs)
    return jump.coerceIn(-time, lengthMs - time)
}

private const val DP_PER_INCH = 160f
private const val CM_PER_INCH = 2.54f

/** Which action a double tap triggers, by horizontal position. */
enum class DoubleTapZone { REWIND, PLAY_PAUSE, FORWARD }

internal fun doubleTapZone(x: Float, width: Float): DoubleTapZone = when {
    width <= 0f -> DoubleTapZone.PLAY_PAUSE
    x < width * 0.35f -> DoubleTapZone.REWIND
    x > width * 0.65f -> DoubleTapZone.FORWARD
    else -> DoubleTapZone.PLAY_PAUSE
}

/**
 * A run of seek taps on the same side. Further taps inside [SeekBurstWindowMs] keep adding to the
 * same burst, so tapping three times quickly reads as one "+30s" jump.
 */
data class SeekBurst(
    val forward: Boolean,
    val baseTimeMs: Long,
    val totalMs: Long,
    val taps: Int,
    val lastTapAtMs: Long,
)

internal const val SeekBurstWindowMs = 700L

internal fun SeekBurst?.isActiveAt(nowMs: Long): Boolean =
    this != null && nowMs - lastTapAtMs <= SeekBurstWindowMs

internal fun accumulateSeekBurst(
    previous: SeekBurst?,
    forward: Boolean,
    stepMs: Long,
    currentTimeMs: Long,
    nowMs: Long,
): SeekBurst =
    if (previous != null && previous.forward == forward && previous.isActiveAt(nowMs)) {
        previous.copy(totalMs = previous.totalMs + stepMs, taps = previous.taps + 1, lastTapAtMs = nowMs)
    } else {
        SeekBurst(forward = forward, baseTimeMs = currentTimeMs, totalMs = stepMs, taps = 1, lastTapAtMs = nowMs)
    }

/** The position a burst seeks to, clamped to the media when its length is known. */
internal fun SeekBurst.targetMs(lengthMs: Long): Long {
    val raw = if (forward) baseTimeMs + totalMs else baseTimeMs - totalMs
    return if (lengthMs > 0L) raw.coerceIn(0L, lengthMs) else raw.coerceAtLeast(0L)
}

/**
 * Two-finger zoom only toggles between the two choices people reach for: show the whole picture,
 * or fill the screen by cropping. Small, accidental pinches do nothing.
 */
internal fun pinchScaleMode(cumulativeZoom: Float, current: VideoScaleMode): VideoScaleMode? = when {
    cumulativeZoom > 1.12f && current != VideoScaleMode.FIT_SCREEN -> VideoScaleMode.FIT_SCREEN
    cumulativeZoom < 0.88f && current != VideoScaleMode.BEST_FIT -> VideoScaleMode.BEST_FIT
    else -> null
}

/** Live visual stretch while pinching, kept subtle so the picture never jumps. */
internal fun pinchPreviewScale(cumulativeZoom: Float): Float =
    (1f + (cumulativeZoom - 1f) * 0.35f).coerceIn(0.9f, 1.12f)

/** Volume step used by keyboard and remote arrows: one of fifteen device steps. */
internal const val KeyVolumeStep = 1f / 15f
