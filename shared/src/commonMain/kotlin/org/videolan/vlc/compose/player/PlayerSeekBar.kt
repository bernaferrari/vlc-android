package org.videolan.vlc.compose.player

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
 * A quiet timeline: a slim rounded track and a round thumb that grows only while it is held.
 * It keeps Material's slider semantics and keyboard support while avoiding the expressive gap
 * and end-stop dot, which read as a broken bar at the very start of playback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSeekBar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
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
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
        thumb = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(thumbSize).background(activeColor, CircleShape))
            }
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f
            Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(color = inactiveColor, cornerRadius = radius)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = activeColor,
                        size = Size((size.width * fraction).coerceAtLeast(size.height), size.height),
                        topLeft = Offset.Zero,
                        cornerRadius = radius,
                    )
                }
            }
        },
    )
}
