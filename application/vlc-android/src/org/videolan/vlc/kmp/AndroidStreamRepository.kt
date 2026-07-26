package org.videolan.vlc.kmp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.repository.StreamRepository

/**
 * Android [StreamRepository] backed by medialibrary network history entries.
 */
class AndroidStreamRepository(
    private val medialibrary: Medialibrary,
) : StreamRepository {

    override fun observeStreams(): Flow<List<MediaItem>> = callbackFlow {
        fun emitLatest() {
            try {
                trySend(loadStreams())
            } catch (_: Exception) {
                trySend(emptyList())
            }
        }

        val historyCb = object : Medialibrary.HistoryCb {
            override fun onHistoryModified() = emitLatest()
        }
        val mediaCb = object : Medialibrary.MediaCb {
            override fun onMediaAdded() = emitLatest()
            override fun onMediaModified() = emitLatest()
            override fun onMediaDeleted(id: LongArray?) = emitLatest()
            override fun onMediaConvertedToExternal(id: LongArray?) = emitLatest()
        }
        medialibrary.addHistoryCb(historyCb)
        medialibrary.addMediaCb(mediaCb)
        emitLatest()
        awaitClose {
            medialibrary.removeHistoryCb(historyCb)
            medialibrary.removeMediaCb(mediaCb)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun addStream(title: String, uri: String): MediaItem? = withContext(Dispatchers.IO) {
        if (!medialibrary.isInitiated) return@withContext null
        val mrl = uri.trim()
        if (mrl.isEmpty()) return@withContext null
        val label = title.trim().ifBlank { mrl }
        medialibrary.addStream(mrl, label)?.toMediaItem()
    }

    override suspend fun renameStream(id: Long, title: String) = withContext(Dispatchers.IO) {
        if (!medialibrary.isInitiated) return@withContext
        val media = medialibrary.getMedia(id) ?: return@withContext
        media.rename(title.trim())
        Unit
    }

    override suspend fun deleteStream(id: Long) = withContext(Dispatchers.IO) {
        if (!medialibrary.isInitiated) return@withContext
        medialibrary.removeExternalMedia(id)
        Unit
    }

    private fun loadStreams(): List<MediaItem> {
        if (!medialibrary.isInitiated) return emptyList()
        val history = medialibrary.history(Medialibrary.HISTORY_TYPE_NETWORK) ?: return emptyList()
        return history.map { it.toMediaItem() }
    }
}
