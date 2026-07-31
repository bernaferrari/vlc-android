package org.videolan.vlc.compose.app

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCIconChip
import org.videolan.vlc.compose.components.VLCEmptyState
import org.videolan.vlc.compose.components.VLCPageHeader
import org.videolan.vlc.compose.player.FallbackPlayerSurface
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.compose.theme.VLCAppTheme
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

    VLCAppTheme {
        val colors = VLCThemeDefaults.colors
        val motion = LocalVLCMotion.current
        val initialRoute = remember(initialTab) { initialTab.toVlcShellRoute() }
        val backStack = rememberNavBackStack(vlcShellNavSavedStateConfiguration, initialRoute)
        LaunchedEffect(backStack, initialRoute) {
            val canonical = canonicalVlcShellRouteStack(
                restored = backStack.filterIsInstance<VlcShellRoute>(),
                fallbackRoot = initialRoute,
            )
            if (canonical != backStack) {
                backStack.clear()
                backStack.addAll(canonical)
            }
        }
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
        val showSecondaryChrome = currentRoute == SettingsRoute ||
            currentRoute == AboutRoute ||
            currentRoute == AboutLibrariesRoute ||
            currentRoute == AboutAuthorsRoute
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
        val moreState by moreVm.state.collectAsState()
        val settingsState by settingsVm.state.collectAsState()
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
            } + NavDisplay.predictivePopTransitionSpec {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                )
            }
        }
        // Match QuietGuard's intentionally small Nav3 motion contract: push the new destination
        // in, and on pop let the old destination leave. Keeping the other surface stationary
        // prevents a second header from reading as a stacked or expanding app bar.
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
            } + NavDisplay.predictivePopTransitionSpec { _ ->
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
        // The player is reached from the persistent mini-player, so it should feel like that
        // surface is expanding into a full-screen destination rather than another horizontal
        // page being pushed on top of the library. The short fade keeps the native video surface
        // from flashing through while the vertical travel preserves that spatial relationship.
        val playerTransitionMetadata = remember(motion) {
            NavDisplay.transitionSpec {
                ContentTransform(
                    targetContentEnter = slideInVertically(
                        animationSpec = tween(
                            durationMillis = if (motion.reducedMotion) 0 else 260,
                            easing = VLCMotion.EmphasizedDecelerate,
                        ),
                        initialOffsetY = { fullHeight -> fullHeight },
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = if (motion.reducedMotion) 0 else 150,
                            easing = VLCMotion.EmphasizedDecelerate,
                        ),
                    ),
                    initialContentExit = ExitTransition.None,
                )
            } + NavDisplay.popTransitionSpec {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = slideOutVertically(
                        animationSpec = tween(
                            durationMillis = if (motion.reducedMotion) 0 else 190,
                            easing = VLCMotion.EmphasizedAccelerate,
                        ),
                        targetOffsetY = { fullHeight -> fullHeight },
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = if (motion.reducedMotion) 0 else 120,
                            easing = VLCMotion.EmphasizedAccelerate,
                        ),
                    ),
                )
            } + NavDisplay.predictivePopTransitionSpec { _ ->
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = slideOutVertically(
                        animationSpec = tween(
                            durationMillis = if (motion.reducedMotion) 0 else 190,
                            easing = VLCMotion.EmphasizedAccelerate,
                        ),
                        targetOffsetY = { fullHeight -> fullHeight },
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = if (motion.reducedMotion) 0 else 120,
                            easing = VLCMotion.EmphasizedAccelerate,
                        ),
                    ),
                )
            }
        }
        // Mirror QuietGuard's selective list-detail use: an empty library is one clear state, not
        // an empty half-screen plus an unrelated "select an item" message. Once a library has
        // content, wide hosts retain the productive list/detail relationship.
        val videoHasLibraryContent = videoState.items.isNotEmpty() || videoState.groups.isNotEmpty()
        val audioHasLibraryContent = audioState.items.isNotEmpty() || audioState.audioEntities.isNotEmpty()
        val browserHasLibraryContent = browserState.favorites.isNotEmpty() ||
            browserState.folders.isNotEmpty() ||
            browserState.networkRoots.isNotEmpty() ||
            browserState.media.isNotEmpty()
        val playlistsHaveLibraryContent = playlistsState.playlists.isNotEmpty()
        val videoListMetadata = remember(
            singlePaneLayout,
            rootTransitionMetadata,
            videoHasLibraryContent,
        ) {
            if (
                !shouldUseWideLibraryDetailLayout(
                    singlePaneLayout = singlePaneLayout,
                    hasLibraryContent = videoHasLibraryContent,
                )
            ) {
                rootTransitionMetadata
            } else {
                ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        LibraryDetailPlaceholder(MaterialSymbols.Filled.VideoLibrary)
                    },
                )
            }
        }
        val audioListMetadata = remember(
            singlePaneLayout,
            rootTransitionMetadata,
            audioHasLibraryContent,
        ) {
            if (
                !shouldUseWideLibraryDetailLayout(
                    singlePaneLayout = singlePaneLayout,
                    hasLibraryContent = audioHasLibraryContent,
                )
            ) {
                rootTransitionMetadata
            } else {
                ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        LibraryDetailPlaceholder(MaterialSymbols.Filled.MusicNote)
                    },
                )
            }
        }
        val browserListMetadata = remember(
            singlePaneLayout,
            rootTransitionMetadata,
            browserHasLibraryContent,
        ) {
            if (
                !shouldUseWideLibraryDetailLayout(
                    singlePaneLayout = singlePaneLayout,
                    hasLibraryContent = browserHasLibraryContent,
                )
            ) {
                rootTransitionMetadata
            } else {
                ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        LibraryDetailPlaceholder(MaterialSymbols.Filled.Folder)
                    },
                )
            }
        }
        val playlistsListMetadata = remember(singlePaneLayout, rootTransitionMetadata, playlistsHaveLibraryContent) {
            if (!shouldUseWideLibraryDetailLayout(singlePaneLayout, playlistsHaveLibraryContent)) {
                rootTransitionMetadata
            } else {
                ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        LibraryDetailPlaceholder(MaterialSymbols.Filled.QueueMusic)
                    },
                )
            }
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
            if (!showPlayer) pushNav3Route(backStack, PlayerRoute)
        }

        fun popRoute(): Boolean = popNav3Route(backStack)

        fun replaceOrPushDetail(route: VlcShellRoute) {
            val current = backStack.lastOrNull() as? VlcShellRoute
            val replacesCurrentDetail = shouldReplaceDetailRoute(singlePaneLayout, current, route)
            if (replacesCurrentDetail) backStack[backStack.lastIndex] = route else pushNav3Route(backStack, route)
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
            // A navigation-suite item is a root destination, not a secondary action. Always
            // collapse the current journey first (including Player, Settings, and library
            // details), then notify a controlled host so its selected-tab state stays in sync.
            resetToTab(t)
            if (onTabChange != null && tab != t) onTabChange(t)
        }
        val appLocked = settingsState.appLock.supported &&
            settingsState.appLock.enabled &&
            settingsState.appLock.locked
        val canNavigateBack = backStack.size > 1
        val hasActiveSelection = when (currentRoute) {
            VideoRoute -> videoState.selection.isNotEmpty()
            is VideoContainerRoute -> detailVideoState.selection.isNotEmpty()
            AudioRoute -> audioState.selection.isNotEmpty()
            is AudioEntityRoute -> detailAudioState.selection.isNotEmpty()
            BrowserRoute -> browserState.selection.isNotEmpty()
            is BrowserFolderRoute -> detailBrowserState.selection.isNotEmpty()
            PlaylistsRoute -> playlistsState.selection.isNotEmpty()
            is PlaylistDetailRoute -> detailPlaylistsState.selection.isNotEmpty()
            MoreRoute -> moreState.historySelection.isNotEmpty()
            PlayerRoute, SettingsRoute, AboutRoute, AboutLibrariesRoute, AboutAuthorsRoute -> false
        }

        fun clearCurrentSelection(): Boolean = when (currentRoute) {
            VideoRoute -> videoState.selection.isNotEmpty().also { if (it) videoVm.clearSelection() }
            is VideoContainerRoute -> detailVideoState.selection.isNotEmpty().also { if (it) detailVideoVm.clearSelection() }
            AudioRoute -> audioState.selection.isNotEmpty().also { if (it) audioVm.clearSelection() }
            is AudioEntityRoute -> detailAudioState.selection.isNotEmpty().also { if (it) detailAudioVm.clearSelection() }
            BrowserRoute -> browserState.selection.isNotEmpty().also { if (it) browserVm.clearSelection() }
            is BrowserFolderRoute -> detailBrowserState.selection.isNotEmpty().also { if (it) detailBrowserVm.clearSelection() }
            PlaylistsRoute -> playlistsState.selection.isNotEmpty().also { if (it) playlistsVm.clearSelection() }
            is PlaylistDetailRoute -> detailPlaylistsState.selection.isNotEmpty().also { if (it) detailPlaylistsVm.clearSelection() }
            MoreRoute -> moreState.historySelection.isNotEmpty().also { if (it) moreVm.clearHistorySelection() }
            PlayerRoute, SettingsRoute, AboutRoute, AboutLibrariesRoute, AboutAuthorsRoute -> false
        }

        fun navigateBack() {
            if (clearCurrentSelection()) return
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
            popRoute()
        }

        if (appLocked) {
            AppLockGate(onUnlock = settingsVm::unlockAppLock)
        } else {
            HandleShellBackPress(
                enabled = shouldInterceptShellBack(
                    appLocked = false,
                    hasActiveSelection = hasActiveSelection,
                    canNavigateBack = canNavigateBack,
                ),
                onBack = ::navigateBack,
            )

            VlcAdaptiveNavigationSuite(
                modifier = modifier,
                enabled = showBottomBar && !showPlayer,
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
                floatingActionButton = {
                    if (!showPlayer && !showSecondaryChrome) {
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
                    AnimatedVisibility(
                        visible = showBottomBar && !showPlayer && playerState.hasMedia,
                        enter = slideInVertically(
                            animationSpec = tween(
                                durationMillis = motion.durationShort,
                                easing = VLCMotion.EmphasizedDecelerate,
                            ),
                            initialOffsetY = { height -> height / 2 },
                        ) + fadeIn(animationSpec = tween(motion.durationShort)),
                        exit = slideOutVertically(
                            animationSpec = tween(
                                durationMillis = motion.durationShort,
                                easing = VLCMotion.EmphasizedAccelerate,
                            ),
                            targetOffsetY = { height -> height / 2 },
                        ) + fadeOut(animationSpec = tween(motion.durationShort)),
                    ) {
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
                // Every non-player route owns its header inside Nav3. Applying the status inset
                // once here keeps root and secondary destinations on the same safe-area baseline.
                val contentMod = Modifier
                    .padding(padding)
                    .then(
                        if (showPlayer) Modifier else Modifier.statusBarsPadding(),
                    )
                    .fillMaxSize()
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
                            onOpenSettings = { pushNav3Route(backStack, SettingsRoute) },
                            onOpenAbout = { pushNav3Route(backStack, AboutRoute) },
                            onOpenRemote = null,
                            hostCallbacks = hostCallbacks,
                            onOpenPlayer = ::openPlayer,
                        )
                    }
                    entry<PlayerRoute>(metadata = playerTransitionMetadata) {
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
                        SecondaryNav3Destination(
                            title = ShellStrings.settings(),
                            onBack = ::navigateBack,
                        ) {
                            SettingsDestination(
                                modifier = Modifier.fillMaxSize(),
                                viewModel = settingsVm,
                            )
                        }
                    }
                    entry<AboutRoute>(metadata = detailTransitionMetadata) {
                        SecondaryNav3Destination(
                            title = ShellStrings.about(),
                            onBack = ::navigateBack,
                        ) {
                            AboutDestination(
                                hostCallbacks = hostCallbacks,
                                onBack = ::navigateBack,
                                onOpenLibraries = { pushNav3Route(backStack, AboutLibrariesRoute) },
                                onOpenAuthors = { pushNav3Route(backStack, AboutAuthorsRoute) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    entry<AboutLibrariesRoute>(metadata = detailTransitionMetadata) {
                        SecondaryNav3Destination(
                            title = ShellStrings.libraries(),
                            onBack = ::navigateBack,
                        ) {
                            AboutLibrariesDestination(
                                hostCallbacks = hostCallbacks,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    entry<AboutAuthorsRoute>(metadata = detailTransitionMetadata) {
                        SecondaryNav3Destination(
                            title = ShellStrings.authors(),
                            onBack = ::navigateBack,
                        ) {
                            AboutAuthorsDestination(
                                hostCallbacks = hostCallbacks,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
                )
            }
        }
    }
}
}

/**
 * One Nav3-owned header for every non-library detail route. Keeping it inside the destination
 * means the header moves with its page during push/pop instead of living outside Nav3 and briefly
 * stacking with the destination below it.
 */
@Composable
private fun SecondaryNav3Destination(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    // The destination owns an opaque surface so slide transitions never reveal the route below
    // through an empty/loading state. This is especially important for Settings and About
    // details, which otherwise appeared transparent until the animation completed.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VLCThemeDefaults.colors.backgroundDefault,
    ) {
        VLCUtilityPane {
            Column(modifier = Modifier.fillMaxSize()) {
                VLCPageHeader(
                    title = title,
                    navigationIcon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                    navigationContentDescription = ShellStrings.back(),
                    onNavigate = onBack,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    content()
                }
            }
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
        modifier = Modifier
            .fillMaxSize()
            .semantics { dialog() },
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
