package org.videolan.vlc.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist

/**
 * In-memory stub implementations used until the real medialibrary-backed
 * repositories are wired per platform. These live in commonMain so both
 * Android and iOS can use them as fallbacks during development.
 */

class StubPlaylistRepository : PlaylistRepository {
    private val playlists = mutableMapOf<Long, Playlist>()

    override fun observePlaylists(): Flow<List<Playlist>> = flow { emit(playlists.values.toList()) }

    override suspend fun getPlaylist(id: Long): Playlist? = playlists[id]

    override suspend fun createPlaylist(name: String): Playlist {
        val id = (playlists.keys.maxOrNull() ?: 0) + 1
        val playlist = Playlist(id, name)
        playlists[id] = playlist
        return playlist
    }

    override suspend fun addToPlaylist(playlistId: Long, items: List<MediaItem>) {}
    override suspend fun removeFromPlaylist(playlistId: Long, itemIds: List<Long>) {}
    override suspend fun deletePlaylist(id: Long) { playlists.remove(id) }
    override suspend fun renamePlaylist(id: Long, name: String) {
        playlists[id]?.let { playlists[id] = it.copy(name = name) }
    }
}

class StubHistoryRepository : HistoryRepository {
    override fun observeHistory(limit: Int): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override suspend fun addToHistory(item: MediaItem) {}
    override suspend fun clearHistory() {}
    override suspend fun removeHistoryEntry(id: Long) {}
}

class StubMediaRepository : MediaRepository {
    override fun observeMedia(type: org.videolan.vlc.model.MediaType): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override suspend fun getMedia(id: Long): MediaItem? = null
    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> = emptyList()
    override fun search(query: String, type: org.videolan.vlc.model.MediaType): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override suspend fun count(type: org.videolan.vlc.model.MediaType): Int = 0
    override suspend fun markAsPlayed(id: Long) {}
    override suspend fun incrementPlayCount(id: Long) {}
}
