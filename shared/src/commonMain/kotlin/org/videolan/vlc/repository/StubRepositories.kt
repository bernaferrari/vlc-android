package org.videolan.vlc.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.PlaylistInfo

class StubPlaylistRepository : PlaylistRepository {
    private val playlists = MutableStateFlow<Map<Long, Playlist>>(emptyMap())

    override fun observePlaylists(): Flow<List<PlaylistInfo>> =
        playlists.map { map ->
            map.values.map {
                PlaylistInfo(it.id, it.name, it.items.size, duration = it.items.sumOf { m -> m.duration })
            }
        }

    override suspend fun getPlaylist(id: Long): Playlist? = playlists.value[id]

    override suspend fun createPlaylist(name: String): Playlist {
        val id = (playlists.value.keys.maxOrNull() ?: 0) + 1
        val playlist = Playlist(id, name)
        playlists.value = playlists.value + (id to playlist)
        return playlist
    }

    override suspend fun addToPlaylist(playlistId: Long, items: List<MediaItem>) {
        val current = playlists.value[playlistId] ?: return
        playlists.value = playlists.value + (playlistId to current.copy(items = current.items + items))
    }

    override suspend fun removeFromPlaylist(playlistId: Long, itemIds: List<Long>) {
        val current = playlists.value[playlistId] ?: return
        val set = itemIds.toSet()
        playlists.value = playlists.value + (playlistId to current.copy(items = current.items.filterNot { it.id in set }))
    }

    override suspend fun deletePlaylist(id: Long) {
        playlists.value = playlists.value - id
    }

    override suspend fun renamePlaylist(id: Long, name: String) {
        val current = playlists.value[id] ?: return
        playlists.value = playlists.value + (id to current.copy(name = name))
    }
}

class StubHistoryRepository : HistoryRepository {
    private val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> =
        entries.map { it.take(limit) }

    override suspend fun addToHistory(item: MediaItem) {
        val entry = HistoryEntry(item, playedAt = item.lastPlayed.takeIf { it > 0 } ?: 0L)
        entries.value = listOf(entry) + entries.value.filterNot { it.item.id == item.id }
    }

    override suspend fun clearHistory() {
        entries.value = emptyList()
    }

    override suspend fun removeHistoryEntry(id: Long) {
        entries.value = entries.value.filterNot { it.item.id == id }
    }
}

class StubMediaRepository : MediaRepository {
    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override suspend fun getMedia(id: Long): MediaItem? = null
    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> = emptyList()
    override fun search(query: String, type: MediaType): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override suspend fun count(type: MediaType): Int = 0
    override suspend fun markAsPlayed(id: Long) {}
    override suspend fun incrementPlayCount(id: Long) {}
    override fun observeFolders(parentId: Long?): Flow<List<MediaFolder>> = flow { emit(emptyList()) }
    override fun observeFolderMedia(folderId: Long): Flow<List<MediaItem>> = flow { emit(emptyList()) }
}
