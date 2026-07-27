package org.videolan.vlc.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.player.PlaybackState
import org.videolan.vlc.player.PlaybackTracks
import org.videolan.vlc.player.PlaybackDelays
import org.videolan.vlc.player.SleepTimerState
import org.videolan.vlc.player.PlaybackChapters
import org.videolan.vlc.player.VideoScaleMode
import org.videolan.vlc.player.PlaybackEqualizer
import org.videolan.vlc.player.PlaybackVideoCrop
import org.videolan.vlc.player.VideoCropMode
import org.videolan.vlc.player.PlaybackVideoAdjust
import org.videolan.vlc.player.VideoAdjustParameter
import org.videolan.vlc.player.PlaybackBookmarks
import org.videolan.vlc.platform.RendererInfo

data class PlayerUiState(
    val title: String = "",
    val subtitle: String = "",
    /** The active source; platform surfaces use this without duplicating shared navigation state. */
    val uri: String = "",
    val artworkUri: String? = null,
    val playing: Boolean = false,
    val progress: Progress = Progress(),
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val rate: Float = 1f,
    val queue: List<MediaItem> = emptyList(),
    val currentQueueIndex: Int = 0,
    val abRepeat: ABRepeat = ABRepeat(),
    val abRepeatEnabled: Boolean = false,
    val stopAfterCurrent: Boolean = false,
    val videoScaleMode: VideoScaleMode = VideoScaleMode.BEST_FIT,
    val tracks: PlaybackTracks = PlaybackTracks(),
    val delays: PlaybackDelays = PlaybackDelays(),
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val chapters: PlaybackChapters = PlaybackChapters(),
    /** The native host can transition the active video to system Picture-in-Picture. */
    val pictureInPictureAvailable: Boolean = false,
    val rendererSelectionAvailable: Boolean = false,
    val renderers: List<RendererInfo> = emptyList(),
    val selectedRendererId: String? = null,
    val equalizer: PlaybackEqualizer = PlaybackEqualizer(),
    val videoCrop: PlaybackVideoCrop = PlaybackVideoCrop(),
    val videoAdjust: PlaybackVideoAdjust = PlaybackVideoAdjust(),
    val bookmarks: PlaybackBookmarks = PlaybackBookmarks(),
    val hasMedia: Boolean = false,
    /** True for known video and network streams, which may expose video after probing. */
    val hasVideoOutput: Boolean = false,
    val error: String? = null,
)

/** Prefer constructing with Koin [org.videolan.vlc.player.PlaybackController] service. */
class PlayerViewModel(
    private val playback: PlaybackService = runCatching {
        VlcKoin.get().get<PlaybackService>()
    }.getOrElse { error("PlaybackService unavailable") },
    private val controller: PlaybackController? = runCatching {
        VlcKoin.get().get<PlaybackController>()
    }.getOrNull(),
) : VlcViewModel() {

    private val _state = MutableStateFlow(
        PlayerUiState(
            pictureInPictureAvailable = controller?.isPictureInPictureAvailable == true,
            rendererSelectionAvailable = controller?.isRendererSelectionAvailable == true,
            renderers = controller?.rendererBridge?.listRenderers().orEmpty(),
            selectedRendererId = controller?.rendererBridge?.currentRendererId(),
        ),
    )
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        launch {
            playback.equalizer.collect { equalizer ->
                _state.update { it.copy(equalizer = equalizer) }
            }
        }
        launch {
            playback.videoCrop.collect { videoCrop ->
                _state.update { it.copy(videoCrop = videoCrop) }
            }
        }
        launch {
            playback.videoAdjust.collect { videoAdjust ->
                _state.update { it.copy(videoAdjust = videoAdjust) }
            }
        }
        launch {
            playback.bookmarks.collect { bookmarks ->
                _state.update { it.copy(bookmarks = bookmarks) }
            }
        }
        launch {
            combine(
                playback.state,
                playback.progress,
                combine(
                    combine(playback.currentPlaylist, playback.stopAfterCurrent, playback.videoScaleMode, playback.tracks, playback.delays) { playlist, stopAfter, scale, tracks, delays ->
                        PlayerContext(playlist, stopAfter, scale, tracks, delays)
                    },
                    playback.sleepTimer,
                ) { context, sleepTimer -> context.copy(sleepTimer = sleepTimer) }.combine(playback.chapters) { context, chapters -> context.copy(chapters = chapters) },
                playback.abRepeat,
                playback.abRepeatEnabled,
            ) { st, prog, playlistAndStopAfter, abRepeat, abRepeatEnabled ->
                PlayerSnapshot(
                    state = st,
                    progress = prog,
                    playlist = playlistAndStopAfter.playlist,
                    abRepeat = abRepeat,
                    abRepeatEnabled = abRepeatEnabled,
                    stopAfterCurrent = playlistAndStopAfter.stopAfterCurrent,
                    videoScaleMode = playlistAndStopAfter.videoScaleMode,
                    tracks = playlistAndStopAfter.tracks,
                    delays = playlistAndStopAfter.delays,
                    sleepTimer = playlistAndStopAfter.sleepTimer,
                    chapters = playlistAndStopAfter.chapters,
                )
            }.collect { (st, prog, pl, abRepeat, abRepeatEnabled, stopAfterCurrent, videoScaleMode, tracks, delays, sleepTimer, chapters) ->
                val item = when (st) {
                    is PlaybackState.Playing -> st.item
                    is PlaybackState.Paused -> st.item
                    is PlaybackState.Stopped -> st.item
                    is PlaybackState.Ended -> st.item
                    else -> pl.current
                }
                _state.update {
                    it.copy(
                        title = item?.displayTitle.orEmpty(),
                        subtitle = listOfNotNull(item?.artist, item?.album).joinToString(" · "),
                        uri = item?.uri.orEmpty(),
                        artworkUri = item?.artworkUri,
                        playing = st is PlaybackState.Playing,
                        // PlaybackState describes the lifecycle transition; the dedicated flow is
                        // the continuously updated native clock and must remain authoritative.
                        progress = prog,
                        shuffle = pl.shuffle,
                        repeatMode = pl.repeatMode,
                        rate = playback.getRate(),
                        queue = pl.items,
                        currentQueueIndex = pl.currentIndex,
                        abRepeat = abRepeat,
                        abRepeatEnabled = abRepeatEnabled,
                        stopAfterCurrent = stopAfterCurrent,
                        videoScaleMode = videoScaleMode,
                        tracks = tracks,
                        delays = delays,
                        sleepTimer = sleepTimer,
                        chapters = chapters,
                        hasMedia = item != null || pl.items.isNotEmpty(),
                        hasVideoOutput = item?.let { it.isVideo || it.isStream } == true,
                        error = (st as? PlaybackState.Error)?.message,
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        if (_state.value.playing) playback.pause() else playback.resume()
    }

    fun play(item: MediaItem, playlist: List<MediaItem> = emptyList()) {
        playback.play(item, playlist)
    }

    fun next() = playback.next()
    fun previous() = playback.previous()
    fun stop() = playback.stop()
    fun seekTo(position: Long) = playback.seekTo(position)
    fun seekRelative(delta: Long) = playback.seekRelative(delta)

    fun setPlaybackRate(rate: Float) {
        val safeRate = rate.takeIf(Float::isFinite)?.coerceIn(0.25f, 4f) ?: 1f
        playback.setRate(safeRate)
        _state.update { it.copy(rate = playback.getRate()) }
    }

    fun playQueueItem(index: Int) {
        val queue = _state.value.queue
        if (index in queue.indices && index != _state.value.currentQueueIndex) {
            playback.playFromIndex(queue, index)
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val queue = _state.value.queue
        if (from in queue.indices && to in queue.indices && from != to) {
            playback.moveItem(from, to)
        }
    }

    fun removeQueueItem(index: Int) {
        if (index in _state.value.queue.indices) playback.removeAt(index)
    }

    fun toggleABRepeat() = playback.toggleABRepeat()

    fun setABRepeatMarker() = playback.setABRepeatValue(_state.value.progress.time)

    fun resetABRepeat() = playback.resetABRepeat()

    fun clearABRepeat() = playback.clearABRepeat()

    fun toggleStopAfterCurrent() {
        if (_state.value.stopAfterCurrent) playback.clearStopAfter()
        else playback.setStopAfterThis()
    }

    fun setVideoScaleMode(mode: VideoScaleMode) = playback.setVideoScaleMode(mode)

    fun selectAudioTrack(id: String) = playback.selectAudioTrack(id)

    fun selectSubtitleTrack(id: String) = playback.selectSubtitleTrack(id)

    fun setAudioDelay(delayUs: Long) = playback.setAudioDelay(delayUs)

    fun setSubtitleDelay(delayUs: Long) = playback.setSubtitleDelay(delayUs)

    fun setSleepTimer(durationMillis: Long, waitForCurrentItem: Boolean) =
        playback.setSleepTimer(durationMillis, waitForCurrentItem)

    fun clearSleepTimer() = playback.clearSleepTimer()

    fun selectChapter(index: Int) = playback.selectChapter(index)

    fun loadExternalSubtitle(uri: String): Boolean = playback.loadExternalSubtitle(uri)

    /** Delegates the OS-owned transition while keeping visibility/capability policy in common UI. */
    fun enterPictureInPicture(): Boolean = controller?.enterPip() == true

    fun startRendererDiscovery() {
        controller?.startRendererDiscovery()
        refreshRenderers()
    }

    fun stopRendererDiscovery() = controller?.stopRendererDiscovery()

    /** Renderer discovery is asynchronous on native hosts, so the shared picker polls safely. */
    fun refreshRenderers() {
        val bridge = controller?.rendererBridge ?: return
        _state.update {
            it.copy(
                renderers = bridge.listRenderers(),
                selectedRendererId = bridge.currentRendererId(),
            )
        }
    }

    fun selectRenderer(id: String?): Boolean {
        val selected = controller?.selectRenderer(id) == true
        refreshRenderers()
        return selected
    }

    fun setEqualizerEnabled(enabled: Boolean) = playback.setEqualizerEnabled(enabled)

    fun selectEqualizerPreset(id: String) = playback.selectEqualizerPreset(id)

    fun setEqualizerPreamp(preampDb: Float) = playback.setEqualizerPreamp(preampDb)

    fun setEqualizerBand(index: Int, amplificationDb: Float) =
        playback.setEqualizerBand(index, amplificationDb)

    fun setVideoCrop(mode: VideoCropMode) = playback.setVideoCrop(mode)

    fun setVideoAdjustEnabled(enabled: Boolean) = playback.setVideoAdjustEnabled(enabled)

    fun setVideoAdjust(parameter: VideoAdjustParameter, value: Float) =
        playback.setVideoAdjust(parameter, value)

    fun resetVideoAdjust() = playback.resetVideoAdjust()

    fun addBookmark() = playback.addBookmark()

    fun removeBookmark(id: String) = playback.removeBookmark(id)

    fun renameBookmark(id: String, title: String) = playback.renameBookmark(id, title)

    /** Mirrors the upstream player controls: jump to the nearest marker either side of the playhead. */
    fun previousBookmark() {
        _state.value.bookmarks.entries.sortedBy { it.timeMs }
            .lastOrNull { it.timeMs < _state.value.progress.time }
            ?.let { playback.seekTo(it.timeMs) }
    }

    fun nextBookmark() {
        _state.value.bookmarks.entries.sortedBy { it.timeMs }
            .firstOrNull { it.timeMs > _state.value.progress.time }
            ?.let { playback.seekTo(it.timeMs) }
    }

    fun cycleRepeat() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        playback.setRepeatMode(next)
    }

    fun toggleShuffle() {
        playback.setShuffle(!_state.value.shuffle)
    }
}

private data class PlayerSnapshot(
    val state: PlaybackState,
    val progress: Progress,
    val playlist: Playlist,
    val abRepeat: ABRepeat,
    val abRepeatEnabled: Boolean,
    val stopAfterCurrent: Boolean,
    val videoScaleMode: VideoScaleMode,
    val tracks: PlaybackTracks,
    val delays: PlaybackDelays,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val chapters: PlaybackChapters = PlaybackChapters(),
)

private data class PlayerContext(
    val playlist: Playlist,
    val stopAfterCurrent: Boolean,
    val videoScaleMode: VideoScaleMode,
    val tracks: PlaybackTracks,
    val delays: PlaybackDelays,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val chapters: PlaybackChapters = PlaybackChapters(),
)
