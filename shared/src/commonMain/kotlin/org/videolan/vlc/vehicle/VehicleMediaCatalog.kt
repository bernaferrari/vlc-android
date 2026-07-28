package org.videolan.vlc.vehicle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.MediaRepository

/**
 * Shared, safety-bounded source of truth for vehicle UIs.
 *
 * CarPlay and Android Auto keep their own OS-required templates and services,
 * but must never reimplement catalog filtering, queue construction, or a
 * separate player. The catalog deliberately exposes audio only and limits the
 * visible list while retaining the complete queue for a selected item.
 */
class VehicleMediaCatalog(
    private val media: MediaRepository,
    private val player: PlaybackController,
    private val visibleLimit: Int = DEFAULT_VISIBLE_LIMIT,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val allAudio = MutableStateFlow<List<MediaItem>>(emptyList())
    private val mutableVisibleItems = MutableStateFlow<List<MediaItem>>(emptyList())

    /** The bounded, audio-only snapshot suitable for a vehicle template. */
    val visibleItems: StateFlow<List<MediaItem>> = mutableVisibleItems

    init {
        scope.launch {
            media.observeMedia(MediaType.AUDIO).collectLatest { items ->
                val audio = items.filter { it.type == MediaType.AUDIO }
                allAudio.value = audio
                mutableVisibleItems.value = audio.take(visibleLimit.coerceAtLeast(1))
            }
        }
    }

    fun snapshot(): List<MediaItem> = visibleItems.value

    /** Selects an exposed item and plays it against the complete audio queue. */
    fun play(id: Long): Boolean {
        val queue = allAudio.value
        val item = queue.firstOrNull { it.id == id } ?: return false
        player.play(item, queue)
        return true
    }

    companion object {
        const val DEFAULT_VISIBLE_LIMIT = 100
    }
}
