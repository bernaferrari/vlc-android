/*****************************************************************************
 * TvAudioQuickActions.kt
 *
 * Copyright © 2014-2015 VLC authors, VideoLAN and VideoLabs
 * Author: Geoffrey Métais
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package org.videolan.television.ui.audioplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.R as VlcR

internal data class TvAudioQuickActionsState(
    val speedText: String? = null,
    val sleepText: String? = null,
    val speedUsesGlobalRate: Boolean = false
) {
    val hasVisibleActions: Boolean
        get() = speedText != null || sleepText != null
}

@Composable
internal fun TvAudioQuickActions(
    state: TvAudioQuickActionsState,
    focusEnabled: Boolean,
    onSpeedClick: () -> Unit,
    onSpeedLongClick: () -> Unit,
    onSleepClick: () -> Unit,
    onSleepLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.hasVisibleActions) return

    val playbackSpeed = stringResource(VlcR.string.playback_speed)
    val sleepIn = stringResource(VlcR.string.sleep_in)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.speedText?.let { text ->
            TvAudioQuickActionChip(
                icon = if (state.speedUsesGlobalRate) VlcR.drawable.ic_speed_all else VlcR.drawable.ic_speed,
                text = text,
                contentDescription = "$playbackSpeed. $text",
                focusEnabled = focusEnabled,
                onClick = onSpeedClick,
                onLongClick = onSpeedLongClick
            )
        }
        state.sleepText?.let { text ->
            TvAudioQuickActionChip(
                icon = VlcR.drawable.ic_sleep,
                text = text,
                contentDescription = "$sleepIn $text",
                focusEnabled = focusEnabled,
                onClick = onSleepClick,
                onLongClick = onSleepLongClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvAudioQuickActionChip(
    icon: Int,
    text: String,
    contentDescription: String,
    focusEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .widthIn(max = 144.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (focused) FocusedChipBackground else NormalChipBackground)
            .focusProperties { canFocus = focusEnabled }
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = focusEnabled)
            .combinedClickable(
                enabled = focusEnabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics { this.contentDescription = contentDescription }
            .padding(start = 4.dp, top = 4.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = if (focused) 1F else 0.72F),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val NormalChipBackground = Color(0xBF000000)
private val FocusedChipBackground = Color(0x80BDBDBD)
