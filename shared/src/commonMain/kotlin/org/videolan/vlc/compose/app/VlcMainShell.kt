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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCSettingsCard
import org.videolan.vlc.compose.components.VLCIconChip
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.player.FallbackPlayerSurface
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.VLCMotion
import org.videolan.vlc.compose.theme.LocalVLCMotion
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
 * Compact hosts use a focused single pane. Wide hosts use Nav3's shared
 * list-detail scene strategy, keeping the selected library context visible
 * beside its detail without introducing platform-specific navigation.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
)
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
    title: String = "",
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
        val motion = LocalVLCMotion.current
        val initialRoute = remember(initialTab) { initialTab.toVlcShellRoute() }
        val backStack = rememberNavBackStack(vlcShellNavSavedStateConfiguration, initialRoute)
        val paneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
        val singlePaneLayout = paneScaffoldDirective.maxHorizontalPartitions == 1
        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
            directive = paneScaffoldDirective,
        )
        // QuietGuard keeps root and detail state independent on wide displays. Media routes need
        // the same separation: a selected album/folder must not replace the library list. These
        // state owners do not exist on compact hosts, where one pane is both list and detail.
        val detailVideoVm = remember(singlePaneLayout, videoVm) {
            if (singlePaneLayout) videoVm else VideoListViewModel()
        }
        val detailAudioVm = remember(singlePaneLayout, audioVm) {
            if (singlePaneLayout) audioVm else AudioListViewModel()
        }
        val detailBrowserVm = remember(singlePaneLayout, browserVm) {
            if (singlePaneLayout) browserVm else BrowserViewModel()
        }
        val detailPlaylistsVm = remember(singlePaneLayout, playlistsVm) {
            if (singlePaneLayout) playlistsVm else PlaylistsViewModel()
        }
        DisposableEffect(detailVideoVm, detailAudioVm, detailBrowserVm, detailPlaylistsVm) {
            onDispose {
                if (detailVideoVm !== videoVm) detailVideoVm.onCleared()
                if (detailAudioVm !== audioVm) detailAudioVm.onCleared()
                if (detailBrowserVm !== browserVm) detailBrowserVm.onCleared()
                if (detailPlaylistsVm !== playlistsVm) detailPlaylistsVm.onCleared()
            }
        }
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
        val detailTransitionMetadata = remember(motion) {
            NavDisplay.transitionSpec {
                ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = if (motion.reducedMotion) 0 else 220,
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
                            durationMillis = if (motion.reducedMotion) 0 else 180,
                            easing = VLCMotion.EmphasizedAccelerate,
                        ),
                        targetOffsetX = { fullWidth -> fullWidth },
                    ),
                )
            }
        }
        val videoListMetadata = remember(singlePaneLayout, rootTransitionMetadata) {
            if (singlePaneLayout) rootTransitionMetadata else ListDetailSceneStrategy.listPane(
                detailPlaceholder = {
                    LibraryDetailPlaceholder(MaterialSymbols.Filled.VideoLibrary)
                },
            )
        }
        val audioListMetadata = remember(singlePaneLayout, rootTransitionMetadata) {
            if (singlePaneLayout) rootTransitionMetadata else ListDetailSceneStrategy.listPane(
                detailPlaceholder = {
                    LibraryDetailPlaceholder(MaterialSymbols.Filled.MusicNote)
                },
            )
        }
        val browserListMetadata = remember(singlePaneLayout, rootTransitionMetadata) {
            if (singlePaneLayout) rootTransitionMetadata else ListDetailSceneStrategy.listPane(
                detailPlaceholder = {
                    LibraryDetailPlaceholder(MaterialSymbols.Filled.Folder)
                },
            )
        }
        val playlistsListMetadata = remember(singlePaneLayout, rootTransitionMetadata) {
            if (singlePaneLayout) rootTransitionMetadata else ListDetailSceneStrategy.listPane(
                detailPlaceholder = {
                    LibraryDetailPlaceholder(MaterialSymbols.Filled.QueueMusic)
                },
            )
        }
        val libraryDetailMetadata = remember(singlePaneLayout, detailTransitionMetadata) {
            val paneMetadata = ListDetailSceneStrategy.detailPane()
            if (singlePaneLayout) paneMetadata + detailTransitionMetadata else paneMetadata
        }

        fun closeFeatureDetails() {
            if (detailVideoVm.state.value.containerId != null) detailVideoVm.closeContainer()
            if (detailAudioVm.state.value.openedEntityTitle != null) detailAudioVm.closeEntity()
            if (detailBrowserVm.state.value.stack.isNotEmpty()) detailBrowserVm.openRoot()
            if (detailPlaylistsVm.state.value.openPlaylistId != null) detailPlaylistsVm.closeDetail()
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

        fun replaceOrPushDetail(route: VlcShellRoute) {
            val current = backStack.lastOrNull() as? VlcShellRoute
            val replacesCurrentDetail = shouldReplaceDetailRoute(singlePaneLayout, current, route)
            if (replacesCurrentDetail) backStack[backStack.lastIndex] = route else backStack.add(route)
        }

        fun openVideoContainer(folder: MediaFolder) {
            detailVideoVm.openContainer(folder)
            replaceOrPushDetail(folder.toVideoContainerRoute())
        }

        fun openAudioEntity(item: MediaItem) {
            val route = item.toAudioEntityRoute() ?: return
            detailAudioVm.openAudioEntityFromItem(item)
            replaceOrPushDetail(route)
        }

        fun openBrowserFolder(folder: MediaFolder) {
            val target = folder.uri.ifBlank { folder.path }
            if (target.equals("otg://", ignoreCase = true) || target.startsWith("otg://", ignoreCase = true)) {
                hostCallbacks.onRequestOtgRoot()
            }
            detailBrowserVm.openFolder(folder)
            replaceOrPushDetail(BrowserFolderRoute.from(detailBrowserVm.state.value.stack))
        }

        fun openPlaylist(info: PlaylistInfo) {
            detailPlaylistsVm.openPlaylist(info)
            replaceOrPushDetail(PlaylistDetailRoute(id = info.id, name = info.name))
        }

        LaunchedEffect(tab) {
            if (tab != null && currentTab != tab) resetToTab(tab)
        }

        fun selectTab(t: MainTab) {
            if (onTabChange != null) onTabChange(t) else resetToTab(t)
        }
        val playerState by playerVm.state.collectAsState()
        val videoState by videoVm.state.collectAsState()
        val detailVideoState by detailVideoVm.state.collectAsState()
        val audioState by audioVm.state.collectAsState()
        val detailAudioState by detailAudioVm.state.collectAsState()
        val audioSection by audioVm.section.collectAsState()
        val detailAudioSection by detailAudioVm.section.collectAsState()
        val browserState by browserVm.state.collectAsState()
        val detailBrowserState by detailBrowserVm.state.collectAsState()
        val playlistsState by playlistsVm.state.collectAsState()
        val detailPlaylistsState by detailPlaylistsVm.state.collectAsState()
        val settingsState by settingsVm.state.collectAsState()
        val appLocked = settingsState.appLock.supported &&
            settingsState.appLock.enabled &&
            settingsState.appLock.locked
        val detailBackTarget = shellBackTarget(
            showOverlay = false,
            hasPlaylistDetail = detailPlaylistsState.openPlaylistId != null,
            hasBrowserFolder = detailBrowserState.stack.isNotEmpty(),
            hasAudioEntity = detailAudioState.openedEntityTitle != null,
            hasVideoContainer = detailVideoState.containerId != null,
        )
        val canNavigateBack = backStack.size > 1 || detailBackTarget != null

        fun navigateBack() {
            when (currentRoute) {
                is VideoContainerRoute -> {
                    detailVideoVm.closeContainer()
                    popRoute()
                    return
                }
                is AudioEntityRoute -> {
                    detailAudioVm.closeEntity()
                    popRoute()
                    return
                }
                is BrowserFolderRoute -> {
                    val stackSizeBeforeBack = detailBrowserVm.state.value.stack.size
                    detailBrowserVm.goUp()
                    if (shouldReplaceBrowserDetailAfterBack(singlePaneLayout, stackSizeBeforeBack)) {
                        replaceOrPushDetail(BrowserFolderRoute.from(detailBrowserVm.state.value.stack))
                    } else {
                        popRoute()
                    }
                    return
                }
                is PlaylistDetailRoute -> {
                    detailPlaylistsVm.closeDetail()
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
                ShellBackTarget.PLAYLIST_DETAIL -> detailPlaylistsVm.closeDetail()
                ShellBackTarget.BROWSER_FOLDER -> detailBrowserVm.goUp()
                ShellBackTarget.AUDIO_ENTITY -> detailAudioVm.closeEntity()
                ShellBackTarget.VIDEO_CONTAINER -> detailVideoVm.closeContainer()
                ShellBackTarget.OVERLAY -> Unit
                null -> Unit
            }
        }

        HandleShellBackPress(enabled = canNavigateBack && !appLocked, onBack = ::navigateBack)

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
                // NavigationSuiteScaffold owns the system/navigation insets for
                // its compact bar and wide rail. Keeping this inner scaffold
                // inset-free avoids a second invisible safe-area band between
                // the shared content and adaptive navigation.
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = colors.backgroundDefault,
                topBar = {
                    // Root destinations own their hierarchy (Videos, Audio, Browse, More).
                    // A permanent "VLC" app bar above them duplicates that hierarchy and leaves
                    // empty libraries with three unrelated headers. Only Settings needs shell
                    // chrome while the player owns its immersive HUD.
                    if (showSettings) {
                        TopAppBar(
                            title = {
                                Text(
                                    ShellStrings.settings(),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colors.backgroundDefault,
                                titleContentColor = colors.fontDefault,
                            ),
                            navigationIcon = {
                                if (canNavigateBack) IconButton(onClick = ::navigateBack) {
                                    Icon(
                                        icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = ShellStrings.back(),
                                    )
                                }
                            },
                        )
                    }
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
                                            contentDescription = ShellStrings.shuffleAll(),
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
                            title = playerState.title.ifBlank { ShellStrings.notPlaying() },
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
                    sceneStrategies = listOf(listDetailStrategy),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                    entry<VideoRoute>(metadata = videoListMetadata) {
                        VideoDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = videoState,
                            viewModel = videoVm,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                            onOpenContainer = ::openVideoContainer,
                        )
                    }
                    entry<VideoContainerRoute>(metadata = libraryDetailMetadata) { route ->
                        LaunchedEffect(route) {
                            if (detailVideoVm.state.value.containerId != route.id) {
                                detailVideoVm.openContainer(route.toMediaFolder())
                            }
                        }
                        VideoDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = detailVideoState,
                            viewModel = detailVideoVm,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                            onOpenContainer = ::openVideoContainer,
                            onNavigateBack = ::navigateBack,
                        )
                    }
                    entry<AudioRoute>(metadata = audioListMetadata) {
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
                    entry<AudioEntityRoute>(metadata = libraryDetailMetadata) { route ->
                        LaunchedEffect(route) {
                            if (detailAudioVm.state.value.containerId != route.id) {
                                detailAudioVm.openAudioEntity(route.toAudioEntity())
                            }
                        }
                        AudioDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = detailAudioState,
                            section = detailAudioSection,
                            viewModel = detailAudioVm,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                            onOpenEntity = ::openAudioEntity,
                            onNavigateBack = ::navigateBack,
                        )
                    }
                    entry<BrowserRoute>(metadata = browserListMetadata) {
                        BrowserDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = browserState,
                            viewModel = browserVm,
                            onOpenPlayer = ::openPlayer,
                            onOpenFolder = ::openBrowserFolder,
                        )
                    }
                    entry<BrowserFolderRoute>(metadata = libraryDetailMetadata) { route ->
                        LaunchedEffect(route) {
                            detailBrowserVm.restoreFolderStack(route.toMediaFolders())
                        }
                        BrowserDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = detailBrowserState,
                            viewModel = detailBrowserVm,
                            onOpenPlayer = ::openPlayer,
                            onOpenFolder = ::openBrowserFolder,
                            onNavigateUp = ::navigateBack,
                        )
                    }
                    entry<PlaylistsRoute>(metadata = playlistsListMetadata) {
                        PlaylistsDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = playlistsState,
                            viewModel = playlistsVm,
                            onOpenPlayer = ::openPlayer,
                            onOpenPlaylist = ::openPlaylist,
                        )
                    }
                    entry<PlaylistDetailRoute>(metadata = libraryDetailMetadata) { route ->
                        LaunchedEffect(route) {
                            if (detailPlaylistsVm.state.value.openPlaylistId != route.id) {
                                detailPlaylistsVm.openPlaylist(route.toPlaylistInfo())
                            }
                        }
                        PlaylistsDestination(
                            modifier = Modifier.fillMaxSize(),
                            state = detailPlaylistsState,
                            viewModel = detailPlaylistsVm,
                            onOpenPlayer = ::openPlayer,
                            onOpenPlaylist = ::openPlaylist,
                            onNavigateBack = ::navigateBack,
                        )
                    }
                    entry<MoreRoute>(metadata = rootTransitionMetadata) {
                        MoreDestination(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = moreVm,
                            onOpenSettings = {
                                if (onOpenSettings != null) onOpenSettings() else backStack.add(SettingsRoute)
                            },
                            onOpenAbout = { backStack.add(AboutRoute) },
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
                            hostCallbacks = hostCallbacks,
                        )
                    }
                    entry<SettingsRoute>(metadata = detailTransitionMetadata) {
                        SettingsDestination(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = settingsVm,
                        )
                    }
                    entry<AboutRoute>(metadata = detailTransitionMetadata) {
                        AboutDestination(
                            hostCallbacks = hostCallbacks,
                            onBack = ::navigateBack,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            )
        }
        }

        if (appLocked) {
            AppLockGate(onUnlock = settingsVm::unlockAppLock)
        }
    }
}

@Composable
private fun LibraryDetailPlaceholder(icon: MaterialIcon) {
    VLCEmptyState(
        loading = false,
        text = ShellStrings.selectLibraryItem(),
        symbol = icon,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun AppLockGate(onUnlock: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VLCThemeDefaults.colors.backgroundDefault,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VLCIconChip(size = 56.dp) { tint ->
                    Icon(
                        icon = MaterialSymbols.Filled.Lock,
                        contentDescription = null,
                        tint = tint,
                    )
                }
                Text(ShellStrings.appLocked(), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onUnlock) { Text(ShellStrings.unlock()) }
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

@Composable
private fun MainTab.displayName(): String =
    when (this) {
        MainTab.VIDEO -> ShellStrings.video()
        MainTab.AUDIO -> ShellStrings.audio()
        MainTab.BROWSER -> ShellStrings.browse()
        MainTab.PLAYLISTS -> ShellStrings.playlists()
        MainTab.MORE -> ShellStrings.more()
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
    val state by vm.state.collectAsStateWithLifecycle()
    VLCUtilityPane(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        item {
            SettingsGroup(title = ShellStrings.playback()) {
                row { ToggleRow(ShellStrings.resumeAudio(), state.audioResume, vm::setAudioResume) }
                row { ToggleRow(ShellStrings.resumeVideo(), state.videoResume, vm::setVideoResume) }
                row { PlaybackSpeedStepperRow(
                    title = ShellStrings.defaultAudioPlaybackSpeed(),
                    rate = state.defaultAudioPlaybackSpeed,
                    onChange = vm::setDefaultAudioPlaybackSpeed,
                ) }
                row { PlaybackSpeedStepperRow(
                    title = ShellStrings.defaultVideoPlaybackSpeed(),
                    rate = state.defaultVideoPlaybackSpeed,
                    onChange = vm::setDefaultVideoPlaybackSpeed,
                ) }
                row { ToggleRow(ShellStrings.playbackHistory(), state.playbackHistory, vm::setPlaybackHistory) }
                row { ToggleRow(ShellStrings.incognito(), state.incognito, vm::setIncognito) }
                row { ValueStepperRow(
                    title = ShellStrings.videoHudTimeout(), value = "${state.videoHudTimeoutSeconds}s",
                    decreaseEnabled = state.videoHudTimeoutSeconds > 1, increaseEnabled = state.videoHudTimeoutSeconds < 10,
                    onDecrease = { vm.setVideoHudTimeout(state.videoHudTimeoutSeconds - 1) },
                    onIncrease = { vm.setVideoHudTimeout(state.videoHudTimeoutSeconds + 1) },
                ) }
            }
        }
        if (state.appLock.supported) item {
            SettingsGroup(title = ShellStrings.privacy()) {
                row {
                    ToggleRow(
                        title = ShellStrings.appLock(),
                        checked = state.appLock.enabled,
                        onChange = { enabled ->
                            if (enabled) vm.enableAppLock() else vm.disableAppLock()
                        },
                    )
                }
                row {
                    Text(
                        ShellStrings.appLockSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = VLCThemeDefaults.colors.fontLight,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    )
                }
                if (state.appLock.enabled && state.appLock.biometricsAvailable) {
                    row {
                        ToggleRow(
                            title = ShellStrings.useBiometrics(),
                            checked = state.appLock.biometricsEnabled,
                            onChange = vm::setAppLockBiometrics,
                        )
                    }
                    row {
                        Text(
                            ShellStrings.useBiometricsSummary(),
                            style = MaterialTheme.typography.bodySmall,
                            color = VLCThemeDefaults.colors.fontLight,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup(title = ShellStrings.library()) {
                row { ToggleRow(ShellStrings.videoThumbnails(), state.showVideoThumbs, vm::setShowVideoThumbs) }
                row { ToggleRow(ShellStrings.showHeaders(), state.showHeaders, vm::setShowHeaders) }
                row { ToggleRow(ShellStrings.showTrackNumbers(), state.showTrackNumbers, vm::setShowTrackNumbers) }
            }
        }
        item {
            SettingsGroup(title = ShellStrings.browser()) {
                row { ToggleRow(ShellStrings.showHiddenFiles(), state.showHiddenFiles, vm::setShowHiddenFiles) }
                row { ToggleRow(ShellStrings.multimediaFilesOnly(), state.showOnlyMultimedia, vm::setShowOnlyMultimedia) }
            }
        }
        if (state.supportsNetworkBrowsing || state.supportsRemoteAccess) item {
            val remoteAccessAddress = state.remoteAccessAddress
            val remoteAccessError = state.remoteAccessError
            SettingsGroup(title = ShellStrings.network()) {
                if (state.supportsNetworkBrowsing) {
                    row { ToggleRow(ShellStrings.browseNetwork(), state.browseNetwork, vm::setBrowseNetwork) }
                }
                if (state.supportsRemoteAccess) {
                    row { ToggleRow(ShellStrings.remoteAccessServer(), state.remoteAccess, vm::setRemoteAccess) }
                    if (state.remoteAccessStarting || remoteAccessAddress != null || remoteAccessError != null) {
                        row {
                            when {
                                state.remoteAccessStarting -> Text(
                                    ShellStrings.remoteAccessStarting(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                )
                                remoteAccessAddress != null -> Column(
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(ShellStrings.remoteAccessUploadAddress(), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        remoteAccessAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VLCThemeDefaults.colors.primary,
                                    )
                                }
                                remoteAccessError != null -> Text(
                                    remoteAccessError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                )
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private class SettingsGroupScope {
    val rows = mutableListOf<@Composable () -> Unit>()
    fun row(content: @Composable () -> Unit) { rows += content }
}

@Composable
private fun SettingsGroup(title: String, content: SettingsGroupScope.() -> Unit) {
    val scope = SettingsGroupScope().apply(content)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = VLCThemeDefaults.colors.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        VLCSettingsCard(rows = scope.rows, dividerInset = 20.dp)
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
private fun ValueStepperRow(
    title: String,
    value: String,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onDecrease, enabled = decreaseEnabled) { Text("−") }
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 4.dp))
        TextButton(onClick = onIncrease, enabled = increaseEnabled) { Text("+") }
    }
}

private val PlaybackSpeedChoices = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 6f, 8f)

@Composable
private fun PlaybackSpeedStepperRow(title: String, rate: Float, onChange: (Float) -> Unit) {
    val currentIndex = PlaybackSpeedChoices.indexOf(rate).takeIf { it >= 0 }
        ?: PlaybackSpeedChoices.indices.minBy { kotlin.math.abs(PlaybackSpeedChoices[it] - rate) }
    ValueStepperRow(
        title = title,
        value = "${PlaybackSpeedChoices[currentIndex]}×",
        decreaseEnabled = currentIndex > 0,
        increaseEnabled = currentIndex < PlaybackSpeedChoices.lastIndex,
        onDecrease = { onChange(PlaybackSpeedChoices[currentIndex - 1]) },
        onIncrease = { onChange(PlaybackSpeedChoices[currentIndex + 1]) },
    )
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
                    contentDescription = if (playing) ShellStrings.pause() else ShellStrings.play(),
                )
            }
        }
    }
}
