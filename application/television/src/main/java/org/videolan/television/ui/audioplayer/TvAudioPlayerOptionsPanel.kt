/*****************************************************************************
 * TvAudioPlayerOptionsPanel.kt
 *
 * Copyright © 2014-2015 VLC authors and VideoLAN
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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.videolan.vlc.gui.helpers.PlayerOption
import org.videolan.vlc.gui.view.VLCPlayerOptionsPanelContent

internal data class TvAudioPlayerOptionsPanelState(
    val visible: Boolean = false,
    val options: List<PlayerOption> = emptyList(),
    val focusRequestToken: Int = 0
)

@Composable
internal fun TvAudioPlayerOptionsPanel(
    state: TvAudioPlayerOptionsPanelState,
    onDismissClick: () -> Unit,
    onOptionClick: (PlayerOption) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.visible) return

    VLCPlayerOptionsPanelContent(
        options = state.options,
        focusRequestToken = state.focusRequestToken,
        onDismissClick = onDismissClick,
        onOptionClick = onOptionClick,
        modifier = modifier
    )
}
