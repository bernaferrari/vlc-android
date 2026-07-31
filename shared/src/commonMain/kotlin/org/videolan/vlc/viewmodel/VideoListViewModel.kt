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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.ContainerKind
import org.videolan.vlc.repository.MediaQuery
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.util.ContextOption
import org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.util.DefaultPlaybackAction
import org.videolan.vlc.util.DefaultPlaybackKeys

class VideoListViewModel(
    private val repo: MediaRepository = mainShellMediaRepo(),
    private val player: PlaybackController = mainShellPlayback(),
    private val prefs: VlcPreferences? = prefsOrNull(),
    initialDefaultAction: String? = null,
) : VlcViewModel() {
    private val _state = MutableStateFlow(
        MediaListUiState(
            viewMode = ViewMode.GRID,
            showTrackNumbers = runCatching { VlcSettings.showTrackNumber.value }.getOrDefault(true),
            supportsRescan = repo.supportsRescan,
        ),
    )
    val state: StateFlow<MediaListUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow(
        MediaQuery(type = MediaType.VIDEO, sort = MediaSort.TITLE),
    )
    /** Bumps when grouping mode alone must refresh paging without query changes. */
    private val pagingEpoch = MutableStateFlow(0)

    /** Paged library flow — rebuilt when query/sort/fav/container/grouping changes. */
    val pagingFlow: Flow<PagingData<MediaItem>> = combine(queryFlow, pagingEpoch) { q, _ -> q }
        .flatMapLatest { q ->
            if (_state.value.groupingMode.isGrouped() &&
                q.containerKind == ContainerKind.NONE
            ) {
                flowOf(PagingData.empty())
            } else {
                repo.observeMediaPaged(q)
            }
        }
        .cachedIn(viewModelScope)
    private var raw: List<MediaItem> = emptyList()
    private var job: Job? = null
    private var groupsJob: Job? = null

    init {
        loadDefaultPlaybackAction(initialDefaultAction)
        launch {
            VlcSettings.showTrackNumber.collectLatest { show ->
                _state.update { it.copy(showTrackNumbers = show) }
            }
        }
        observe()
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
                prefs?.getString(DefaultPlaybackKeys.VIDEO, DefaultPlaybackAction.PLAY.name)
            }.getOrNull()
            if (name != null) {
                _state.update {
                    it.copy(defaultPlaybackAction = DefaultPlaybackAction.fromName(name).name)
                }
            }
        }
    }
    fun setQuery(q: String) {
        _state.update { it.copy(query = q, loading = true) }
        rebuildQuery()
        observe()
    }

    fun setViewMode(mode: ViewMode) = _state.update { it.copy(viewMode = mode) }

    fun setSort(mode: SortMode) {
        _state.update { it.copy(sortMode = mode) }
        rebuildQuery()
        publish(raw)
    }

    fun toggleSortDesc() {
        _state.update { it.copy(sortDesc = !it.sortDesc) }
        rebuildQuery()
        publish(raw)
    }

    fun setSortDesc(desc: Boolean) {
        _state.update { it.copy(sortDesc = desc) }
        rebuildQuery()
        publish(raw)
    }

    fun toggleOnlyFavorites() {
        _state.update { it.copy(onlyFavorites = !it.onlyFavorites, loading = true) }
        rebuildQuery()
        observe()
        if (_state.value.groupingMode.isGrouped()) observeGroups()
    }

    fun setOnlyFavorites(only: Boolean) {
        _state.update { it.copy(onlyFavorites = only, loading = true) }
        rebuildQuery()
        observe()
        if (_state.value.groupingMode.isGrouped()) observeGroups()
    }

    fun setGroupingMode(mode: VideoGroupingMode) {
        _state.update {
            it.copy(
                groupingMode = mode,
                containerId = null,
                containerKind = ContainerKind.NONE,
                containerTitle = null,
                groups = if (mode == VideoGroupingMode.NONE) emptyList() else it.groups,
                loading = true,
            )
        }
        rebuildQuery()
        pagingEpoch.update { it + 1 }
        if (mode.isGrouped()) {
            observeGroups()
            job?.cancel()
            _state.update { it.copy(items = emptyList(), count = 0, loading = false) }
        } else {
            groupsJob?.cancel()
            _state.update { it.copy(groups = emptyList()) }
            observe()
        }
    }

    fun openContainer(folder: MediaFolder) {
        val kind = when (folder.kind) {
            FolderKind.VIDEO_GROUP -> ContainerKind.VIDEO_GROUP
            else -> ContainerKind.FOLDER
        }
        _state.update {
            it.copy(
                containerId = folder.id,
                containerKind = kind,
                containerTitle = folder.title,
                groupingMode = VideoGroupingMode.NONE,
                groups = emptyList(),
                loading = true,
            )
        }
        groupsJob?.cancel()
        rebuildQuery()
        observe()
    }

    fun closeContainer() {
        _state.update {
            it.copy(
                containerId = null,
                containerKind = ContainerKind.NONE,
                containerTitle = null,
                loading = true,
            )
        }
        rebuildQuery()
        observe()
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
        val action = DefaultPlaybackAction.fromName(_state.value.defaultPlaybackAction)
        applyDefaultPlayback(action, item, list, player)
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

    fun appendSelection() {
        val selected = _state.value.items.filter { it.uri in _state.value.selection }
        if (selected.isNotEmpty()) player.append(selected)
    }

    fun favoriteSelection(favorite: Boolean = true) = launchIo {
        val selected = _state.value.items.filter { it.uri in _state.value.selection }
        selected.forEach { item ->
            runCatching { repo.setFavorite(item.id, favorite) }
        }
    }

    fun setFavorite(item: MediaItem, favorite: Boolean) = launchIo {
        runCatching { repo.setFavorite(item.id, favorite) }
    }

    fun handleCtx(item: MediaItem, opt: ContextOption) {
        when (opt) {
            ContextOption.CTX_PLAY -> play(item)
            ContextOption.CTX_PLAY_NEXT -> playNext(item)
            ContextOption.CTX_APPEND -> append(item)
            ContextOption.CTX_PLAY_ALL -> playAll()
            ContextOption.CTX_FAV_ADD -> setFavorite(item, true)
            ContextOption.CTX_FAV_REMOVE -> setFavorite(item, false)
            ContextOption.CTX_MARK_AS_PLAYED -> launchIo {
                runCatching { repo.markAsPlayed(item.id) }
            }
            ContextOption.CTX_MARK_AS_UNPLAYED -> launchIo {
                runCatching { repo.markAsUnplayed(item.id) }
            }
            ContextOption.CTX_STOP_AFTER_THIS -> {
                play(item)
                player.setStopAfterThis()
            }
            in HOST_CONTEXT_OPTIONS -> Unit // Hosted via ShellHostCallbacks from the shell.
            else -> play(item)
        }
    }

    fun setDefaultPlaybackAction(name: String) {
        val action = DefaultPlaybackAction.fromName(name)
        _state.update { it.copy(defaultPlaybackAction = action.name) }
        persistString(DefaultPlaybackKeys.VIDEO, action.name)
        launchIo {
            runCatching { prefs?.putString(DefaultPlaybackKeys.VIDEO, action.name) }
        }
    }

    fun setShowTrackNumbers(show: Boolean) {
        _state.update { it.copy(showTrackNumbers = show) }
        persistBool(ALBUMS_SHOW_TRACK_NUMBER, show)
        launchIo {
            runCatching { prefs?.let { VlcSettings.updateBoolean(it, ALBUMS_SHOW_TRACK_NUMBER, show) } }
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        if (_state.value.groupingMode.isGrouped() &&
            _state.value.containerKind == ContainerKind.NONE
        ) {
            observeGroups()
        } else {
            observe()
        }
        rebuildQuery()
    }

    fun rescan() = launch {
        if (runCatching { repo.rescan() }.getOrDefault(false)) refresh()
    }
    private fun rebuildQuery() {
        val s = _state.value
        queryFlow.value = MediaQuery(
            type = MediaType.VIDEO,
            query = s.query,
            sort = s.sortMode.toMediaSort(),
            desc = s.sortDesc,
            onlyFavorites = s.onlyFavorites,
            containerId = s.containerId,
            containerKind = s.containerKind,
        )
    }

    private fun observe() {
        job?.cancel()
        job = launch {
            val s = _state.value
            val q = s.query.trim()
            val flow = when {
                s.containerKind == ContainerKind.FOLDER && s.containerId != null ->
                    repo.observeFolderMedia(s.containerId)
                s.containerKind == ContainerKind.VIDEO_GROUP && s.containerId != null ->
                    repo.observeVideoGroupMedia(s.containerId)
                q.isNotEmpty() -> repo.search(q, MediaType.VIDEO)
                else -> repo.observeMedia(MediaType.VIDEO)
            }
            flow.catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { list ->
                    var filtered = list
                    if (s.onlyFavorites) filtered = filtered.filter { it.isFavorite }
                    raw = filtered
                    publish(filtered)
                }
        }
    }

    private fun observeGroups() {
        groupsJob?.cancel()
        groupsJob = launch {
            val s = _state.value
            val flow = when (s.groupingMode) {
                VideoGroupingMode.FOLDER -> repo.observeVideoFolders(
                    sort = s.sortMode.toMediaSort(),
                    desc = s.sortDesc,
                    onlyFavorites = s.onlyFavorites,
                )
                else -> repo.observeVideoGroups(
                    sort = s.sortMode.toMediaSort(),
                    desc = s.sortDesc,
                    onlyFavorites = s.onlyFavorites,
                )
            }
            flow.catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { groups ->
                    val q = _state.value.query.trim()
                    val filtered = if (q.isEmpty()) groups
                    else groups.filter { it.title.contains(q, ignoreCase = true) }
                    _state.update {
                        it.copy(
                            groups = filtered,
                            items = emptyList(),
                            count = filtered.size,
                            loading = false,
                            error = null,
                            sections = emptyList(),
                        )
                    }
                }
        }
    }

    private fun publish(list: List<MediaItem>) {
        val sorted = sortItems(list, _state.value.sortMode, _state.value.sortDesc)
        _state.update {
            it.copy(items = sorted, count = sorted.size, loading = false, error = null, sections = emptyList())
        }
    }
}
