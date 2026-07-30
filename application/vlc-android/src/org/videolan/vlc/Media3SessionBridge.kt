/*
 * Copyright © 2026 VLC authors and VideoLAN
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.videolan.vlc

import android.os.Bundle
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import org.videolan.resources.VlcPlaybackState
import org.videolan.vlc.media.MediaDescriptionCompat
import java.util.WeakHashMap

/**
 * Keeps the old service-side publication call sites focused on VLC state while
 * Media3 owns the externally visible player/session state. Remove these thin
 * adapters as each publication site is simplified.
 */
private val activeSessions = WeakHashMap<MediaLibrarySession, Boolean>()

internal var MediaLibrarySession.isActive: Boolean
    get() = activeSessions[this] == true
    set(value) {
        activeSessions[this] = value
        (player as? VlcMedia3Player)?.syncState()
    }

internal fun MediaLibrarySession.setPlaybackState(state: VlcPlaybackState.PublishedState) {
    (player as? VlcMedia3Player)?.syncState()
}

internal fun MediaLibrarySession.setRepeatMode(mode: Int) {
    (player as? VlcMedia3Player)?.syncState()
}

internal fun MediaLibrarySession.setShuffleMode(mode: Int) {
    (player as? VlcMedia3Player)?.syncState()
}

internal fun MediaLibrarySession.setExtras(extras: Bundle) = Unit
internal fun MediaLibrarySession.setQueueTitle(title: CharSequence) = Unit
internal fun MediaLibrarySession.setQueue(queue: List<MediaSessionCompat.QueueItem>) {
    (player as? VlcMedia3Player)?.syncState()
}

/** Legacy queue data retained only inside VLC while Media3 publishes the real timeline. */
internal object MediaSessionCompat {
    data class QueueItem(val description: MediaDescriptionCompat, val queueId: Long)
}
