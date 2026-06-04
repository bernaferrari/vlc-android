/*
 * ************************************************************************
 *  VideoHudIconButtonHost.kt
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Compose-backed icon button for video HUD controls that still need stable
 * IDs and View-level click/visibility hooks during the HUD migration.
 */
internal fun VLCComposeView.installVideoHudIconButtonHost(
    @DrawableRes icon: Int,
    contentDescription: String?,
    enabled: Boolean = isEnabled
) {
    val host = VideoHudIconButtonHost(this)
    setTag(R.id.video_hud_icon_button_host, host)
    host.setImageResource(icon)
    host.setContentDescription(contentDescription)
    host.setEnabled(enabled)
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.videoHudIconButtonHost(): VideoHudIconButtonHost =
    getTag(R.id.video_hud_icon_button_host) as? VideoHudIconButtonHost ?: error("Missing video HUD icon button host")

internal fun VLCComposeView.setVideoHudIconResource(@DrawableRes resourceId: Int) =
    videoHudIconButtonHost().setImageResource(resourceId)

internal fun VLCComposeView.setVideoHudIconContentDescription(contentDescription: CharSequence?) =
    videoHudIconButtonHost().setContentDescription(contentDescription)

internal fun VLCComposeView.setVideoHudIconEnabled(enabled: Boolean) =
    videoHudIconButtonHost().setEnabled(enabled)

internal class VideoHudIconButtonHost(private val view: VLCComposeView) {

    private var iconRes by mutableIntStateOf(0)
    private var contentDescriptionState by mutableStateOf(view.contentDescription?.toString())
    private var enabledState by mutableStateOf(view.isEnabled)

    fun setImageResource(@DrawableRes resourceId: Int) {
        iconRes = resourceId
    }

    fun setContentDescription(contentDescription: CharSequence?) {
        view.contentDescription = contentDescription
        contentDescriptionState = contentDescription?.toString()
    }

    fun setEnabled(enabled: Boolean) {
        view.isEnabled = enabled
        enabledState = enabled
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Content() {
        Box(
            modifier = Modifier
                .combinedClickable(
                    enabled = enabledState,
                    onClick = { view.performClick() },
                    onLongClick = { view.performLongClick() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != 0) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescriptionState
                )
            }
        }
    }
}
