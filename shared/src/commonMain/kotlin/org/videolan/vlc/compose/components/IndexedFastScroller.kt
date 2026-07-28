package org.videolan.vlc.compose.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** A scroll position paired with the label displayed in the library index. */
data class VLCIndexScrollTarget(val itemIndex: Int, val labelSource: String)

/**
 * Normalizes library index labels so punctuation, numerals, and blank names group under '#'.
 * Keeping this pure makes the same ordering available to every media collection and common tests.
 */
fun vlcIndexLabel(value: String): String {
    val first = value.trim().firstOrNull()?.uppercaseChar() ?: return "#"
    return if (first in 'A'..'Z') first.toString() else "#"
}

/**
 * A direct-manipulation index for long, alphabetic media collections. It deliberately appears
 * only after [minItemCount] items: short lists remain visually quiet and use ordinary scrolling.
 */
@Composable
fun VLCIndexedFastScroller(
    targets: List<VLCIndexScrollTarget>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    minItemCount: Int = 24,
) {
    if (targets.size < minItemCount) return

    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val labelToTarget = remember(targets) {
        buildMap {
            targets.forEach { target -> putIfAbsent(vlcIndexLabel(target.labelSource), target.itemIndex) }
        }
    }
    val labels = remember(labelToTarget) {
        labelToTarget.keys.sortedWith(compareBy<String> { if (it == "#") 0 else 1 }.thenBy { it })
    }
    if (labels.isEmpty()) return

    val trackInset = 14.dp
    val bubbleSize = 64.dp
    val bubbleOffsetX = (-56).dp
    val preferredHeight = maxOf(bubbleSize, 22.dp * labels.size + trackInset * 2)
    val trackInsetPx = with(density) { trackInset.toPx() }
    val bubbleSizePx = with(density) { bubbleSize.toPx() }
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var currentLabel by remember { mutableStateOf("") }
    var priorLabel by remember { mutableStateOf("") }
    var interacting by remember { mutableStateOf(false) }

    fun selectAt(y: Float) {
        if (trackSize.height <= 0) return
        val height = trackSize.height.toFloat()
        val usable = (height - trackInsetPx * 2).coerceAtLeast(1f)
        val progress = ((y.coerceIn(trackInsetPx, height - trackInsetPx) - trackInsetPx) / usable).coerceIn(0f, 1f)
        val index = (progress * (labels.lastIndex)).roundToInt().coerceIn(0, labels.lastIndex)
        val label = labels[index]
        if (label != priorLabel) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            priorLabel = label
        }
        currentLabel = label
        dragY = if (labels.size == 1) height / 2 else trackInsetPx + index / labels.lastIndex.toFloat() * usable
        labelToTarget[label]?.let { target -> scope.launch { listState.scrollToItem(target) } }
    }

    Box(modifier = modifier.width(44.dp).height(preferredHeight)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = trackInset)
                .onGloballyPositioned { trackSize = it.size }
                .pointerInput(labels, targets) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            interacting = true
                            selectAt(down.position.y)
                            val change = awaitTouchSlopOrCancellation(down.id) { pointerChange, _ -> pointerChange.consume() }
                            if (change != null) drag(change.id) { dragChange -> selectAt(dragChange.position.y) }
                            interacting = false
                            priorLabel = ""
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val usable = (trackSize.height.toFloat() - trackInsetPx * 2).coerceAtLeast(1f)
            val progress = ((dragY - trackInsetPx).coerceIn(0f, usable) / usable)
            labels.forEachIndexed { index, label ->
                val labelPosition = if (labels.size == 1) .5f else index / labels.lastIndex.toFloat()
                val distance = abs(labelPosition - progress)
                val scale by animateFloatAsState(
                    targetValue = if (interacting && distance < .06f) 1.35f else 1f,
                    animationSpec = spring(dampingRatio = .82f),
                    label = "vlcIndexScale$index",
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (interacting && distance > .12f) .58f else .8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                )
            }
        }
        if (interacting && currentLabel.isNotEmpty()) {
            val y = (dragY - bubbleSizePx / 2).coerceIn(0f, (trackSize.height - bubbleSizePx).coerceAtLeast(0f))
            Surface(
                modifier = Modifier.align(Alignment.TopStart).size(bubbleSize).offset {
                    IntOffset(with(density) { bubbleOffsetX.roundToPx() }, y.roundToInt())
                },
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(currentLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
