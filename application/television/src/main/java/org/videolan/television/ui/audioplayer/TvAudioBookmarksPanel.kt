/*****************************************************************************
 * TvAudioBookmarksPanel.kt
 *
 * Copyright © 2014-2015 VLC authors and VideoLAN
 * Author: Geoffrey Métais
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
package org.videolan.television.ui.audioplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.videolan.medialibrary.interfaces.media.Bookmark
import org.videolan.vlc.gui.view.BookmarkPanelItem
import org.videolan.vlc.gui.view.VLCBookmarksPanelContent

internal data class TvAudioBookmarksPanelState(
    val visible: Boolean = false,
    val bookmarks: List<BookmarkPanelItem> = emptyList(),
    val jumpDelayText: String = "",
    val rewindContentDescription: String = "",
    val forwardContentDescription: String = "",
    val progressTopPx: Float = -1F,
    val addBookmarkFocusToken: Int = 0
)

@Composable
internal fun TvAudioBookmarksPanel(
    state: TvAudioBookmarksPanelState,
    onCloseClick: () -> Unit,
    onAddBookmarkClick: () -> Unit,
    onPreviousBookmarkClick: () -> Unit,
    onNextBookmarkClick: () -> Unit,
    onRewindClick: () -> Unit,
    onForwardClick: () -> Unit,
    onRewindLongClick: () -> Unit,
    onForwardLongClick: () -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkRenameClick: (Bookmark) -> Unit,
    onBookmarkDeleteClick: (Bookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.visible) return

    VLCBookmarksPanelContent(
        bookmarks = state.bookmarks,
        jumpDelayText = state.jumpDelayText,
        rewindContentDescription = state.rewindContentDescription,
        forwardContentDescription = state.forwardContentDescription,
        progressTopPx = state.progressTopPx,
        addBookmarkFocusToken = state.addBookmarkFocusToken,
        onCloseClick = onCloseClick,
        onAddBookmarkClick = onAddBookmarkClick,
        onPreviousBookmarkClick = onPreviousBookmarkClick,
        onNextBookmarkClick = onNextBookmarkClick,
        onRewindClick = onRewindClick,
        onForwardClick = onForwardClick,
        onRewindLongClick = onRewindLongClick,
        onForwardLongClick = onForwardLongClick,
        onBookmarkClick = onBookmarkClick,
        onBookmarkRenameClick = onBookmarkRenameClick,
        onBookmarkDeleteClick = onBookmarkDeleteClick,
        modifier = modifier
    )
}
