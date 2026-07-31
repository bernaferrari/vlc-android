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
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.AudioEntity
import org.videolan.vlc.repository.AudioEntityKind
import org.videolan.vlc.repository.ContainerKind
import org.videolan.vlc.repository.MediaQuery
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.MediaSort
import org.videolan.vlc.repository.PlaylistRepository
import org.videolan.vlc.util.ContextOption
import org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER
import org.videolan.tools.KEY_ARTISTS_SHOW_ALL
import org.videolan.tools.KEY_AUDIO_SHOW_TRACK_NUMBERS
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.util.DefaultPlaybackAction
import org.videolan.vlc.util.DefaultPlaybackKeys
import kotlin.random.Random

class AudioListViewModel(
    private val repo: MediaRepository = mainShellMediaRepo(),
    private val playlists: PlaylistRepository = mainShellPlaylistRepo(),
    private val player: PlaybackController = mainShellPlayback(),
    private val prefs: VlcPreferences? = prefsOrNull(),
    initialDefaultAction: String? = null,
) : VlcViewModel() {
    private val _state = MutableStateFlow(
        MediaListUiState(
            showAllArtists = false,
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
        val entity = openedEntity
        when {
            entity != null -> observeEntityTracks(entity)
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
        if (item.uri.isPlaylistUri()) {
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
            it.isVirtualAudioEntry()
        }
        val action = DefaultPlaybackAction.fromName(_state.value.defaultPlaybackAction)
        applyDefaultPlayback(action, item, list, player)
    }

    fun playAll() {
        val list = _state.value.items.filterNot {
            it.isVirtualAudioEntry()
        }
        if (list.isNotEmpty()) player.playFromIndex(list, 0)
    }

    fun shuffleAll() {
        val list = _state.value.items.filterNot {
            it.isVirtualAudioEntry()
        }
        if (list.isEmpty()) return
        player.setShuffle(true)
        val start = if (list.size == 1) 0 else Random.nextInt(list.size)
        player.playFromIndex(list, start)
    }

    fun playNext(item: MediaItem) {
        if (item.isVirtualAudioEntry()) return
        player.insertNext(listOf(item))
    }

    fun append(item: MediaItem) {
        if (item.isVirtualAudioEntry()) return
        player.append(listOf(item))
    }

    fun playSelection() {
        val selected = _state.value.items.filter {
            it.uri in _state.value.selection &&
                !it.isVirtualAudioEntry()
        }
        if (selected.isNotEmpty()) player.playFromIndex(selected, 0)
    }

    fun appendSelection() {
        val selected = _state.value.items.filter {
            it.uri in _state.value.selection &&
                !it.isVirtualAudioEntry()
        }
        if (selected.isNotEmpty()) player.append(selected)
    }

    fun favoriteSelection(favorite: Boolean = true) = launchIo {
        val selected = _state.value.items.filter { it.uri in _state.value.selection }
        selected.forEach { item ->
            if (item.uri.isPlaylistUri()) {
                runCatching { playlists.setFavorite(item.id, favorite) }
            } else if (!item.uri.isAudioEntityUri()) {
                runCatching { repo.setFavorite(item.id, favorite) }
            }
        }
    }

    fun setFavorite(item: MediaItem, favorite: Boolean) = launchIo {
        if (item.uri.isPlaylistUri()) {
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
        val entity = openedEntity
        when {
            entity != null -> observeEntityTracks(entity)
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
