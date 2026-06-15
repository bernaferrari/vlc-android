/*****************************************************************************
 * TvAudioNowPlayingText.kt
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TvAudioTrackInfoState(
    val title: String = "",
    val artist: String = ""
)

data class TvAudioProgressLabelsState(
    val elapsed: String = "",
    val length: String = ""
)

@Composable
fun TvAudioTrackInfo(
    state: TvAudioTrackInfoState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = state.title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = state.artist,
            color = Color.White.copy(alpha = 0.72F),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
fun TvAudioProgressLabels(
    state: TvAudioProgressLabelsState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvAudioProgressLabel(state.elapsed)
        TvAudioProgressLabel(state.length)
    }
}

@Composable
private fun TvAudioProgressLabel(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
