@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.viewmodel

import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.tools.VlcPreferences
import org.videolan.vlc.util.DefaultPlaybackAction
import org.videolan.vlc.util.DefaultPlaybackKeys
import kotlin.random.Random

class PlaylistsViewModel(
    private val repo: PlaylistRepository = mainShellPlaylistRepo(),
    private val player: PlaybackController = mainShellPlayback(),
    private val prefs: VlcPreferences? = prefsOrNull(),
    initialDefaultAction: String? = null,
) : VlcViewModel() {
    private val _state = MutableStateFlow(PlaylistsUiState())
    val state: StateFlow<PlaylistsUiState> = _state.asStateFlow()

    private val filterFlow = MutableStateFlow(Filter(onlyFavorites = false, desc = false, query = ""))
    private var job: Job? = null

    val pagingFlow: Flow<PagingData<PlaylistInfo>> = filterFlow
        .flatMapLatest { f ->
            repo.observePlaylistsPaged(
                sort = MediaSort.TITLE,
                desc = f.desc,
                onlyFavorites = f.onlyFavorites,
                query = f.query,
            )
        }
        .cachedIn(viewModelScope)

    private data class Filter(
        val onlyFavorites: Boolean,
        val desc: Boolean,
        val query: String,
    )

    init {
        loadDefaultPlaybackAction(initialDefaultAction)
        observeList()
    }

    private fun loadDefaultPlaybackAction(injected: String?) {
        if (injected != null) {
            _state.update {
                it.copy(defaultPlaybackAction = DefaultPlaybackAction.fromName(injected).name)
            }
            return
        }
        launchIo {
            val name = runCatching {
                prefs?.getString(DefaultPlaybackKeys.PLAYLIST, DefaultPlaybackAction.PLAY.name)
            }.getOrNull()
            if (name != null) {
                _state.update {
                    it.copy(defaultPlaybackAction = DefaultPlaybackAction.fromName(name).name)
                }
            }
        }
    }

    private fun observeList() {
        job?.cancel()
        job = launch {
            combine(
                repo.observePlaylists(),
                filterFlow,
            ) { list, filter ->
                var out = list
                if (filter.onlyFavorites) out = out.filter { it.isFavorite }
                if (filter.query.isNotBlank()) {
                    out = out.filter { it.name.contains(filter.query, ignoreCase = true) }
                }
                out = out.sortedBy { it.name.lowercase() }
                if (filter.desc) out = out.reversed()
                out
            }.catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { list ->
                    _state.update { it.copy(playlists = list, loading = false, error = null) }
                }
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        observeList()
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        filterFlow.update { it.copy(query = q) }
    }

    fun toggleOnlyFavorites() {
        val next = !_state.value.onlyFavorites
        _state.update { it.copy(onlyFavorites = next) }
        filterFlow.update { it.copy(onlyFavorites = next) }
    }

    fun toggleSortDesc() {
        val next = !_state.value.sortDesc
        _state.update { it.copy(sortDesc = next) }
        filterFlow.update { it.copy(desc = next) }
    }

    fun setViewMode(mode: ViewMode) = _state.update { it.copy(viewMode = mode) }

    fun create(name: String) = launchIo {
        runCatching { repo.createPlaylist(name) }
    }

    fun playPlaylist(info: PlaylistInfo) = launchIo {
        val pl = repo.getPlaylist(info.id) ?: return@launchIo
        if (pl.items.isEmpty()) return@launchIo
        player.setShuffle(false)
        val action = DefaultPlaybackAction.fromName(_state.value.defaultPlaybackAction)
        applyDefaultPlayback(action, pl.items.first(), pl.items, player)
    }

    fun shufflePlay(info: PlaylistInfo) = launchIo {
        val pl = repo.getPlaylist(info.id) ?: return@launchIo
        if (pl.items.isEmpty()) return@launchIo
        player.setShuffle(true)
        val idx = if (pl.items.size == 1) 0 else Random.nextInt(pl.items.size)
        player.playFromIndex(pl.items, idx)
    }

    fun openPlaylist(info: PlaylistInfo) = launchIo {
        val pl = repo.getPlaylist(info.id)
        _state.update {
            it.copy(
                openPlaylistId = info.id,
                openPlaylistName = info.name,
                openItems = pl?.items.orEmpty(),
            )
        }
    }

    fun closeDetail() = _state.update {
        it.copy(openPlaylistId = null, openPlaylistName = null, openItems = emptyList())
    }

    fun playItem(item: MediaItem) {
        val items = _state.value.openItems
        val action = DefaultPlaybackAction.fromName(_state.value.defaultPlaybackAction)
        applyDefaultPlayback(action, item, items, player)
    }

    fun removeTrackAt(index: Int) = launchIo {
        val playlistId = _state.value.openPlaylistId ?: return@launchIo
        if (index !in _state.value.openItems.indices) return@launchIo
        val removal = runCatching { repo.removeFromPlaylistAt(playlistId, index) }
        if (removal.isFailure) {
            _state.update { it.copy(actionError = removal.exceptionOrNull().playlistActionMessage("remove track")) }
            return@launchIo
        }
        val pl = runCatching { repo.getPlaylist(playlistId) }.getOrNull()
        _state.update { it.copy(openItems = pl?.items.orEmpty(), actionError = null) }
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) = launchIo {
        val playlistId = _state.value.openPlaylistId ?: return@launchIo
        val items = _state.value.openItems
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return@launchIo
        runCatching { repo.moveInPlaylist(playlistId, fromIndex, toIndex) }
        val pl = runCatching { repo.getPlaylist(playlistId) }.getOrNull()
        if (pl != null) {
            _state.update { it.copy(openItems = pl.items) }
        } else {
            // Optimistic local reorder when repo is a no-op stub.
            val reordered = items.toMutableList()
            val moved = reordered.removeAt(fromIndex)
            reordered.add(toIndex, moved)
            _state.update { it.copy(openItems = reordered) }
        }
    }

    fun moveTrackUp(index: Int) {
        if (index > 0) moveTrack(index, index - 1)
    }

    fun moveTrackDown(index: Int) {
        val items = _state.value.openItems
        if (index in 0 until items.lastIndex) moveTrack(index, index + 1)
    }
    fun setDefaultPlaybackAction(name: String) {
        val action = DefaultPlaybackAction.fromName(name)
        _state.update { it.copy(defaultPlaybackAction = action.name) }
        persistString(DefaultPlaybackKeys.PLAYLIST, action.name)
        launchIo {
            runCatching { prefs?.putString(DefaultPlaybackKeys.PLAYLIST, action.name) }
        }
    }

    fun delete(id: Long) = launchIo {
        runCatching { repo.deletePlaylist(id) }
            .onSuccess { _state.update { state -> state.copy(actionError = null) } }
            .onFailure { error ->
                _state.update { state -> state.copy(actionError = error.playlistActionMessage("delete playlist")) }
            }
    }

    fun rename(id: Long, name: String) = launchIo {
        if (name.isBlank()) return@launchIo
        runCatching { repo.renamePlaylist(id, name.trim()) }
        val openId = _state.value.openPlaylistId
        if (openId == id) _state.update { it.copy(openPlaylistName = name.trim()) }
    }

    fun setFavorite(id: Long, favorite: Boolean) = launchIo {
        runCatching { repo.setFavorite(id, favorite) }
    }

    fun toggleSelect(id: Long) {
        _state.update {
            val sel = it.selection.toMutableSet()
            if (!sel.add(id)) sel.remove(id)
            it.copy(selection = sel)
        }
    }

    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun deleteSelection() = launchIo {
        val ids = _state.value.selection.toList()
        val failed = ids.filter { id -> runCatching { repo.deletePlaylist(id) }.isFailure }
        _state.update {
            it.copy(
                selection = failed.toSet(),
                actionError = if (failed.isEmpty()) null else "Couldn’t delete ${failed.size} playlist(s). Try again.",
            )
        }
    }

    fun clearActionError() = _state.update { it.copy(actionError = null) }
}

private fun Throwable?.playlistActionMessage(action: String): String =
    this?.message?.takeIf { it.isNotBlank() } ?: "Couldn’t $action. Try again."
