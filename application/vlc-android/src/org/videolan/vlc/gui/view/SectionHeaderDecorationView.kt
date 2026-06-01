/*****************************************************************************
 * SectionHeaderDecorationView.kt
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

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.compose.components.VLCSectionHeader
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

class SectionHeaderDecorationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var state by mutableStateOf(SectionHeaderDecorationState())

    fun bind(title: String, isTv: Boolean) {
        state = SectionHeaderDecorationState(title = title, isTv = isTv)
    }

    fun attachToOverlayHost(parent: ViewGroup, height: Int) {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        if (this.parent == null) parent.overlay.add(this)
        translationY = -(parent.height + height).toFloat()
    }

    @Composable
    override fun WidgetContent() {
        VLCSectionHeader(text = state.title, isTv = state.isTv)
    }
}

private data class SectionHeaderDecorationState(
    val title: String = "",
    val isTv: Boolean = false
)
