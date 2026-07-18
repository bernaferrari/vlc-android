package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.PlaylistRepository

enum class MainTab {
    VIDEO, AUDIO, BROWSER, PLAYLISTS, MORE
}

data class MediaListUiState(
    val items: List<MediaItem> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val count: Int = 0,
)

data class BrowserUiState(
    val folders: List<MediaFolder> = emptyList(),
    val media: List<MediaItem> = emptyList(),
    val currentFolder: MediaFolder? = null,
    val stack: List<MediaFolder> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

data class PlaylistsUiState(
    val playlists: List<PlaylistInfo> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

data class MoreUiState(
    val history: List<HistoryEntry> = emptyList(),
    val platformName: String = "",
    val loading: Boolean = true,
)

private fun mediaRepo() = runCatching { VlcKoin.get().get<MediaRepository>() }
    .getOrElse { error("MediaRepository unavailable") }

private fun playlistRepo() = runCatching { VlcKoin.get().get<PlaylistRepository>() }
    .getOrElse { error("PlaylistRepository unavailable") }

private fun historyRepo() = runCatching { VlcKoin.get().get<HistoryRepository>() }
    .getOrElse { error("HistoryRepository unavailable") }

private fun playback() = runCatching { PlaybackController.get() }
    .getOrElse { error("PlaybackController unavailable") }

class VideoListViewModel(
    private val repo: MediaRepository = mediaRepo(),
    private val player: PlaybackController = playback(),
) : VlcViewModel() {
    private val _state = MutableStateFlow(MediaListUiState())
    val state: StateFlow<MediaListUiState> = _state.asStateFlow()
    private var job: Job? = null

    init { observe() }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q, loading = true) }
        observe()
    }

    fun play(item: MediaItem) {
        val list = _state.value.items
        player.play(item, list.ifEmpty { listOf(item) })
        launchIo { runCatching { repo.markAsPlayed(item.id) } }
    }

    fun playAll() {
        val list = _state.value.items
        if (list.isNotEmpty()) player.playFromIndex(list, 0)
    }

    fun refresh() = observe()

    private fun observe() {
        job?.cancel()
        job = launch {
            val q = _state.value.query.trim()
            val flow = if (q.isEmpty()) repo.observeMedia(MediaType.VIDEO) else repo.search(q, MediaType.VIDEO)
            flow.catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { list ->
                    _state.update {
                        it.copy(items = list, count = list.size, loading = false, error = null)
                    }
                }
        }
    }
}

class AudioListViewModel(
    private val repo: MediaRepository = mediaRepo(),
    private val player: PlaybackController = playback(),
) : VlcViewModel() {
    private val _state = MutableStateFlow(MediaListUiState())
    val state: StateFlow<MediaListUiState> = _state.asStateFlow()
    private var job: Job? = null

    init { observe() }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q, loading = true) }
        observe()
    }

    fun play(item: MediaItem) {
        val list = _state.value.items
        player.play(item, list.ifEmpty { listOf(item) })
        launchIo { runCatching { repo.markAsPlayed(item.id) } }
    }

    fun playAll() {
        val list = _state.value.items
        if (list.isNotEmpty()) player.playFromIndex(list, 0)
    }

    fun refresh() = observe()

    private fun observe() {
        job?.cancel()
        job = launch {
            val q = _state.value.query.trim()
            val flow = if (q.isEmpty()) repo.observeMedia(MediaType.AUDIO) else repo.search(q, MediaType.AUDIO)
            flow.catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { list ->
                    _state.update {
                        it.copy(items = list, count = list.size, loading = false, error = null)
                    }
                }
        }
    }
}

class BrowserViewModel(
    private val repo: MediaRepository = mediaRepo(),
    private val player: PlaybackController = playback(),
) : VlcViewModel() {
    private val _state = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()
    private var job: Job? = null

    init { openRoot() }

    fun openRoot() {
        _state.update { it.copy(currentFolder = null, stack = emptyList(), loading = true) }
        observe(null)
    }

    fun openFolder(folder: MediaFolder) {
        val stack = _state.value.stack + folder
        _state.update { it.copy(currentFolder = folder, stack = stack, loading = true) }
        observe(folder.id)
    }

    fun goUp(): Boolean {
        val stack = _state.value.stack
        if (stack.isEmpty()) return false
        val next = stack.dropLast(1)
        if (next.isEmpty()) {
            openRoot()
        } else {
            val folder = next.last()
            _state.update { it.copy(currentFolder = folder, stack = next, loading = true) }
            observe(folder.id)
        }
        return true
    }

    fun play(item: MediaItem) {
        val list = _state.value.media
        player.play(item, list.ifEmpty { listOf(item) })
    }

    private fun observe(folderId: Long?) {
        job?.cancel()
        job = launch {
            if (folderId == null) {
                repo.observeFolders(null)
                    .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                    .collectLatest { folders ->
                        _state.update {
                            it.copy(folders = folders, media = emptyList(), loading = false, error = null)
                        }
                    }
            } else {
                // parallel-ish: folders under parent + media in folder
                launch {
                    repo.observeFolders(folderId)
                        .catch { }
                        .collectLatest { folders -> _state.update { it.copy(folders = folders) } }
                }
                repo.observeFolderMedia(folderId)
                    .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                    .collectLatest { media ->
                        _state.update { it.copy(media = media, loading = false, error = null) }
                    }
            }
        }
    }
}

class PlaylistsViewModel(
    private val repo: PlaylistRepository = playlistRepo(),
    private val player: PlaybackController = playback(),
) : VlcViewModel() {
    private val _state = MutableStateFlow(PlaylistsUiState())
    val state: StateFlow<PlaylistsUiState> = _state.asStateFlow()

    init {
        launch {
            repo.observePlaylists()
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { list ->
                    _state.update { it.copy(playlists = list, loading = false, error = null) }
                }
        }
    }

    fun create(name: String) = launchIo {
        runCatching { repo.createPlaylist(name) }
    }

    fun playPlaylist(info: PlaylistInfo) = launchIo {
        val pl = repo.getPlaylist(info.id) ?: return@launchIo
        if (pl.items.isNotEmpty()) player.playFromIndex(pl.items, 0)
    }

    fun delete(id: Long) = launchIo {
        runCatching { repo.deletePlaylist(id) }
    }
}

class MoreHubViewModel(
    private val history: HistoryRepository = historyRepo(),
) : VlcViewModel() {
    private val _state = MutableStateFlow(MoreUiState())
    val state: StateFlow<MoreUiState> = _state.asStateFlow()

    init {
        launch {
            history.observeHistory(50)
                .catch { _state.update { it.copy(loading = false) } }
                .collectLatest { list ->
                    _state.update { it.copy(history = list, loading = false) }
                }
        }
        launch {
            val info = runCatching {
                org.videolan.vlc.platform.PlatformInfoProvider.current
            }.getOrNull()
            val name = info?.let { "${it.platform} ${it.osVersion}" }.orEmpty()
            _state.update { it.copy(platformName = name) }
        }
    }

    fun clearHistory() = launchIo { runCatching { history.clearHistory() } }
}
