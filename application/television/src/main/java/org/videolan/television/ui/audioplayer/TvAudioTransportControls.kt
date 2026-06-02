/*****************************************************************************
 * TvAudioTransportControls.kt
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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.videolan.television.R
import org.videolan.vlc.compose.theme.VLCThemeDefaults

internal data class TvAudioTransportControlsState(
    @DrawableRes val shuffleIcon: Int = R.drawable.ic_shuffle_audio,
    val shuffleContentDescription: String = "",
    val shuffleActive: Boolean = false,
    @DrawableRes val playPauseIcon: Int = R.drawable.ic_play_player,
    val playPauseContentDescription: String = "",
    @DrawableRes val repeatIcon: Int = R.drawable.ic_repeat_audio,
    val repeatContentDescription: String = "",
    val repeatActive: Boolean = false
)

@Composable
internal fun TvAudioTransportControls(
    state: TvAudioTransportControlsState,
    onShuffleClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { playFocusRequester.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvAudioTransportButton(
                icon = state.shuffleIcon,
                contentDescription = state.shuffleContentDescription,
                active = state.shuffleActive,
                onClick = onShuffleClick
            )
            TvAudioTransportButton(
                icon = R.drawable.ic_player_previous,
                contentDescription = stringResource(R.string.previous),
                onClick = onPreviousClick
            )
            TvAudioTransportButton(
                icon = state.playPauseIcon,
                contentDescription = state.playPauseContentDescription,
                iconSize = 48,
                onClick = onPlayPauseClick,
                modifier = Modifier.focusRequester(playFocusRequester)
            )
            TvAudioTransportButton(
                icon = R.drawable.ic_player_next,
                contentDescription = stringResource(R.string.next),
                onClick = onNextClick
            )
            TvAudioTransportButton(
                icon = state.repeatIcon,
                contentDescription = state.repeatContentDescription,
                active = state.repeatActive,
                onClick = onRepeatClick
            )
        }
        TvAudioTransportButton(
            icon = R.drawable.ic_overflow_tv_audio,
            contentDescription = stringResource(R.string.more_actions),
            iconSize = 24,
            onClick = onMoreClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvAudioTransportButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    iconSize: Int = 32
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = VLCThemeDefaults.colors

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (focused) Color.White.copy(alpha = 0.20F) else Color.Transparent)
            .focusable(interactionSource = interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (active) colors.primary else colors.playerIconColor,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}
