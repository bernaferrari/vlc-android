/*****************************************************************************
 * VideoPlaylistSearchHost.kt
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

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioPlaylistSearchField
import org.videolan.vlc.compose.interop.VLCComposeView

internal interface VideoPlaylistSearchHost {
    fun setOnQueryChangeListener(listener: (String) -> Unit)
    fun setQuery(query: String)
    fun requestSearchFocus()
}

internal fun VLCComposeView.installVideoPlaylistSearchHost(hint: String) {
    val host = VideoPlaylistSearchController(hint)
    setTag(R.id.playlist_search_text, host)
    setContent {
        VLCAudioPlaylistSearchField(
                query = host.query.value,
                hint = host.hint,
                focusRequest = host.focusRequest.intValue,
                onQueryChange = host::onQueryChanged
        )
    }
}

internal fun VLCComposeView.videoPlaylistSearchHost(): VideoPlaylistSearchHost =
        getTag(R.id.playlist_search_text) as? VideoPlaylistSearchHost ?: error("Missing video playlist search host")

private class VideoPlaylistSearchController(val hint: String) : VideoPlaylistSearchHost {
    val query = mutableStateOf("")
    val focusRequest = mutableIntStateOf(0)
    private var onQueryChange: (String) -> Unit = {}

    override fun setOnQueryChangeListener(listener: (String) -> Unit) {
        onQueryChange = listener
    }

    override fun setQuery(query: String) {
        if (this.query.value == query) return
        this.query.value = query
        onQueryChange(query)
    }

    override fun requestSearchFocus() {
        focusRequest.intValue += 1
    }

    fun onQueryChanged(query: String) {
        this.query.value = query
        onQueryChange(query)
    }
}
