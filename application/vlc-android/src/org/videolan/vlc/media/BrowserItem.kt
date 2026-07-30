/*
 * Copyright © 2026 VLC authors and VideoLAN
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.videolan.vlc.media

import android.net.Uri
import android.os.Bundle

/** Internal representation of VLC's Media3 library tree before it is exposed to a controller. */
object MediaBrowserCompat {
    data class MediaItem(
        val description: MediaDescriptionCompat,
        val flags: Int,
    ) {
        val isBrowsable get() = flags and FLAG_BROWSABLE != 0
        val isPlayable get() = flags and FLAG_PLAYABLE != 0

        companion object {
            const val FLAG_BROWSABLE = 1
            const val FLAG_PLAYABLE = 2
        }
    }
}

/** Small immutable browser-node description kept independent from a platform session API. */
data class MediaDescriptionCompat(
    val mediaId: String?,
    val title: CharSequence?,
    val subtitle: CharSequence?,
    val iconUri: Uri?,
    val mediaUri: Uri?,
    val extras: Bundle?,
) {
    class Builder {
        private var mediaId: String? = null
        private var title: CharSequence? = null
        private var subtitle: CharSequence? = null
        private var iconUri: Uri? = null
        private var mediaUri: Uri? = null
        private var extras: Bundle? = null

        fun setMediaId(value: String?) = apply { mediaId = value }
        fun setTitle(value: CharSequence?) = apply { title = value }
        fun setSubtitle(value: CharSequence?) = apply { subtitle = value }
        fun setDescription(value: CharSequence?) = apply { }
        fun setIconUri(value: Uri?) = apply { iconUri = value }
        fun setMediaUri(value: Uri?) = apply { mediaUri = value }
        fun setExtras(value: Bundle?) = apply { extras = value?.let(::Bundle) }
        fun build() = MediaDescriptionCompat(mediaId, title, subtitle, iconUri, mediaUri, extras?.let(::Bundle))
    }
}

typealias BrowserItem = MediaBrowserCompat.MediaItem
