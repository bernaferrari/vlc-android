package org.videolan.vlc.compose.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCThemeAccent
import org.videolan.vlc.compose.theme.VLCThemeAppearance
import org.videolan.vlc.compose.theme.availableVLCThemeAccents

/**
 * Appearance picker: three illustrated mode tiles and a row of two-tone accent swatches. The
 * selection morphs its shape with an expressive spring (tile corners open up, the swatch turns
 * from circle to squircle) and gains a check, so the choice never relies on colour alone.
 */
@OptIn(ExperimentalLayoutApi::class)
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
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        VLCThemeAppearance.Light to ShellStrings.appearanceLight(),
                        VLCThemeAppearance.Dark to ShellStrings.appearanceDark(),
                        VLCThemeAppearance.System to ShellStrings.systemTheme(),
                    ).forEach { (mode, label) ->
                        ModeTile(
                            mode = mode,
                            label = label,
                            selected = mode == appearance,
                            onClick = { onAppearanceChange(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
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
                FlowRow(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 6,
                ) {
                    availableVLCThemeAccents().forEach { option ->
                        AccentSwatch(option, selected = option == accent, onClick = { onAccentChange(option) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTile(
    mode: VLCThemeAppearance,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalVLCMotion.current
    val corner by animateDpAsState(
        targetValue = if (selected) 28.dp else 14.dp,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "mode-corner",
    )
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "mode-container",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "mode-content",
    )
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .height(80.dp)
            .clip(shape)
            .background(container, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ModeGlyph(mode, content, Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            maxLines = 1,
        )
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
                    drawLine(color, center + dir * (r * 0.64f), center + dir * (r * 0.92f), strokeWidth = r * 0.14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            VLCThemeAppearance.Dark -> {
                // Crescent: a disc minus an offset disc, drawn as an arc band for portability.
                val path = androidx.compose.ui.graphics.Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(center, r * 0.82f))
                    val cut = androidx.compose.ui.graphics.Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(center + Offset(r * 0.42f, -r * 0.36f), r * 0.7f))
                    }
                    op(this, cut, androidx.compose.ui.graphics.PathOperation.Difference)
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

@Composable
private fun AccentSwatch(accent: VLCThemeAccent, selected: Boolean, onClick: () -> Unit) {
    val motion = LocalVLCMotion.current
    val label = ShellStrings.themeAccent(accent)
    val color = if (accent == VLCThemeAccent.Dynamic) MaterialTheme.colorScheme.primary else accent.swatchColor
    // Circle → squircle with a soft overshoot: the M3 Expressive "selected" shape morph.
    val corner by animateFloatAsState(
        targetValue = if (selected) 30f else 50f,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "swatch-corner",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "swatch-scale",
    )
    val shape = RoundedCornerShape(percent = corner.toInt())
    val checkTint = if (color.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        // Two tones: the accent itself and a lighter tint, hinting at the container colours it brings.
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f).background(color))
            Box(Modifier.fillMaxWidth().weight(1f).background(lerp(color, Color.White, 0.42f)))
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = selected || accent == VLCThemeAccent.Dynamic,
            enter = if (motion.reducedMotion) fadeIn(snap()) else scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = if (motion.reducedMotion) fadeOut(snap()) else scaleOut() + fadeOut(),
        ) {
            Icon(
                if (selected) MaterialSymbols.Filled.CheckCircle else MaterialSymbols.Filled.Palette,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
