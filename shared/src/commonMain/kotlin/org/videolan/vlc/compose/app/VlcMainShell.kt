package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.MainTab
import org.videolan.vlc.viewmodel.MediaListUiState
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel

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

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = colors.backgroundDefault,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                showPlayer -> "Now Playing"
                                showSettings -> "Settings"
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
                        if (showPlayer || showSettings) {
                            TextButton(onClick = {
                                showPlayer = false
                                showSettings = false
                            }) { Text("Back") }
                        }
                    }
                )
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
                        val st = videoVm.state.collectAsState().value
                        RichMediaListPane(
                            state = st,
                            title = "Videos",
                            emptyLabel = "No videos",
                            onQuery = videoVm::setQuery,
                            onPlay = { videoVm.play(it); showPlayer = true },
                            onPlayAll = { videoVm.playAll(); showPlayer = true },
                            onPlayNext = videoVm::playNext,
                            onAppend = videoVm::append,
                            onToggleSelect = videoVm::toggleSelect,
                            onSelectAll = videoVm::selectAll,
                            onClearSelection = videoVm::clearSelection,
                            onSetViewMode = videoVm::setViewMode,
                            onSetSort = videoVm::setSort,
                            onCtx = videoVm::handleCtx,
                            modifier = contentMod,
                        )
                    }
                    MainTab.AUDIO -> {
                        val st = audioVm.state.collectAsState().value
                        val sec = audioVm.section.collectAsState().value
                        Column(contentMod) {
                            SectionTabs(
                                tabs = listOf("Tracks", "Artists", "Albums"),
                                selected = sec.ordinal,
                                onSelect = {
                                    audioVm.setSection(org.videolan.vlc.viewmodel.AudioSection.entries[it])
                                },
                            )
                            RichMediaListPane(
                                state = st,
                                title = "Audio",
                                emptyLabel = "No audio",
                                sections = st.sections,
                                onQuery = audioVm::setQuery,
                                onPlay = { audioVm.play(it); showPlayer = true },
                                onPlayAll = { audioVm.playAll(); showPlayer = true },
                                onPlayNext = audioVm::playNext,
                                onAppend = audioVm::append,
                                onToggleSelect = audioVm::toggleSelect,
                                onSelectAll = audioVm::selectAll,
                                onClearSelection = audioVm::clearSelection,
                                onSetViewMode = audioVm::setViewMode,
                                onSetSort = audioVm::setSort,
                                onCtx = audioVm::handleCtx,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    MainTab.BROWSER -> {
                        val st = browserVm.state.collectAsState().value
                        BrowserRichPane(
                            folders = st.folders,
                            media = st.media,
                            title = st.currentFolder?.title ?: "Storage",
                            loading = st.loading,
                            canGoUp = st.stack.isNotEmpty(),
                            onUp = { browserVm.goUp() },
                            onOpenFolder = browserVm::openFolder,
                            onPlay = { browserVm.play(it); showPlayer = true },
                            onPlayNext = browserVm::playNext,
                            onAppend = browserVm::append,
                            modifier = contentMod,
                        )
                    }
                    MainTab.PLAYLISTS -> {
                        val st = playlistsVm.state.collectAsState().value
                        PlaylistsRichPane(
                            playlists = st.playlists,
                            loading = st.loading,
                            detailItems = st.openItems,
                            detailName = st.openPlaylistName,
                            onCreate = playlistsVm::create,
                            onOpen = playlistsVm::openPlaylist,
                            onPlay = { playlistsVm.playPlaylist(it); showPlayer = true },
                            onDelete = playlistsVm::delete,
                            onPlayItem = { playlistsVm.playItem(it); showPlayer = true },
                            onBack = playlistsVm::closeDetail,
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
private fun MediaListPane(
    modifier: Modifier,
    state: MediaListUiState,
    onQuery: (String) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onPlayAll: () -> Unit,
    emptyLabel: String,
) {
    val colors = VLCThemeDefaults.colors
    Column(modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            singleLine = true,
            label = { Text("Search") },
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${state.count} items", color = colors.fontLight, style = MaterialTheme.typography.labelMedium)
            if (state.items.isNotEmpty()) {
                TextButton(onClick = onPlayAll) { Text("Play all") }
            }
        }
        if (state.loading) LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!state.loading && state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyLabel, color = colors.fontLight)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.items, key = { "${it.id}:${it.uri}" }) { item ->
                    MediaRow(item) { onPlay(item) }
                }
            }
        }
    }
}

@Composable
private fun BrowserPane(
    modifier: Modifier,
    vm: BrowserViewModel,
    onPlay: (MediaItem) -> Unit,
) {
    val state by vm.state.collectAsState()
    val colors = VLCThemeDefaults.colors
    Column(modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.stack.isNotEmpty()) {
                TextButton(onClick = { vm.goUp() }) { Text("Up") }
            }
            Text(
                state.currentFolder?.title ?: "Storage",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (state.loading) LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.folders, key = { "f:${it.id}:${it.path}" }) { folder ->
                FolderRow(folder) { vm.openFolder(folder) }
            }
            items(state.media, key = { "m:${it.id}:${it.uri}" }) { item ->
                MediaRow(item) { onPlay(item) }
            }
            if (!state.loading && state.folders.isEmpty() && state.media.isEmpty()) {
                item {
                    Text(
                        "No folders yet — add storage or import media",
                        color = colors.fontLight,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsPane(
    modifier: Modifier,
    vm: PlaylistsViewModel,
    onPlay: (PlaylistInfo) -> Unit,
) {
    val state by vm.state.collectAsState()
    var newName by remember { mutableStateOf("") }
    Column(modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("New playlist") },
            )
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        vm.create(newName.trim())
                        newName = ""
                    }
                }
            ) { Text("Add") }
        }
        if (state.loading) LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.playlists, key = { it.id }) { pl ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onPlay(pl) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pl.name, fontWeight = FontWeight.Medium)
                        Text("${pl.itemCount} items", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { vm.delete(pl.id) }) { Text("Del") }
                }
            }
            if (!state.loading && state.playlists.isEmpty()) {
                item { Text("No playlists", modifier = Modifier.padding(24.dp)) }
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
    onPlayHistory: (org.videolan.vlc.model.HistoryEntry) -> Unit,
) {
    val state by vm.state.collectAsState()
    val colors = VLCThemeDefaults.colors
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
        item {
            MoreAction("Settings", onOpenSettings)
        }
        if (onOpenRemote != null) {
            item { MoreAction("Connect to VLC", onOpenRemote) }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = vm::clearHistory) { Text("Clear") }
            }
        }
        item {
            Text("Streams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.streams, key = { "s:${it.id}:${it.uri}" }) { item ->
            MediaRow(item) { vm.playStream(item) }
        }
        if (state.streams.isEmpty()) {
            item { Text("No streams", color = colors.fontLight) }
        }
        items(state.history, key = { "h:${it.item.id}:${it.playedAt}" }) { entry ->
            MediaRow(entry.item) { onPlayHistory(entry) }
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
private fun FolderRow(folder: MediaFolder, onClick: () -> Unit) {
    val colors = VLCThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("DIR", color = colors.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
        Column {
            Text(folder.title, fontWeight = FontWeight.Medium, color = colors.listTitle)
            if (folder.childCount > 0) {
                Text("${folder.childCount} items", style = MaterialTheme.typography.bodySmall, color = colors.fontLight)
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
        TextButton(onClick = onToggle) { Text(if (playing) "Pause" else "Play") }
    }
}
