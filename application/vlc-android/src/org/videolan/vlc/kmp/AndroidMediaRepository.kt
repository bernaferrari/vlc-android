package org.videolan.vlc.kmp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.model.MediaFolder
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

    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> = mediaCallbackFlow {
        queryMedia(type)
    }

    override suspend fun getMedia(id: Long): MediaItem? = withContext(Dispatchers.IO) {
        medialibrary.getMedia(id)?.toMediaItem()
    }

    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> = withContext(Dispatchers.IO) {
        ids.mapNotNull { id -> medialibrary.getMedia(id)?.toMediaItem() }
    }

    override fun search(query: String, type: MediaType): Flow<List<MediaItem>> = mediaCallbackFlow {
        if (query.isBlank()) emptyList() else querySearch(query, type)
    }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> = mediaCallbackFlow {
        val history = medialibrary.history(Medialibrary.HISTORY_TYPE_LOCAL) ?: emptyArray()
        history.asList()
            .take(limit.coerceAtLeast(0))
            .map { it.toMediaItem() }
    }

    override suspend fun count(type: MediaType): Int = withContext(Dispatchers.IO) {
        if (!medialibrary.isInitiated) return@withContext 0
        when (type) {
            MediaType.AUDIO -> medialibrary.audioCount
            MediaType.VIDEO -> medialibrary.videoCount
            MediaType.ALL -> medialibrary.audioCount + medialibrary.videoCount
            else -> 0
        }
    }

    override suspend fun markAsPlayed(id: Long) {
        withContext(Dispatchers.IO) {
            medialibrary.getMedia(id)?.markAsPlayed()
        }
    }

    override suspend fun incrementPlayCount(id: Long) {
        withContext(Dispatchers.IO) {
            val media = medialibrary.getMedia(id) ?: return@withContext
            val next = media.playCount + 1L
            media.playCount = next
            if (media.seen == 0L) media.seen = 1L
            // markAsPlayed also updates ML history / last-played bookkeeping
            media.markAsPlayed()
        }
    }

    private fun queryMedia(type: MediaType): List<MediaItem> {
        if (!medialibrary.isInitiated) return emptyList()
        val includeMissing = Settings.includeMissing
        return when (type) {
            MediaType.AUDIO -> medialibrary.getAudio(
                Medialibrary.SORT_DEFAULT, false, includeMissing, false
            )
            MediaType.VIDEO -> medialibrary.getVideos(
                Medialibrary.SORT_DEFAULT, false, includeMissing, false
            )
            MediaType.ALL -> {
                val audio = medialibrary.getAudio(
                    Medialibrary.SORT_DEFAULT, false, includeMissing, false
                )
                val video = medialibrary.getVideos(
                    Medialibrary.SORT_DEFAULT, false, includeMissing, false
                )
                arrayOf(*audio, *video)
            }
            else -> emptyArray()
        }.map { it.toMediaItem() }
    }

    private fun querySearch(query: String, type: MediaType): List<MediaItem> {
        if (!medialibrary.isInitiated) return emptyList()
        val includeMissing = Settings.includeMissing
        val results: Array<MediaWrapper> = when (type) {
            MediaType.AUDIO -> medialibrary.searchAudio(
                query, Medialibrary.SORT_DEFAULT, false, includeMissing, false, Int.MAX_VALUE, 0
            ) ?: emptyArray()
            MediaType.VIDEO -> medialibrary.searchVideo(
                query, Medialibrary.SORT_DEFAULT, false, includeMissing, false, Int.MAX_VALUE, 0
            ) ?: emptyArray()
            else -> medialibrary.searchMedia(query) ?: emptyArray()
        }
        return results.map { it.toMediaItem() }
    }

    /**
     * Emits [query] immediately and again whenever the medialibrary reports media changes.
     */
    private fun mediaCallbackFlow(query: () -> List<MediaItem>): Flow<List<MediaItem>> = callbackFlow {
        fun emitLatest() {
            try {
                trySend(query())
            } catch (_: Exception) {
                trySend(emptyList())
            }
        }

        val mediaCb = object : Medialibrary.MediaCb {
            override fun onMediaAdded() = emitLatest()
            override fun onMediaModified() = emitLatest()
            override fun onMediaDeleted(id: LongArray?) = emitLatest()
            override fun onMediaConvertedToExternal(id: LongArray?) = emitLatest()
        }
        val historyCb = object : Medialibrary.HistoryCb {
            override fun onHistoryModified() = emitLatest()
        }
        medialibrary.addMediaCb(mediaCb)
        medialibrary.addHistoryCb(historyCb)
        emitLatest()

        awaitClose {
            medialibrary.removeMediaCb(mediaCb)
            medialibrary.removeHistoryCb(historyCb)
        }
    }.flowOn(Dispatchers.IO)


    override fun observeFolders(parentId: Long?): Flow<List<MediaFolder>> = callbackFlow {
        val folders = if (parentId == null) {
            AndroidDevices.externalStorageDirectories.map { path ->
                MediaFolder(
                    id = path.hashCode().toLong(),
                    title = path.substringAfterLast('/').ifBlank { path },
                    path = path,
                    uri = "file://$path",
                    isRoot = true,
                )
            }
        } else emptyList()
        trySend(folders)
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override fun observeFolderMedia(folderId: Long): Flow<List<MediaItem>> = mediaCallbackFlow {
        val roots = AndroidDevices.externalStorageDirectories
        val root = roots.firstOrNull { it.hashCode().toLong() == folderId }
        if (root == null) emptyList()
        else queryMedia(MediaType.ALL).filter { it.uri.contains(root) }
    }


}
