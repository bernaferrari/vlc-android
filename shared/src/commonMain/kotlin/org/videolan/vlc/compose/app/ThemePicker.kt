@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.videolan.vlc.compose.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCThemeAccent
import org.videolan.vlc.compose.theme.VLCThemeAppearance
import org.videolan.vlc.compose.theme.availableVLCThemeAccents
import kotlin.math.roundToInt

/**
 * Appearance picker: a single connected button group for Light / Dark / System, and a spectrum
 * track for the accent. The track is one continuous gradient through every curated accent; its
 * thumb can be tapped or dragged, follows the finger, tints itself with the colour underneath and
 * springs onto the nearest accent on release, recolouring the app live as it passes each stop.
 */
@Composable
internal fun ThemePicker(
    appearance: VLCThemeAppearance,
    accent: VLCThemeAccent,
    onAppearanceChange: (VLCThemeAppearance) -> Unit,
    onAccentChange: (VLCThemeAccent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            ShellStrings.appearance(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.padding(16.dp)) {
                AppearanceButtons(appearance, onAppearanceChange)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        ShellStrings.themeColor(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    val motion = LocalVLCMotion.current
                    AnimatedContent(
                        targetState = accent,
                        transitionSpec = {
                            if (motion.reducedMotion) fadeIn(snap()) togetherWith fadeOut(snap())
                            else (slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 } + fadeOut())
                        },
                        label = "accent-name",
                    ) { shown ->
                        Text(
                            ShellStrings.themeAccent(shown),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                val available = availableVLCThemeAccents()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (VLCThemeAccent.Dynamic in available) {
                        DynamicSwatch(selected = accent == VLCThemeAccent.Dynamic, onClick = { onAccentChange(VLCThemeAccent.Dynamic) })
                    }
                    AccentSpectrum(
                        stops = available - VLCThemeAccent.Dynamic,
                        selected = accent,
                        onSelect = onAccentChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** M3 Expressive connected button group: icon and label on one line, the selection morphs round. */
@Composable
private fun AppearanceButtons(appearance: VLCThemeAppearance, onChange: (VLCThemeAppearance) -> Unit) {
    val modes = listOf(
        VLCThemeAppearance.Light to ShellStrings.appearanceLight(),
        VLCThemeAppearance.Dark to ShellStrings.appearanceDark(),
        VLCThemeAppearance.System to ShellStrings.systemTheme(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        modes.forEachIndexed { index, (mode, label) ->
            val selected = mode == appearance
            ToggleButton(
                checked = selected,
                onCheckedChange = { onChange(mode) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.weight(1f).height(48.dp).semantics { role = Role.RadioButton },
            ) {
                val tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                ModeGlyph(mode, tint, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

private val TrackHeight = 48.dp
private val ThumbSize = 34.dp

@Composable
private fun AccentSpectrum(
    stops: List<VLCThemeAccent>,
    selected: VLCThemeAccent,
    onSelect: (VLCThemeAccent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalVLCMotion.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val colors = remember(stops) { stops.map { it.swatchColor } }
    val last = (stops.size - 1).coerceAtLeast(1)
    val selectedIndex = stops.indexOf(selected)
    val currentSelected by rememberUpdatedState(selected)
    // Thumb position in "stop units" (0 = first accent, last = final accent).
    val position = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex, dragging) {
        if (!dragging && selectedIndex >= 0) {
            if (motion.reducedMotion) position.snapTo(selectedIndex.toFloat())
            else position.animateTo(selectedIndex.toFloat(), spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    fun choose(index: Int) {
        val accent = stops[index.coerceIn(0, stops.lastIndex)]
        if (accent != currentSelected) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onSelect(accent)
        }
    }

    val name = ShellStrings.themeAccent(selected)
    val label = ShellStrings.themeColor()
    BoxWithConstraints(
        modifier
            .height(TrackHeight)
            .semantics {
                contentDescription = label
                stateDescription = name
                progressBarRangeInfo = ProgressBarRangeInfo(selectedIndex.coerceAtLeast(0).toFloat(), 0f..last.toFloat(), steps = (last - 1).coerceAtLeast(0))
                setProgress { value -> choose(value.roundToInt()); true }
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { choose((selectedIndex - 1).coerceAtLeast(0)); true }
                    Key.DirectionRight -> { choose(selectedIndex + 1); true }
                    else -> false
                }
            },
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val insetPx = with(density) { (TrackHeight / 2).toPx() }
        val spanPx = (widthPx - insetPx * 2).coerceAtLeast(1f)
        fun unitsAt(x: Float) = ((x - insetPx) / spanPx).coerceIn(0f, 1f) * last

        fun follow(x: Float) {
            val units = unitsAt(x)
            scope.launch { position.snapTo(units) }
            choose(units.roundToInt())
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .pointerInput(stops) { detectTapGestures(onTap = { choose(unitsAt(it.x).roundToInt()) }) }
                .pointerInput(stops) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true; follow(it.x) },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onHorizontalDrag = { change, _ -> change.consume(); follow(change.position.x) },
                    )
                },
        ) {
            drawRect(Brush.horizontalGradient(colors, startX = insetPx, endX = size.width - insetPx))
            // A soft dot marks each accent so the track reads as a set of choices, not a free slider.
            colors.indices.forEach { i ->
                drawCircle(Color.White.copy(alpha = 0.55f), radius = 2.5.dp.toPx(), center = Offset(insetPx + spanPx * i / last, size.height / 2))
            }
        }

        val units = position.value
        val lower = units.toInt().coerceIn(0, colors.lastIndex)
        val upper = (lower + 1).coerceAtMost(colors.lastIndex)
        val underneath = lerp(colors[lower], colors[upper], units - lower)
        val thumbScale by animateFloatAsState(
            targetValue = when {
                selectedIndex < 0 -> 0f
                dragging -> 1.22f
                else -> 1f
            },
            animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
            label = "thumb-scale",
        )
        val thumbPx = with(density) { ThumbSize.toPx() }
        Box(
            Modifier
                .offset { IntOffset((insetPx + spanPx * units / last - thumbPx / 2).roundToInt(), 0) }
                .align(Alignment.CenterStart)
                .size(ThumbSize)
                .scale(thumbScale)
                .shadow(6.dp, CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .padding(3.dp)
                .background(underneath, CircleShape),
        )
    }
}

/** Wallpaper colours (Android 12+): a sweep of the dynamic scheme that rings itself when chosen. */
@Composable
private fun DynamicSwatch(selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ring by animateColorAsState(if (selected) scheme.onSurface else Color.Transparent, label = "dynamic-ring")
    val label = ShellStrings.themeAccent(VLCThemeAccent.Dynamic)
    Box(
        Modifier
            .size(TrackHeight)
            .border(2.dp, ring, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.sweepGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary)))
        }
        Icon(MaterialSymbols.Filled.Palette, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
    }
}

/** Sun, moon and half-disc drawn in code so the glyphs stay crisp and theme-aware everywhere. */
@Composable
private fun ModeGlyph(mode: VLCThemeAppearance, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        when (mode) {
            VLCThemeAppearance.Light -> {
                drawCircle(color, radius = r * 0.42f)
                repeat(8) { i ->
                    val angle = (i * 45.0) * kotlin.math.PI / 180.0
                    val dir = Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat())
                    drawLine(color, center + dir * (r * 0.64f), center + dir * (r * 0.92f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
                }
            }
            VLCThemeAppearance.Dark -> {
                val path = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(center, r * 0.82f))
                    val cut = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(center + Offset(r * 0.42f, -r * 0.36f), r * 0.7f))
                    }
                    op(this, cut, PathOperation.Difference)
                }
                drawPath(path, color)
            }
            VLCThemeAppearance.System -> {
                drawCircle(color, radius = r * 0.8f, style = Stroke(width = r * 0.16f))
                drawArc(color, startAngle = 90f, sweepAngle = 180f, useCenter = true, topLeft = Offset(center.x - r * 0.8f, center.y - r * 0.8f), size = Size(r * 1.6f, r * 1.6f))
            }
        }
    }
}
