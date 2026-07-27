package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.videolan.vlc.compose.player.FallbackPlayerSurface
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.compose.player.VideoSurfaceWithHud
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

@Composable
internal fun VideoDestination(
    modifier: Modifier,
    state: MediaListUiState,
    viewModel: VideoListViewModel,
    hostCallbacks: ShellHostCallbacks,
    onOpenPlayer: () -> Unit,
    onOpenContainer: (MediaFolder) -> Unit,
) {
    RichMediaListPane(
        state = state,
        title = "Videos",
        emptyLabel = "No videos",
        pagingFlow = viewModel.pagingFlow,
        groups = state.groups,
        onQuery = viewModel::setQuery,
        onRetry = viewModel::refresh,
        onPlay = { viewModel.play(it); onOpenPlayer() },
        onPlayAll = { viewModel.playAll(); onOpenPlayer() },
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
        canHandleHostAction = hostCallbacks::supportsContextAction,
        onCtx = { item, option ->
            when (option) {
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
                    if (option == ContextOption.CTX_PLAY || option == ContextOption.CTX_PLAY_ALL) {
                        onOpenPlayer()
                    }
                }
            }
        },
        onOpenGroup = onOpenContainer,
        onCloseContainer = viewModel::closeContainer,
        onSetGroupingMode = viewModel::setGroupingMode,
        showGroupingToggle = true,
        onDefaultAction = viewModel::setDefaultPlaybackAction,
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
) {
    Column(modifier) {
        if (state.openedEntityTitle == null) {
            SectionTabs(
                tabs = listOf("Tracks", "Artists", "Albums", "Genres", "Playlists"),
                selected = section.ordinal,
                onSelect = { viewModel.setSection(AudioSection.entries[it]) },
            )
        }
        RichMediaListPane(
            state = state,
            title = "Audio",
            emptyLabel = "No audio",
            sections = state.sections,
            pagingFlow = if (section == AudioSection.TRACKS && state.openedEntityTitle == null) {
                viewModel.pagingFlow
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
                    onOpenPlayer()
                }
            },
            onPlayAll = { viewModel.playAll(); onOpenPlayer() },
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
            canHandleHostAction = hostCallbacks::supportsContextAction,
            onCtx = { item, option ->
                when (option) {
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
                        if (option == ContextOption.CTX_PLAY || option == ContextOption.CTX_PLAY_ALL) {
                            onOpenPlayer()
                        }
                    }
                }
            },
            onCloseContainer = viewModel::closeEntity,
            showAllArtistsToggle = true,
            showTrackNumbersToggle = true,
            onShowAllArtists = viewModel::setShowAllArtists,
            onShowTrackNumbers = viewModel::setShowTrackNumbers,
            onDefaultAction = viewModel::setDefaultPlaybackAction,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun BrowserDestination(
    modifier: Modifier,
    state: BrowserUiState,
    viewModel: BrowserViewModel,
    onOpenPlayer: () -> Unit,
    onOpenFolder: (MediaFolder) -> Unit,
) {
    BrowserRichPane(
        state = state,
        onUp = viewModel::goUp,
        onRetry = viewModel::refresh,
        onOpenFolder = onOpenFolder,
        onPlay = { viewModel.play(it); onOpenPlayer() },
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
) {
    PlaylistsRichPane(
        state = state,
        onCreate = viewModel::create,
        onOpen = onOpenPlaylist,
        onPlay = { viewModel.playPlaylist(it); onOpenPlayer() },
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
        onPlayItem = { viewModel.playItem(it); onOpenPlayer() },
        onRemoveTrack = viewModel::removeTrackAt,
        onMoveTrackUp = viewModel::moveTrackUp,
        onMoveTrackDown = viewModel::moveTrackDown,
        onBack = viewModel::closeDetail,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
internal fun PlayerDestination(
    modifier: Modifier,
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    playerSurface: PlayerSurface = FallbackPlayerSurface,
) {
    VideoSurfaceWithHud(
        title = state.title,
        subtitle = state.subtitle,
        error = state.error,
        playing = state.playing,
        progress = state.progress,
        shuffle = state.shuffle,
        repeatMode = state.repeatMode,
        rate = state.rate,
        queue = state.queue,
        currentQueueIndex = state.currentQueueIndex,
        abRepeat = state.abRepeat,
        abRepeatEnabled = state.abRepeatEnabled,
        onTogglePlay = viewModel::togglePlayPause,
        onSeek = viewModel::seekTo,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeat,
        onSetRate = viewModel::setPlaybackRate,
        onPlayQueueItem = viewModel::playQueueItem,
        onMoveQueueItem = viewModel::moveQueueItem,
        onRemoveQueueItem = viewModel::removeQueueItem,
        onToggleABRepeat = viewModel::toggleABRepeat,
        onSetABRepeatMarker = viewModel::setABRepeatMarker,
        onResetABRepeat = viewModel::resetABRepeat,
        onClearABRepeat = viewModel::clearABRepeat,
        onClose = onClose,
        modifier = modifier,
    ) { chromeVisible -> playerSurface(state, chromeVisible) }
}

@Composable
internal fun MoreDestination(
    modifier: Modifier,
    viewModel: MoreHubViewModel,
    onOpenSettings: () -> Unit,
    onOpenRemote: (() -> Unit)?,
    hostCallbacks: ShellHostCallbacks,
    onOpenPlayer: () -> Unit,
) {
    MorePane(
        modifier = modifier,
        vm = viewModel,
        onOpenSettings = onOpenSettings,
        onOpenRemote = onOpenRemote,
        onOpenAbout = hostCallbacks::onOpenAbout,
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
internal fun SettingsDestination(modifier: Modifier, viewModel: SettingsViewModel) {
    SettingsOnlyPane(modifier = modifier, vm = viewModel)
}
