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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.colorResource
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.components.VLCVideoInfoOverlay
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
        VLCVideoInfoOverlay(
            text = text,
            subText = subText,
            bgColor = colorResource(R.color.playerbackground),
            textColor = colorResource(R.color.white)
        )
    }
}
