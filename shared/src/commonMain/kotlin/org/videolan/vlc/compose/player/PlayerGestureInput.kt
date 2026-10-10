package org.videolan.vlc.compose.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Receiver for the raw player gestures; all policy lives in the shared player. */
internal interface PlayerGestureHandler {
    val doubleTapEnabled: Boolean
    val longPressEnabled: Boolean
    val pinchEnabled: Boolean

    fun updateViewport(size: IntSize)

    fun onTap(position: Offset)
    /** True when a running seek burst should absorb this single tap immediately. */
    fun continuesSeekBurst(position: Offset): Boolean
    fun onDoubleTap(position: Offset)
    fun onLongPressStart()
    fun onLongPressEnd()

    /** Returns false when nothing should happen for a vertical swipe starting at [start]. */
    fun onVerticalDragStart(start: Offset): Boolean
    fun onVerticalDrag(totalDy: Float)
    fun onVerticalDragEnd()

    fun onHorizontalDragStart(): Boolean
    fun onHorizontalDrag(totalDxDp: Float)
    fun onHorizontalDragEnd()

    fun onPinch(cumulativeZoom: Float)
    fun onPinchEnd(cumulativeZoom: Float)
}

private enum class GestureStart { TAP, VERTICAL, HORIZONTAL, PINCH, CANCELLED }

/** VLC's hold delay for fast play: short enough to feel direct, long enough to not steal taps. */
private const val LongPressMillis = 300L

/**
 * One state machine for every touch on the video picture, so taps, swipes, pinches and holds
 * never fight over the same pointer. A gesture commits to one meaning as soon as it is clear.
 */
internal suspend fun PointerInputScope.detectPlayerGestures(handler: () -> PlayerGestureHandler) =
    awaitEachGesture {
        val current = handler()
        current.updateViewport(size)
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = viewConfiguration.touchSlop
        // System gestures own the top and bottom edges (notification shade, home/back swipes).
        val edgeGuard = 28.dp.toPx()
        val nearEdge = down.position.y < edgeGuard || down.position.y > size.height - edgeGuard

        val decision: GestureStart? = if (current.longPressEnabled) {
            withTimeoutOrNull(LongPressMillis) { classify(down.id, down.position, slop) }
        } else {
            classify(down.id, down.position, slop)
        }

        when (decision) {
            null -> {
                // Held still: fast play until every finger lifts.
                current.onLongPressStart()
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                } finally {
                    current.onLongPressEnd()
                }
            }
            GestureStart.TAP -> handleTap(current, down.position)
            GestureStart.VERTICAL -> {
                if (!nearEdge && current.onVerticalDragStart(down.position)) {
                    trackDrag(down.id) { change -> current.onVerticalDrag(change.y - down.position.y) }
                    current.onVerticalDragEnd()
                }
            }
            GestureStart.HORIZONTAL -> {
                if (!nearEdge && current.onHorizontalDragStart()) {
                    trackDrag(down.id) { change ->
                        current.onHorizontalDrag((change.x - down.position.x) / density)
                    }
                    current.onHorizontalDragEnd()
                }
            }
            GestureStart.PINCH -> if (current.pinchEnabled) trackPinch(current)
            GestureStart.CANCELLED -> Unit
        }
    }

private suspend fun AwaitPointerEventScope.classify(id: PointerId, origin: Offset, slop: Float): GestureStart {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.count { it.pressed } >= 2) return GestureStart.PINCH
        val change = event.changes.firstOrNull { it.id == id } ?: return GestureStart.CANCELLED
        if (change.isConsumed) return GestureStart.CANCELLED
        if (!change.pressed) return GestureStart.TAP
        val delta = change.position - origin
        if (delta.getDistance() > slop) {
            return if (abs(delta.x) > abs(delta.y)) GestureStart.HORIZONTAL else GestureStart.VERTICAL
        }
    }
}

private suspend fun AwaitPointerEventScope.handleTap(handler: PlayerGestureHandler, position: Offset) {
    if (handler.continuesSeekBurst(position)) {
        handler.onDoubleTap(position)
        return
    }
    if (!handler.doubleTapEnabled) {
        handler.onTap(position)
        return
    }
    val second = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        awaitFirstDown(requireUnconsumed = false)
    }
    if (second == null) {
        handler.onTap(position)
        return
    }
    // Act on the second touch-down, like the platform double-tap detectors, so it feels instant.
    handler.onDoubleTap(second.position)
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.none { it.pressed }) break
    }
}

private suspend fun AwaitPointerEventScope.trackDrag(id: PointerId, onMove: (Offset) -> Unit) {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == id } ?: break
        if (!change.pressed) break
        change.consume()
        onMove(change.position)
    }
}

private suspend fun AwaitPointerEventScope.trackPinch(handler: PlayerGestureHandler) {
    var zoom = 1f
    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.count { it.pressed }
        if (pressed >= 2) {
            zoom *= event.calculateZoom()
            handler.onPinch(zoom)
            event.changes.forEach { it.consume() }
        } else {
            break
        }
    }
    handler.onPinchEnd(zoom)
    // Ignore the remaining finger so lifting a pinch never turns into a swipe or a tap.
    while (true) {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) break
    }
}
