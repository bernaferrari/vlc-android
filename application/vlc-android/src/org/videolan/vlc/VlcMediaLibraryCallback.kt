/*
 * Copyright © 2026 VLC authors and VideoLAN
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.videolan.vlc

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.videolan.vlc.media.BrowserItem
import org.videolan.vlc.media.MediaSessionBrowser
import org.videolan.vlc.util.Permissions

/** Bridges VLC's established media-library hierarchy to Media3 controllers and Android Auto. */
internal class VlcMediaLibraryCallback(
    private val service: PlaybackService,
) : MediaLibraryService.MediaLibrarySession.Callback {

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        if (!Permissions.canReadStorage(service)) {
            return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_PERMISSION_DENIED, params))
        }
        val rootId = if (params?.isSuggested == true) MediaSessionBrowser.ID_SUGGESTED else MediaSessionBrowser.ID_ROOT
        return Futures.immediateFuture(LibraryResult.ofItem(root(rootId, params), params))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
        val items = MediaSessionBrowser.browse(service, parentId, service.isShuffling, params?.extras)
            .drop(page * pageSize)
            .take(pageSize)
            .map { it.toMedia3() }
        return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> = Futures.immediateFuture(LibraryResult.ofVoid(params))

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
        val items = MediaSessionBrowser.search(service, query, params?.extras)
            .drop(page * pageSize)
            .take(pageSize)
            .map { it.toMedia3() }
        return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
    }

    private fun root(id: String, params: MediaLibraryService.LibraryParams?): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("VLC")
                .setIsBrowsable(true)
                .setExtras(params?.extras?.let(::Bundle))
                .build(),
        )
        .build()

    private fun BrowserItem.toMedia3(): MediaItem = MediaItem.Builder()
        .setMediaId(description.mediaId ?: "")
        .setUri(description.mediaUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(description.title)
                .setSubtitle(description.subtitle)
                .setArtworkUri(description.iconUri)
                .setIsBrowsable(isBrowsable)
                .setIsPlayable(isPlayable)
                .setExtras(description.extras?.let(::Bundle))
                .build(),
        )
        .build()
}
