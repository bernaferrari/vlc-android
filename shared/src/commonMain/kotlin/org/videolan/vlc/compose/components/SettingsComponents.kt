package org.videolan.vlc.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import org.videolan.vlc.compose.theme.VLCLayout
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.LocalVLCMotion

/**
 * Shared Material 3 Expressive building blocks for VLC's settings-style sheets and pickers.
 *
 * These capture the visual vocabulary used across DisplaySettings, the renderer picker,
 * SavePlaylist, the equalizer and the subtitle downloader so each sheet reads as one family:
 * rounded tonal grouping cards, hairline inset dividers, tonal icon chips that morph to a filled
 * accent fill for the active/selected state, and a soft accent wash on selected rows.
 */

/** Default leading-icon chip diameter; text content lines up to [VLCSettingsDividerInset]. */
val VLCSettingsChipSize: Dp = VLCLayout.IconChip

/** Divider inset that clears a [VLCSettingsChipSize] chip plus standard row padding (20 + 40 + 20). */
val VLCSettingsDividerInset: Dp = 80.dp

/**
 * A leading icon rendered inside a rounded tonal chip. When [selected] it morphs to a filled accent
 * fill. The on-chip color is both driven into [LocalContentColor] (so a plain `Icon(painter, ...)`
 * picks it up automatically) and passed to [content], for callers whose icon slot takes an explicit
 * tint.
 */
@Composable
fun VLCIconChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: Dp = VLCSettingsChipSize,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = Color.Unspecified,
    content: @Composable (tint: Color) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val motion = LocalVLCMotion.current
    val targetContainer = (when {
        containerColor.isSpecified -> containerColor
        selected -> colors.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }).copy(alpha = if (enabled) 1f else 0.5f)
    val targetContent = when {
        !enabled -> colors.fontDisabled
        selected -> colors.onPrimary
        else -> colors.fontDefault
    }
    val container by animateColorAsState(
        targetContainer,
        animationSpec = tween(motion.durationShort),
        label = "vlcChipContainer",
    )
    val contentColor by animateColorAsState(
        targetContent,
        animationSpec = tween(motion.durationShort),
        label = "vlcChipContent",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content(contentColor)
        }
    }
}

/** Clips to a large rounded shape and fills it with the tonal grouping-card color. */
@Composable
fun Modifier.vlcSettingsCard(shape: Shape = MaterialTheme.shapes.large): Modifier =
    this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerLow)

/**
 * A soft tonal callout for a block of explanatory or cautionary dialog text. Shared by the
 * confirmation / warning sheets so their messages read as one family rather than loose
 * paragraphs floating on the dialog background.
 */
@Composable
fun VLCMessageCallout(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified
) {
    val resolvedContainer = if (containerColor.isSpecified) containerColor
        else MaterialTheme.colorScheme.surfaceContainer
    val resolvedContent = if (contentColor.isSpecified) contentColor
        else VLCThemeDefaults.colors.fontDefault
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = resolvedContainer,
        contentColor = resolvedContent
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

/** A soft accent wash applied to a selected row inside a grouping card. */
@Composable
fun Modifier.vlcSelectionWash(selected: Boolean): Modifier {
    val motion = LocalVLCMotion.current
    val wash by animateColorAsState(
        targetValue = if (selected) VLCThemeDefaults.colors.primary.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(motion.durationShort, easing = VLCMotion.Standard),
        label = "vlcSelectionWash",
    )
    return background(wash)
}

/** Hairline divider between rows in a grouping card, inset to clear the leading chip. */
@Composable
fun VLCSettingsCardDivider(startInset: Dp = VLCSettingsDividerInset) {
    HorizontalDivider(
        color = VLCThemeDefaults.colors.defaultDivider,
        modifier = Modifier.padding(start = startInset)
    )
}

/**
 * Convenience grouping card for the common static case: a list of row composables stacked in a
 * tonal rounded container with inset dividers between them. Empty lists render nothing.
 */
@Composable
fun VLCSettingsCard(
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    dividerInset: Dp = VLCSettingsDividerInset
) {
    if (rows.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        // Grouped settings are structural surfaces, not hero cards. Keep their
        // radius aligned with QuietGuard's 24dp outer group corners.
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            rows.forEachIndexed { index, row ->
                if (index > 0) VLCSettingsCardDivider(dividerInset)
                row()
            }
        }
    }
}

/**
 * Settings choices that belong to a short, scannable group. Each row is a real segment rather
 * than a single tall radio-card: the 2dp join and asymmetric outer corners match browser,
 * playlist, and About rows throughout the shared shell.
 */
@Composable
fun VLCSettingsSegmentedCard(
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        rows.forEachIndexed { index, row ->
            val position = when {
                rows.size == 1 -> VLCListItemPosition.Single
                index == 0 -> VLCListItemPosition.First
                index == rows.lastIndex -> VLCListItemPosition.Last
                else -> VLCListItemPosition.Middle
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = position.segmentShape(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                row()
            }
        }
    }
}

/**
 * One full-width settings toggle.  The whole row is the target rather than only the small switch,
 * which gives touch, keyboard and accessibility users the same predictable action.
 */
@Composable
fun VLCSettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VLCLayout.RowHeight)
            // Match QuietGuard's quiet state cue: an enabled choice is visible at a glance
            // without turning the entire settings group into a high-contrast dashboard.
            .vlcSelectionWash(checked)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
            )
            summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = null)
    }
}

/** A dense but calm radio choice row for display and playback configuration. */
@Composable
fun VLCSettingsChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VLCLayout.RowHeight)
            .vlcSelectionWash(selected)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
            )
            summary?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RadioButton(selected = selected, enabled = enabled, onClick = null)
    }
}
