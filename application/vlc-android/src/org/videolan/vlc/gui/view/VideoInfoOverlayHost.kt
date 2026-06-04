/*
 * ************************************************************************
 *  VideoInfoOverlayHost.kt
 * *************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
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
 * **************************************************************************
 */

package org.videolan.vlc.gui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose replacement for the former transient video info XML overlay. The
 * delegate owns timing and fade-out; this host only renders text state.
 */
internal fun VLCComposeView.installVideoInfoOverlayHost() {
    val host = VideoInfoOverlayHost()
    setTag(R.id.player_info_stub, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoInfoOverlayHost(): VideoInfoOverlayHost =
    getTag(R.id.player_info_stub) as? VideoInfoOverlayHost ?: error("Missing video info overlay host")

internal class VideoInfoOverlayHost {
    private var text by mutableStateOf("")
    private var subText by mutableStateOf("")

    fun updateInfo(text: String, subText: String) {
        this.text = text
        this.subText = subText
    }

    @Composable
    fun Content() {
        Column(
            modifier = Modifier.background(
                color = colorResource(R.color.playerbackground),
                shape = RoundedCornerShape(8.dp)
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                color = colorResource(R.color.white),
                fontSize = 36.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (subText.isNotBlank()) {
                Text(
                    text = subText,
                    color = colorResource(R.color.white),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }
        }
    }
}
