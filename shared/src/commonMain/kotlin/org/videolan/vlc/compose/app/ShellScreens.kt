package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCPageHeader
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.AudioSection
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel

/**
 * Every navigation intent a shell screen can raise. The Compose shell turns these into Nav3
 * back-stack changes; native hosts push and present platform view controllers instead.
 */
interface ShellNavigator {
    fun openVideoContainer(folder: MediaFolder)
    fun openAudioEntity(item: MediaItem)
    fun openBrowserFolder(folder: MediaFolder)
    fun openPlaylist(info: PlaylistInfo)
    fun openSettings()
    fun openSettingsSection(id: String)
    fun open(route: VlcShellRoute)
    fun openPlayer()
    fun openMediaPlayer(item: MediaItem)
    fun back()
}

/**
 * The feature models a screen reads. They are created on first use, so a native host that gives
 * every screen its own models only pays for the features that screen shows.
 */
class ShellScreenModels(
    video: () -> VideoListViewModel,
    audio: () -> AudioListViewModel,
    browser: () -> BrowserViewModel,
    playlists: () -> PlaylistsViewModel,
    more: () -> MoreHubViewModel,
    player: () -> PlayerViewModel,
    settings: () -> SettingsViewModel,
) {
    private val created = mutableListOf<() -> Unit>()
    private fun <T> owned(factory: () -> T, clear: (T) -> Unit) = lazy { factory().also { created += { clear(it) } } }

    val video by owned(video) { it.onCleared() }
    val audio by owned(audio) { it.onCleared() }
    val browser by owned(browser) { it.onCleared() }
    val playlists by owned(playlists) { it.onCleared() }
    val more by owned(more) { it.onCleared() }
    val player by owned(player) { it.onCleared() }
    val settings by owned(settings) { it.onCleared() }

    /** Clears only the models this instance created; never call it for models a shell shares. */
    fun clear() = created.forEach { it() }
}

/**
 * The content of one shell destination, independent of how it is hosted. Detail routes restore
 * their own model state from the route, so the same call works for a Nav3 entry and for a
 * standalone native screen.
 */
@Composable
fun ShellScreenContent(
    route: VlcShellRoute,
    models: ShellScreenModels,
    navigator: ShellNavigator,
    hostCallbacks: ShellHostCallbacks,
    modifier: Modifier = Modifier,
    selectedSettingsSectionId: String? = null,
) {
    val screenModifier = modifier.fillMaxSize().background(VLCThemeDefaults.colors.backgroundDefault)
    when (route) {
        VideoRoute -> {
            val state by models.video.state.collectAsState()
            val playerState by models.player.state.collectAsState()
            VideoDestination(
                modifier = screenModifier,
                state = state,
                onOpenVideo = navigator::openMediaPlayer,
                playerState = playerState,
                onResumeVideo = {
                    if (!playerState.playing) models.player.togglePlayPause()
                    navigator.openPlayer()
                },
                viewModel = models.video,
                hostCallbacks = hostCallbacks,
                onOpenPlayer = navigator::openPlayer,
                onOpenContainer = navigator::openVideoContainer,
                showPlayAllInMenu = state.count <= 0 && state.items.isEmpty(),
            )
        }
        is VideoContainerRoute -> {
            LaunchedEffect(route) {
                if (models.video.state.value.containerId != route.id) models.video.openContainer(route.toMediaFolder())
            }
            val state by models.video.state.collectAsState()
            VideoDestination(
                modifier = screenModifier,
                state = state,
                onOpenVideo = navigator::openMediaPlayer,
                viewModel = models.video,
                hostCallbacks = hostCallbacks,
                onOpenPlayer = navigator::openPlayer,
                onOpenContainer = navigator::openVideoContainer,
                onNavigateBack = navigator::back,
            )
        }
        AudioRoute -> {
            val state by models.audio.state.collectAsState()
            val section by models.audio.section.collectAsState()
            AudioDestination(
                modifier = screenModifier,
                state = state,
                onOpenMedia = navigator::openMediaPlayer,
                section = section,
                viewModel = models.audio,
                hostCallbacks = hostCallbacks,
                onOpenPlayer = navigator::openPlayer,
                onOpenEntity = navigator::openAudioEntity,
                showPlayAllInMenu = section != AudioSection.TRACKS || state.count <= 1,
            )
        }
        is AudioEntityRoute -> {
            LaunchedEffect(route) {
                if (models.audio.state.value.containerId != route.id) models.audio.openAudioEntity(route.toAudioEntity())
            }
            val state by models.audio.state.collectAsState()
            val section by models.audio.section.collectAsState()
            AudioDestination(
                modifier = screenModifier,
                state = state,
                onOpenMedia = navigator::openMediaPlayer,
                section = section,
                viewModel = models.audio,
                hostCallbacks = hostCallbacks,
                onOpenPlayer = navigator::openPlayer,
                onOpenEntity = navigator::openAudioEntity,
                onNavigateBack = navigator::back,
            )
        }
        BrowserRoute -> {
            val state by models.browser.state.collectAsState()
            BrowserDestination(
                modifier = screenModifier,
                state = state,
                viewModel = models.browser,
                onOpenPlayer = navigator::openPlayer,
                onOpenFolder = navigator::openBrowserFolder,
            )
        }
        is BrowserFolderRoute -> {
            LaunchedEffect(route) { models.browser.restoreFolderStack(route.toMediaFolders()) }
            val state by models.browser.state.collectAsState()
            BrowserDestination(
                modifier = screenModifier,
                state = state,
                viewModel = models.browser,
                onOpenPlayer = navigator::openPlayer,
                onOpenFolder = navigator::openBrowserFolder,
                onNavigateUp = navigator::back,
            )
        }
        PlaylistsRoute -> {
            val state by models.playlists.state.collectAsState()
            PlaylistsDestination(
                modifier = screenModifier,
                state = state,
                viewModel = models.playlists,
                onOpenPlayer = navigator::openPlayer,
                onOpenPlaylist = navigator::openPlaylist,
            )
        }
        is PlaylistDetailRoute -> {
            LaunchedEffect(route) {
                if (models.playlists.state.value.openPlaylistId != route.id) models.playlists.openPlaylist(route.toPlaylistInfo())
            }
            val state by models.playlists.state.collectAsState()
            PlaylistsDestination(
                modifier = screenModifier,
                state = state,
                viewModel = models.playlists,
                onOpenPlayer = navigator::openPlayer,
                onOpenPlaylist = navigator::openPlaylist,
                onNavigateBack = navigator::back,
            )
        }
        MoreRoute -> MoreDestination(
            modifier = screenModifier,
            viewModel = models.more,
            onOpenSettings = navigator::openSettings,
            onOpenAbout = { navigator.open(AboutRoute) },
            onOpenRemote = null,
            hostCallbacks = hostCallbacks,
            onOpenPlayer = navigator::openPlayer,
        )
        SettingsRoute -> SecondaryShellDestination(title = ShellStrings.settings(), onBack = navigator::back) {
            SettingsDestination(
                modifier = screenModifier,
                viewModel = models.settings,
                onOpenSection = navigator::openSettingsSection,
                selectedSectionId = selectedSettingsSectionId,
            )
        }
        is SettingsSectionRoute -> {
            val contributed by models.settings.contributed.collectAsState()
            SecondaryShellDestination(
                title = contributed.section(route.id)?.title ?: ShellStrings.settings(),
                onBack = navigator::back,
                actions = { SettingsSectionInfoAction(models.settings, route.id) },
            ) {
                SettingsSectionDestination(
                    modifier = screenModifier,
                    viewModel = models.settings,
                    sectionId = route.id,
                    onOpenSection = navigator::openSettingsSection,
                )
            }
        }
        AboutRoute -> SecondaryShellDestination(title = ShellStrings.about(), onBack = navigator::back) {
            AboutDestination(
                hostCallbacks = hostCallbacks,
                onBack = navigator::back,
                onOpenLibraries = { navigator.open(AboutLibrariesRoute) },
                onOpenAuthors = { navigator.open(AboutAuthorsRoute) },
                modifier = screenModifier,
            )
        }
        AboutLibrariesRoute -> SecondaryShellDestination(title = ShellStrings.libraries(), onBack = navigator::back) {
            AboutLibrariesDestination(hostCallbacks = hostCallbacks, modifier = screenModifier)
        }
        AboutAuthorsRoute -> SecondaryShellDestination(title = ShellStrings.authors(), onBack = navigator::back) {
            AboutAuthorsDestination(hostCallbacks = hostCallbacks, modifier = screenModifier)
        }
        PlayerRoute -> Unit
    }
}

/** The navigation-bar title a native host shows for a pushed route ("" when a hero carries it). */
@Composable
fun shellRouteTitle(route: VlcShellRoute, models: ShellScreenModels): String = when (route) {
    VideoRoute -> ShellStrings.videosTitle()
    AudioRoute -> ShellStrings.audio()
    BrowserRoute -> ShellStrings.browse()
    PlaylistsRoute -> ShellStrings.playlists()
    MoreRoute -> ShellStrings.more()
    is VideoContainerRoute -> route.title
    is AudioEntityRoute -> route.title
    is BrowserFolderRoute -> route.stack.last().title
    is PlaylistDetailRoute -> route.name
    SettingsRoute -> ShellStrings.settings()
    is SettingsSectionRoute -> {
        val contributed by models.settings.contributed.collectAsState()
        contributed.section(route.id)?.title ?: ShellStrings.settings()
    }
    AboutRoute -> ShellStrings.about()
    AboutLibrariesRoute -> ShellStrings.libraries()
    AboutAuthorsRoute -> ShellStrings.authors()
    PlayerRoute -> ""
}

/** Album, artist and playlist pages open with a hero that already shows the title. */
fun VlcShellRoute.hasCollectionHero(): Boolean = this is AudioEntityRoute || this is PlaylistDetailRoute

/**
 * The player as a standalone screen. While a just-requested item has not reached the player state
 * yet, show a neutral stage rather than flashing the previous media.
 */
@Composable
fun ShellPlayerScreen(
    player: PlayerViewModel,
    requestedMedia: MediaItem?,
    onClose: () -> Unit,
    playerSurface: PlayerSurface,
    hostCallbacks: ShellHostCallbacks,
    modifier: Modifier = Modifier,
) {
    val playerState by player.state.collectAsState()
    val awaitingMedia = requestedMedia != null && requestedMedia.uri != playerState.uri
    if (awaitingMedia) {
        val video = requestedMedia?.isVideo == true
        val background = if (video) Color.Black else MaterialTheme.colorScheme.background
        val foreground = if (video) Color.White else MaterialTheme.colorScheme.onBackground
        Box(modifier.fillMaxSize().background(background)) {
            playerState.error?.let { message ->
                Text(message, color = foreground, modifier = Modifier.align(Alignment.Center).padding(32.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()) {
                Icon(MaterialSymbols.AutoMirrored.Filled.ArrowBack, ShellStrings.back(), tint = foreground)
            }
        }
    } else {
        PlayerDestination(
            modifier = modifier.fillMaxSize().background(VLCThemeDefaults.colors.backgroundDefault),
            viewModel = player,
            onClose = onClose,
            playerSurface = playerSurface,
            hostCallbacks = hostCallbacks,
        )
    }
}

/**
 * One header for every non-library detail route, kept inside the destination so it moves with the
 * page during push/pop. Under native navigation the platform bar replaces it entirely.
 */
@Composable
internal fun SecondaryShellDestination(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    // The destination owns an opaque surface so slide transitions never reveal the route below.
    Surface(modifier = Modifier.fillMaxSize(), color = VLCThemeDefaults.colors.backgroundDefault) {
        VLCUtilityPane {
            Column(modifier = Modifier.fillMaxSize()) {
                VLCPageHeader(
                    title = title,
                    navigationIcon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                    navigationContentDescription = ShellStrings.back(),
                    onNavigate = onBack,
                    actions = actions,
                )
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) { content() }
            }
        }
    }
}
