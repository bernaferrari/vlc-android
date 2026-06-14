/*****************************************************************************
 * TvAudioTimeline.kt
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCAudioTimelineSlider

internal data class TvAudioTimelineState(
    val progress: Int = 0,
    val max: Int = 100,
    val contentDescription: String = ""
)

@Composable
internal fun TvAudioTimeline(
    state: TvAudioTimelineState,
    onUserDragStarted: () -> Unit,
    onUserProgressChange: (Int) -> Unit,
    onUserDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        VLCAudioTimelineSlider(
            progress = state.progress,
            max = state.max,
            contentDescription = state.contentDescription,
            onUserDragStarted = onUserDragStarted,
            onUserProgressChange = onUserProgressChange,
            onUserDragStopped = onUserDragStopped,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
