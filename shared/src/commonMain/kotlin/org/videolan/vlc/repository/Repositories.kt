package org.videolan.vlc.repository

import kotlinx.coroutines.flow.Flow
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.PlaylistInfo

/**
 * Repository contract for accessing the media library.
 *
 * Each platform provides its own implementation:
 *   - Android: wraps the JNI medialibrary
 *   - iOS: in-process catalog + Files/Photos intake (expanding toward full ML)
 */
interface MediaRepository {
    fun observeMedia(type: MediaType): Flow<List<MediaItem>>

    suspend fun getMedia(id: Long): MediaItem?

    suspend fun getMediaByIds(ids: List<Long>): List<MediaItem>

    fun search(query: String, type: MediaType = MediaType.ALL): Flow<List<MediaItem>>

    fun observeRecentlyPlayed(limit: Int = 50): Flow<List<MediaItem>>

    suspend fun count(type: MediaType): Int

    suspend fun markAsPlayed(id: Long)

    suspend fun incrementPlayCount(id: Long)

    /** Optional folder tree — empty if platform has no browser roots yet. */
    fun observeFolders(parentId: Long? = null): Flow<List<MediaFolder>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    /** Media contained in a folder path/id. */
    fun observeFolderMedia(folderId: Long): Flow<List<MediaItem>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
}

/**
 * Repository contract for managing playlists.
 */
interface PlaylistRepository {
    fun observePlaylists(): Flow<List<PlaylistInfo>>

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
    fun observeHistory(limit: Int = 100): Flow<List<HistoryEntry>>

    suspend fun addToHistory(item: MediaItem)

    suspend fun clearHistory()

    suspend fun removeHistoryEntry(id: Long)
}
