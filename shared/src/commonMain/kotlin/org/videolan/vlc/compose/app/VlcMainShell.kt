package org.videolan.vlc.compose.app

import androidx.compose.foundation.clickable
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.player.FallbackPlayerSurface
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.MainTab
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel
import org.videolan.vlc.viewmodel.AudioSection

/**
 * Multiplatform main shell — Video / Audio / Browser / Playlists / More.
 *
 * This is the shared product chrome for iOS and the Android main path
 * (when [useSharedMainShell] is enabled). Platform engines feed data via
 * [org.videolan.vlc.repository.MediaRepository] / [org.videolan.vlc.player.PlaybackController].
 * Navigation remains intentionally single-pane on compact and wide hosts;
 * Nav3 owns route restoration and back handling while feature panes provide
 * their own responsive content. A future list-detail layout can reuse these
 * typed routes without changing their serialized form.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
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
    playerSurface: PlayerSurface = FallbackPlayerSurface,
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
        val initialRoute = remember(initialTab) { initialTab.toVlcShellRoute() }
        val backStack = rememberNavBackStack(vlcShellNavSavedStateConfiguration, initialRoute)
        val currentRoute = backStack.lastOrNull() as? VlcShellRoute ?: initialRoute
        val currentTab = backStack.filterIsInstance<VlcShellRoute>().activeTab()
        val showPlayer = currentRoute == PlayerRoute
        val showSettings = currentRoute == SettingsRoute
        // Root destinations switch immediately: they are frequent mode changes, not a journey.
        val rootTransitionMetadata = remember {
            NavDisplay.transitionSpec {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                )
            } + NavDisplay.popTransitionSpec {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                )
            }
        }
        // Details move in from the right and leave to the right, so navigation is spatial rather
        // than the default cross-fade that made the shell feel disconnected.
        val detailTransitionMetadata = remember {
            NavDisplay.transitionSpec {
                ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = 220,
                            easing = VLCMotion.EmphasizedDecelerate,
                        ),
                        initialOffsetX = { fullWidth -> fullWidth },
                    ),
                    initialContentExit = ExitTransition.None,
                )
            } + NavDisplay.popTransitionSpec {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = 180,
                            easing = VLCMotion.EmphasizedAccelerate,
                        ),
                        targetOffsetX = { fullWidth -> fullWidth },
                    ),
                )
            }
        }

        fun closeFeatureDetails() {
            if (videoVm.state.value.containerId != null) videoVm.closeContainer()
            if (audioVm.state.value.openedEntityTitle != null) audioVm.closeEntity()
            if (browserVm.state.value.stack.isNotEmpty()) browserVm.openRoot()
            if (playlistsVm.state.value.openPlaylistId != null) playlistsVm.closeDetail()
        }

        fun resetToTab(tab: MainTab) {
            closeFeatureDetails()
            backStack.clear()
            backStack.add(tab.toVlcShellRoute())
        }

        fun openPlayer() {
            if (!showPlayer) backStack.add(PlayerRoute)
        }

        fun popRoute() {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        }

        fun openVideoContainer(folder: MediaFolder) {
            videoVm.openContainer(folder)
            backStack.add(folder.toVideoContainerRoute())
        }

        fun openAudioEntity(item: MediaItem) {
            val route = item.toAudioEntityRoute() ?: return
            audioVm.openAudioEntityFromItem(item)
            backStack.add(route)
        }

        fun openBrowserFolder(folder: MediaFolder) {
            val target = folder.uri.ifBlank { folder.path }
            if (target.equals("otg://", ignoreCase = true) || target.startsWith("otg://", ignoreCase = true)) {
                hostCallbacks.onRequestOtgRoot()
            }
            browserVm.openFolder(folder)
            backStack.add(BrowserFolderRoute.from(browserVm.state.value.stack))
        }

        fun openPlaylist(info: PlaylistInfo) {
            playlistsVm.openPlaylist(info)
            backStack.add(PlaylistDetailRoute(id = info.id, name = info.name))
        }

        LaunchedEffect(tab) {
            if (tab != null && currentTab != tab) resetToTab(tab)
        }

        fun selectTab(t: MainTab) {
            if (onTabChange != null) onTabChange(t) else resetToTab(t)
        }
        val playerState by playerVm.state.collectAsState()
        val videoState by videoVm.state.collectAsState()
        val audioState by audioVm.state.collectAsState()
        val audioSection by audioVm.section.collectAsState()
        val browserState by browserVm.state.collectAsState()
        val playlistsState by playlistsVm.state.collectAsState()
        val detailBackTarget = shellBackTarget(
            showOverlay = false,
            hasPlaylistDetail = playlistsState.openPlaylistId != null,
            hasBrowserFolder = browserState.stack.isNotEmpty(),
            hasAudioEntity = audioState.openedEntityTitle != null,
            hasVideoContainer = videoState.containerId != null,
        )
        val canNavigateBack = backStack.size > 1 || detailBackTarget != null

        fun navigateBack() {
            when (currentRoute) {
                is VideoContainerRoute -> {
                    videoVm.closeContainer()
                    popRoute()
                    return
                }
                is AudioEntityRoute -> {
                    audioVm.closeEntity()
                    popRoute()
                    return
                }
                is BrowserFolderRoute -> {
                    browserVm.goUp()
                    popRoute()
                    return
                }
                is PlaylistDetailRoute -> {
                    playlistsVm.closeDetail()
                    popRoute()
                    return
                }
                else -> Unit
            }
            if (backStack.size > 1) {
                popRoute()
                return
            }
            when (detailBackTarget) {
                ShellBackTarget.PLAYLIST_DETAIL -> playlistsVm.closeDetail()
                ShellBackTarget.BROWSER_FOLDER -> browserVm.goUp()
                ShellBackTarget.AUDIO_ENTITY -> audioVm.closeEntity()
                ShellBackTarget.VIDEO_CONTAINER -> videoVm.closeContainer()
                ShellBackTarget.OVERLAY -> Unit
                null -> Unit
            }
        }

        HandleShellBackPress(enabled = canNavigateBack, onBack = ::navigateBack)

        VlcAdaptiveNavigationSuite(
            modifier = modifier,
            enabled = showBottomBar && !showPlayer && !showSettings,
            navigationSuiteItems = {
                MainTab.entries.forEach { t ->
                    item(
                        selected = currentTab == t,
                        onClick = { selectTab(t) },
                        icon = {
                            Icon(
                                icon = t.navigationIcon(selected = currentTab == t),
                                contentDescription = t.displayName(),
                            )
                        },
                        label = { Text(t.displayName()) },
                    )
                }
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = colors.backgroundDefault,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when {
                                    showPlayer -> "Now Playing"
                                    showSettings -> ShellStrings.settings()
                                    else -> title
                                },
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.backgroundDefault,
                            titleContentColor = colors.fontDefault,
                        ),
                        actions = {
                            if (!showPlayer && !showSettings && hostCallbacks.supportsMediaImport()) {
                                IconButton(onClick = hostCallbacks::onImportMedia) {
                                    Icon(
                                        icon = MaterialSymbols.Filled.Add,
                                        contentDescription = "Import media",
                                    )
                                }
                            }
                            if (canNavigateBack) {
                                IconButton(onClick = ::navigateBack) {
                                    Icon(
                                        icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = ShellStrings.back(),
                                    )
                                }
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
                                        openPlayer()
                                    }) {
                                        Icon(
                                            icon = MaterialSymbols.Filled.PlayArrow,
                                            contentDescription = ShellStrings.playAll(),
                                        )
                                    }
                                }
                            }
                            MainTab.AUDIO -> {
                                if (audioSection == AudioSection.TRACKS && audioState.count > 1) {
                                    FloatingActionButton(onClick = {
                                        audioVm.shuffleAll()
                                        openPlayer()
                                    }) {
                                        Icon(
                                            icon = MaterialSymbols.Filled.Shuffle,
                                            contentDescription = "Shuffle all",
                                        )
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }
                },
                bottomBar = {
                    if (showBottomBar && !showPlayer && !showSettings && playerState.hasMedia) {
                        MiniBar(
                            title = playerState.title.ifBlank { "Not playing" },
                            subtitle = playerState.subtitle,
                            playing = playerState.playing,
                            onExpand = ::openPlayer,
                            onToggle = playerVm::togglePlayPause,
                        )
                    }
                },
            ) { padding ->
                val contentMod = Modifier.padding(padding).fillMaxSize()
                NavDisplay(
                    backStack = backStack,
                    modifier = contentMod,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                    entry<VideoRoute>(metadata = rootTransitionMetadata) {
                        VideoDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = videoState,
                            viewModel = videoVm,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                            onOpenContainer = ::openVideoContainer,
                        )
                    }
                    entry<VideoContainerRoute>(metadata = detailTransitionMetadata) { route ->
                        LaunchedEffect(route) {
                            if (videoVm.state.value.containerId != route.id) {
                                videoVm.openContainer(route.toMediaFolder())
                            }
                        }
                        VideoDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = videoState,
                            viewModel = videoVm,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                            onOpenContainer = ::openVideoContainer,
                        )
                    }
                    entry<AudioRoute>(metadata = rootTransitionMetadata) {
                        AudioDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = audioState,
                            section = audioSection,
                            viewModel = audioVm,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                            onOpenEntity = ::openAudioEntity,
                        )
                    }
                    entry<AudioEntityRoute>(metadata = detailTransitionMetadata) { route ->
                        LaunchedEffect(route) {
                            if (audioVm.state.value.containerId != route.id) {
                                audioVm.openAudioEntity(route.toAudioEntity())
                            }
                        }
                        AudioDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = audioState,
                            section = audioSection,
                            viewModel = audioVm,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                            onOpenEntity = ::openAudioEntity,
                        )
                    }
                    entry<BrowserRoute>(metadata = rootTransitionMetadata) {
                        BrowserDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = browserState,
                            viewModel = browserVm,
                            onOpenPlayer = ::openPlayer,
                            onOpenFolder = ::openBrowserFolder,
                        )
                    }
                    entry<BrowserFolderRoute>(metadata = detailTransitionMetadata) { route ->
                        LaunchedEffect(route) {
                            browserVm.restoreFolderStack(route.toMediaFolders())
                        }
                        BrowserDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = browserState,
                            viewModel = browserVm,
                            onOpenPlayer = ::openPlayer,
                            onOpenFolder = ::openBrowserFolder,
                        )
                    }
                    entry<PlaylistsRoute>(metadata = rootTransitionMetadata) {
                        PlaylistsDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = playlistsState,
                            viewModel = playlistsVm,
                            onOpenPlayer = ::openPlayer,
                            onOpenPlaylist = ::openPlaylist,
                        )
                    }
                    entry<PlaylistDetailRoute>(metadata = detailTransitionMetadata) { route ->
                        LaunchedEffect(route) {
                            if (playlistsVm.state.value.openPlaylistId != route.id) {
                                playlistsVm.openPlaylist(route.toPlaylistInfo())
                            }
                        }
                        PlaylistsDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = playlistsState,
                            viewModel = playlistsVm,
                            onOpenPlayer = ::openPlayer,
                            onOpenPlaylist = ::openPlaylist,
                        )
                    }
                    entry<MoreRoute>(metadata = rootTransitionMetadata) {
                        MoreDestination(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = moreVm,
                            onOpenSettings = {
                                if (onOpenSettings != null) onOpenSettings() else backStack.add(SettingsRoute)
                            },
                            onOpenRemote = onOpenRemoteClient,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                        )
                    }
                    entry<PlayerRoute>(metadata = detailTransitionMetadata) {
                        PlayerDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = playerState,
                            viewModel = playerVm,
                            onClose = ::popRoute,
                            playerSurface = playerSurface,
                        )
                    }
                    entry<SettingsRoute>(metadata = detailTransitionMetadata) {
                        SettingsDestination(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = settingsVm,
                        )
                    }
                },
            )
        }
        }
    }
}

/**
 * QuietGuard's adaptive navigation behavior, shared by every VlcMainShell host.
 *
 * NavigationSuiteScaffold switches automatically between a bottom bar on compact screens and a
 * rail on wider ones. Its layout assumes finite constraints, while ComposeViewport can issue a
 * transient unbounded probe on Wasm; rendering nothing for that probe prevents an overflow before
 * the real viewport measure arrives. See https://youtrack.jetbrains.com/issue/CMP-8543.
 */
@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
private fun VlcAdaptiveNavigationSuite(
    modifier: Modifier,
    enabled: Boolean,
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier.fillMaxSize()) { content() }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (
            constraints.hasBoundedWidth &&
                constraints.hasBoundedHeight &&
                constraints.maxWidth > 0 &&
                constraints.maxHeight > 0
        ) {
            NavigationSuiteScaffold(
                navigationSuiteItems = navigationSuiteItems,
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
        }
    }
}

private fun MainTab.displayName(): String =
    when (this) {
        MainTab.VIDEO -> "Video"
        MainTab.AUDIO -> "Audio"
        MainTab.BROWSER -> "Browse"
        MainTab.PLAYLISTS -> "Playlists"
        MainTab.MORE -> "More"
    }

private fun MainTab.navigationIcon(selected: Boolean): MaterialIcon =
    when (this) {
        MainTab.VIDEO ->
            if (selected) MaterialSymbols.Filled.VideoLibrary else MaterialSymbols.Outlined.VideoLibrary
        MainTab.AUDIO ->
            if (selected) MaterialSymbols.Filled.MusicNote else MaterialSymbols.Outlined.MusicNote
        MainTab.BROWSER ->
            if (selected) MaterialSymbols.Filled.Folder else MaterialSymbols.Outlined.Folder
        MainTab.PLAYLISTS ->
            if (selected) MaterialSymbols.Filled.QueueMusic else MaterialSymbols.Outlined.QueueMusic
        MainTab.MORE -> MaterialSymbols.Filled.MoreVert
    }


@Composable
internal fun SettingsOnlyPane(modifier: Modifier, vm: SettingsViewModel) {
    val state by vm.state.collectAsState()
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SettingsGroup(title = "Playback") {
                ToggleRow("Resume audio", state.audioResume, vm::setAudioResume)
                ToggleRow("Resume video", state.videoResume, vm::setVideoResume)
                ToggleRow("Playback history", state.playbackHistory, vm::setPlaybackHistory)
                ToggleRow("Incognito", state.incognito, vm::setIncognito)
            }
        }
        item {
            SettingsGroup(title = "Library") {
                ToggleRow("Video thumbnails", state.showVideoThumbs, vm::setShowVideoThumbs)
            }
        }
        if (state.supportsRemoteAccess) item {
            SettingsGroup(title = "Network") {
                ToggleRow("Remote access server", state.remoteAccess, vm::setRemoteAccess)
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = VLCThemeDefaults.colors.primary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = colors.fontLight)
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    icon = if (playing) MaterialSymbols.Filled.Pause else MaterialSymbols.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else ShellStrings.play(),
                )
            }
        }
    }
}
