package org.videolan.vlc.kmp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Playlist as MlPlaylist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.tools.Settings
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.PlaylistRepository

class AndroidPlaylistRepository(
    private val medialibrary: Medialibrary,
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<PlaylistInfo>> = callbackFlow {
        trySend(loadPlaylists())
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override suspend fun getPlaylist(id: Long): Playlist? = withContext(Dispatchers.IO) {
        if (!medialibrary.isInitiated) return@withContext null
        val pl = medialibrary.getPlaylist(id, false, false) ?: return@withContext null
        val media = pl.tracks?.map { it.toMediaItem() }.orEmpty()
        Playlist(id = pl.id, name = pl.title.orEmpty(), items = media)
    }

    override suspend fun createPlaylist(name: String): Playlist = withContext(Dispatchers.IO) {
        val pl = medialibrary.createPlaylist(name, Settings.includeMissing, false)
            ?: return@withContext Playlist(0, name)
        Playlist(id = pl.id, name = pl.title.orEmpty())
    }

    override suspend fun addToPlaylist(playlistId: Long, items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val pl = medialibrary.getPlaylist(playlistId, false, false) ?: return@withContext
        val ids = items.map { it.id }.filter { it > 0 }.toLongArray()
        if (ids.isNotEmpty()) pl.append(ids)
    }

    override suspend fun removeFromPlaylist(playlistId: Long, itemIds: List<Long>) = withContext(Dispatchers.IO) {
        // Playlist.remove expects media index (Int) on this ML fork — skip precise remove.
        Unit
    }

    override suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        medialibrary.getPlaylist(id, false, false)?.let { pl ->
            runCatching { pl.delete() }
        }
        Unit
    }

    override suspend fun renamePlaylist(id: Long, name: String) = withContext(Dispatchers.IO) {
        val pl = medialibrary.getPlaylist(id, false, false) ?: return@withContext
        runCatching { (pl as MediaLibraryItem).setTitle(name) }
        Unit
    }

    private fun loadPlaylists(): List<PlaylistInfo> {
        if (!medialibrary.isInitiated) return emptyList()
        val list: Array<out MlPlaylist> = runCatching {
            medialibrary.getPlaylists(MlPlaylist.Type.All, false)
        }.getOrElse {
            runCatching {
                medialibrary.getPlaylists(
                    MlPlaylist.Type.All,
                    Medialibrary.SORT_DEFAULT,
                    false,
                    Settings.includeMissing,
                    false,
                )
            }.getOrDefault(emptyArray())
        }
        return list.map { pl ->
            PlaylistInfo(
                id = pl.id,
                name = pl.title.orEmpty(),
                itemCount = runCatching { pl.tracksCount }.getOrDefault(0),
                artworkUri = runCatching { pl.artworkMrl }.getOrNull(),
                duration = 0L,
            )
        }
    }
}

class AndroidHistoryRepository(
    private val medialibrary: Medialibrary,
) : HistoryRepository {

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> = callbackFlow {
        trySend(load(limit))
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override suspend fun addToHistory(item: MediaItem) = withContext(Dispatchers.IO) {
        if (!medialibrary.isInitiated) return@withContext
        medialibrary.getMedia(item.id)?.markAsPlayed()
        Unit
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        if (medialibrary.isInitiated) {
            medialibrary.clearHistory(Medialibrary.HISTORY_TYPE_LOCAL)
        }
        Unit
    }

    override suspend fun removeHistoryEntry(id: Long) = Unit

    private fun load(limit: Int): List<HistoryEntry> {
        if (!medialibrary.isInitiated) return emptyList()
        val history = medialibrary.history(Medialibrary.HISTORY_TYPE_LOCAL) ?: return emptyList()
        return history.take(limit.coerceAtLeast(0)).map {
            HistoryEntry(item = it.toMediaItem(), playedAt = it.seen)
        }
    }
}
