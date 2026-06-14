package org.videolan.vlc.repository

import kotlinx.coroutines.flow.Flow
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist

/**
 * Repository contract for accessing the media library.
 *
 * Each platform provides its own implementation:
 *   - Android: wraps the JNI medialibrary
 *   - iOS: will wrap the VLCKit medialibrary or a custom implementation
 *
 * All methods return [Flow] or suspend — never LiveData — so the contract
 * is platform-neutral.
 */
interface MediaRepository {
    /** Stream of all media items of the given type. */
    fun observeMedia(type: MediaType): Flow<List<MediaItem>>

    /** Get a single media item by ID. */
    suspend fun getMedia(id: Long): MediaItem?

    /** Get media items by their IDs. */
    suspend fun getMediaByIds(ids: List<Long>): List<MediaItem>

    /** Search media by title/artist. */
    fun search(query: String, type: MediaType = MediaType.ALL): Flow<List<MediaItem>>

    /** Recently played media. */
    fun observeRecentlyPlayed(limit: Int = 50): Flow<List<MediaItem>>

    /** Total count of media items by type. */
    suspend fun count(type: MediaType): Int

    /** Mark a media item as played. */
    suspend fun markAsPlayed(id: Long)

    /** Increment the play count and update last-played timestamp. */
    suspend fun incrementPlayCount(id: Long)
}

/**
 * Repository contract for managing playlists.
 */
interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>

    suspend fun getPlaylist(id: Long): Playlist?

    suspend fun createPlaylist(name: String): Playlist

    suspend fun addToPlaylist(playlistId: Long, items: List<MediaItem>)

    suspend fun removeFromPlaylist(playlistId: Long, itemIds: List<Long>)

    suspend fun deletePlaylist(id: Long)

    suspend fun renamePlaylist(id: Long, name: String)
}

/**
 * Repository contract for media history.
 */
interface HistoryRepository {
    fun observeHistory(limit: Int = 100): Flow<List<MediaItem>>

    suspend fun addToHistory(item: MediaItem)

    suspend fun clearHistory()

    suspend fun removeHistoryEntry(id: Long)
}
