package org.videolan.vlc.kmp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.repository.MediaRepository

/**
 * Android implementation of [MediaRepository] backed by the VLC medialibrary JNI.
 *
 * Bridges the shared KMP domain interface to the existing Android medialibrary.
 * Created lazily once the medialibrary is initialized.
 */
class AndroidMediaRepository(
    private val medialibrary: Medialibrary
) : MediaRepository {

    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> = flow {
        // The medialibrary JNI returns MediaWrapper[] for various queries.
        // Full wiring depends on Medialibrary's audio/video query methods.
        // TODO: Wire to medialibrary.getAudio() / getVideo() when available
        emit(emptyList())
    }

    override suspend fun getMedia(id: Long): MediaItem? {
        return medialibrary.getMedia(id)?.let { mw ->
            (mw as? MediaWrapper)?.toMediaItem()
        }
    }

    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> {
        return ids.mapNotNull { id ->
            medialibrary.getMedia(id)?.let { mw ->
                (mw as? MediaWrapper)?.toMediaItem()
            }
        }
    }

    override fun search(query: String, type: MediaType): Flow<List<MediaItem>> = flow {
        // TODO: Wire to medialibrary.search() which returns a search result object
        emit(emptyList())
    }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> = flow {
        // TODO: Wire to medialibrary.lastMediaPlayed()
        emit(emptyList())
    }

    override suspend fun count(type: MediaType): Int {
        // TODO: Wire to medialibrary counters
        return 0
    }

    override suspend fun markAsPlayed(id: Long) {
        // TODO: Wire when medialibrary play-count API is confirmed
    }

    override suspend fun incrementPlayCount(id: Long) {
        // TODO: Wire when medialibrary play-count API is confirmed
    }

    /**
     * Convert an Android MediaWrapper to the common MediaItem.
     */
    private fun MediaWrapper.toMediaItem(): MediaItem {
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
            artworkUri = this.artworkURL,
            width = this.width,
            height = this.height,
            rating = this.rating?.toFloatOrNull() ?: 0f,
            playedCount = this.playCount.toInt(),
        )
    }
}
