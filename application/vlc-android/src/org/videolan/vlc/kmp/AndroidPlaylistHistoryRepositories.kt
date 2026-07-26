package org.videolan.vlc.kmp

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
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
import org.videolan.vlc.repository.MEDIA_PAGE_SIZE
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.repository.PlaylistRepository

class AndroidPlaylistRepository(
    private val medialibrary: Medialibrary,
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<PlaylistInfo>> = callbackFlow {
        fun emitLatest() {
            try {
                trySend(loadPlaylists())
            } catch (_: Exception) {
                trySend(emptyList())
            }
        }

        val callback = object : Medialibrary.PlaylistsCb {
            override fun onPlaylistsAdded() = emitLatest()
            override fun onPlaylistsModified() = emitLatest()
            override fun onPlaylistsDeleted() = emitLatest()
        }
        medialibrary.addPlaylistCb(callback)
        emitLatest()
        awaitClose { medialibrary.removePlaylistCb(callback) }
    }.flowOn(Dispatchers.IO)

    override fun observePlaylistsPaged(
        sort: MediaSort,
        desc: Boolean,
        onlyFavorites: Boolean,
        query: String,
    ): Flow<PagingData<PlaylistInfo>> {
        return Pager(
            config = PagingConfig(
                pageSize = MEDIA_PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = MEDIA_PAGE_SIZE,
            ),
            pagingSourceFactory = {
                object : PagingSource<Int, PlaylistInfo>() {
                    private val unregisterInvalidation = registerPlaylistInvalidation(::invalidate)

                    init {
                        registerInvalidatedCallback { unregisterInvalidation() }
                    }

                    override fun getRefreshKey(state: PagingState<Int, PlaylistInfo>): Int? {
                        val anchor = state.anchorPosition ?: return null
                        val page = state.closestPageToPosition(anchor) ?: return null
                        return page.prevKey?.plus(state.config.pageSize)
                            ?: page.nextKey?.minus(state.config.pageSize)?.coerceAtLeast(0)
                    }

                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PlaylistInfo> {
                        return try {
                            val offset = params.key ?: 0
                            val loadSize = params.loadSize
                            val data = withContext(Dispatchers.IO) {
                                loadPagedPlaylists(sort, desc, onlyFavorites, query, loadSize, offset)
                            }
                            val total = withContext(Dispatchers.IO) {
                                countPlaylists(query)
                            }
                            val nextOffset = offset + data.size
                            LoadResult.Page(
                                data = data,
                                prevKey = if (offset <= 0) null else (offset - loadSize).coerceAtLeast(0),
                                nextKey = if (data.isEmpty() || nextOffset >= total) null else nextOffset,
                            )
                        } catch (t: Throwable) {
                            LoadResult.Error(t)
                        }
                    }
                }
            },
        ).flow
    }

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
        if (itemIds.isEmpty()) return@withContext
        val pl = medialibrary.getPlaylist(playlistId, false, false) ?: return@withContext
        val tracks = pl.tracks ?: return@withContext
        // Resolve media ids to playlist indices; remove high→low so earlier indices stay valid.
        val indices = itemIds.mapNotNull { itemId ->
            tracks.indexOfFirst { it.id == itemId }.takeIf { it >= 0 }
        }.distinct().sortedDescending()
        for (index in indices) {
            runCatching { pl.remove(index) }
        }
        Unit
    }

    override suspend fun moveInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0) return@withContext
        val pl = medialibrary.getPlaylist(playlistId, false, false) ?: return@withContext
        runCatching { pl.move(fromIndex, toIndex) }
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

    override suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        medialibrary.getPlaylist(id, false, false)?.setFavorite(favorite)
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
        return list.map { it.toPlaylistInfo() }
    }

    /** Keep the native paging path in sync with playlist mutations. */
    private fun registerPlaylistInvalidation(invalidate: () -> Unit): () -> Unit {
        val callback = object : Medialibrary.PlaylistsCb {
            override fun onPlaylistsAdded() = invalidate()
            override fun onPlaylistsModified() = invalidate()
            override fun onPlaylistsDeleted() = invalidate()
        }
        medialibrary.addPlaylistCb(callback)
        return { medialibrary.removePlaylistCb(callback) }
    }

    private fun loadPagedPlaylists(
        sort: MediaSort,
        desc: Boolean,
        onlyFavorites: Boolean,
        query: String,
        loadSize: Int,
        offset: Int,
    ): List<PlaylistInfo> {
        if (!medialibrary.isInitiated) return emptyList()
        val mlSort = sort.toMlSort()
        val q = query.trim()
        val list: Array<out MlPlaylist> = if (q.isEmpty()) {
            runCatching {
                medialibrary.getPagedPlaylists(
                    MlPlaylist.Type.All,
                    mlSort,
                    desc,
                    Settings.includeMissing,
                    onlyFavorites,
                    loadSize,
                    offset,
                )
            }.getOrDefault(emptyArray())
        } else {
            runCatching {
                medialibrary.searchPlaylist(
                    q,
                    MlPlaylist.Type.All,
                    mlSort,
                    desc,
                    Settings.includeMissing,
                    onlyFavorites,
                    loadSize,
                    offset,
                )
            }.getOrDefault(emptyArray())
        }
        return list.map { it.toPlaylistInfo() }
    }

    private fun countPlaylists(query: String): Int {
        if (!medialibrary.isInitiated) return 0
        val q = query.trim()
        return if (q.isEmpty()) {
            runCatching { medialibrary.playlistsCount }.getOrElse {
                runCatching { medialibrary.getPlaylistsCount() }.getOrDefault(0)
            }
        } else {
            runCatching { medialibrary.getPlaylistsCount(q) }.getOrDefault(0)
        }
    }

    private fun MlPlaylist.toPlaylistInfo(): PlaylistInfo = PlaylistInfo(
        id = id,
        name = title.orEmpty(),
        itemCount = runCatching { tracksCount }.getOrDefault(0),
        artworkUri = runCatching { artworkMrl }.getOrNull(),
        duration = runCatching { duration }.getOrDefault(0L),
        isFavorite = isFavorite,
    )
}

class AndroidHistoryRepository(
    private val medialibrary: Medialibrary,
) : HistoryRepository {

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> = callbackFlow {
        fun emitLatest() {
            try {
                trySend(load(limit))
            } catch (_: Exception) {
                trySend(emptyList())
            }
        }

        val callback = object : Medialibrary.HistoryCb {
            override fun onHistoryModified() = emitLatest()
        }
        medialibrary.addHistoryCb(callback)
        emitLatest()
        awaitClose { medialibrary.removeHistoryCb(callback) }
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

    override suspend fun removeHistoryEntry(id: Long) = withContext(Dispatchers.IO) {
        if (!medialibrary.isInitiated) return@withContext
        medialibrary.history(Medialibrary.HISTORY_TYPE_LOCAL)
            ?.firstOrNull { it.id == id }
            ?.removeFromHistory()
        Unit
    }

    private fun load(limit: Int): List<HistoryEntry> {
        if (!medialibrary.isInitiated) return emptyList()
        val history = medialibrary.history(Medialibrary.HISTORY_TYPE_LOCAL) ?: return emptyList()
        return history.take(limit.coerceAtLeast(0)).map {
            HistoryEntry(item = it.toMediaItem(), playedAt = it.seen)
        }
    }
}
