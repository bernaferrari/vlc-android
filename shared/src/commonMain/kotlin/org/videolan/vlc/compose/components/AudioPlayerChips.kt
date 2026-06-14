package org.videolan.vlc.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose equivalent of the playback quick-action chips from:
 *   - application/vlc-android/res/layout/audio_player.xml
 *   - application/vlc-android/res/layout-land/audio_player.xml
 *
 * Replaces the legacy Material ChipGroup for playback speed and sleep timer
 * without touching the hard player gesture/switcher surface. The host supplies
 * icon slots so app resources can stay in :application:vlc-android while this
 * reusable leaf remains resource-agnostic inside :application:compose.
 */
data class VLCAudioPlayerChipsState(
    val speedText: String? = null,
    val sleepText: String? = null,
    val speedUsesGlobalRate: Boolean = false
) {
    val hasVisibleChips: Boolean
        get() = speedText != null || sleepText != null
}

@Composable
fun VLCAudioPlayerChips(
    state: VLCAudioPlayerChipsState,
    modifier: Modifier = Modifier,
    speedIconContent: @Composable (() -> Unit)? = null,
    sleepIconContent: @Composable (() -> Unit)? = null,
    onSpeedClick: () -> Unit = {},
    onSpeedLongClick: () -> Unit = {},
    onSleepClick: () -> Unit = {},
    onSleepLongClick: () -> Unit = {}
) {
    VLCTheme {
        if (state.hasVisibleChips) {
            Row(
                modifier = modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.speedText?.let { text ->
                    AudioPlayerChip(
                        text = text,
                        iconContent = speedIconContent,
                        contentDescription = text,
                        onClick = onSpeedClick,
                        onLongClick = onSpeedLongClick
                    )
                }
                state.sleepText?.let { text ->
                    AudioPlayerChip(
                        text = text,
                        iconContent = sleepIconContent,
                        contentDescription = text,
                        onClick = onSleepClick,
                        onLongClick = onSleepLongClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioPlayerChip(
    text: String,
    iconContent: @Composable (() -> Unit)?,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .widthIn(max = 144.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.audioChipsColor)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics { this.contentDescription = contentDescription }
            .padding(
                start = if (iconContent == null) 12.dp else 8.dp,
                top = 6.dp,
                end = 12.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        iconContent?.let {
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                it()
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = colors.audioChipsTextColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PreviewChipIcon(label: String) {
    Text(
        text = label,
        color = VLCThemeDefaults.colors.audioChipsTextColor,
        style = MaterialTheme.typography.labelMedium
    )
}
