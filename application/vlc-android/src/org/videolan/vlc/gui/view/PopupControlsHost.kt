/*****************************************************************************
 * PopupControlsHost.kt
 *
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
 */

package org.videolan.vlc.gui.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.interop.VLCComposeView

internal interface PopupIconHost {
    fun setIcon(@DrawableRes icon: Int)
}

internal fun VLCComposeView.installPopupIconHost(@DrawableRes icon: Int) {
    val host = PopupIconController(icon)
    setTag(id, host)
    setContent {
        Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    painter = painterResource(host.icon.intValue),
                    contentDescription = null,
                    tint = Color.Unspecified
            )
        }
    }
}

internal fun VLCComposeView.popupIconHost(): PopupIconHost =
        getTag(id) as? PopupIconHost ?: error("Missing popup icon host")

private class PopupIconController(@DrawableRes icon: Int) : PopupIconHost {
    val icon = mutableIntStateOf(icon)

    override fun setIcon(icon: Int) {
        this.icon.intValue = icon
    }
}
