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
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.view.bookmarkMarkerContainerHost
import org.videolan.vlc.gui.view.BookmarkPanelItem
import org.videolan.vlc.gui.view.BookmarkPanelHost
import org.videolan.vlc.gui.view.bookmarksPanelHost
import org.videolan.vlc.util.LocaleUtil
import org.videolan.vlc.viewmodels.BookmarkModel

interface BookmarkMarkerHost {
    fun show()
    fun hide()
    fun setMarkerFractions(fractions: List<Float>)
    fun clearMarkers()
}

class BookmarkListDelegate(
    val activity: ComponentActivity,
    val service: PlaybackService,
    private val bookmarkModel: BookmarkModel,
    val forVideo: Boolean
) : LifecycleObserver {

    private var markerHost: BookmarkMarkerHost? = null
    private var panelHost: BookmarkPanelHost? = null
    private var panelHostConfigured = false
    var markerContainer: VLCComposeView
        get() = (markerHost as? BookmarkMarkerContainerHost)?.view ?: error("Bookmark marker container is not a view host")
        set(value) {
            markerHost = BookmarkMarkerContainerHost(value)
        }
    lateinit var visibilityListener: () -> Unit
    lateinit var seekListener: (Boolean, Boolean) -> Unit
    val visible: Boolean
        get() = panelHost?.visible == true

    fun show() {
        val panel = panelHost ?: activity.findViewById<VLCComposeView>(R.id.bookmarks_background)?.bookmarksPanelHost()?.also {
            panelHost = it
        } ?: return
        if (!panelHostConfigured) {
            setupRootView()
            observeBookmarks()
            panelHostConfigured = true
        }
        bookmarkModel.refresh()
        panel.show()
        markerHost?.show()
        visibilityListener.invoke()
        updateJumpDelay()
    }

    private fun setupRootView() {
        service.lifecycle.addObserver(this)
        activity.lifecycle.addObserver(this)
        val panel = panelHost ?: return

        panel.setOnCloseClickListener { hide() }
        panel.setOnAddBookmarkClickListener {
            bookmarkModel.addBookmark(activity)
            panel.announceBookmarkAdded(activity.getString(R.string.bookmark_added))
        }
        panel.setOnPreviousBookmarkClickListener {
            val bookmark = if (LocaleUtil.isRtl()) bookmarkModel.findNext() else bookmarkModel.findPrevious()
            bookmark?.let { service.setTime(it.time) }
        }
        panel.setOnNextBookmarkClickListener {
            val bookmark = if (LocaleUtil.isRtl()) bookmarkModel.findPrevious() else bookmarkModel.findNext()
            bookmark?.let { service.setTime(it.time) }
        }
        panel.setOnRewindClickListener { seekListener.invoke(false, false) }
        panel.setOnForwardClickListener { seekListener.invoke(true, false) }
        panel.setOnRewindLongClickListener { seekListener.invoke(false, true) }
        panel.setOnForwardLongClickListener { seekListener.invoke(true, true) }
        panel.setOnBookmarkClickListener { service.setTime(it.time) }
        panel.setOnBookmarkRenameClickListener { bookmark ->
            activity.showRenameComposeDialog(bookmark) { media, name ->
                renameBookmark(media as Bookmark, name)
            }
        }
        panel.setOnBookmarkDeleteClickListener { bookmarkModel.delete(it) }
    }

    private fun observeBookmarks() {
        bookmarkModel.dataset.observe(activity) { bookmarkList ->
            panelHost?.setBookmarks(bookmarkList.toPanelItems())
            markerHost?.let { showBookmarks(it, service, bookmarkList) }
        }
    }

    private fun updateJumpDelay() {
        val jumpDelay = if (forVideo) Settings.videoJumpDelay else Settings.audioJumpDelay
        panelHost?.setJumpDelay(
            jumpDelay = jumpDelay,
            rewindDescription = activity.getString(R.string.talkback_action_rewind, jumpDelay.toString()),
            forwardDescription = activity.getString(R.string.talkback_action_forward, jumpDelay.toString())
        )
    }

    fun hide() {
        panelHost?.hide()
        markerHost?.hide()
        if (::visibilityListener.isInitialized) visibilityListener.invoke()
    }

    fun setPanelHost(host: BookmarkPanelHost) {
        panelHost = host
    }

    fun setMarkerHost(host: BookmarkMarkerHost) {
        markerHost = host
    }

    fun setProgressHeight(y: Float) {
        panelHost?.setProgressTop(y)
    }

    fun sendAddBookmarkAccessibilityEvent() {
        panelHost?.sendAddBookmarkAccessibilityEvent()
    }

    fun requestFocus() {
        panelHost?.requestPanelFocus()
    }

    fun renameBookmark(media: Bookmark, name: String) {
        activity.lifecycleScope.launch {
            val bookmarks = bookmarkModel.rename(media, name)
            panelHost?.setBookmarks(bookmarks.toPanelItems())
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
        fun showBookmarks(markerContainer: VLCComposeView, service: PlaybackService, bookmarkList: List<Bookmark>) {
            showBookmarks(BookmarkMarkerContainerHost(markerContainer), service, bookmarkList)
        }

        fun showBookmarks(markerHost: BookmarkMarkerHost, service: PlaybackService, bookmarkList: List<Bookmark>) {
            val mediaLength = service.currentMediaWrapper?.length
            if (mediaLength == null || mediaLength < 1) {
                markerHost.clearMarkers()
                return
            }
            markerHost.setMarkerFractions(bookmarkList.map { it.time.toFloat() / mediaLength.toFloat() })
        }
    }
}

private class BookmarkMarkerContainerHost(val view: VLCComposeView) : BookmarkMarkerHost {
    override fun show() {
        view.setVisible()
    }

    override fun hide() {
        view.setGone()
    }

    override fun setMarkerFractions(fractions: List<Float>) {
        view.bookmarkMarkerContainerHost().setMarkerFractions(fractions)
    }

    override fun clearMarkers() {
        view.bookmarkMarkerContainerHost().clearMarkers()
    }
}
