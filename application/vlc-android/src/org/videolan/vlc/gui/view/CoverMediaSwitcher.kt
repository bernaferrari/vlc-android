/*****************************************************************************
 * CoverMediaSwitcher.java
 *
 * Copyright © 2011-2014 VLC authors and VideoLAN
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
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import org.videolan.tools.KEY_AUDIO_SHOW_CHAPTER_BUTTONS
import org.videolan.tools.Settings
import org.videolan.vlc.util.LocaleUtil
import org.videolan.vlc.util.TextUtils

class CoverMediaSwitcher(context: Context, attrs: AttributeSet) : AudioMediaSwitcher(context, attrs) {

    override fun addMediaView(
        inflater: LayoutInflater,
        title: String?,
        artist: String?,
        album: String?,
        cover: Bitmap?,
        trackInfo: String?,
        hasChapters: Boolean
    ) {
        addView(CoverMediaSwitcherItemView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            bind(
                title = title,
                subtitle = TextUtils.separatedString(artist, album),
                trackInfo = trackInfo,
                cover = cover,
                showChapterButtons = hasChapters && Settings.getInstance(context).getBoolean(KEY_AUDIO_SHOW_CHAPTER_BUTTONS, true),
                showTrackInfo = Settings.showAudioTrackInfo,
                onTextClick = ::onTextClicked,
                onPreviousChapterClick = { onChapterSwitching(LocaleUtil.isRtl()) },
                onNextChapterClick = { onChapterSwitching(!LocaleUtil.isRtl()) }
            )
        })
    }
}
