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
import org.videolan.vlc.util.ContextOption

enum class MainTab {
    VIDEO, AUDIO, BROWSER, PLAYLISTS, MORE
}

enum class ViewMode { LIST, GRID }

enum class SortMode { TITLE, ARTIST, ALBUM, DURATION, RECENT }

enum class AudioSection { TRACKS, ARTISTS, ALBUMS }

data class MediaListUiState(
    val items: List<MediaItem> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val count: Int = 0,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortMode: SortMode = SortMode.TITLE,
    val selection: Set<String> = emptySet(), // by uri
    val sections: List<Pair<String, List<MediaItem>>> = emptyList(),
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
    val openPlaylistId: Long? = null,
    val openPlaylistName: String? = null,
    val openItems: List<MediaItem> = emptyList(),
)

data class MoreUiState(
    val history: List<HistoryEntry> = emptyList(),
    val platformName: String = "",
    val loading: Boolean = true,
    val streams: List<MediaItem> = emptyList(),
)

private fun mediaRepo() = runCatching { VlcKoin.get().get<MediaRepository>() }
    .getOrElse { error("MediaRepository unavailable") }

private fun playlistRepo() = runCatching { VlcKoin.get().get<PlaylistRepository>() }
    .getOrElse { error("PlaylistRepository unavailable") }

private fun historyRepo() = runCatching { VlcKoin.get().get<HistoryRepository>() }
    .getOrElse { error("HistoryRepository unavailable") }

private fun playback() = runCatching { PlaybackController.get() }
    .getOrElse { error("PlaybackController unavailable") }

private fun sortItems(items: List<MediaItem>, mode: SortMode): List<MediaItem> = when (mode) {
    SortMode.TITLE -> items.sortedBy { it.displayTitle.lowercase() }
    SortMode.ARTIST -> items.sortedWith(compareBy({ it.artist?.lowercase().orEmpty() }, { it.displayTitle.lowercase() }))
    SortMode.ALBUM -> items.sortedWith(compareBy({ it.album?.lowercase().orEmpty() }, { it.trackNumber }, { it.displayTitle.lowercase() }))
    SortMode.DURATION -> items.sortedByDescending { it.duration }
    SortMode.RECENT -> items.sortedByDescending { it.lastPlayed }
}

private fun sectionByArtist(items: List<MediaItem>): List<Pair<String, List<MediaItem>>> =
    items.groupBy { it.artist?.takeIf { a -> a.isNotBlank() } ?: "Unknown artist" }
        .toList()
        .sortedBy { it.first.lowercase() }

private fun sectionByAlbum(items: List<MediaItem>): List<Pair<String, List<MediaItem>>> =
    items.groupBy { it.album?.takeIf { a -> a.isNotBlank() } ?: "Unknown album" }
        .toList()
        .sortedBy { it.first.lowercase() }

class VideoListViewModel(
    private val repo: MediaRepository = mediaRepo(),
    private val player: PlaybackController = playback(),
) : VlcViewModel() {
    private val _state = MutableStateFlow(MediaListUiState(viewMode = ViewMode.GRID))
    val state: StateFlow<MediaListUiState> = _state.asStateFlow()
    private var raw: List<MediaItem> = emptyList()
    private var job: Job? = null

    init { observe() }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q, loading = true) }
        observe()
    }

    fun setViewMode(mode: ViewMode) = _state.update { it.copy(viewMode = mode) }
    fun setSort(mode: SortMode) {
        _state.update { it.copy(sortMode = mode) }
        publish(raw)
    }

    fun toggleSelect(item: MediaItem) {
        _state.update {
            val sel = it.selection.toMutableSet()
            if (!sel.add(item.uri)) sel.remove(item.uri)
            it.copy(selection = sel)
        }
    }

    fun selectAll() = _state.update { it.copy(selection = raw.map { m -> m.uri }.toSet()) }
    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun play(item: MediaItem) {
        val list = _state.value.items
        player.play(item, list.ifEmpty { listOf(item) })
        launchIo { runCatching { repo.markAsPlayed(item.id) } }
    }

    fun playAll() {
        val list = _state.value.items
        if (list.isNotEmpty()) player.playFromIndex(list, 0)
    }

    fun playNext(item: MediaItem) = player.insertNext(listOf(item))
    fun append(item: MediaItem) = player.append(listOf(item))
    fun playSelection() {
        val selected = _state.value.items.filter { it.uri in _state.value.selection }
        if (selected.isNotEmpty()) player.playFromIndex(selected, 0)
    }

    fun handleCtx(item: MediaItem, opt: ContextOption) {
        when (opt) {
            ContextOption.CTX_PLAY -> play(item)
            ContextOption.CTX_PLAY_NEXT -> playNext(item)
            ContextOption.CTX_APPEND -> append(item)
            ContextOption.CTX_PLAY_ALL -> playAll()
            ContextOption.CTX_STOP_AFTER_THIS -> {
                play(item)
                player.setStopAfterThis()
            }
            else -> play(item)
        }
    }

    fun refresh() = observe()

    private fun observe() {
        job?.cancel()
        job = launch {
            val q = _state.value.query.trim()
            val flow = if (q.isEmpty()) repo.observeMedia(MediaType.VIDEO) else repo.search(q, MediaType.VIDEO)
            flow.catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { list ->
                    raw = list
                    publish(list)
                }
        }
    }

    private fun publish(list: List<MediaItem>) {
        val sorted = sortItems(list, _state.value.sortMode)
        _state.update {
            it.copy(items = sorted, count = sorted.size, loading = false, error = null, sections = emptyList())
        }
    }
}

class AudioListViewModel(
    private val repo: MediaRepository = mediaRepo(),
    private val player: PlaybackController = playback(),
) : VlcViewModel() {
    private val _state = MutableStateFlow(MediaListUiState())
    val state: StateFlow<MediaListUiState> = _state.asStateFlow()
    private val _section = MutableStateFlow(AudioSection.TRACKS)
    val section: StateFlow<AudioSection> = _section.asStateFlow()
    private var raw: List<MediaItem> = emptyList()
    private var job: Job? = null

    init { observe() }

    fun setSection(section: AudioSection) {
        _section.value = section
        publish(raw)
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q, loading = true) }
        observe()
    }

    fun setViewMode(mode: ViewMode) = _state.update { it.copy(viewMode = mode) }
    fun setSort(mode: SortMode) {
        _state.update { it.copy(sortMode = mode) }
        publish(raw)
    }

    fun toggleSelect(item: MediaItem) {
        _state.update {
            val sel = it.selection.toMutableSet()
            if (!sel.add(item.uri)) sel.remove(item.uri)
            it.copy(selection = sel)
        }
    }

    fun selectAll() = _state.update { it.copy(selection = raw.map { m -> m.uri }.toSet()) }
    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun play(item: MediaItem) {
        val list = _state.value.items
        player.play(item, list.ifEmpty { listOf(item) })
        launchIo { runCatching { repo.markAsPlayed(item.id) } }
    }

    fun playAll() {
        val list = _state.value.items
        if (list.isNotEmpty()) player.playFromIndex(list, 0)
    }

    fun playNext(item: MediaItem) = player.insertNext(listOf(item))
    fun append(item: MediaItem) = player.append(listOf(item))

    fun handleCtx(item: MediaItem, opt: ContextOption) {
        when (opt) {
            ContextOption.CTX_PLAY -> play(item)
            ContextOption.CTX_PLAY_NEXT -> playNext(item)
            ContextOption.CTX_APPEND -> append(item)
            ContextOption.CTX_PLAY_ALL -> playAll()
            else -> play(item)
        }
    }

    fun refresh() = observe()

    private fun observe() {
        job?.cancel()
        job = launch {
            val q = _state.value.query.trim()
            val flow = if (q.isEmpty()) repo.observeMedia(MediaType.AUDIO) else repo.search(q, MediaType.AUDIO)
            flow.catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { list ->
                    raw = list
                    publish(list)
                }
        }
    }

    private fun publish(list: List<MediaItem>) {
        val sorted = sortItems(list, _state.value.sortMode)
        val sections = when (_section.value) {
            AudioSection.TRACKS -> emptyList()
            AudioSection.ARTISTS -> sectionByArtist(sorted)
            AudioSection.ALBUMS -> sectionByAlbum(sorted)
        }
        val flat = if (sections.isEmpty()) sorted else sections.flatMap { it.second }
        _state.update {
            it.copy(
                items = flat,
                count = sorted.size,
                loading = false,
                error = null,
                sections = sections,
            )
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
        if (next.isEmpty()) openRoot()
        else {
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

    fun playNext(item: MediaItem) = player.insertNext(listOf(item))
    fun append(item: MediaItem) = player.append(listOf(item))

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
        player.play(item, items.ifEmpty { listOf(item) })
    }

    fun delete(id: Long) = launchIo {
        runCatching { repo.deletePlaylist(id) }
    }
}

class MoreHubViewModel(
    private val history: HistoryRepository = historyRepo(),
    private val media: MediaRepository = mediaRepo(),
    private val player: PlaybackController = playback(),
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
            // Streams: media typed as STREAM when available
            media.observeMedia(MediaType.STREAM)
                .catch { }
                .collectLatest { list -> _state.update { it.copy(streams = list) } }
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
    fun playHistory(entry: HistoryEntry) = player.play(entry.item)
    fun playStream(item: MediaItem) = player.play(item)
}
