@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.platform.platformCapabilities
import org.videolan.vlc.repository.MediaRepository
import org.videolan.tools.BROWSER_SHOW_HIDDEN_FILES
import org.videolan.tools.BROWSER_SHOW_ONLY_MULTIMEDIA
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.util.DefaultPlaybackAction
import org.videolan.vlc.util.DefaultPlaybackKeys

class BrowserViewModel(
    private val repo: MediaRepository = mainShellMediaRepo(),
    private val player: PlaybackController = mainShellPlayback(),
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
        launch {
            VlcSettings.showOnlyMultimedia.collectLatest { only ->
                _state.update { it.copy(showOnlyMultimedia = only) }
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
        // The root list and the preference flow are asynchronous. A stale row can remain visible
        // for one frame while the setting is restored; do not turn that tap into a silent no-op.
        // Capability is the hard platform gate, while the repository remains responsible for
        // returning an empty/error listing when network browsing is disabled.
        if (folder.kind == FolderKind.NETWORK && !capabilities.networkBrowsing) return
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
            runCatching { prefs?.let { VlcSettings.updateBoolean(it, BROWSER_SHOW_ONLY_MULTIMEDIA, only) } }
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
                    .catch { error ->
                        _state.update { state ->
                            state.copy(error = error.message ?: "Unable to load folders")
                        }
                    }
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
