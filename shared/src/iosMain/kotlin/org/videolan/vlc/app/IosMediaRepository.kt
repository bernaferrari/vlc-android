package org.videolan.vlc.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.repository.MediaRepository

/**
 * iOS [MediaRepository] backed by an in-process catalog that Swift/VLCKit can
 * populate (file picker, Photos, VLCMediaLibrary, etc.).
 *
 * Until a full medialibrary port exists, this is the integration seam:
 * ```swift
 * let repo = IosMediaRepository.shared
 * repo.replaceAll(items)
 * ```
 */
class IosMediaRepository : MediaRepository, IosMediaRepositoryMarker {

    private val items = MutableStateFlow<List<MediaItem>>(emptyList())
    private val recent = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun replaceAllPublic(media: List<MediaItem>) = replaceAll(media)

    /** Replace the entire library snapshot (main-thread safe). */
    fun replaceAll(media: List<MediaItem>) {
        items.value = media
    }

    /** Append / upsert by uri. */
    fun upsert(media: MediaItem) {
        val without = items.value.filterNot { it.uri == media.uri }
        items.value = without + media
    }

    fun removeByUri(uri: String) {
        items.value = items.value.filterNot { it.uri == uri }
    }

    fun clear() {
        items.value = emptyList()
        recent.value = emptyList()
    }

    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> =
        items.map { list -> list.filterByType(type) }

    override suspend fun getMedia(id: Long): MediaItem? =
        items.value.firstOrNull { it.id == id }

    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> {
        val set = ids.toSet()
        return items.value.filter { it.id in set }
    }

    override fun search(query: String, type: MediaType): Flow<List<MediaItem>> =
        items.map { list ->
            val q = query.trim()
            if (q.isEmpty()) emptyList()
            else list.filterByType(type).filter {
                it.title.contains(q, ignoreCase = true) ||
                    (it.artist?.contains(q, ignoreCase = true) == true) ||
                    (it.album?.contains(q, ignoreCase = true) == true)
            }
        }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> =
        recent.map { it.take(limit.coerceAtLeast(0)) }

    override suspend fun count(type: MediaType): Int =
        items.value.filterByType(type).size

    override suspend fun markAsPlayed(id: Long) {
        val item = getMedia(id) ?: return
        val updated = recent.value.toMutableList()
        updated.removeAll { it.id == id }
        updated.add(0, item)
        recent.value = updated
    }

    override suspend fun incrementPlayCount(id: Long) {
        markAsPlayed(id)
        val current = items.value
        items.value = current.map {
            if (it.id == id) it.copy(playedCount = it.playedCount + 1) else it
        }
    }

    private fun List<MediaItem>.filterByType(type: MediaType): List<MediaItem> =
        when (type) {
            MediaType.ALL -> this
            else -> filter { it.type == type }
        }

    companion object {
        val shared: IosMediaRepository by lazy { IosMediaRepository() }
    }
}
