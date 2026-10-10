package org.videolan.vlc.compose.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion

/**
 * VLC's one slider look: a slim rounded track and a round thumb that grows only while held.
 * It keeps Material's slider semantics, steps and keyboard support while avoiding the expressive
 * gap, tick dots and end-stop marker, which read as noise on timelines and equalizer bands.
 *
 * With [anchor] set, the active fill grows from that value instead of the start — the natural
 * reading for bipolar values such as ±dB gain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLCSlimSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
    steps: Int = 0,
    enabled: Boolean = true,
    anchor: Float? = null,
) {
    val active = if (enabled) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val interactionSource = remember { MutableInteractionSource() }
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val engaged = dragged || pressed
    val motion = LocalVLCMotion.current
    val thumbSize by animateDpAsState(
        targetValue = if (engaged) 20.dp else 14.dp,
        animationSpec = tween(if (motion.reducedMotion) 0 else motion.durationShort, easing = VLCMotion.Emphasized),
        label = "seek-thumb",
    )
    val trackHeight by animateDpAsState(
        targetValue = if (engaged) 6.dp else 4.dp,
        animationSpec = tween(if (motion.reducedMotion) 0 else motion.durationShort, easing = VLCMotion.Emphasized),
        label = "seek-track",
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
        thumb = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(thumbSize).background(active, CircleShape))
            }
        },
        track = { state ->
            val start = state.valueRange.start
            val span = state.valueRange.endInclusive - start
            fun fraction(v: Float) = if (span > 0f) ((v - start) / span).coerceIn(0f, 1f) else 0f
            val valueFraction = fraction(state.value)
            val anchorFraction = anchor?.let(::fraction) ?: 0f
            Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(color = inactiveColor, cornerRadius = radius)
                val from = minOf(anchorFraction, valueFraction) * size.width
                val to = maxOf(anchorFraction, valueFraction) * size.width
                // A start-anchored fill keeps a rounded cap even at zero; a centred fill at its
                // anchor shows nothing but the thumb, which is the honest "flat" state.
                val width = if (anchor == null) (to - from).coerceAtLeast(size.height) else to - from
                if (width > 0f) {
                    drawRoundRect(color = active, topLeft = Offset(from, 0f), size = Size(width, size.height), cornerRadius = radius)
                }
            }
        },
    )
}
