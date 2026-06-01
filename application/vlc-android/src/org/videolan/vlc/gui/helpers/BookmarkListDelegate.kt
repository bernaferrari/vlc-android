/*
 * ************************************************************************
 *  BookmarkListDelegate.kt
 * *************************************************************************
 * Copyright © 2021 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 *
 *
 */

package org.videolan.vlc.gui.helpers

import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.Bookmark
import org.videolan.tools.Settings
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.view.BookmarkMarkerContainerView
import org.videolan.vlc.gui.view.BookmarkPanelItem
import org.videolan.vlc.gui.view.BookmarksPanelView
import org.videolan.vlc.util.LocaleUtil
import org.videolan.vlc.viewmodels.BookmarkModel

class BookmarkListDelegate(
    val activity: ComponentActivity,
    val service: PlaybackService,
    private val bookmarkModel: BookmarkModel,
    val forVideo: Boolean
) : LifecycleObserver {

    lateinit var markerContainer: BookmarkMarkerContainerView
    private lateinit var rootView: BookmarksPanelView
    lateinit var visibilityListener: () -> Unit
    lateinit var seekListener: (Boolean, Boolean) -> Unit
    val visible: Boolean
        get() = ::rootView.isInitialized && rootView.visibility != View.GONE

    fun show() {
        if (!::rootView.isInitialized) {
            rootView = activity.findViewById<BookmarksPanelView>(R.id.bookmarks_background) ?: return
            setupRootView()
            observeBookmarks()
        }
        bookmarkModel.refresh()
        rootView.setVisible()
        markerContainer.setVisible()
        visibilityListener.invoke()
        updateJumpDelay()
    }

    private fun setupRootView() {
        service.lifecycle.addObserver(this)
        activity.lifecycle.addObserver(this)

        rootView.setOnCloseClickListener { hide() }
        rootView.setOnAddBookmarkClickListener {
            bookmarkModel.addBookmark(activity)
            rootView.announceBookmarkAdded(activity.getString(R.string.bookmark_added))
        }
        rootView.setOnPreviousBookmarkClickListener {
            val bookmark = if (LocaleUtil.isRtl()) bookmarkModel.findNext() else bookmarkModel.findPrevious()
            bookmark?.let { service.setTime(it.time) }
        }
        rootView.setOnNextBookmarkClickListener {
            val bookmark = if (LocaleUtil.isRtl()) bookmarkModel.findPrevious() else bookmarkModel.findNext()
            bookmark?.let { service.setTime(it.time) }
        }
        rootView.setOnRewindClickListener { seekListener.invoke(false, false) }
        rootView.setOnForwardClickListener { seekListener.invoke(true, false) }
        rootView.setOnRewindLongClickListener { seekListener.invoke(false, true) }
        rootView.setOnForwardLongClickListener { seekListener.invoke(true, true) }
        rootView.setOnBookmarkClickListener { service.setTime(it.time) }
        rootView.setOnBookmarkRenameClickListener { bookmark ->
            activity.showRenameComposeDialog(bookmark) { media, name ->
                renameBookmark(media as Bookmark, name)
            }
        }
        rootView.setOnBookmarkDeleteClickListener { bookmarkModel.delete(it) }
    }

    private fun observeBookmarks() {
        bookmarkModel.dataset.observe(activity) { bookmarkList ->
            rootView.setBookmarks(bookmarkList.toPanelItems())
            showBookmarks(markerContainer, service, bookmarkList)
        }
    }

    private fun updateJumpDelay() {
        val jumpDelay = if (forVideo) Settings.videoJumpDelay else Settings.audioJumpDelay
        rootView.setJumpDelay(
            jumpDelay = jumpDelay,
            rewindDescription = activity.getString(R.string.talkback_action_rewind, jumpDelay.toString()),
            forwardDescription = activity.getString(R.string.talkback_action_forward, jumpDelay.toString())
        )
    }

    fun hide() {
        if (::rootView.isInitialized) rootView.setGone()
        if (::markerContainer.isInitialized) markerContainer.setGone()
        if (::visibilityListener.isInitialized) visibilityListener.invoke()
    }

    fun setProgressHeight(y: Float) {
        if (::rootView.isInitialized) rootView.setProgressTop(y)
    }

    fun sendAddBookmarkAccessibilityEvent() {
        if (::rootView.isInitialized) rootView.sendAddBookmarkAccessibilityEvent()
    }

    fun requestFocus() {
        if (::rootView.isInitialized) rootView.requestFocus()
    }

    fun renameBookmark(media: Bookmark, name: String) {
        activity.lifecycleScope.launch {
            val bookmarks = bookmarkModel.rename(media, name)
            rootView.setBookmarks(bookmarks.toPanelItems())
            bookmarkModel.refresh()
        }
    }

    private fun List<Bookmark>.toPanelItems() = map { bookmark ->
        BookmarkPanelItem(
            bookmark = bookmark,
            id = bookmark.id,
            title = bookmark.title,
            timeText = Tools.millisToString(bookmark.time),
            timeContentDescription = TalkbackUtil.millisToString(activity, bookmark.time)
        )
    }

    companion object {
        fun showBookmarks(markerContainer: BookmarkMarkerContainerView, service: PlaybackService, bookmarkList: List<Bookmark>) {
            val mediaLength = service.currentMediaWrapper?.length
            if (mediaLength == null || mediaLength < 1) {
                markerContainer.clearMarkers()
                return
            }
            markerContainer.setMarkerFractions(bookmarkList.map { it.time.toFloat() / mediaLength.toFloat() })
        }
    }
}
