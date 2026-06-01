/*
 * ************************************************************************
 *  AudioPlayerHostView.kt
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

package org.videolan.vlc.gui

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import org.videolan.vlc.R
import org.videolan.vlc.gui.view.AudioPlayerTipsHostView

internal fun Context.createAudioPlayerHostView() = FrameLayout(this).apply {
    id = R.id.audio_player_stub
    visibility = View.GONE
    addView(
        FrameLayout(context).apply {
            id = R.id.audio_player
        },
        FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    )
}

internal fun Context.createAudioPlayerTipsHostView() = AudioPlayerTipsHostView(this)
