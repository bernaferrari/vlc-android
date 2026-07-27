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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.platform.platformCapabilities
import org.videolan.vlc.repository.AudioEntity
import org.videolan.vlc.repository.AudioEntityKind
import org.videolan.vlc.repository.ContainerKind
import org.videolan.vlc.repository.MediaQuery
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.util.ContextOption
import org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER
import org.videolan.tools.BROWSER_SHOW_HIDDEN_FILES
import org.videolan.tools.BROWSER_SHOW_ONLY_MULTIMEDIA
import org.videolan.tools.KEY_ARTISTS_SHOW_ALL
import org.videolan.tools.KEY_AUDIO_SHOW_TRACK_NUMBERS
import org.videolan.tools.SettingsWriteBridge
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.util.DefaultPlaybackAction
import org.videolan.vlc.util.DefaultPlaybackKeys
import kotlin.random.Random

enum class MainTab {
    VIDEO, AUDIO, BROWSER, PLAYLISTS, MORE
}

enum class ViewMode { LIST, GRID }

/** UI sort picker — maps onto [MediaSort] for repository queries. */
enum class SortMode { TITLE, ARTIST, ALBUM, DURATION, RECENT, FILENAME }

enum class AudioSection { TRACKS, ARTISTS, ALBUMS, GENRES, PLAYLISTS }

enum class VideoGroupingMode { NONE, NAME, FOLDER }

data class MediaListUiState(
    val items: List<MediaItem> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val count: Int = 0,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortMode: SortMode = SortMode.TITLE,
    val sortDesc: Boolean = false,
    val onlyFavorites: Boolean = false,
    val selection: Set<String> = emptySet(), // by uri
    val sections: List<Pair<String, List<MediaItem>>> = emptyList(),
    val defaultPlaybackAction: String = DefaultPlaybackAction.PLAY.name,
    val containerId: Long? = null,
    val containerTitle: String? = null,
    val containerKind: ContainerKind = ContainerKind.NONE,
    val groups: List<MediaFolder> = emptyList(),
    val groupingMode: VideoGroupingMode = VideoGroupingMode.NONE,
    /** Typed artists/albums/genres when the platform exposes them. */
    val audioEntities: List<AudioEntity> = emptyList(),
    /** Non-null while drilled into an artist/album/genre. */
    val openedEntityTitle: String? = null,
    val showAllArtists: Boolean = false,
    val showTrackNumbers: Boolean = true,
    /** Whether the current repository can re-enumerate its local media source. */
    val supportsRescan: Boolean = false,
)

data class BrowserUiState(
    val favorites: List<MediaItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val networkRoots: List<MediaFolder> = emptyList(),
    val media: List<MediaItem> = emptyList(),
    val currentFolder: MediaFolder? = null,
    val stack: List<MediaFolder> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val selection: Set<String> = emptySet(),
    val defaultPlaybackAction: String = DefaultPlaybackAction.PLAY.name,
    val showHiddenFiles: Boolean = false,
    val showOnlyMultimedia: Boolean = false,
)

data class PlaylistsUiState(
    val playlists: List<PlaylistInfo> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val openPlaylistId: Long? = null,
    val openPlaylistName: String? = null,
    val openItems: List<MediaItem> = emptyList(),
    val onlyFavorites: Boolean = false,
    val sortDesc: Boolean = false,
    val query: String = "",
    val selection: Set<Long> = emptySet(),
    val viewMode: ViewMode = ViewMode.LIST,
    val defaultPlaybackAction: String = DefaultPlaybackAction.PLAY.name,
)

private fun mediaRepo() = runCatching { VlcKoin.get().get<MediaRepository>() }
    .getOrElse { error("MediaRepository unavailable") }

private fun playlistRepo() = runCatching { VlcKoin.get().get<PlaylistRepository>() }
    .getOrElse { error("PlaylistRepository unavailable") }

private fun playback() = runCatching { PlaybackController.get() }
    .getOrElse { error("PlaybackController unavailable") }

internal fun SortMode.toMediaSort(): MediaSort = when (this) {
    SortMode.TITLE -> MediaSort.TITLE
    SortMode.ARTIST -> MediaSort.ARTIST
    SortMode.ALBUM -> MediaSort.ALBUM
    SortMode.DURATION -> MediaSort.DURATION
    SortMode.RECENT -> MediaSort.RECENT
    SortMode.FILENAME -> MediaSort.FILENAME
}

internal fun MediaSort.toSortMode(): SortMode = when (this) {
    MediaSort.ARTIST -> SortMode.ARTIST
    MediaSort.ALBUM -> SortMode.ALBUM
    MediaSort.DURATION -> SortMode.DURATION
    MediaSort.RECENT, MediaSort.INSERTION_DATE -> SortMode.RECENT
    MediaSort.FILENAME -> SortMode.FILENAME
    else -> SortMode.TITLE
}

private fun AudioEntityKind.uriScheme(): String = when (this) {
    AudioEntityKind.ARTIST -> "artist"
    AudioEntityKind.ALBUM -> "album"
    AudioEntityKind.GENRE -> "genre"
}

private fun AudioEntity.toSyntheticItem(): MediaItem {
    val scheme = kind.uriScheme()
    val desc = subtitle?.takeIf { it.isNotBlank() }
        ?: buildString {
            if (trackCount > 0) append("$trackCount tracks")
            if (albumCount > 0) {
                if (isNotEmpty()) append(" · ")
                append("$albumCount albums")
            }
        }.ifBlank { null }
    return MediaItem(
        id = id,
        title = title,
        uri = "$scheme://$id",
        type = MediaType.DIR,
        artworkUri = artworkUri,
        isFavorite = isFavorite,
        description = desc,
        artist = if (kind == AudioEntityKind.ARTIST) title else subtitle,
        album = if (kind == AudioEntityKind.ALBUM) title else null,
        genre = if (kind == AudioEntityKind.GENRE) title else null,
    )
}

private fun String.isAudioEntityUri(): Boolean =
    startsWith("artist://") || startsWith("album://") || startsWith("genre://")

private fun parseAudioEntityUri(uri: String): Pair<AudioEntityKind, Long>? {
    val kind = when {
        uri.startsWith("artist://") -> AudioEntityKind.ARTIST
        uri.startsWith("album://") -> AudioEntityKind.ALBUM
        uri.startsWith("genre://") -> AudioEntityKind.GENRE
        else -> return null
    }
    val id = uri.substringAfter("://").toLongOrNull() ?: return null
    return kind to id
}

private fun sortItems(items: List<MediaItem>, mode: SortMode, desc: Boolean): List<MediaItem> {
    val sorted = when (mode) {
        SortMode.TITLE -> items.sortedBy { it.displayTitle.lowercase() }
        SortMode.ARTIST -> items.sortedWith(
            compareBy({ it.artist?.lowercase().orEmpty() }, { it.displayTitle.lowercase() }),
        )
        SortMode.ALBUM -> items.sortedWith(
            compareBy({ it.album?.lowercase().orEmpty() }, { it.trackNumber }, { it.displayTitle.lowercase() }),
        )
        SortMode.DURATION -> items.sortedBy { it.duration }
        SortMode.RECENT -> items.sortedBy { it.lastPlayed }
        SortMode.FILENAME -> items.sortedBy { (it.fileName ?: it.displayTitle).lowercase() }
    }
    return if (desc) sorted.reversed() else sorted
}

private fun sectionByArtist(items: List<MediaItem>): List<Pair<String, List<MediaItem>>> =
    items.groupBy { it.artist?.takeIf { a -> a.isNotBlank() } ?: "Unknown artist" }
        .toList()
        .sortedBy { it.first.lowercase() }

private fun sectionByAlbum(items: List<MediaItem>): List<Pair<String, List<MediaItem>>> =
    items.groupBy { it.album?.takeIf { a -> a.isNotBlank() } ?: "Unknown album" }
        .toList()
        .sortedBy { it.first.lowercase() }

private fun sectionByGenre(items: List<MediaItem>): List<Pair<String, List<MediaItem>>> =
    items.groupBy { it.genre?.takeIf { g -> g.isNotBlank() } ?: "Unknown genre" }
        .toList()
        .sortedBy { it.first.lowercase() }

private fun prefsOrNull(): VlcPreferences? =
    runCatching { VlcKoin.get().get<VlcPreferences>() }.getOrNull()

private fun persistBool(key: String, value: Boolean) {
    SettingsWriteBridge.onBoolean?.invoke(key, value)
}

private fun persistString(key: String, value: String) {
    SettingsWriteBridge.onString?.invoke(key, value)
}

/** Apply the configured primary-click playback action. */
internal fun applyDefaultPlayback(
    action: DefaultPlaybackAction,
    item: MediaItem,
    queue: List<MediaItem>,
    player: PlaybackController,
) {
    val list = queue.ifEmpty { listOf(item) }
    when (action) {
        DefaultPlaybackAction.PLAY -> player.play(item, list)
        DefaultPlaybackAction.PLAY_ALL -> {
            val idx = list.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
            player.playFromIndex(list, idx)
        }
        DefaultPlaybackAction.ADD_TO_QUEUE -> player.append(listOf(item))
        DefaultPlaybackAction.INSERT_NEXT -> player.insertNext(listOf(item))
    }
}

private fun isUriBrowseTarget(folder: MediaFolder): Boolean {
    when (folder.kind) {
        FolderKind.NETWORK, FolderKind.STORAGE -> return true
        FolderKind.MEDIA_FOLDER -> {
            val uri = folder.uri.ifBlank { folder.path }
            if (uri.isNotBlank()) return true
        }
        else -> Unit
    }
    val uri = folder.uri.ifBlank { folder.path }
    if (uri.isBlank()) return false
    val scheme = uri.substringBefore(':', missingDelimiterValue = "").lowercase()
    return scheme in URI_BROWSE_SCHEMES
}

private val URI_BROWSE_SCHEMES = setOf(
    "file", "content", "otg",
    "smb", "ftp", "ftps", "sftp", "upnp", "nfs", "http", "https", "rtp", "rtsp",
)

private val HOST_CONTEXT_OPTIONS = setOf(
    ContextOption.CTX_DELETE,
    ContextOption.CTX_RENAME,
    ContextOption.CTX_INFORMATION,
    ContextOption.CTX_SHARE,
    ContextOption.CTX_DOWNLOAD_SUBTITLES,
    ContextOption.CTX_ADD_SHORTCUT,
    ContextOption.CTX_SET_RINGTONE,
    ContextOption.CTX_BAN_FOLDER,
    ContextOption.CTX_ADD_TO_PLAYLIST,
)

class VideoListViewModel(
    private val repo: MediaRepository = mediaRepo(),
    private val player: PlaybackController = playback(),
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

class AudioListViewModel(
    private val repo: MediaRepository = mediaRepo(),
    private val playlists: PlaylistRepository = playlistRepo(),
    private val player: PlaybackController = playback(),
    private val prefs: VlcPreferences? = prefsOrNull(),
    initialDefaultAction: String? = null,
) : VlcViewModel() {
    private val _state = MutableStateFlow(
        MediaListUiState(
            showAllArtists = runCatching {
                // Prefer legacy include-missing as artists-show-all stand-in until hydrated.
                false
            }.getOrDefault(false),
            showTrackNumbers = runCatching {
                VlcSettings.audioShowTrackNumbers.value || VlcSettings.showTrackNumber.value
            }.getOrDefault(true),
            supportsRescan = repo.supportsRescan,
        ),
    )
    val state: StateFlow<MediaListUiState> = _state.asStateFlow()
    private val _section = MutableStateFlow(AudioSection.TRACKS)
    val section: StateFlow<AudioSection> = _section.asStateFlow()

    private val queryFlow = MutableStateFlow(
        MediaQuery(type = MediaType.AUDIO, sort = MediaSort.TITLE),
    )

    val pagingFlow: Flow<PagingData<MediaItem>> = combine(queryFlow, _section) { q, section ->
        q to section
    }.flatMapLatest { (q, section) ->
        if (section == AudioSection.TRACKS) repo.observeMediaPaged(q)
        else flowOf(PagingData.empty())
    }.cachedIn(viewModelScope)

    private var raw: List<MediaItem> = emptyList()
    private var playlistRaw: List<PlaylistInfo> = emptyList()
    private var entityRaw: List<AudioEntity> = emptyList()
    private var openedEntity: AudioEntity? = null
    private var job: Job? = null
    private var playlistJob: Job? = null
    private var entityJob: Job? = null
    private var entityTracksJob: Job? = null

    init {
        loadDisplayPrefs(initialDefaultAction)
        launch {
            combine(VlcSettings.audioShowTrackNumbers, VlcSettings.showTrackNumber) { audio, album ->
                audio || album
            }.collectLatest { show -> _state.update { it.copy(showTrackNumbers = show) } }
        }
        observe()
        observePlaylists()
    }

    private fun loadDisplayPrefs(injectedDefault: String?) {
        launchIo {
            val showAll = runCatching {
                prefs?.getBoolean(KEY_ARTISTS_SHOW_ALL, false)
            }.getOrNull()
            val trackNums = runCatching {
                prefs?.getBoolean(KEY_AUDIO_SHOW_TRACK_NUMBERS, VlcSettings.audioShowTrackNumbers.value)
            }.getOrNull()
            val actionName = injectedDefault ?: runCatching {
                prefs?.getString(DefaultPlaybackKeys.TRACK, DefaultPlaybackAction.PLAY.name)
            }.getOrNull()
            _state.update { st ->
                st.copy(
                    showAllArtists = showAll ?: st.showAllArtists,
                    showTrackNumbers = trackNums ?: st.showTrackNumbers,
                    defaultPlaybackAction = actionName?.let {
                        DefaultPlaybackAction.fromName(it).name
                    } ?: st.defaultPlaybackAction,
                )
            }
        }
    }

    fun setSection(section: AudioSection) {
        clearOpenedEntity(cancelJobs = true)
        _section.value = section
        rebuildQuery()
        when (section) {
            AudioSection.TRACKS -> {
                entityJob?.cancel()
                entityRaw = emptyList()
                observe()
            }
            AudioSection.PLAYLISTS -> {
                entityJob?.cancel()
                entityRaw = emptyList()
                job?.cancel()
                observePlaylists()
                publish(raw)
            }
            AudioSection.ARTISTS, AudioSection.ALBUMS, AudioSection.GENRES -> {
                job?.cancel()
                observeEntities()
            }
        }
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q, loading = true) }
        rebuildQuery()
        when {
            openedEntity != null -> observeEntityTracks(openedEntity!!)
            _section.value == AudioSection.PLAYLISTS -> observePlaylists()
            isEntitySection(_section.value) -> observeEntities()
            else -> {
                observe()
                observePlaylists()
            }
        }
    }

    fun setViewMode(mode: ViewMode) = _state.update { it.copy(viewMode = mode) }

    fun setSort(mode: SortMode) {
        _state.update { it.copy(sortMode = mode) }
        rebuildQuery()
        refreshCurrent()
    }

    fun toggleSortDesc() {
        _state.update { it.copy(sortDesc = !it.sortDesc) }
        rebuildQuery()
        refreshCurrent()
    }

    fun setSortDesc(desc: Boolean) {
        _state.update { it.copy(sortDesc = desc) }
        rebuildQuery()
        refreshCurrent()
    }

    fun toggleOnlyFavorites() {
        _state.update { it.copy(onlyFavorites = !it.onlyFavorites, loading = true) }
        rebuildQuery()
        refreshCurrent()
    }

    fun setOnlyFavorites(only: Boolean) {
        _state.update { it.copy(onlyFavorites = only, loading = true) }
        rebuildQuery()
        refreshCurrent()
    }

    fun toggleSelect(item: MediaItem) {
        _state.update {
            val sel = it.selection.toMutableSet()
            if (!sel.add(item.uri)) sel.remove(item.uri)
            it.copy(selection = sel)
        }
    }

    fun selectAll() = _state.update { it.copy(selection = _state.value.items.map { m -> m.uri }.toSet()) }
    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun openAudioEntity(entity: AudioEntity) {
        openedEntity = entity
        _state.update {
            it.copy(
                openedEntityTitle = entity.title,
                containerTitle = entity.title,
                containerId = entity.id,
                loading = true,
                selection = emptySet(),
                sections = emptyList(),
                audioEntities = emptyList(),
            )
        }
        observeEntityTracks(entity)
    }

    fun openAudioEntityFromItem(item: MediaItem) {
        val parsed = parseAudioEntityUri(item.uri) ?: return
        val (kind, id) = parsed
        val known = entityRaw.firstOrNull { it.kind == kind && it.id == id }
        openAudioEntity(
            known ?: AudioEntity(
                id = id,
                title = item.title,
                kind = kind,
                artworkUri = item.artworkUri,
                subtitle = item.description,
                isFavorite = item.isFavorite,
            ),
        )
    }

    fun closeEntity() {
        val wasOpen = openedEntity != null
        clearOpenedEntity(cancelJobs = true)
        if (!wasOpen) return
        _state.update { it.copy(loading = true, selection = emptySet()) }
        when (_section.value) {
            AudioSection.ARTISTS, AudioSection.ALBUMS, AudioSection.GENRES -> observeEntities()
            AudioSection.PLAYLISTS -> {
                observePlaylists()
                publish(raw)
            }
            AudioSection.TRACKS -> observe()
        }
    }

    fun play(item: MediaItem) {
        if (item.uri.isAudioEntityUri()) {
            playAudioEntity(item)
            return
        }
        if (item.uri.startsWith("playlist://")) {
            val id = item.id
            launchIo {
                val pl = playlists.getPlaylist(id) ?: return@launchIo
                if (pl.items.isNotEmpty()) {
                    val action = DefaultPlaybackAction.fromName(_state.value.defaultPlaybackAction)
                    applyDefaultPlayback(action, pl.items.first(), pl.items, player)
                }
            }
            return
        }
        val list = _state.value.items.filterNot {
            it.uri.startsWith("playlist://") || it.uri.isAudioEntityUri()
        }
        val action = DefaultPlaybackAction.fromName(_state.value.defaultPlaybackAction)
        applyDefaultPlayback(action, item, list, player)
    }

    fun playAll() {
        val list = _state.value.items.filterNot {
            it.uri.startsWith("playlist://") || it.uri.isAudioEntityUri()
        }
        if (list.isNotEmpty()) player.playFromIndex(list, 0)
    }

    fun shuffleAll() {
        val list = _state.value.items.filterNot {
            it.uri.startsWith("playlist://") || it.uri.isAudioEntityUri()
        }
        if (list.isEmpty()) return
        player.setShuffle(true)
        val start = if (list.size == 1) 0 else Random.nextInt(list.size)
        player.playFromIndex(list, start)
    }

    fun playNext(item: MediaItem) {
        if (item.uri.startsWith("playlist://") || item.uri.isAudioEntityUri()) return
        player.insertNext(listOf(item))
    }

    fun append(item: MediaItem) {
        if (item.uri.startsWith("playlist://") || item.uri.isAudioEntityUri()) return
        player.append(listOf(item))
    }

    fun playSelection() {
        val selected = _state.value.items.filter {
            it.uri in _state.value.selection &&
                !it.uri.startsWith("playlist://") &&
                !it.uri.isAudioEntityUri()
        }
        if (selected.isNotEmpty()) player.playFromIndex(selected, 0)
    }

    fun appendSelection() {
        val selected = _state.value.items.filter {
            it.uri in _state.value.selection &&
                !it.uri.startsWith("playlist://") &&
                !it.uri.isAudioEntityUri()
        }
        if (selected.isNotEmpty()) player.append(selected)
    }

    fun favoriteSelection(favorite: Boolean = true) = launchIo {
        val selected = _state.value.items.filter { it.uri in _state.value.selection }
        selected.forEach { item ->
            if (item.uri.startsWith("playlist://")) {
                runCatching { playlists.setFavorite(item.id, favorite) }
            } else if (!item.uri.isAudioEntityUri()) {
                runCatching { repo.setFavorite(item.id, favorite) }
            }
        }
    }

    fun setFavorite(item: MediaItem, favorite: Boolean) = launchIo {
        if (item.uri.startsWith("playlist://")) {
            runCatching { playlists.setFavorite(item.id, favorite) }
        } else if (!item.uri.isAudioEntityUri()) {
            runCatching { repo.setFavorite(item.id, favorite) }
        }
    }

    /** Open album entity matching [item.album] name when available. */
    fun goToAlbum(item: MediaItem) {
        val albumName = item.album?.takeIf { it.isNotBlank() } ?: return
        val cached = entityRaw.firstOrNull {
            it.kind == AudioEntityKind.ALBUM && it.title.equals(albumName, ignoreCase = true)
        }
        if (cached != null) {
            openAudioEntity(cached)
            return
        }
        launchIo {
            val albums = runCatching {
                repo.observeAudioEntities(kind = AudioEntityKind.ALBUM).first()
            }.getOrNull().orEmpty()
            val found = albums.firstOrNull { it.title.equals(albumName, ignoreCase = true) }
                ?: AudioEntity(
                    id = item.id,
                    title = albumName,
                    kind = AudioEntityKind.ALBUM,
                    artworkUri = item.artworkUri,
                    subtitle = item.artist,
                )
            openAudioEntity(found)
        }
    }

    fun handleCtx(item: MediaItem, opt: ContextOption) {
        when (opt) {
            ContextOption.CTX_PLAY -> play(item)
            ContextOption.CTX_PLAY_NEXT -> playNext(item)
            ContextOption.CTX_APPEND -> append(item)
            ContextOption.CTX_PLAY_ALL -> playAll()
            ContextOption.CTX_FAV_ADD -> setFavorite(item, true)
            ContextOption.CTX_FAV_REMOVE -> setFavorite(item, false)
            ContextOption.CTX_GO_TO_ALBUM -> goToAlbum(item)
            ContextOption.CTX_MARK_AS_PLAYED -> launchIo {
                runCatching { repo.markAsPlayed(item.id) }
            }
            ContextOption.CTX_MARK_AS_UNPLAYED -> launchIo {
                runCatching { repo.markAsUnplayed(item.id) }
            }
            in HOST_CONTEXT_OPTIONS -> Unit // Hosted via ShellHostCallbacks from the shell.
            else -> play(item)
        }
    }

    fun setDefaultPlaybackAction(name: String) {
        val action = DefaultPlaybackAction.fromName(name)
        _state.update { it.copy(defaultPlaybackAction = action.name) }
        persistString(DefaultPlaybackKeys.TRACK, action.name)
        launchIo {
            runCatching { prefs?.putString(DefaultPlaybackKeys.TRACK, action.name) }
        }
    }

    fun setShowAllArtists(show: Boolean) {
        _state.update { it.copy(showAllArtists = show) }
        persistBool(KEY_ARTISTS_SHOW_ALL, show)
        launchIo {
            runCatching { prefs?.putBoolean(KEY_ARTISTS_SHOW_ALL, show) }
        }
        if (_section.value == AudioSection.ARTISTS && openedEntity == null) {
            observeEntities()
        }
    }

    fun setShowTrackNumbers(show: Boolean) {
        _state.update { it.copy(showTrackNumbers = show) }
        persistBool(KEY_AUDIO_SHOW_TRACK_NUMBERS, show)
        persistBool(ALBUMS_SHOW_TRACK_NUMBER, show)
        launchIo {
            runCatching {
                prefs?.let {
                    VlcSettings.updateBoolean(it, KEY_AUDIO_SHOW_TRACK_NUMBERS, show)
                    VlcSettings.updateBoolean(it, ALBUMS_SHOW_TRACK_NUMBER, show)
                }
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        rebuildQuery()
        refreshCurrent()
    }

    fun rescan() = launch {
        if (runCatching { repo.rescan() }.getOrDefault(false)) refresh()
    }

    private fun refreshCurrent() {
        when {
            openedEntity != null -> observeEntityTracks(openedEntity!!)
            _section.value == AudioSection.PLAYLISTS -> {
                observePlaylists()
                publish(raw)
            }
            isEntitySection(_section.value) -> observeEntities()
            else -> {
                observe()
                observePlaylists()
            }
        }
    }

    private fun rebuildQuery() {
        val s = _state.value
        queryFlow.value = MediaQuery(
            type = MediaType.AUDIO,
            query = s.query,
            sort = s.sortMode.toMediaSort(),
            desc = s.sortDesc,
            onlyFavorites = s.onlyFavorites,
        )
    }

    private fun isEntitySection(section: AudioSection): Boolean =
        section == AudioSection.ARTISTS ||
            section == AudioSection.ALBUMS ||
            section == AudioSection.GENRES

    private fun sectionKind(section: AudioSection): AudioEntityKind? = when (section) {
        AudioSection.ARTISTS -> AudioEntityKind.ARTIST
        AudioSection.ALBUMS -> AudioEntityKind.ALBUM
        AudioSection.GENRES -> AudioEntityKind.GENRE
        else -> null
    }

    private fun clearOpenedEntity(cancelJobs: Boolean) {
        openedEntity = null
        if (cancelJobs) entityTracksJob?.cancel()
        _state.update {
            it.copy(
                openedEntityTitle = null,
                containerTitle = null,
                containerId = null,
                containerKind = ContainerKind.NONE,
            )
        }
    }

    private fun playAudioEntity(item: MediaItem) {
        val parsed = parseAudioEntityUri(item.uri) ?: return
        val (kind, id) = parsed
        val entity = entityRaw.firstOrNull { it.kind == kind && it.id == id }
            ?: AudioEntity(id = id, title = item.title, kind = kind, artworkUri = item.artworkUri)
        launchIo {
            val s = _state.value
            val tracks = runCatching {
                repo.observeAudioEntityTracks(
                    kind = entity.kind,
                    entityId = entity.id,
                    sort = s.sortMode.toMediaSort(),
                    desc = s.sortDesc,
                    onlyFavorites = s.onlyFavorites,
                ).first()
            }.getOrDefault(emptyList())
            if (tracks.isNotEmpty()) player.playFromIndex(tracks, 0)
        }
    }

    private fun observe() {
        job?.cancel()
        job = launch {
            val q = _state.value.query.trim()
            val flow = if (q.isEmpty()) repo.observeMedia(MediaType.AUDIO) else repo.search(q, MediaType.AUDIO)
            flow.catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { list ->
                    var filtered = list
                    if (_state.value.onlyFavorites) filtered = filtered.filter { it.isFavorite }
                    raw = filtered
                    if (openedEntity == null && !isEntitySection(_section.value)) {
                        publish(filtered)
                    } else if (openedEntity == null && isEntitySection(_section.value) && entityRaw.isEmpty()) {
                        // Keep track fallback available for iOS when entities stay empty.
                        publish(filtered)
                    }
                }
        }
    }

    private fun observePlaylists() {
        playlistJob?.cancel()
        playlistJob = launch {
            playlists.observePlaylists()
                .catch { }
                .collectLatest { list ->
                    var filtered = list
                    if (_state.value.onlyFavorites) filtered = filtered.filter { it.isFavorite }
                    val q = _state.value.query.trim()
                    if (q.isNotEmpty()) {
                        filtered = filtered.filter { it.name.contains(q, ignoreCase = true) }
                    }
                    playlistRaw = filtered
                    if (_section.value == AudioSection.PLAYLISTS && openedEntity == null) publish(raw)
                }
        }
    }

    private fun observeEntities() {
        val kind = sectionKind(_section.value) ?: return
        entityJob?.cancel()
        entityTracksJob?.cancel()
        entityJob = launch {
            val s = _state.value
            repo.observeAudioEntities(
                kind = kind,
                sort = s.sortMode.toMediaSort(),
                desc = s.sortDesc,
                onlyFavorites = s.onlyFavorites,
                query = s.query,
            ).catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { entities ->
                    entityRaw = entities
                    if (openedEntity != null) return@collectLatest
                    if (entities.isNotEmpty()) {
                        publishEntities(entities)
                    } else {
                        // Fallback: group tracks client-side (iOS / empty backends).
                        if (raw.isEmpty()) {
                            // Ensure track snapshot is loading for section fallback.
                            observe()
                        } else {
                            publish(raw)
                        }
                    }
                }
        }
    }

    private fun observeEntityTracks(entity: AudioEntity) {
        entityTracksJob?.cancel()
        entityTracksJob = launch {
            val s = _state.value
            repo.observeAudioEntityTracks(
                kind = entity.kind,
                entityId = entity.id,
                sort = s.sortMode.toMediaSort(),
                desc = s.sortDesc,
                onlyFavorites = s.onlyFavorites,
            ).catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { tracks ->
                    var filtered = tracks
                    val q = _state.value.query.trim()
                    if (q.isNotEmpty()) {
                        filtered = filtered.filter {
                            it.displayTitle.contains(q, ignoreCase = true) ||
                                it.artist.orEmpty().contains(q, ignoreCase = true) ||
                                it.album.orEmpty().contains(q, ignoreCase = true)
                        }
                    }
                    val sorted = sortItems(filtered, _state.value.sortMode, _state.value.sortDesc)
                    _state.update {
                        it.copy(
                            items = sorted,
                            count = sorted.size,
                            loading = false,
                            error = null,
                            sections = emptyList(),
                            audioEntities = emptyList(),
                            openedEntityTitle = entity.title,
                            containerTitle = entity.title,
                            containerId = entity.id,
                        )
                    }
                }
        }
    }

    private fun publishEntities(entities: List<AudioEntity>) {
        val items = entities.map { it.toSyntheticItem() }
        _state.update {
            it.copy(
                items = items,
                count = items.size,
                loading = false,
                error = null,
                sections = emptyList(),
                audioEntities = entities,
                openedEntityTitle = null,
                containerTitle = null,
                containerId = null,
            )
        }
    }

    private fun publish(list: List<MediaItem>) {
        if (openedEntity != null) return
        val sorted = sortItems(list, _state.value.sortMode, _state.value.sortDesc)
        when (_section.value) {
            AudioSection.TRACKS -> {
                _state.update {
                    it.copy(
                        items = sorted,
                        count = sorted.size,
                        loading = false,
                        error = null,
                        sections = emptyList(),
                        audioEntities = emptyList(),
                        openedEntityTitle = null,
                        containerTitle = null,
                    )
                }
            }
            AudioSection.ARTISTS -> {
                if (entityRaw.isNotEmpty()) {
                    publishEntities(entityRaw)
                    return
                }
                val sections = sectionByArtist(sorted)
                _state.update {
                    it.copy(
                        items = sections.flatMap { s -> s.second },
                        count = sorted.size,
                        loading = false,
                        error = null,
                        sections = sections,
                        audioEntities = emptyList(),
                    )
                }
            }
            AudioSection.ALBUMS -> {
                if (entityRaw.isNotEmpty()) {
                    publishEntities(entityRaw)
                    return
                }
                val sections = sectionByAlbum(sorted)
                _state.update {
                    it.copy(
                        items = sections.flatMap { s -> s.second },
                        count = sorted.size,
                        loading = false,
                        error = null,
                        sections = sections,
                        audioEntities = emptyList(),
                    )
                }
            }
            AudioSection.GENRES -> {
                if (entityRaw.isNotEmpty()) {
                    publishEntities(entityRaw)
                    return
                }
                val sections = sectionByGenre(sorted)
                _state.update {
                    it.copy(
                        items = sections.flatMap { s -> s.second },
                        count = sorted.size,
                        loading = false,
                        error = null,
                        sections = sections,
                        audioEntities = emptyList(),
                    )
                }
            }
            AudioSection.PLAYLISTS -> {
                val items = playlistRaw.map { pl ->
                    MediaItem(
                        id = pl.id,
                        title = pl.name,
                        uri = "playlist://${pl.id}",
                        type = MediaType.DIR,
                        duration = pl.duration,
                        artworkUri = pl.artworkUri,
                        isFavorite = pl.isFavorite,
                        description = "${pl.itemCount} items",
                    )
                }
                _state.update {
                    it.copy(
                        items = items,
                        count = items.size,
                        loading = false,
                        error = null,
                        sections = emptyList(),
                        audioEntities = emptyList(),
                        openedEntityTitle = null,
                        containerTitle = null,
                    )
                }
            }
        }
    }
}

class BrowserViewModel(
    private val repo: MediaRepository = mediaRepo(),
    private val player: PlaybackController = playback(),
    private val prefs: VlcPreferences? = prefsOrNull(),
    private val capabilities: VlcPlatformCapabilities = platformCapabilities,
    initialDefaultAction: String? = null,
) : VlcViewModel() {
    private val _state = MutableStateFlow(
        BrowserUiState(
            showHiddenFiles = runCatching { VlcSettings.showHiddenFiles.value }.getOrDefault(false),
        ),
    )
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        loadPrefs(initialDefaultAction)
        openRoot()
        launch {
            VlcSettings.showHiddenFiles.collectLatest { show ->
                _state.update { it.copy(showHiddenFiles = show) }
                refreshCurrentUriListing()
            }
        }
        if (capabilities.networkBrowsing) launch {
            VlcSettings.browseNetwork.collectLatest { enabled ->
                val folder = _state.value.currentFolder
                if (!enabled && folder?.kind == FolderKind.NETWORK) {
                    openRoot()
                } else if (folder == null) {
                    observeRoot()
                }
            }
        }
    }

    private fun loadPrefs(injectedDefault: String?) {
        launchIo {
            val hidden = runCatching {
                prefs?.getBoolean(BROWSER_SHOW_HIDDEN_FILES, VlcSettings.showHiddenFiles.value)
            }.getOrNull()
            val multi = runCatching {
                prefs?.getBoolean(BROWSER_SHOW_ONLY_MULTIMEDIA, false)
            }.getOrNull()
            val actionName = injectedDefault ?: runCatching {
                prefs?.getString(DefaultPlaybackKeys.FILE, DefaultPlaybackAction.PLAY.name)
            }.getOrNull()
            _state.update { st ->
                st.copy(
                    showHiddenFiles = hidden ?: st.showHiddenFiles,
                    showOnlyMultimedia = multi ?: st.showOnlyMultimedia,
                    defaultPlaybackAction = actionName?.let {
                        DefaultPlaybackAction.fromName(it).name
                    } ?: st.defaultPlaybackAction,
                )
            }
        }
    }

    fun openRoot() {
        _state.update {
            it.copy(currentFolder = null, stack = emptyList(), loading = true, selection = emptySet())
        }
        observeRoot()
    }

    fun openFolder(folder: MediaFolder) {
        if (folder.kind == FolderKind.NETWORK && (!capabilities.networkBrowsing || !VlcSettings.browseNetwork.value)) return
        val stack = _state.value.stack + folder
        _state.update {
            it.copy(
                currentFolder = folder,
                stack = stack,
                loading = true,
                favorites = emptyList(),
                networkRoots = emptyList(),
                selection = emptySet(),
            )
        }
        if (isUriBrowseTarget(folder)) {
            browseUri(folder)
        } else {
            observeFolder(folder.id)
        }
    }

    /** Restores a saved Navigation 3 browser route without replaying each ancestor. */
    fun restoreFolderStack(folders: List<MediaFolder>) {
        val supportedFolders = if (capabilities.networkBrowsing && VlcSettings.browseNetwork.value) {
            folders
        } else {
            folders.filterNot { it.kind == FolderKind.NETWORK }
        }
        if (supportedFolders == _state.value.stack) return
        val folder = supportedFolders.lastOrNull()
        if (folder == null) {
            openRoot()
            return
        }
        _state.update {
            it.copy(
                currentFolder = folder,
                stack = supportedFolders,
                loading = true,
                favorites = emptyList(),
                networkRoots = emptyList(),
                selection = emptySet(),
            )
        }
        if (isUriBrowseTarget(folder)) {
            browseUri(folder)
        } else {
            observeFolder(folder.id)
        }
    }

    fun goUp(): Boolean {
        val stack = _state.value.stack
        if (stack.isEmpty()) return false
        val next = stack.dropLast(1)
        if (next.isEmpty()) {
            openRoot()
        } else {
            val folder = next.last()
            _state.update {
                it.copy(
                    currentFolder = folder,
                    stack = next,
                    loading = true,
                    favorites = emptyList(),
                    networkRoots = emptyList(),
                    selection = emptySet(),
                )
            }
            if (isUriBrowseTarget(folder)) {
                browseUri(folder)
            } else {
                observeFolder(folder.id)
            }
        }
        return true
    }

    fun play(item: MediaItem) {
        val list = _state.value.media
        val action = DefaultPlaybackAction.fromName(_state.value.defaultPlaybackAction)
        applyDefaultPlayback(action, item, list, player)
    }

    fun playNext(item: MediaItem) = player.insertNext(listOf(item))
    fun append(item: MediaItem) = player.append(listOf(item))

    fun toggleSelect(item: MediaItem) {
        _state.update {
            val sel = it.selection.toMutableSet()
            if (!sel.add(item.uri)) sel.remove(item.uri)
            it.copy(selection = sel)
        }
    }

    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun playSelection() {
        val selected = _state.value.media.filter { it.uri in _state.value.selection }
        if (selected.isNotEmpty()) player.playFromIndex(selected, 0)
    }

    fun appendSelection() {
        val selected = _state.value.media.filter { it.uri in _state.value.selection }
        if (selected.isNotEmpty()) player.append(selected)
    }

    fun setDefaultPlaybackAction(name: String) {
        val action = DefaultPlaybackAction.fromName(name)
        _state.update { it.copy(defaultPlaybackAction = action.name) }
        persistString(DefaultPlaybackKeys.FILE, action.name)
        launchIo {
            runCatching { prefs?.putString(DefaultPlaybackKeys.FILE, action.name) }
        }
    }

    fun setShowHiddenFiles(show: Boolean) {
        _state.update { it.copy(showHiddenFiles = show) }
        persistBool(BROWSER_SHOW_HIDDEN_FILES, show)
        launchIo {
            runCatching { prefs?.let { VlcSettings.updateBoolean(it, BROWSER_SHOW_HIDDEN_FILES, show) } }
        }
        refreshCurrentUriListing()
    }

    fun setShowOnlyMultimedia(only: Boolean) {
        _state.update { it.copy(showOnlyMultimedia = only) }
        persistBool(BROWSER_SHOW_ONLY_MULTIMEDIA, only)
        launchIo {
            runCatching { prefs?.putBoolean(BROWSER_SHOW_ONLY_MULTIMEDIA, only) }
        }
        refreshCurrentUriListing()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        val folder = _state.value.currentFolder
        when {
            folder == null -> observeRoot()
            isUriBrowseTarget(folder) -> browseUri(folder)
            else -> observeFolder(folder.id)
        }
    }

    private fun refreshCurrentUriListing() {
        val folder = _state.value.currentFolder ?: return
        if (!isUriBrowseTarget(folder)) return
        _state.update { it.copy(loading = true, error = null) }
        browseUri(folder)
    }

    private fun observeRoot() {
        job?.cancel()
        job = launch {
            combine(
                repo.observeBrowserFavorites(),
                repo.observeFolders(null),
                if (capabilities.networkBrowsing && VlcSettings.browseNetwork.value) {
                    repo.observeNetworkRoots()
                } else {
                    flowOf(emptyList())
                },
            ) { favs, storage, network -> Triple(favs, storage, network) }
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { (favs, storage, network) ->
                    _state.update {
                        it.copy(
                            favorites = favs,
                            folders = storage,
                            networkRoots = network,
                            media = emptyList(),
                            loading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun observeFolder(folderId: Long) {
        job?.cancel()
        job = launch {
            launch {
                repo.observeFolders(folderId)
                    .catch { }
                    .collectLatest { folders -> _state.update { it.copy(folders = folders) } }
            }
            repo.observeFolderMedia(folderId)
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { media ->
                    _state.update { state ->
                        state.copy(media = media.filterBrowserMedia(state), loading = false, error = null)
                    }
                }
        }
    }

    private fun browseUri(folder: MediaFolder) {
        job?.cancel()
        val uri = folder.uri.ifBlank { folder.path }
        if (uri.isBlank()) {
            observeFolder(folder.id)
            return
        }
        job = launch {
            repo.browseUri(uri)
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collectLatest { listing ->
                    _state.update {
                        it.copy(
                            folders = listing.folders.filterBrowserFolders(it),
                            media = listing.media.filterBrowserMedia(it),
                            loading = false,
                            error = null,
                        )
                    }
                }
        }
    }
}

private fun List<MediaFolder>.filterBrowserFolders(state: BrowserUiState): List<MediaFolder> =
    if (state.showHiddenFiles) this else filterNot { it.isHiddenBrowserEntry() }

private fun List<MediaItem>.filterBrowserMedia(state: BrowserUiState): List<MediaItem> =
    asSequence()
        .filter { state.showHiddenFiles || !it.isHiddenBrowserEntry() }
        .filter { !state.showOnlyMultimedia || it.type == MediaType.AUDIO || it.type == MediaType.VIDEO }
        .toList()

private fun MediaFolder.isHiddenBrowserEntry(): Boolean =
    path.substringBefore('?').trimEnd('/').substringAfterLast('/').startsWith('.') || title.startsWith('.')

private fun MediaItem.isHiddenBrowserEntry(): Boolean =
    uri.substringBefore('?').trimEnd('/').substringAfterLast('/').startsWith('.') || title.startsWith('.')

private fun VideoGroupingMode.isGrouped(): Boolean =
    this == VideoGroupingMode.NAME || this == VideoGroupingMode.FOLDER

class PlaylistsViewModel(
    private val repo: PlaylistRepository = playlistRepo(),
    private val player: PlaybackController = playback(),
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
        runCatching { repo.removeFromPlaylistAt(playlistId, index) }
        val pl = runCatching { repo.getPlaylist(playlistId) }.getOrNull()
        _state.update { it.copy(openItems = pl?.items.orEmpty()) }
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
        ids.forEach { id -> runCatching { repo.deletePlaylist(id) } }
        _state.update { it.copy(selection = emptySet()) }
    }
}
