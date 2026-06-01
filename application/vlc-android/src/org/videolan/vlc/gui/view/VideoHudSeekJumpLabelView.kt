/*
 * ************************************************************************
 *  VideoHudSeekJumpLabelView.kt
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose-backed replacement for player_hud.xml's tiny seek jump number labels.
 * VideoPlayerOverlayDelegate still updates the same IDs through the normal
 * ViewBinding text/visibility contract.
 */
class VideoHudSeekJumpLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    var text: CharSequence? = null
        set(value) {
            field = value
            label = value?.toString().orEmpty()
        }

    private var label by mutableStateOf("")

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    @Composable
    override fun WidgetContent() {
        Text(
            text = label,
            color = VLCThemeDefaults.colors.playerIconColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
