/*
 * ************************************************************************
 *  VideoHudIconButtonView.kt
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

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

/**
 * Compose-backed icon button for video HUD controls that still need stable XML
 * IDs and View-level click/visibility hooks during the player_hud migration.
 */
class VideoHudIconButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var iconRes by mutableIntStateOf(readIconResource(attrs))
    private var enabledState by mutableStateOf(isEnabled)

    fun setImageResource(@DrawableRes resourceId: Int) {
        iconRes = resourceId
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        enabledState = enabled
    }

    @Composable
    override fun WidgetContent() {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(enabled = enabledState) { performClick() },
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != 0) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription?.toString()
                )
            }
        }
    }

    private fun readIconResource(attrs: AttributeSet?): Int {
        val typedArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.src))
        return try {
            typedArray.getResourceId(0, 0)
        } finally {
            typedArray.recycle()
        }
    }
}
