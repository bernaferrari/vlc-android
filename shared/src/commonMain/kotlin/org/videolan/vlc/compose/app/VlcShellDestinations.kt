package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.components.VLCSectionOption
import org.videolan.vlc.compose.components.VLCSectionSelector
import org.videolan.vlc.compose.player.FallbackPlayerSurface
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.compose.player.VideoSurfaceWithHud
import org.videolan.vlc.compose.player.VlcPlayerScreen
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.AudioSection
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.BrowserUiState
import org.videolan.vlc.viewmodel.MediaListUiState
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerUiState
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsUiState
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel
import org.videolan.vlc.viewmodel.isVirtualAudioEntry
import org.videolan.vlc.viewmodel.defaultPlaybackActionOpensPlayer

@Composable
internal fun VideoDestination(
    modifier: Modifier,
    state: MediaListUiState,
    viewModel: VideoListViewModel,
    hostCallbacks: ShellHostCallbacks,
    onOpenPlayer: () -> Unit,
    onOpenContainer: (MediaFolder) -> Unit,
    onNavigateBack: () -> Unit = viewModel::closeContainer,
    showPlayAllInMenu: Boolean = true,
    playerState: PlayerUiState? = null,
    onResumeVideo: (() -> Unit)? = null,
    onOpenVideo: ((MediaItem) -> Unit)? = null,
) {
    RichMediaListPane(
        state = state,
        title = ShellStrings.videosTitle(),
        emptyLabel = ShellStrings.noVideos(),
        emptyMessage = ShellStrings.noVideosMessage(),
        pagingFlow = viewModel.pagingFlow,
        groups = state.groups,
        headerContent = if (playerState != null && onResumeVideo != null &&
            shouldShowContinueWatching(state, playerState)
        ) {
            {
                ContinueWatchingCard(
                    state = playerState,
                    onClick = onResumeVideo,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        } else null,
        onQuery = viewModel::setQuery,
        onRetry = viewModel::refresh,
        onPlay = {
            viewModel.play(it)
            if (defaultPlaybackActionOpensPlayer(state.defaultPlaybackAction)) {
                if (onOpenVideo != null) onOpenVideo(it) else onOpenPlayer()
            }
        },
        onPlayAll = { viewModel.playAll(); onOpenPlayer() },
        showPlayAllInMenu = showPlayAllInMenu,
        primaryAction = LibraryPrimaryAction(ShellStrings.playAll(), MaterialSymbols.Filled.PlayArrow) {
            viewModel.playAll()
            onOpenPlayer()
        }.takeIf { state.containerTitle == null && state.count > 0 },
        onPlayNext = viewModel::playNext,
        onAppend = viewModel::append,
        onToggleSelect = viewModel::toggleSelect,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
        onPlaySelection = { viewModel.playSelection(); onOpenPlayer() },
        onAppendSelection = viewModel::appendSelection,
        onFavoriteSelection = viewModel::favoriteSelection,
        onSetViewMode = viewModel::setViewMode,
        onSetSort = viewModel::setSort,
        onToggleSortDesc = viewModel::toggleSortDesc,
        onToggleFavorites = viewModel::toggleOnlyFavorites,
        onRescan = viewModel::rescan,
        canHandleHostAction = hostCallbacks::supportsContextAction,
        onCtx = { item, option ->
            when (option) {
                ContextOption.CTX_DELETE,
                ContextOption.CTX_RENAME,
                ContextOption.CTX_INFORMATION,
                ContextOption.CTX_SHARE,
                ContextOption.CTX_DOWNLOAD_SUBTITLES,
                ContextOption.CTX_ADD_SHORTCUT,
                ContextOption.CTX_SET_RINGTONE,
                ContextOption.CTX_BAN_FOLDER,
                ContextOption.CTX_ADD_TO_PLAYLIST,
                -> hostCallbacks.dispatch(item, option)
                else -> {
                    viewModel.handleCtx(item, option)
                    if (option == ContextOption.CTX_PLAY_ALL) {
                        onOpenPlayer()
                    } else if (option == ContextOption.CTX_PLAY &&
                        defaultPlaybackActionOpensPlayer(state.defaultPlaybackAction)
                    ) {
                        if (onOpenVideo != null) onOpenVideo(item) else onOpenPlayer()
                    }
                }
            }
        },
        onOpenGroup = onOpenContainer,
        onCloseContainer = onNavigateBack,
        onSetGroupingMode = viewModel::setGroupingMode,
        showGroupingToggle = true,
        onDefaultAction = viewModel::setDefaultPlaybackAction,
        emptySymbol = MaterialSymbols.Filled.VideoLibrary,
        emptyActionText = ShellStrings.importMedia().takeIf { hostCallbacks.supportsMediaImport() },
        onEmptyAction = hostCallbacks::onImportMedia,
        modifier = modifier,
    )
}

@Composable
internal fun AudioDestination(
    modifier: Modifier,
    state: MediaListUiState,
    section: AudioSection,
    viewModel: AudioListViewModel,
    hostCallbacks: ShellHostCallbacks,
    onOpenPlayer: () -> Unit,
    onOpenEntity: (MediaItem) -> Unit,
    onNavigateBack: () -> Unit = viewModel::closeEntity,
    showPlayAllInMenu: Boolean = true,
    onOpenMedia: ((MediaItem) -> Unit)? = null,
) {
    RichMediaListPane(
        state = state,
        title = ShellStrings.audio(),
        emptyLabel = ShellStrings.noAudio(),
        emptyMessage = ShellStrings.noAudioMessage(),
        gridArtworkAspectRatio = 1f,
        sections = state.sections,
        pagingFlow = if (section == AudioSection.TRACKS && state.openedEntityTitle == null) {
            viewModel.pagingFlow
        } else {
            null
        },
        headerContent = if (shouldShowAudioSectionSelector(state)) {
            {
                val sections = listOf(
                    AudioSection.TRACKS to ShellStrings.tracks(),
                    AudioSection.ARTISTS to ShellStrings.artists(),
                    AudioSection.ALBUMS to ShellStrings.albums(),
                    AudioSection.GENRES to ShellStrings.genres(),
                )
                VLCSectionSelector(
                    options = sections.map { VLCSectionOption(it.second) },
                    selectedIndex = sections.indexOfFirst { it.first == section }.coerceAtLeast(0),
                    onSelect = { index -> sections.getOrNull(index)?.first?.let(viewModel::setSection) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        } else {
            null
        },
        onQuery = viewModel::setQuery,
        onRetry = viewModel::refresh,
        onPlay = { item ->
            val isEntity = item.uri.startsWith("artist://") ||
                item.uri.startsWith("album://") ||
                item.uri.startsWith("genre://")
            if (isEntity && section != AudioSection.TRACKS) {
                onOpenEntity(item)
            } else {
                viewModel.play(item)
                if (defaultPlaybackActionOpensPlayer(state.defaultPlaybackAction)) {
                    if (onOpenMedia != null && !item.isVirtualAudioEntry()) onOpenMedia(item) else onOpenPlayer()
                }
            }
        },
        onPlayAll = { viewModel.playAll(); onOpenPlayer() },
        showPlayAllInMenu = showPlayAllInMenu,
        primaryAction = LibraryPrimaryAction(ShellStrings.shufflePlay(), MaterialSymbols.Filled.Shuffle) {
            viewModel.shuffleAll()
            onOpenPlayer()
        }.takeIf { section == AudioSection.TRACKS && state.openedEntityTitle == null && state.count > 1 },
        collectionHero = state.openedEntityTitle?.let { entityTitle ->
            {
                AudioEntityHero(
                    title = entityTitle,
                    kind = state.openedEntityKind,
                    items = state.items,
                    onPlay = { viewModel.playAll(); onOpenPlayer() },
                    onShuffle = { viewModel.shuffleAll(); onOpenPlayer() },
                )
            }
        },
        onPlayNext = viewModel::playNext,
        onAppend = viewModel::append,
        onToggleSelect = viewModel::toggleSelect,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
        onPlaySelection = { viewModel.playSelection(); onOpenPlayer() },
        onAppendSelection = viewModel::appendSelection,
        onFavoriteSelection = viewModel::favoriteSelection,
        onSetViewMode = viewModel::setViewMode,
        onSetSort = viewModel::setSort,
        onToggleSortDesc = viewModel::toggleSortDesc,
        onToggleFavorites = viewModel::toggleOnlyFavorites,
        onRescan = viewModel::rescan,
        canHandleHostAction = hostCallbacks::supportsContextAction,
        onCtx = { item, option ->
            when (option) {
                ContextOption.CTX_DELETE,
                ContextOption.CTX_RENAME,
                ContextOption.CTX_INFORMATION,
                ContextOption.CTX_SHARE,
                ContextOption.CTX_DOWNLOAD_SUBTITLES,
                ContextOption.CTX_ADD_SHORTCUT,
                ContextOption.CTX_SET_RINGTONE,
                ContextOption.CTX_BAN_FOLDER,
                ContextOption.CTX_ADD_TO_PLAYLIST,
                -> hostCallbacks.dispatch(item, option)
                else -> {
                    viewModel.handleCtx(item, option)
                    if (option == ContextOption.CTX_PLAY_ALL ||
                        (option == ContextOption.CTX_PLAY &&
                            defaultPlaybackActionOpensPlayer(state.defaultPlaybackAction))
                    ) {
                        if (option == ContextOption.CTX_PLAY && onOpenMedia != null &&
                            !item.isVirtualAudioEntry()) {
                            onOpenMedia(item)
                        } else onOpenPlayer()
                    }
                }
            }
        },
        onCloseContainer = onNavigateBack,
        showAllArtistsToggle = true,
        showTrackNumbersToggle = true,
        onShowAllArtists = viewModel::setShowAllArtists,
        onShowTrackNumbers = viewModel::setShowTrackNumbers,
        onDefaultAction = viewModel::setDefaultPlaybackAction,
        emptySymbol = MaterialSymbols.Filled.MusicNote,
        emptyActionText = ShellStrings.importMedia().takeIf { hostCallbacks.supportsMediaImport() },
        onEmptyAction = hostCallbacks::onImportMedia,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun AudioEntityHero(
    title: String,
    kind: org.videolan.vlc.repository.AudioEntityKind?,
    items: List<MediaItem>,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val subtitle = when (kind) {
        org.videolan.vlc.repository.AudioEntityKind.ALBUM -> listOfNotNull(
            items.firstNotNullOfOrNull { it.artist?.takeIf(String::isNotBlank) },
            items.firstNotNullOfOrNull { it.year.takeIf { year -> year > 0 }?.toString() },
        ).joinToString(" · ")
        org.videolan.vlc.repository.AudioEntityKind.ARTIST -> items.mapNotNull { it.album?.takeIf(String::isNotBlank) }
            .distinct().size.takeIf { it > 0 }?.let { ShellStrings.albumsCount(it) }
        else -> null
    }
    CollectionHero(
        eyebrow = when (kind) {
            org.videolan.vlc.repository.AudioEntityKind.ARTIST -> ShellStrings.artist()
            org.videolan.vlc.repository.AudioEntityKind.GENRE -> ShellStrings.genre()
            else -> ShellStrings.album()
        },
        title = title,
        subtitle = subtitle,
        items = items,
        onPlay = onPlay,
        onShuffle = onShuffle,
        circularArtwork = kind == org.videolan.vlc.repository.AudioEntityKind.ARTIST,
    )
}

/** A library selector is useful only while the shared Audio root has content to filter. */
internal fun shouldShowAudioSectionSelector(state: MediaListUiState): Boolean =
    state.openedEntityTitle == null &&
        (state.count > 0 || state.items.isNotEmpty() || state.sections.isNotEmpty())

@Composable
internal fun BrowserDestination(
    modifier: Modifier,
    state: BrowserUiState,
    viewModel: BrowserViewModel,
    onOpenPlayer: () -> Unit,
    onOpenFolder: (MediaFolder) -> Unit,
    onNavigateUp: () -> Unit = viewModel::goUp,
) {
    BrowserRichPane(
        state = state,
        onUp = onNavigateUp,
        onRetry = viewModel::refresh,
        onOpenFolder = onOpenFolder,
        onPlay = {
            viewModel.play(it)
            if (defaultPlaybackActionOpensPlayer(state.defaultPlaybackAction)) onOpenPlayer()
        },
        onPlayNext = viewModel::playNext,
        onAppend = viewModel::append,
        onToggleSelect = viewModel::toggleSelect,
        onClearSelection = viewModel::clearSelection,
        onPlaySelection = { viewModel.playSelection(); onOpenPlayer() },
        onAppendSelection = viewModel::appendSelection,
        onDefaultAction = viewModel::setDefaultPlaybackAction,
        onShowHiddenFiles = viewModel::setShowHiddenFiles,
        onShowOnlyMultimedia = viewModel::setShowOnlyMultimedia,
        modifier = modifier,
    )
}

@Composable
internal fun PlaylistsDestination(
    modifier: Modifier,
    state: PlaylistsUiState,
    viewModel: PlaylistsViewModel,
    onOpenPlayer: () -> Unit,
    onOpenPlaylist: (PlaylistInfo) -> Unit,
    onNavigateBack: () -> Unit = viewModel::closeDetail,
) {
    PlaylistsRichPane(
        state = state,
        onCreate = viewModel::create,
        onOpen = onOpenPlaylist,
        onPlay = {
            viewModel.playPlaylist(it)
            if (defaultPlaybackActionOpensPlayer(state.defaultPlaybackAction)) onOpenPlayer()
        },
        onShufflePlay = { viewModel.shufflePlay(it); onOpenPlayer() },
        onDelete = viewModel::delete,
        onRename = viewModel::rename,
        onSetFavorite = viewModel::setFavorite,
        onToggleSelect = viewModel::toggleSelect,
        onClearSelection = viewModel::clearSelection,
        onDeleteSelection = viewModel::deleteSelection,
        onToggleFavorites = viewModel::toggleOnlyFavorites,
        onToggleSortDesc = viewModel::toggleSortDesc,
        onSetViewMode = viewModel::setViewMode,
        onPlayItem = {
            viewModel.playItem(it)
            if (defaultPlaybackActionOpensPlayer(state.defaultPlaybackAction)) onOpenPlayer()
        },
        onRemoveTrack = viewModel::removeTrackAt,
        onMoveTrackUp = viewModel::moveTrackUp,
        onMoveTrackDown = viewModel::moveTrackDown,
        onBack = onNavigateBack,
        onRetry = viewModel::refresh,
        onClearActionError = viewModel::clearActionError,
        modifier = modifier,
    )
}

@Composable
internal fun PlayerDestination(
    modifier: Modifier,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    playerSurface: PlayerSurface = FallbackPlayerSurface,
    hostCallbacks: ShellHostCallbacks,
) {
    // One wiring of the player for every host: the shell, Android's player activity and TV.
    VlcPlayerScreen(
        viewModel = viewModel,
        onClose = onClose,
        modifier = modifier,
        hostControls = remember(hostCallbacks) { hostCallbacks.playerHostControls() },
        showSubtitleImport = hostCallbacks.supportsSubtitleImport(),
        onImportSubtitle = hostCallbacks::onImportSubtitle,
        surface = playerSurface,
    )
}

@Composable
internal fun MoreDestination(
    modifier: Modifier,
    viewModel: MoreHubViewModel,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenRemote: (() -> Unit)?,
    hostCallbacks: ShellHostCallbacks,
    onOpenPlayer: () -> Unit,
) {
    MorePane(
        modifier = modifier,
        vm = viewModel,
        onOpenSettings = onOpenSettings,
        onOpenRemote = onOpenRemote,
        onOpenAbout = onOpenAbout,
        onOpenDonate = hostCallbacks::onOpenDonate,
        onPlayHistory = { entry ->
            viewModel.playHistory(entry)
            onOpenPlayer()
        },
        onPlayStream = { stream ->
            viewModel.playStream(stream)
            onOpenPlayer()
        },
        onOpenStream = { title, uri ->
            viewModel.playStream(title, uri)
            onOpenPlayer()
        },
    )
}

@Composable
internal fun SettingsDestination(
    modifier: Modifier,
    viewModel: SettingsViewModel,
    onOpenSection: (String) -> Unit,
    selectedSectionId: String? = null,
) {
    SettingsRootPane(modifier = modifier, vm = viewModel, onOpenSection = onOpenSection, selectedSectionId = selectedSectionId)
}

@Composable
internal fun SettingsSectionDestination(
    modifier: Modifier,
    viewModel: SettingsViewModel,
    sectionId: String,
    onOpenSection: (String) -> Unit,
) {
    SettingsSectionPane(modifier = modifier, vm = viewModel, sectionId = sectionId, onOpenSection = onOpenSection)
}
