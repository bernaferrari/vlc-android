package org.videolan.vlc.kmp

import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.libvlc.MediaPlayer
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackObserver
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState
import org.videolan.vlc.player.PlaybackTrack
import org.videolan.vlc.player.PlaybackTracks
import org.videolan.vlc.player.PlaybackDelays
import org.videolan.vlc.player.SleepTimerController
import org.videolan.vlc.player.SleepTimerState
import org.videolan.vlc.player.PlaybackChapter
import org.videolan.vlc.player.PlaybackChapters
import org.videolan.vlc.player.VideoScaleMode
import org.videolan.vlc.PlaybackService as AndroidPlaybackHost

private const val MIN_PLAYBACK_RATE = 0.25f
private const val MAX_PLAYBACK_RATE = 4f

/**
 * Android implementation of [PlaybackService] that delegates to the existing
 * [PlaylistManager] / [org.videolan.vlc.media.PlayerController] stack.
 *
 * [PlaylistManager] only exists while the Android [AndroidPlaybackHost] is alive,
 * so it is resolved lazily via [playlistManagerProvider] (defaults to the live service).
 *
 * Prefer [org.videolan.vlc.player.PlaybackController] from UI/shared code.
 * This adapter is the only Android type allowed to touch PlaylistManager.
 *
 * State/progress: observes [PlaylistManager.playingState] / [PlaylistManager.currentPlayedMedia]
 * and the active player progress LiveData when a manager is available. Full MediaPlayer event
 * parity is partial — ended/error paths still rely on host service lifecycle.
 */
class AndroidPlaybackService(
    private val playlistManagerProvider: () -> PlaylistManager? = {
        AndroidPlaybackHost.instance?.playlistManager
    }
) : PlaybackService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: Flow<PlaybackState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(Progress())
    override val progress: Flow<Progress> = _progress.asStateFlow()

    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    override val currentPlaylist: Flow<Playlist> = _playlist.asStateFlow()

    private val _abRepeat = MutableStateFlow(ABRepeat())
    override val abRepeat: Flow<ABRepeat> = _abRepeat.asStateFlow()

    private val _abRepeatEnabled = MutableStateFlow(false)
    override val abRepeatEnabled: Flow<Boolean> = _abRepeatEnabled.asStateFlow()

    private val _stopAfterCurrent = MutableStateFlow(false)
    override val stopAfterCurrent: Flow<Boolean> = _stopAfterCurrent.asStateFlow()

    private val _videoScaleMode = MutableStateFlow(VideoScaleMode.BEST_FIT)
    override val videoScaleMode: Flow<VideoScaleMode> = _videoScaleMode.asStateFlow()

    private val _tracks = MutableStateFlow(PlaybackTracks())
    override val tracks: Flow<PlaybackTracks> = _tracks.asStateFlow()

    private val _delays = MutableStateFlow(PlaybackDelays())
    override val delays: Flow<PlaybackDelays> = _delays.asStateFlow()

    private val sleepTimerController = SleepTimerController(
        isPlaying = { _state.value is PlaybackState.Playing },
        stopPlayback = ::stop,
    )
    override val sleepTimer: Flow<SleepTimerState> = sleepTimerController.state

    private val _chapters = MutableStateFlow(PlaybackChapters())
    override val chapters: Flow<PlaybackChapters> = _chapters.asStateFlow()

    private val observers = mutableListOf<PlaybackObserver>()

    private var boundManager: PlaylistManager? = null
    private val playingObserver = Observer<Boolean> { playing ->
        pushStateFromHost(playing == true)
    }
    private val mediaObserver = Observer<MediaWrapper?> { mw ->
        pushStateFromHost(PlaylistManager.playingState.value == true, mw)
    }
    private val progressObserver = Observer<org.videolan.vlc.media.Progress> { p ->
        updateProgress(p.time, p.length)
    }
    private val abRepeatObserver = Observer<org.videolan.vlc.media.ABRepeat> { repeat ->
        _abRepeat.value = ABRepeat(start = repeat.start, stop = repeat.stop)
    }
    private val abRepeatEnabledObserver = Observer<Boolean> { enabled ->
        _abRepeatEnabled.value = enabled == true
    }

    init {
        // Bridge host LiveData into KMP flows for as long as this adapter lives.
        PlaylistManager.playingState.observeForever(playingObserver)
        PlaylistManager.currentPlayedMedia.observeForever(mediaObserver)
        ensurePlayerProgressBound()
    }

    private fun manager(): PlaylistManager? {
        val current = playlistManagerProvider()
        if (current !== boundManager) {
            boundManager?.player?.progress?.removeObserver(progressObserver)
            boundManager?.abRepeat?.removeObserver(abRepeatObserver)
            boundManager?.abRepeatOn?.removeObserver(abRepeatEnabledObserver)
            boundManager = current
            current?.player?.progress?.observeForever(progressObserver)
            current?.abRepeat?.observeForever(abRepeatObserver)
            current?.abRepeatOn?.observeForever(abRepeatEnabledObserver)
        }
        return current
    }

    private fun ensurePlayerProgressBound() {
        manager()
    }

    override fun play(item: MediaItem, playlist: List<MediaItem>) {
        _state.value = PlaybackState.Loading
        val list = if (playlist.isEmpty()) listOf(item) else playlist
        val index = list.indexOfFirst { it.id == item.id && it.uri == item.uri }
            .takeIf { it >= 0 } ?: list.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
        playFromIndex(list, index)
    }

    override fun playFromIndex(playlist: List<MediaItem>, index: Int) {
        _state.value = PlaybackState.Loading
        if (playlist.isEmpty()) {
            updateState(PlaybackState.Error("Empty playlist"))
            return
        }
        val pm = manager()
        if (pm == null) {
            _state.value = PlaybackState.Error("PlaybackService not running")
            return
        }
        val wrappers = playlist.map { it.toMediaWrapper() }
        val safeIndex = index.coerceIn(0, (wrappers.size - 1).coerceAtLeast(0))
        // Streams can expose video only after LibVLC probes them. Match the
        // shared PlayerUiState contract so they wait for the route-owned
        // VLCVideoLayout instead of escaping into the legacy activity.
        if (playlist.getOrNull(safeIndex)?.let { it.isVideo || it.isStream } == true) {
            SharedVideoSurfaceRegistry.requestInlinePlayback()
        }
        _playlist.value = Playlist(
            id = 0,
            name = "Current",
            items = playlist,
            currentIndex = safeIndex,
            shuffle = pm.shuffling,
            repeatMode = repeatModeFromHost()
        )
        _stopAfterCurrent.value = pm.stopAfter == safeIndex
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
        // PlaylistManager.load is @MainThread + suspend
        pm.launch {
            try {
                pm.load(wrappers, safeIndex, mlUpdate = true)
                // Loading can fail asynchronously or leave the host paused. Read the native
                // source of truth instead of optimistically reporting a false Playing state.
                pushStateFromHost(playing = PlaylistManager.playingState.value == true)
            } catch (t: Throwable) {
                _state.value = PlaybackState.Error(t.message ?: "Failed to load playlist")
                observers.forEach { it.onStateChanged(_state.value) }
            }
        }
    }

    override fun pause() {
        manager()?.pause()
        pushStateFromHost(playing = false)
    }

    override fun resume() {
        manager()?.play()
        pushStateFromHost(playing = true)
    }

    override fun stop() {
        manager()?.stop()
        val item = currentItem()
        val stopped = PlaybackState.Stopped(item)
        _state.value = stopped
        observers.forEach { it.onStateChanged(stopped) }
    }

    override fun seekTo(position: Long) {
        val length = _progress.value.length
        val target = position.coerceAtLeast(0L).let { requested ->
            if (length > 0L) requested.coerceAtMost(length) else requested
        }
        manager()?.player?.setTime(target)
        // LibVLC progress callbacks are asynchronous; reflect a user-confirmed seek immediately
        // so the shared scrubber does not snap backwards while waiting for the native callback.
        updateProgress(target, length)
    }

    override fun seekRelative(delta: Long) {
        val current = manager()?.player?.getCurrentTime() ?: _progress.value.time
        seekTo(current + delta)
    }

    override fun next() {
        manager()?.next()
    }

    override fun previous() {
        manager()?.previous(true)
    }

    override fun setShuffle(enabled: Boolean) {
        val pm = manager() ?: return
        if (pm.shuffling != enabled) {
            pm.shuffle()
        }
        _playlist.value = _playlist.value.copy(shuffle = enabled)
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        val pm = manager() ?: return
        when (mode) {
            RepeatMode.NONE -> pm.setRepeatType(PlaybackStateCompat.REPEAT_MODE_NONE)
            RepeatMode.ONE -> pm.setRepeatType(PlaybackStateCompat.REPEAT_MODE_ONE)
            RepeatMode.ALL -> pm.setRepeatType(PlaybackStateCompat.REPEAT_MODE_ALL)
        }
        _playlist.value = _playlist.value.copy(repeatMode = mode)
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
    }

    override fun setVolume(volume: Int) {
        manager()?.player?.setVolume(volume.coerceIn(0, 200))
    }

    override fun getVolume(): Int {
        return manager()?.player?.getVolume() ?: 100
    }

    override fun setRate(rate: Float) {
        val safeRate = rate.takeIf(Float::isFinite)?.coerceIn(MIN_PLAYBACK_RATE, MAX_PLAYBACK_RATE) ?: 1f
        manager()?.player?.setRate(safeRate, true)
    }

    override fun getRate(): Float {
        return manager()?.player?.getRate() ?: 1.0f
    }

    override fun setVideoScaleMode(mode: VideoScaleMode) {
        _videoScaleMode.value = mode
        manager()?.player?.mediaplayer?.videoScale = mode.toAndroidScaleType()
    }

    override fun selectAudioTrack(id: String) {
        manager()?.player?.setAudioTrack(id)
        refreshTracksFromHost()
    }

    override fun selectSubtitleTrack(id: String) {
        manager()?.player?.setSpuTrack(id)
        refreshTracksFromHost()
    }

    override fun setAudioDelay(delayUs: Long) {
        manager()?.setAudioDelay(delayUs)
        refreshDelaysFromHost()
    }

    override fun setSubtitleDelay(delayUs: Long) {
        manager()?.setSpuDelay(delayUs)
        refreshDelaysFromHost()
    }

    override fun setSleepTimer(durationMillis: Long, waitForCurrentItem: Boolean) =
        sleepTimerController.start(durationMillis, waitForCurrentItem)

    override fun clearSleepTimer() = sleepTimerController.clear()

    override fun selectChapter(index: Int) {
        val player = manager()?.player ?: return
        if (index in player.getChapters(-1).orEmpty().indices) {
            player.setChapterIdx(index)
            refreshChaptersFromHost()
        }
    }

    override fun addObserver(observer: PlaybackObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: PlaybackObserver) {
        observers.remove(observer)
    }

    override fun append(items: List<MediaItem>) {
        val pm = manager() ?: return
        if (items.isEmpty()) return
        pm.launch {
            pm.append(items.map { it.toMediaWrapper() })
            syncPlaylistFromHost()
        }
    }

    override fun insertNext(items: List<MediaItem>) {
        val pm = manager() ?: return
        if (items.isEmpty()) return
        pm.insertNext(items.map { it.toMediaWrapper() })
        syncPlaylistFromHost()
    }

    override fun moveItem(from: Int, to: Int) {
        manager()?.moveItem(from, to)
        syncPlaylistFromHost()
    }

    override fun removeAt(index: Int) {
        manager()?.remove(index)
        syncPlaylistFromHost()
    }

    override fun removeByUri(uri: String) {
        manager()?.removeLocation(uri)
        syncPlaylistFromHost()
    }

    override fun clearQueue() {
        manager()?.stop()
        _playlist.value = Playlist(0, "Current")
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
    }

    override fun setStopAfterThis() {
        val pm = manager() ?: return
        pm.stopAfter = pm.currentIndex
        _stopAfterCurrent.value = pm.currentIndex >= 0
    }

    override fun clearStopAfter() {
        manager()?.stopAfter = -1
        _stopAfterCurrent.value = false
    }

    override fun toggleABRepeat() {
        manager()?.toggleABRepeat()
    }

    override fun setABRepeatValue(timeMs: Long) {
        val pm = manager() ?: return
        pm.setABRepeatValue(pm.getCurrentMedia(), timeMs)
    }

    override fun resetABRepeat() {
        manager()?.resetABRepeatValues(manager()?.getCurrentMedia())
    }

    override fun clearABRepeat() {
        manager()?.clearABRepeat()
    }

    private fun syncPlaylistFromHost() {
        val pm = manager() ?: return
        val items = pm.getMediaList().map { it.toMediaItem() }
        _playlist.value = Playlist(
            id = 0,
            name = "Current",
            items = items,
            currentIndex = pm.currentIndex.coerceAtLeast(0),
            shuffle = pm.shuffling,
            repeatMode = repeatModeFromHost(),
        )
        _stopAfterCurrent.value = pm.stopAfter == pm.currentIndex && pm.currentIndex >= 0
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
    }


    /**
     * Called by the platform to push native playback updates into shared state.
     */
    fun updateState(newState: PlaybackState) {
        _state.value = newState
        observers.forEach { it.onStateChanged(newState) }
    }

    fun updateProgress(time: Long, length: Long) {
        val p = Progress(time, length)
        _progress.value = p
        observers.forEach { it.onProgressChanged(p) }
        // Keep Playing/Paused progress in sync when we already have an item
        when (val current = _state.value) {
            is PlaybackState.Playing -> {
                val updated = current.copy(progress = p)
                _state.value = updated
                observers.forEach { it.onStateChanged(updated) }
            }
            is PlaybackState.Paused -> {
                val updated = current.copy(progress = p)
                _state.value = updated
                observers.forEach { it.onStateChanged(updated) }
            }
            else -> Unit
        }
    }

    private fun pushStateFromHost(playing: Boolean, media: MediaWrapper? = PlaylistManager.currentPlayedMedia.value) {
        ensurePlayerProgressBound()
        refreshTracksFromHost()
        refreshDelaysFromHost()
        refreshChaptersFromHost()
        val item = media?.toMediaItem() ?: currentItem()
        val progress = _progress.value
        val newState = when {
            item == null && !PlaylistManager.hasMedia() -> PlaybackState.Idle
            item == null -> PlaybackState.Stopped(null)
            playing -> PlaybackState.Playing(item, progress)
            PlayerControllerState.isStopped() && !PlaylistManager.hasMedia() -> PlaybackState.Stopped(item)
            else -> PlaybackState.Paused(item, progress)
        }
        if (_state.value != newState) {
            _state.value = newState
            observers.forEach { it.onStateChanged(newState) }
        }
    }

    private fun currentItem(): MediaItem? =
        PlaylistManager.currentPlayedMedia.value?.toMediaItem()
            ?: _playlist.value.current

    private fun refreshTracksFromHost() {
        val player = manager()?.player ?: run {
            _tracks.value = PlaybackTracks()
            return
        }
        val selectedAudio = player.getAudioTrack()
        val selectedSubtitle = player.getSpuTrack()
        _tracks.value = PlaybackTracks(
            audio = player.getAudioTracks().orEmpty().map { track ->
                PlaybackTrack(
                    id = track.getId(),
                    label = track.getName().ifBlank { "Audio" },
                    selected = track.getId() == selectedAudio,
                )
            },
            subtitles = player.getSpuTracks().orEmpty().map { track ->
                PlaybackTrack(
                    id = track.getId(),
                    label = track.getName().ifBlank { "Subtitle" },
                    selected = track.getId() == selectedSubtitle,
                )
            },
        )
    }

    private fun refreshDelaysFromHost() {
        val player = manager()?.player ?: run {
            _delays.value = PlaybackDelays()
            return
        }
        _delays.value = PlaybackDelays(
            audioUs = player.getAudioDelay(),
            subtitleUs = player.getSpuDelay(),
            supported = true,
        )
    }

    private fun refreshChaptersFromHost() {
        val player = manager()?.player ?: run {
            _chapters.value = PlaybackChapters()
            return
        }
        val selected = player.getChapterIdx()
        _chapters.value = PlaybackChapters(
            player.getChapters(-1).orEmpty().mapIndexed { index, chapter ->
                PlaybackChapter(
                    index = index,
                    title = chapter.name.ifBlank { "Chapter ${index + 1}" },
                    positionMs = chapter.timeOffset,
                    selected = index == selected,
                )
            }
        )
    }

    private fun repeatModeFromHost(): RepeatMode = when (PlaylistManager.repeating.value) {
        PlaybackStateCompat.REPEAT_MODE_ONE -> RepeatMode.ONE
        PlaybackStateCompat.REPEAT_MODE_ALL,
        PlaybackStateCompat.REPEAT_MODE_GROUP -> RepeatMode.ALL
        else -> RepeatMode.NONE
    }

    private fun MediaItem.toMediaWrapper(): MediaWrapper {
        if (id != 0L) {
            Medialibrary.getInstance().getMedia(id)?.let { return it }
        }
        val parsedUri = Uri.parse(uri)
        val itemType = type
        return MLServiceLocator.getAbstractMediaWrapper(parsedUri).apply {
            if (title.isNotBlank()) this.title = title
            this.type = when (itemType) {
                MediaType.VIDEO -> MediaWrapper.TYPE_VIDEO
                MediaType.AUDIO -> MediaWrapper.TYPE_AUDIO
                MediaType.STREAM -> MediaWrapper.TYPE_STREAM
                MediaType.DIR -> MediaWrapper.TYPE_DIR
                MediaType.SUBTITLE -> MediaWrapper.TYPE_SUBTITLE
                MediaType.PLAYLIST -> MediaWrapper.TYPE_PLAYLIST
                MediaType.GROUP -> MediaWrapper.TYPE_GROUP
                MediaType.ALL -> MediaWrapper.TYPE_ALL
            }
            if (duration > 0L) length = duration
        }
    }

}

private fun VideoScaleMode.toAndroidScaleType(): MediaPlayer.ScaleType = when (this) {
    VideoScaleMode.BEST_FIT -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
    VideoScaleMode.FIT_SCREEN -> MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
    VideoScaleMode.FILL -> MediaPlayer.ScaleType.SURFACE_FILL
    VideoScaleMode.RATIO_16_9 -> MediaPlayer.ScaleType.SURFACE_16_9
    VideoScaleMode.RATIO_4_3 -> MediaPlayer.ScaleType.SURFACE_4_3
    VideoScaleMode.RATIO_16_10 -> MediaPlayer.ScaleType.SURFACE_16_10
    VideoScaleMode.RATIO_2_1 -> MediaPlayer.ScaleType.SURFACE_2_1
    VideoScaleMode.RATIO_221_1 -> MediaPlayer.ScaleType.SURFACE_221_1
    VideoScaleMode.RATIO_235_1 -> MediaPlayer.ScaleType.SURFACE_235_1
    VideoScaleMode.RATIO_239_1 -> MediaPlayer.ScaleType.SURFACE_239_1
    VideoScaleMode.RATIO_5_4 -> MediaPlayer.ScaleType.SURFACE_5_4
    VideoScaleMode.ORIGINAL -> MediaPlayer.ScaleType.SURFACE_ORIGINAL
}

/**
 * Small helper so we don't import PlayerController companion just for stopped checks
 * without holding a player instance.
 */
private object PlayerControllerState {
    fun isStopped(): Boolean =
        org.videolan.vlc.media.PlayerController.playbackState == PlaybackStateCompat.STATE_STOPPED ||
            org.videolan.vlc.media.PlayerController.playbackState == PlaybackStateCompat.STATE_NONE
}
