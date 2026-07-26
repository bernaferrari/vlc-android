package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.player.VideoSurfaceWithHud
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.MainTab
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel
import org.videolan.vlc.viewmodel.AudioSection
import org.videolan.vlc.util.ContextOption

/**
 * Multiplatform main shell — Video / Audio / Browser / Playlists / More.
 *
 * This is the shared product chrome for iOS and the Android main path
 * (when [useSharedMainShell] is enabled). Platform engines feed data via
 * [org.videolan.vlc.repository.MediaRepository] / [org.videolan.vlc.player.PlaybackController].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VlcMainShell(
    modifier: Modifier = Modifier,
    initialTab: MainTab = MainTab.VIDEO,
    /** When non-null, tab is controlled by the host (Android chrome / tests). */
    tab: MainTab? = null,
    onTabChange: ((MainTab) -> Unit)? = null,
    showBottomBar: Boolean = true,
    videoVm: VideoListViewModel = remember { VideoListViewModel() },
    audioVm: AudioListViewModel = remember { AudioListViewModel() },
    browserVm: BrowserViewModel = remember { BrowserViewModel() },
    playlistsVm: PlaylistsViewModel = remember { PlaylistsViewModel() },
    moreVm: MoreHubViewModel = remember { MoreHubViewModel() },
    playerVm: PlayerViewModel = remember { PlayerViewModel() },
    settingsVm: SettingsViewModel = remember { SettingsViewModel() },
    title: String = "VLC",
    onOpenSettings: (() -> Unit)? = null,
    onOpenRemoteClient: (() -> Unit)? = null,
    hostCallbacks: ShellHostCallbacks = ShellHostCallbacks.NoOp,
) {
    DisposableEffect(videoVm, audioVm, browserVm, playlistsVm, moreVm, playerVm, settingsVm) {
        onDispose {
            videoVm.onCleared()
            audioVm.onCleared()
            browserVm.onCleared()
            playlistsVm.onCleared()
            moreVm.onCleared()
            playerVm.onCleared()
            settingsVm.onCleared()
        }
    }

    VLCTheme {
        val colors = VLCThemeDefaults.colors
        var internalTab by remember { mutableStateOf(initialTab) }
        val currentTab = tab ?: internalTab
        fun selectTab(t: MainTab) {
            if (onTabChange != null) onTabChange(t) else internalTab = t
        }
        var showPlayer by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        val playerState by playerVm.state.collectAsState()
        val videoState by videoVm.state.collectAsState()
        val audioState by audioVm.state.collectAsState()
        val audioSection by audioVm.section.collectAsState()
        val browserState by browserVm.state.collectAsState()
        val playlistsState by playlistsVm.state.collectAsState()
        val backTarget = shellBackTarget(
            showOverlay = showPlayer || showSettings,
            hasPlaylistDetail = playlistsState.openPlaylistId != null,
            hasBrowserFolder = browserState.stack.isNotEmpty(),
            hasAudioEntity = audioState.openedEntityTitle != null,
            hasVideoContainer = videoState.containerId != null,
        )

        fun navigateBack() {
            when (backTarget) {
                ShellBackTarget.OVERLAY -> {
                    showPlayer = false
                    showSettings = false
                }
                ShellBackTarget.PLAYLIST_DETAIL -> playlistsVm.closeDetail()
                ShellBackTarget.BROWSER_FOLDER -> browserVm.goUp()
                ShellBackTarget.AUDIO_ENTITY -> audioVm.closeEntity()
                ShellBackTarget.VIDEO_CONTAINER -> videoVm.closeContainer()
                null -> Unit
            }
        }

        HandleShellBackPress(enabled = backTarget != null, onBack = ::navigateBack)

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = colors.backgroundDefault,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                showPlayer -> "Now Playing"
                                showSettings -> ShellStrings.settings()
                                else -> when (currentTab) {
                                    MainTab.VIDEO -> "$title · Video"
                                    MainTab.AUDIO -> "$title · Audio"
                                    MainTab.BROWSER -> "$title · Browse"
                                    MainTab.PLAYLISTS -> "$title · Playlists"
                                    MainTab.MORE -> "$title · More"
                                }
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        if (backTarget != null) {
                            TextButton(onClick = ::navigateBack) { Text(ShellStrings.back()) }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!showPlayer && !showSettings) {
                    when (currentTab) {
                        MainTab.VIDEO -> {
                            if (videoState.count > 0 || videoState.items.isNotEmpty()) {
                                FloatingActionButton(onClick = {
                                    videoVm.playAll()
                                    showPlayer = true
                                }) { Text("▶") }
                            }
                        }
                        MainTab.AUDIO -> {
                            if (audioSection == AudioSection.TRACKS && audioState.count > 1) {
                                FloatingActionButton(onClick = {
                                    audioVm.shuffleAll()
                                    showPlayer = true
                                }) { Text("⇝") }
                            }
                        }
                        else -> Unit
                    }
                }
            },
            bottomBar = {
                if (showBottomBar && !showPlayer && !showSettings) {
                    Column {
                        if (playerState.hasMedia) {
                            MiniBar(
                                title = playerState.title.ifBlank { "Not playing" },
                                subtitle = playerState.subtitle,
                                playing = playerState.playing,
                                onExpand = { showPlayer = true },
                                onToggle = playerVm::togglePlayPause,
                            )
                        }
                        NavigationBar {
                            MainTab.entries.forEach { t ->
                                NavigationBarItem(
                                    selected = currentTab == t,
                                    onClick = {
                                        selectTab(t)
                                        showPlayer = false
                                        showSettings = false
                                    },
                                    icon = {
                                        Text(
                                            when (t) {
                                                MainTab.VIDEO -> "Vid"
                                                MainTab.AUDIO -> "Aud"
                                                MainTab.BROWSER -> "Dir"
                                                MainTab.PLAYLISTS -> "Pls"
                                                MainTab.MORE -> "More"
                                            }
                                        )
                                    },
                                    label = {
                                        Text(
                                            when (t) {
                                                MainTab.VIDEO -> "Video"
                                                MainTab.AUDIO -> "Audio"
                                                MainTab.BROWSER -> "Browse"
                                                MainTab.PLAYLISTS -> "Playlists"
                                                MainTab.MORE -> "More"
                                            }
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            val contentMod = Modifier.padding(padding).fillMaxSize()
            when {
                showPlayer -> {
                    VideoSurfaceWithHud(
                        title = playerState.title,
                        subtitle = playerState.subtitle,
                        playing = playerState.playing,
                        progress = playerState.progress,
                        shuffle = playerState.shuffle,
                        repeatMode = playerState.repeatMode,
                        onTogglePlay = playerVm::togglePlayPause,
                        onSeek = playerVm::seekTo,
                        onNext = playerVm::next,
                        onPrevious = playerVm::previous,
                        onToggleShuffle = playerVm::toggleShuffle,
                        onCycleRepeat = playerVm::cycleRepeat,
                        onClose = { showPlayer = false },
                        modifier = contentMod,
                    ) {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("♪", style = MaterialTheme.typography.displayLarge, color = colors.primary)
                        }
                    }
                }
                showSettings -> {
                    // Reuse shared settings surface from VlcSharedApp pattern
                    SettingsOnlyPane(modifier = contentMod, vm = settingsVm)
                }
                else -> when (currentTab) {
                    MainTab.VIDEO -> {
                        RichMediaListPane(
                            state = videoState,
                            title = "Videos",
                            emptyLabel = "No videos",
                            pagingFlow = videoVm.pagingFlow,
                            groups = videoState.groups,
                            onQuery = videoVm::setQuery,
                            onRetry = videoVm::refresh,
                            onPlay = { videoVm.play(it); showPlayer = true },
                            onPlayAll = { videoVm.playAll(); showPlayer = true },
                            onPlayNext = videoVm::playNext,
                            onAppend = videoVm::append,
                            onToggleSelect = videoVm::toggleSelect,
                            onSelectAll = videoVm::selectAll,
                            onClearSelection = videoVm::clearSelection,
                            onPlaySelection = {
                                videoVm.playSelection()
                                showPlayer = true
                            },
                            onAppendSelection = videoVm::appendSelection,
                            onFavoriteSelection = videoVm::favoriteSelection,
                            onSetViewMode = videoVm::setViewMode,
                            onSetSort = videoVm::setSort,
                            onToggleSortDesc = videoVm::toggleSortDesc,
                            onToggleFavorites = videoVm::toggleOnlyFavorites,
                            onCtx = { item, opt ->
                                when (opt) {
                                    ContextOption.CTX_INFORMATION,
                                    ContextOption.CTX_SHARE,
                                    ContextOption.CTX_DOWNLOAD_SUBTITLES,
                                    ContextOption.CTX_ADD_SHORTCUT,
                                    ContextOption.CTX_SET_RINGTONE,
                                    ContextOption.CTX_BAN_FOLDER,
                                    ContextOption.CTX_ADD_TO_PLAYLIST,
                                    -> hostCallbacks.dispatch(item, opt)
                                    else -> {
                                        videoVm.handleCtx(item, opt)
                                        if (opt == ContextOption.CTX_PLAY ||
                                            opt == ContextOption.CTX_PLAY_ALL
                                        ) {
                                            showPlayer = true
                                        }
                                    }
                                }
                            },
                            onOpenGroup = videoVm::openContainer,
                            onCloseContainer = videoVm::closeContainer,
                            onSetGroupingMode = videoVm::setGroupingMode,
                            showGroupingToggle = true,
                            onDefaultAction = videoVm::setDefaultPlaybackAction,
                            modifier = contentMod,
                        )
                    }
                    MainTab.AUDIO -> {
                        Column(contentMod) {
                            if (audioState.openedEntityTitle == null) {
                                SectionTabs(
                                    tabs = listOf("Tracks", "Artists", "Albums", "Genres", "Playlists"),
                                    selected = audioSection.ordinal,
                                    onSelect = {
                                        audioVm.setSection(org.videolan.vlc.viewmodel.AudioSection.entries[it])
                                    },
                                )
                            }
                            RichMediaListPane(
                                state = audioState,
                                title = "Audio",
                                emptyLabel = "No audio",
                                sections = audioState.sections,
                                pagingFlow = if (audioSection == org.videolan.vlc.viewmodel.AudioSection.TRACKS &&
                                    audioState.openedEntityTitle == null
                                ) {
                                    audioVm.pagingFlow
                                } else {
                                    null
                                },
                                onQuery = audioVm::setQuery,
                                onRetry = audioVm::refresh,
                                onPlay = { item ->
                                    val isEntityUri = item.uri.startsWith("artist://") ||
                                        item.uri.startsWith("album://") ||
                                        item.uri.startsWith("genre://")
                                    if (isEntityUri && audioSection != org.videolan.vlc.viewmodel.AudioSection.TRACKS) {
                                        audioVm.openAudioEntityFromItem(item)
                                    } else {
                                        audioVm.play(item)
                                        showPlayer = true
                                    }
                                },
                                onPlayAll = { audioVm.playAll(); showPlayer = true },
                                onPlayNext = audioVm::playNext,
                                onAppend = audioVm::append,
                                onToggleSelect = audioVm::toggleSelect,
                                onSelectAll = audioVm::selectAll,
                                onClearSelection = audioVm::clearSelection,
                                onPlaySelection = {
                                    audioVm.playSelection()
                                    showPlayer = true
                                },
                                onAppendSelection = audioVm::appendSelection,
                                onFavoriteSelection = audioVm::favoriteSelection,
                                onSetViewMode = audioVm::setViewMode,
                                onSetSort = audioVm::setSort,
                                onToggleSortDesc = audioVm::toggleSortDesc,
                                onToggleFavorites = audioVm::toggleOnlyFavorites,
                                onCtx = { item, opt ->
                                    when (opt) {
                                        ContextOption.CTX_INFORMATION,
                                        ContextOption.CTX_SHARE,
                                        ContextOption.CTX_DOWNLOAD_SUBTITLES,
                                        ContextOption.CTX_ADD_SHORTCUT,
                                        ContextOption.CTX_SET_RINGTONE,
                                        ContextOption.CTX_BAN_FOLDER,
                                        ContextOption.CTX_ADD_TO_PLAYLIST,
                                        -> hostCallbacks.dispatch(item, opt)
                                        else -> {
                                            audioVm.handleCtx(item, opt)
                                            if (opt == ContextOption.CTX_PLAY ||
                                                opt == ContextOption.CTX_PLAY_ALL
                                            ) {
                                                showPlayer = true
                                            }
                                        }
                                    }
                                },
                                onCloseContainer = audioVm::closeEntity,
                                showAllArtistsToggle = true,
                                showTrackNumbersToggle = true,
                                onShowAllArtists = audioVm::setShowAllArtists,
                                onShowTrackNumbers = audioVm::setShowTrackNumbers,
                                onDefaultAction = audioVm::setDefaultPlaybackAction,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    MainTab.BROWSER -> {
                        BrowserRichPane(
                            state = browserState,
                            onUp = { browserVm.goUp() },
                            onRetry = browserVm::refresh,
                            onOpenFolder = { folder ->
                                val target = folder.uri.ifBlank { folder.path }
                                if (
                                    target.equals("otg://", ignoreCase = true) ||
                                    target.startsWith("otg://", ignoreCase = true)
                                ) {
                                    hostCallbacks.onRequestOtgRoot()
                                }
                                browserVm.openFolder(folder)
                            },
                            onPlay = { browserVm.play(it); showPlayer = true },
                            onPlayNext = browserVm::playNext,
                            onAppend = browserVm::append,
                            onToggleSelect = browserVm::toggleSelect,
                            onClearSelection = browserVm::clearSelection,
                            onPlaySelection = {
                                browserVm.playSelection()
                                showPlayer = true
                            },
                            onAppendSelection = browserVm::appendSelection,
                            onDefaultAction = browserVm::setDefaultPlaybackAction,
                            onShowHiddenFiles = browserVm::setShowHiddenFiles,
                            onShowOnlyMultimedia = browserVm::setShowOnlyMultimedia,
                            modifier = contentMod,
                        )
                    }
                    MainTab.PLAYLISTS -> {
                        PlaylistsRichPane(
                            state = playlistsState,
                            onCreate = playlistsVm::create,
                            onOpen = playlistsVm::openPlaylist,
                            onPlay = { playlistsVm.playPlaylist(it); showPlayer = true },
                            onShufflePlay = { playlistsVm.shufflePlay(it); showPlayer = true },
                            onDelete = playlistsVm::delete,
                            onRename = playlistsVm::rename,
                            onSetFavorite = playlistsVm::setFavorite,
                            onToggleSelect = playlistsVm::toggleSelect,
                            onClearSelection = playlistsVm::clearSelection,
                            onDeleteSelection = playlistsVm::deleteSelection,
                            onToggleFavorites = playlistsVm::toggleOnlyFavorites,
                            onToggleSortDesc = playlistsVm::toggleSortDesc,
                            onSetViewMode = playlistsVm::setViewMode,
                            onPlayItem = { playlistsVm.playItem(it); showPlayer = true },
                            onRemoveTrack = playlistsVm::removeTrackAt,
                            onMoveTrackUp = playlistsVm::moveTrackUp,
                            onMoveTrackDown = playlistsVm::moveTrackDown,
                            onBack = playlistsVm::closeDetail,
                            onRetry = playlistsVm::refresh,
                            modifier = contentMod,
                        )
                    }
                    MainTab.MORE -> MorePane(
                        modifier = contentMod,
                        vm = moreVm,
                        onOpenSettings = {
                            if (onOpenSettings != null) onOpenSettings()
                            else showSettings = true
                        },
                        onOpenRemote = onOpenRemoteClient,
                        onOpenAbout = hostCallbacks::onOpenAbout,
                        onOpenDonate = hostCallbacks::onOpenDonate,
                        onPlayHistory = { entry ->
                            moreVm.playHistory(entry)
                            showPlayer = true
                        },
                    )
                }
            }
        }
    }
}


@Composable
private fun MorePane(
    modifier: Modifier,
    vm: MoreHubViewModel,
    onOpenSettings: () -> Unit,
    onOpenRemote: (() -> Unit)?,
    onOpenAbout: () -> Unit = {},
    onOpenDonate: () -> Unit = {},
    onPlayHistory: (org.videolan.vlc.model.HistoryEntry) -> Unit,
) {
    val state by vm.state.collectAsState()
    val colors = VLCThemeDefaults.colors
    var renameStreamId by remember { mutableStateOf<Long?>(null) }
    var renameStreamText by remember { mutableStateOf("") }
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("VLC", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (state.platformName.isNotBlank()) {
                Text(state.platformName, color = colors.fontLight, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { MoreAction(ShellStrings.settings(), onOpenSettings) }
        item {
            MoreAction(ShellStrings.about(), onOpenAbout)
        }
        item {
            MoreAction("Donate", onOpenDonate)
        }
        if (onOpenRemote != null) {
            item { MoreAction("Remote", onOpenRemote) }
        }

        item {
            Text("Streams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (renameStreamId != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = renameStreamText,
                        onValueChange = { renameStreamText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Rename stream") },
                    )
                    TextButton(onClick = {
                        val id = renameStreamId
                        if (id != null && renameStreamText.isNotBlank()) {
                            vm.renameStream(id, renameStreamText.trim())
                        }
                        renameStreamId = null
                        renameStreamText = ""
                    }) { Text("Save") }
                    TextButton(onClick = {
                        renameStreamId = null
                        renameStreamText = ""
                    }) { Text(ShellStrings.cancel()) }
                }
            }
        }
        items(state.streams, key = { "s:${it.id}:${it.uri}" }) { item ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    MediaRow(item) { vm.playStream(item) }
                }
                if (state.hasStreamRepository) {
                    TextButton(onClick = {
                        renameStreamId = item.id
                        renameStreamText = item.title
                    }) { Text("Ren") }
                    TextButton(onClick = { vm.deleteStream(item.id) }) { Text("Del") }
                }
            }
        }
        if (state.streams.isEmpty()) {
            item { Text("No streams", color = colors.fontLight) }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ShellStrings.history(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    if (state.historySelection.isNotEmpty()) {
                        TextButton(onClick = vm::removeSelectedHistory) {
                            Text("${ShellStrings.remove()} (${state.historySelection.size})")
                        }
                        TextButton(onClick = vm::clearHistorySelection) { Text(ShellStrings.clear()) }
                    }
                    TextButton(onClick = vm::clearHistory) { Text(ShellStrings.clear()) }
                }
            }
        }
        items(state.history, key = { "h:${it.item.id}:${it.playedAt}" }) { entry ->
            val key = "${entry.item.id}:${entry.playedAt}:${entry.item.uri}"
            val selected = key in state.historySelection
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (selected) colors.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    .clickable { onPlayHistory(entry) }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    MediaRow(entry.item) { onPlayHistory(entry) }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (!entry.item.present) {
                        Text("missing", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text("present", color = colors.fontLight, style = MaterialTheme.typography.labelSmall)
                    }
                    Row {
                        TextButton(onClick = { vm.toggleHistorySelect(entry) }) {
                            Text(if (selected) "✓" else "Sel")
                        }
                        TextButton(onClick = { vm.moveUp(entry) }) { Text("↑") }
                    }
                }
            }
        }
        if (!state.loading && state.history.isEmpty()) {
            item { Text("No recent media", color = colors.fontLight) }
        }
    }
}

@Composable
private fun MoreAction(label: String, onClick: () -> Unit) {
    SurfaceRow(label, onClick)
}

@Composable
private fun SurfaceRow(label: String, onClick: () -> Unit) {
    val colors = VLCThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(label, color = colors.listTitle, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsOnlyPane(modifier: Modifier, vm: SettingsViewModel) {
    // Lightweight settings list — mirrors SettingsViewModel toggles
    val state by vm.state.collectAsState()
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Playback", fontWeight = FontWeight.Bold) }
        item { ToggleRow("Resume audio", state.audioResume, vm::setAudioResume) }
        item { ToggleRow("Resume video", state.videoResume, vm::setVideoResume) }
        item { ToggleRow("Playback history", state.playbackHistory, vm::setPlaybackHistory) }
        item { ToggleRow("Incognito", state.incognito, vm::setIncognito) }
        item { Text("Library", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
        item { ToggleRow("Video thumbnails", state.showVideoThumbs, vm::setShowVideoThumbs) }
        item { Text("Network", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
        item { ToggleRow("Remote access server", state.remoteAccess, vm::setRemoteAccess) }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun MediaRow(item: MediaItem, onClick: () -> Unit) {
    val colors = VLCThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    item.isVideo -> "VID"
                    item.isAudio -> "AUD"
                    else -> "•"
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.displayTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.listTitle,
                fontWeight = FontWeight.Medium,
            )
            val sub = listOfNotNull(item.artist, item.album).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.listSubtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


@Composable
private fun MiniBar(
    title: String,
    subtitle: String,
    playing: Boolean,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = VLCThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.audioHeaderBackground)
            .clickable(onClick = onExpand)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = colors.fontLight)
            }
        }
        TextButton(onClick = onToggle) { Text(if (playing) "Pause" else ShellStrings.play()) }
    }
}
