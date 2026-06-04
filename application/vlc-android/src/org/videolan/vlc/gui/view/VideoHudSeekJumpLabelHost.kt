/*
 * ************************************************************************
 *  VideoHudSeekJumpLabelHost.kt
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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose-backed replacement for the video HUD's tiny seek jump number labels.
 * VideoPlayerOverlayDelegate still updates the same IDs through the normal
 * HUD binding while this host owns the label rendering.
 */
internal fun VLCComposeView.installVideoHudSeekJumpLabelHost() {
    val host = VideoHudSeekJumpLabelHost()
    setTag(R.id.video_hud_seek_jump_label_host, host)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoHudSeekJumpLabelHost(): VideoHudSeekJumpLabelHost =
    getTag(R.id.video_hud_seek_jump_label_host) as? VideoHudSeekJumpLabelHost ?: error("Missing video HUD seek jump label host")

internal class VideoHudSeekJumpLabelHost {
    private var label by mutableStateOf("")

    fun setText(text: CharSequence?) {
        label = text?.toString().orEmpty()
    }

    @Composable
    fun Content() {
        Text(
            text = label,
            color = VLCThemeDefaults.colors.playerIconColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
