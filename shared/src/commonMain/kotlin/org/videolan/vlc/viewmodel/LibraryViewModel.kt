package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.repository.MediaRepository

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.VIDEO,
    val items: List<MediaItem> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val count: Int = 0,
)

enum class LibraryTab { VIDEO, AUDIO, ALL, RECENT }

/**
 * Shared library browser VM — same code drives Android and iOS.
 */
class LibraryViewModel(
    private val mediaRepository: MediaRepository = runCatching {
        VlcKoin.get().get<MediaRepository>()
    }.getOrElse { error("MediaRepository unavailable — start Koin first") },
    private val playback: PlaybackService = runCatching {
        VlcKoin.get().get<PlaybackService>()
    }.getOrElse { error("PlaybackService unavailable — start Koin first") },
) : VlcViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        observe()
    }

    fun selectTab(tab: LibraryTab) {
        _state.update { it.copy(tab = tab, loading = true, error = null) }
        observe()
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query, loading = true) }
        observe()
    }

    fun play(item: MediaItem) {
        val playlist = _state.value.items
        playback.play(item, playlist.ifEmpty { listOf(item) })
    }

    fun playAll(shuffle: Boolean = false) {
        val items = _state.value.items
        if (items.isEmpty()) return
        if (shuffle) playback.setShuffle(true)
        playback.playFromIndex(items, 0)
    }

    fun refresh() = observe()

    private fun observe() {
        observeJob?.cancel()
        observeJob = launch {
            val tab = _state.value.tab
            val query = _state.value.query.trim()
            val type = when (tab) {
                LibraryTab.VIDEO -> MediaType.VIDEO
                LibraryTab.AUDIO -> MediaType.AUDIO
                LibraryTab.ALL, LibraryTab.RECENT -> MediaType.ALL
            }
            try {
                val flow = when {
                    query.isNotEmpty() -> mediaRepository.search(query, type)
                    tab == LibraryTab.RECENT -> mediaRepository.observeRecentlyPlayed(100)
                    else -> mediaRepository.observeMedia(type)
                }
                flow
                    .catch { e ->
                        _state.update {
                            it.copy(loading = false, error = e.message ?: "Library error", items = emptyList())
                        }
                    }
                    .collectLatest { list ->
                        _state.update {
                            it.copy(
                                items = list,
                                count = list.size,
                                loading = false,
                                error = null,
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Library error")
                }
            }
        }
    }
}
