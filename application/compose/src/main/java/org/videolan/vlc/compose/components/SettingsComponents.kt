package org.videolan.vlc.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Shared Material 3 Expressive building blocks for VLC's settings-style sheets and pickers.
 *
 * These capture the visual vocabulary used across DisplaySettings, the renderer picker,
 * SavePlaylist, the equalizer and the subtitle downloader so each sheet reads as one family:
 * rounded tonal grouping cards, hairline inset dividers, tonal icon chips that morph to a filled
 * accent fill for the active/selected state, and a soft accent wash on selected rows.
 */

/** Default leading-icon chip diameter; text content lines up to [VLCSettingsDividerInset]. */
val VLCSettingsChipSize: Dp = 40.dp

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
    val container by animateColorAsState(targetContainer, label = "vlcChipContainer")
    val contentColor by animateColorAsState(targetContent, label = "vlcChipContent")
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

/** A soft accent wash applied to a selected row inside a grouping card. */
@Composable
fun Modifier.vlcSelectionWash(selected: Boolean): Modifier =
    if (selected) this.background(VLCThemeDefaults.colors.primary.copy(alpha = 0.08f)) else this

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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .vlcSettingsCard()
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) VLCSettingsCardDivider(dividerInset)
            row()
        }
    }
}
