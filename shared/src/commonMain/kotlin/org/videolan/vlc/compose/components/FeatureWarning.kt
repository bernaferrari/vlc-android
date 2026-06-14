package org.videolan.vlc.compose.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import kotlin.math.roundToInt

/**
 * Compose content for optional-feature confirmation warnings. The app module
 * owns the BottomSheetDialog shell and passes the warning/unlock icon slots.
 */
@Composable
fun VLCFeatureWarningDialogContent(
    title: String,
    genericWarning: String,
    detailWarning: String?,
    swipeText: String,
    isDpadAllowed: Boolean,
    onSwipeStart: () -> Unit,
    onSwipeStop: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    warningIcon: @Composable (() -> Unit)? = null,
    unlockIcon: @Composable (() -> Unit)? = null
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundDefault)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    if (warningIcon != null) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            warningIcon()
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        text = genericWarning,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!detailWarning.isNullOrBlank()) {
                    Text(
                        text = detailWarning,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                VLCSwipeUnlockControl(
                    text = swipeText,
                    isDpadAllowed = isDpadAllowed,
                    onSwipeStart = onSwipeStart,
                    onSwipeStop = onSwipeStop,
                    onUnlock = onUnlock,
                    thumbContent = unlockIcon,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )
            }
        }
    }
}

@Composable
private fun VLCSwipeUnlockControl(
    text: String,
    isDpadAllowed: Boolean,
    onSwipeStart: () -> Unit,
    onSwipeStop: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    thumbContent: @Composable (() -> Unit)? = null
) {
    val colors = VLCThemeDefaults.colors
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val onSwipeStartState by rememberUpdatedState(onSwipeStart)
    val onSwipeStopState by rememberUpdatedState(onSwipeStop)
    val onUnlockState by rememberUpdatedState(onUnlock)
    val offset = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }
    var widthPx by remember { mutableStateOf(0f) }
    var keyJob by remember { mutableStateOf<Job?>(null) }
    var unlocked by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val thumbSize = 48.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val maxOffset = (widthPx - thumbSizePx).coerceAtLeast(0f)
    val acceptedKeys = remember {
        setOf(
            Key.DirectionCenter,
            Key.DirectionDown,
            Key.DirectionLeft,
            Key.DirectionRight,
            Key.DirectionUp,
            Key.Enter,
            Key.NumPadEnter
        )
    }

    fun unlock() {
        if (!unlocked) {
            unlocked = true
            keyJob?.cancel()
            keyJob = null
            onUnlockState()
        }
    }

    fun resetThumb() {
        scope.launch {
            offset.animateTo(0f, tween(durationMillis = 250))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(maxOffset) {
        if (offset.value > maxOffset) offset.snapTo(maxOffset)
    }

    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (!isDpadAllowed || event.key !in acceptedKeys || maxOffset <= 0f || unlocked) {
                    return@onKeyEvent false
                }
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (keyJob?.isActive != true) {
                            onSwipeStartState()
                            keyJob = scope.launch {
                                offset.animateTo(
                                    targetValue = maxOffset,
                                    animationSpec = tween(durationMillis = 2_000, easing = LinearEasing)
                                )
                                unlock()
                            }
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        keyJob?.cancel()
                        keyJob = null
                        onSwipeStopState()
                        resetThumb()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(maxOffset, layoutDirection, unlocked) {
                detectDragGestures(
                    onDragStart = {
                        if (!unlocked) {
                            isDragging = true
                            onSwipeStartState()
                        }
                    },
                    onDragCancel = {
                        if (!unlocked && isDragging) {
                            isDragging = false
                            onSwipeStopState()
                            resetThumb()
                        }
                    },
                    onDragEnd = {
                        if (!unlocked && isDragging) {
                            isDragging = false
                            onSwipeStopState()
                            resetThumb()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!unlocked && maxOffset > 0f) {
                            change.consume()
                            val signedDelta = if (layoutDirection == LayoutDirection.Rtl) -dragAmount.x else dragAmount.x
                            val next = (offset.value + signedDelta).coerceIn(0f, maxOffset)
                            scope.launch { offset.snapTo(next) }
                            if (next >= maxOffset) unlock()
                        }
                    }
                )
            }
            .background(Color(0xDD000000), RoundedCornerShape(percent = 50))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(percent = 50))
            .padding(4.dp)
    ) {
        val progress = if (maxOffset == 0f) 0f else offset.value / maxOffset
        Text(
            text = text.uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 64.dp, end = 24.dp)
                .alpha((1f - progress).coerceIn(0.15f, 1f))
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(thumbSize)
                .background(if (thumbContent == null) colors.primary else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (thumbContent != null) {
                thumbContent()
            } else {
                Text(
                    text = ">",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PreviewWarningDot() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0xFFFF8800), CircleShape)
    )
}
