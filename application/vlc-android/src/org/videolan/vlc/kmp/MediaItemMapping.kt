package org.videolan.vlc.kmp

import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType

internal fun MediaWrapper.toMediaItem(): MediaItem {
    return MediaItem(
        id = this.id,
        title = this.title ?: "",
        uri = this.uri?.toString() ?: "",
        type = when (this.type) {
            MediaWrapper.TYPE_VIDEO -> MediaType.VIDEO
            MediaWrapper.TYPE_AUDIO -> MediaType.AUDIO
            MediaWrapper.TYPE_STREAM -> MediaType.STREAM
            MediaWrapper.TYPE_DIR -> MediaType.DIR
            MediaWrapper.TYPE_SUBTITLE -> MediaType.SUBTITLE
            MediaWrapper.TYPE_PLAYLIST -> MediaType.PLAYLIST
            MediaWrapper.TYPE_GROUP -> MediaType.GROUP
            else -> MediaType.ALL
        },
        duration = this.length,
        artist = this.artistName,
        album = this.albumName,
        albumArtist = this.albumArtistName,
        genre = this.genre,
        year = this.releaseYear,
        trackNumber = this.trackNumber,
        discNumber = this.discNumber,
        artworkUri = this.artworkURL,
        width = this.width,
        height = this.height,
        lastModified = this.lastModified,
        rating = this.rating?.toFloatOrNull() ?: 0f,
        playedCount = this.playCount.toInt(),
        lastPlayed = this.seen,
    )
}
