package org.videolan.vlc.compose.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.videolan.vlc.viewmodel.PlayerViewModel

/**
 * The shared player bound to a [PlayerViewModel], for hosts that show it outside the main shell
 * (Android TV's player activity, external "play this video" launches).
 *
 * It wires exactly the same view-model actions as the shell's player destination, so a feature
 * added to [VideoSurfaceWithHud] reaches every host. [surface] is the platform decoder view.
 */
@Composable
fun VlcPlayerScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    hostControls: PlayerHostControls? = null,
    tvMode: Boolean = false,
    showSubtitleImport: Boolean = false,
    onImportSubtitle: (onPicked: (String) -> Unit) -> Unit = {},
    surface: PlayerSurface = FallbackPlayerSurface,
) {
    val state by viewModel.state.collectAsState()
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
        stopAfterCurrent = state.stopAfterCurrent,
        videoScaleMode = state.videoScaleMode,
        videoCrop = state.videoCrop,
        videoAdjust = state.videoAdjust,
        tracks = state.tracks,
        delays = state.delays,
        sleepTimer = state.sleepTimer,
        chapters = state.chapters,
        hasVideoOutput = state.hasVideoOutput,
        showPictureInPicture = !tvMode && state.pictureInPictureAvailable && state.hasVideoOutput,
        showRendererSelection = state.rendererSelectionAvailable,
        renderers = state.renderers,
        selectedRendererId = state.selectedRendererId,
        equalizer = state.equalizer,
        bookmarks = state.bookmarks,
        hudTimeoutSeconds = state.videoHudTimeoutSeconds,
        hostControls = hostControls,
        resumeOffer = state.resumeOffer,
        onAcceptResumeOffer = viewModel::acceptResumeOffer,
        onDismissResumeOffer = viewModel::dismissResumeOffer,
        onTogglePlay = viewModel::togglePlayPause,
        onSeek = viewModel::seekTo,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeat,
        onSetRate = viewModel::setPlaybackRate,
        onSavePlaylist = viewModel::saveQueueAsPlaylist,
        onPlayQueueItem = viewModel::playQueueItem,
        onMoveQueueItem = viewModel::moveQueueItem,
        onRemoveQueueItem = viewModel::removeQueueItem,
        onToggleABRepeat = viewModel::toggleABRepeat,
        onSetABRepeatMarker = viewModel::setABRepeatMarker,
        onResetABRepeat = viewModel::resetABRepeat,
        onClearABRepeat = viewModel::clearABRepeat,
        onToggleStopAfterCurrent = viewModel::toggleStopAfterCurrent,
        onSetVideoScaleMode = viewModel::setVideoScaleMode,
        onSetVideoCrop = viewModel::setVideoCrop,
        onSetVideoAdjustEnabled = viewModel::setVideoAdjustEnabled,
        onSetVideoAdjust = viewModel::setVideoAdjust,
        onResetVideoAdjust = viewModel::resetVideoAdjust,
        onSelectAudioTrack = viewModel::selectAudioTrack,
        onSelectSubtitleTrack = viewModel::selectSubtitleTrack,
        onSetAudioDelay = viewModel::setAudioDelay,
        onSetSubtitleDelay = viewModel::setSubtitleDelay,
        onSetSleepTimer = viewModel::setSleepTimer,
        onClearSleepTimer = viewModel::clearSleepTimer,
        onSelectChapter = viewModel::selectChapter,
        showSubtitleImport = showSubtitleImport,
        onImportSubtitle = { onImportSubtitle(viewModel::loadExternalSubtitle) },
        onEnterPictureInPicture = viewModel::enterPictureInPicture,
        onStartRendererDiscovery = viewModel::startRendererDiscovery,
        onStopRendererDiscovery = viewModel::stopRendererDiscovery,
        onRefreshRenderers = viewModel::refreshRenderers,
        onSelectRenderer = viewModel::selectRenderer,
        onSetEqualizerEnabled = viewModel::setEqualizerEnabled,
        onSelectEqualizerPreset = viewModel::selectEqualizerPreset,
        onSetEqualizerPreamp = viewModel::setEqualizerPreamp,
        onSetEqualizerBand = viewModel::setEqualizerBand,
        onAddBookmark = viewModel::addBookmark,
        onRemoveBookmark = viewModel::removeBookmark,
        onRenameBookmark = viewModel::renameBookmark,
        onSeekBookmark = viewModel::seekTo,
        onPreviousBookmark = viewModel::previousBookmark,
        onNextBookmark = viewModel::nextBookmark,
        onClose = onClose,
        modifier = modifier,
        tvMode = tvMode,
    ) { chromeVisible -> surface(state, chromeVisible) }
}
