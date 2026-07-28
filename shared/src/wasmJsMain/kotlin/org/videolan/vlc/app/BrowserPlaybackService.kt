@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.videolan.vlc.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackObserver
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState
import org.videolan.vlc.player.PlaybackRate
import org.videolan.vlc.player.VideoScaleMode
import org.videolan.vlc.player.PlaybackTracks
import org.videolan.vlc.player.PlaybackTrack
import org.videolan.vlc.player.SleepTimerController
import org.videolan.vlc.player.SleepTimerState
import org.w3c.dom.HTMLElement

/**
 * Browser playback adapter for user-imported files and browser-decodable streams.
 *
 * The playlist, HUD, and state contract stay shared. A source the browser cannot
 * decode fails visibly instead of being represented as pretend state-only playback.
 */
internal class BrowserPlaybackService : PlaybackService {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val _progress = MutableStateFlow(Progress())
    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    private val _abRepeat = MutableStateFlow(ABRepeat())
    private val _abRepeatEnabled = MutableStateFlow(false)
    private val _stopAfterCurrent = MutableStateFlow(false)
    private val _videoScaleMode = MutableStateFlow(VideoScaleMode.BEST_FIT)
    private val _tracks = MutableStateFlow(PlaybackTracks())
    private val sleepTimerController = SleepTimerController(
        isPlaying = { _state.value is PlaybackState.Playing },
        stopPlayback = ::stop,
    )
    private val observers = mutableSetOf<PlaybackObserver>()
    private var volume = 100
    private var rate = 1f
    private var mediaElement: HTMLElement? = null
    private var fallbackElement: HTMLElement? = null
    private var progressTimer: Int? = null

    override val state: Flow<PlaybackState> = _state.asStateFlow()
    override val progress: Flow<Progress> = _progress.asStateFlow()
    override val currentPlaylist: Flow<Playlist> = _playlist.asStateFlow()
    override val abRepeat: Flow<ABRepeat> = _abRepeat.asStateFlow()
    override val abRepeatEnabled: Flow<Boolean> = _abRepeatEnabled.asStateFlow()
    override val stopAfterCurrent: Flow<Boolean> = _stopAfterCurrent.asStateFlow()
    override val videoScaleMode: Flow<VideoScaleMode> = _videoScaleMode.asStateFlow()
    override val tracks: Flow<PlaybackTracks> = _tracks.asStateFlow()
    override val sleepTimer: Flow<SleepTimerState> = sleepTimerController.state

    init {
        BrowserMediaElementHost.register(this)
    }

    override fun play(item: MediaItem, playlist: List<MediaItem>) {
        val queue = playlist.ifEmpty { listOf(item) }
        val index = queue.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        playFromIndex(queue, index)
    }

    override fun playFromIndex(playlist: List<MediaItem>, index: Int) {
        if (playlist.isEmpty()) return
        clearStopAfter()
        val safeIndex = index.coerceIn(0, playlist.lastIndex)
        val item = playlist[safeIndex]
        _playlist.value = Playlist(
            id = 0,
            name = "Current",
            items = playlist,
            currentIndex = safeIndex,
            shuffle = _playlist.value.shuffle,
            repeatMode = _playlist.value.repeatMode,
        )
        notifyPlaylist()
        _progress.value = Progress(length = item.duration)
        _tracks.value = PlaybackTracks()
        notifyProgress()
        if (item.isBrowserPlayableMedia()) {
            emitState(PlaybackState.Loading)
            mediaElement?.let(::attachCurrentItem)
        } else {
            reportPlaybackFailure("This media source cannot be played in a browser.")
        }
    }

    override fun pause() {
        mediaElement?.let(::pauseHtmlMedia)
        currentItem()?.let { emitState(PlaybackState.Paused(it, _progress.value)) }
    }

    override fun resume() {
        val item = currentItem() ?: return
        if (!item.isBrowserPlayableMedia()) {
            reportPlaybackFailure("This media source cannot be played in a browser.")
            return
        }
        mediaElement?.let { playHtmlMedia(it, ::reportPlaybackFailure) }
        emitState(PlaybackState.Playing(item, _progress.value))
    }

    override fun stop() {
        mediaElement?.let(::pauseHtmlMedia)
        clearStopAfter()
        emitState(PlaybackState.Stopped(currentItem()))
    }

    override fun setSleepTimer(durationMillis: Long, waitForCurrentItem: Boolean) =
        sleepTimerController.start(durationMillis, waitForCurrentItem)

    override fun clearSleepTimer() = sleepTimerController.clear()

    override fun loadExternalSubtitle(uri: String): Boolean {
        val element = mediaElement ?: return false
        attachHtmlSubtitleTrack(element, uri)
        _tracks.value = PlaybackTracks(
            subtitles = listOf(PlaybackTrack(id = uri, label = "External subtitle", selected = true)),
        )
        return true
    }

    override fun seekTo(position: Long) {
        val target = position.coerceAtLeast(0L)
        mediaElement?.let { seekHtmlMedia(it, target) }
        _progress.value = _progress.value.copy(time = target)
        notifyProgress()
        currentItem()?.let { item ->
            val state = if (_state.value is PlaybackState.Playing) {
                PlaybackState.Playing(item, _progress.value)
            } else {
                PlaybackState.Paused(item, _progress.value)
            }
            emitState(state)
        }
    }

    override fun seekRelative(delta: Long) = seekTo(_progress.value.time + delta)

    override fun next() {
        val playlist = _playlist.value
        val next = when {
            playlist.items.isEmpty() -> null
            playlist.repeatMode == RepeatMode.ONE -> playlist.currentIndex
            playlist.shuffle -> (playlist.currentIndex + 1) % playlist.items.size
            playlist.currentIndex < playlist.items.lastIndex -> playlist.currentIndex + 1
            playlist.repeatMode == RepeatMode.ALL -> 0
            else -> null
        }
        if (next == null) currentItem()?.let { emitState(PlaybackState.Ended(it)) }
        else playFromIndex(playlist.items, next)
    }

    override fun previous() {
        val playlist = _playlist.value
        val previous = when {
            playlist.items.isEmpty() -> null
            playlist.currentIndex > 0 -> playlist.currentIndex - 1
            playlist.repeatMode == RepeatMode.ALL -> playlist.items.lastIndex
            else -> null
        }
        if (previous != null) playFromIndex(playlist.items, previous)
    }

    override fun setShuffle(enabled: Boolean) {
        _playlist.value = _playlist.value.copy(shuffle = enabled)
        notifyPlaylist()
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _playlist.value = _playlist.value.copy(repeatMode = mode)
        notifyPlaylist()
    }

    override fun setVolume(volume: Int) {
        this.volume = volume.coerceIn(0, 100)
        mediaElement?.let { setHtmlMediaVolume(it, this.volume) }
    }

    override fun getVolume(): Int = volume

    override fun setRate(rate: Float) {
        this.rate = PlaybackRate.normalize(rate)
        mediaElement?.let { setHtmlMediaRate(it, this.rate) }
    }

    override fun getRate(): Float = rate

    override fun setVideoScaleMode(mode: VideoScaleMode) {
        _videoScaleMode.value = mode
        mediaElement?.let { setHtmlMediaObjectFit(it, mode.cssObjectFit) }
    }

    override fun addObserver(observer: PlaybackObserver) {
        observers += observer
    }

    override fun removeObserver(observer: PlaybackObserver) {
        observers -= observer
    }

    override fun moveItem(from: Int, to: Int) {
        val playlist = _playlist.value
        if (from !in playlist.items.indices || to !in playlist.items.indices || from == to) return
        val items = playlist.items.toMutableList()
        val moved = items.removeAt(from)
        items.add(to, moved)
        val currentIndex = when {
            playlist.currentIndex == from -> to
            from < playlist.currentIndex && to >= playlist.currentIndex -> playlist.currentIndex - 1
            from > playlist.currentIndex && to <= playlist.currentIndex -> playlist.currentIndex + 1
            else -> playlist.currentIndex
        }
        _playlist.value = playlist.copy(items = items, currentIndex = currentIndex)
        notifyPlaylist()
    }

    override fun removeAt(index: Int) {
        val playlist = _playlist.value
        if (index !in playlist.items.indices) return
        val wasCurrent = index == playlist.currentIndex
        val items = playlist.items.toMutableList().also { it.removeAt(index) }
        if (items.isEmpty()) {
            stop()
            _playlist.value = playlist.copy(items = emptyList(), currentIndex = 0)
            notifyPlaylist()
            return
        }
        val currentIndex = when {
            index < playlist.currentIndex -> playlist.currentIndex - 1
            playlist.currentIndex > items.lastIndex -> items.lastIndex
            else -> playlist.currentIndex
        }
        _playlist.value = playlist.copy(items = items, currentIndex = currentIndex)
        notifyPlaylist()
        if (wasCurrent) playFromIndex(items, currentIndex)
    }

    override fun toggleABRepeat() {
        _abRepeatEnabled.value = !_abRepeatEnabled.value
        if (!_abRepeatEnabled.value) _abRepeat.value = ABRepeat()
    }

    override fun setABRepeatValue(timeMs: Long) {
        val time = timeMs.coerceAtLeast(0L)
        _abRepeat.value = when {
            _abRepeat.value.start < 0L -> ABRepeat(start = time)
            time < _abRepeat.value.start -> ABRepeat(start = time, stop = _abRepeat.value.start)
            else -> _abRepeat.value.copy(stop = time)
        }
        _abRepeatEnabled.value = true
    }

    override fun resetABRepeat() {
        _abRepeat.value = ABRepeat()
    }

    override fun clearABRepeat() {
        _abRepeat.value = ABRepeat()
        _abRepeatEnabled.value = false
    }

    override fun setStopAfterThis() {
        _stopAfterCurrent.value = currentItem() != null
    }

    override fun clearStopAfter() {
        _stopAfterCurrent.value = false
    }

    /** The hidden anchor stays mounted so audio survives closing the player route. */
    fun attachFallback(element: HTMLElement) {
        fallbackElement = element
        if (mediaElement == null) {
            attachMediaElement(element)
            attachCurrentItem(element)
        }
    }

    fun detachFallback(element: HTMLElement) {
        if (fallbackElement !== element) return
        fallbackElement = null
        if (mediaElement === element) detachMediaElement()
    }

    /** The visible video element temporarily takes over from the hidden audio anchor. */
    fun attachSurface(element: HTMLElement) {
        if (mediaElement !== element) {
            detachMediaElement()
            attachMediaElement(element)
        }
        attachCurrentItem(element)
    }

    fun detachSurface(element: HTMLElement) {
        if (mediaElement !== element) return
        detachMediaElement()
        fallbackElement?.let { fallback ->
            attachMediaElement(fallback)
            attachCurrentItem(fallback)
        }
    }

    private fun attachMediaElement(element: HTMLElement) {
        mediaElement = element
        observeHtmlMediaEnd(element, ::handleEnded)
        observeHtmlMediaFailure(element, ::reportPlaybackFailure)
        progressTimer = startHtmlMediaProgressPolling(element, ::syncProgressFromElement)
    }

    private fun detachMediaElement() {
        val element = mediaElement ?: return
        // The anchor and video surface are separate DOM decoders. Stop the old one before
        // handing the source over so a route change cannot leave two audio tracks playing.
        pauseHtmlMedia(element)
        progressTimer?.let(::stopHtmlMediaProgressPolling)
        progressTimer = null
        clearHtmlMediaEndListener(element)
        clearHtmlMediaFailureListener(element)
        mediaElement = null
    }

    private fun attachCurrentItem(element: HTMLElement) {
        val item = currentItem()?.takeIf(MediaItem::isBrowserPlayableMedia) ?: return
        setHtmlMediaSource(element, item.uri)
        setHtmlMediaVolume(element, volume)
        setHtmlMediaRate(element, rate)
        setHtmlMediaObjectFit(element, _videoScaleMode.value.cssObjectFit)
        // The visible video element and persistent audio anchor are different DOM nodes.
        // Reapply common progress whenever ownership moves between them.
        seekHtmlMedia(element, _progress.value.time)
        if (_state.value is PlaybackState.Loading || _state.value is PlaybackState.Playing) {
            playHtmlMedia(element, ::reportPlaybackFailure)
        }
    }

    private fun syncProgressFromElement(time: Long, duration: Long, playing: Boolean) {
        val item = currentItem()?.takeIf(MediaItem::isBrowserPlayableMedia) ?: return
        val repeat = _abRepeat.value
        if (_abRepeatEnabled.value && repeat.isActive && time >= repeat.stop) {
            seekTo(repeat.start)
            return
        }
        val progress = Progress(
            time = time.coerceAtLeast(0L),
            length = duration.takeIf { it > 0L } ?: item.duration,
        )
        if (progress != _progress.value) {
            _progress.value = progress
            notifyProgress()
        }
        when (_state.value) {
            PlaybackState.Loading -> if (playing) emitState(PlaybackState.Playing(item, progress))
            is PlaybackState.Playing -> if (!playing) emitState(PlaybackState.Paused(item, progress))
            is PlaybackState.Paused -> if (playing) emitState(PlaybackState.Playing(item, progress))
            else -> Unit
        }
    }

    private fun currentItem(): MediaItem? = _playlist.value.current

    private fun handleEnded() {
        if (sleepTimerController.state.value.awaitingCurrentItemEnd) {
            sleepTimerController.onCurrentItemEnded()
            return
        }
        if (_stopAfterCurrent.value) {
            clearStopAfter()
            stop()
        } else {
            next()
        }
    }

    private fun reportPlaybackFailure(message: String) {
        emitState(PlaybackState.Error(message))
    }

    private fun emitState(state: PlaybackState) {
        _state.value = state
        observers.toList().forEach { it.onStateChanged(state) }
    }

    private fun notifyProgress() {
        observers.toList().forEach { it.onProgressChanged(_progress.value) }
    }

    private fun notifyPlaylist() {
        observers.toList().forEach { it.onPlaylistChanged(_playlist.value) }
    }
}

/** Connects the Compose HTML interop element to the Koin-owned browser player. */
internal object BrowserMediaElementHost {
    private var service: BrowserPlaybackService? = null
    private var fallbackElement: HTMLElement? = null

    fun register(service: BrowserPlaybackService) {
        this.service = service
        fallbackElement?.let(service::attachFallback)
    }

    fun attachFallback(element: HTMLElement) {
        fallbackElement = element
        service?.attachFallback(element)
    }

    fun detachFallback(element: HTMLElement) {
        if (fallbackElement === element) fallbackElement = null
        service?.detachFallback(element)
    }

    fun attachSurface(element: HTMLElement) {
        service?.attachSurface(element)
    }

    fun detachSurface(element: HTMLElement) {
        service?.detachSurface(element)
    }
}

internal fun MediaItem.isBrowserPlayableMedia(): Boolean = uri.isBrowserPlayableUri()

/** Browsers can decode local object URLs and CORS-permitted HTTP(S) media only. */
internal fun String.isBrowserPlayableUri(): Boolean =
    startsWith("blob:") || startsWith("https://") || startsWith("http://")

private fun setHtmlMediaSource(element: HTMLElement, source: String): Unit = js(
    "{ if (element.src !== source) { element.src = source; element.load?.(); } }",
)

private fun playHtmlMedia(element: HTMLElement, onFailure: (String) -> Unit): Unit = js(
    """{
        const result = element.play?.();
        result?.catch?.(error => {
            const message = error?.message || '';
            if (error?.name !== 'AbortError' && !message.includes('interrupted by a call to pause')) {
                onFailure(message || 'This media format cannot be played in this browser.');
            }
        });
    }""",
)

private fun pauseHtmlMedia(element: HTMLElement): Unit = js("{ element.pause?.(); }")

private fun seekHtmlMedia(element: HTMLElement, position: Long): Unit = js(
    "{ element.currentTime = Number(position) / 1000; }",
)

private fun setHtmlMediaVolume(element: HTMLElement, volume: Int): Unit = js(
    "{ element.volume = Math.max(0, Math.min(1, volume / 100)); }",
)

private fun setHtmlMediaRate(element: HTMLElement, rate: Float): Unit = js("{ element.playbackRate = rate; }")

private fun setHtmlMediaObjectFit(element: HTMLElement, objectFit: String): Unit = js(
    "{ element.style.objectFit = objectFit; }",
)

private fun attachHtmlSubtitleTrack(element: HTMLElement, uri: String): Unit = js(
    """{
        const existing = element.querySelector?.('track[data-vlc-external-subtitle]');
        existing?.remove?.();
        const track = document.createElement('track');
        track.kind = 'subtitles';
        track.src = uri;
        track.default = true;
        track.setAttribute('data-vlc-external-subtitle', 'true');
        element.appendChild(track);
        track.track && (track.track.mode = 'showing');
    }""",
)

private fun observeHtmlMediaEnd(element: HTMLElement, onEnded: () -> Unit): Unit = js(
    "{ element.onended = () => onEnded(); }",
)

private fun clearHtmlMediaEndListener(element: HTMLElement): Unit = js("{ element.onended = null; }")

private fun observeHtmlMediaFailure(element: HTMLElement, onFailure: (String) -> Unit): Unit = js(
    """{ element.onerror = () => onFailure('This media format cannot be played in this browser.'); }""",
)

private fun clearHtmlMediaFailureListener(element: HTMLElement): Unit = js("{ element.onerror = null; }")

private fun startHtmlMediaProgressPolling(
    element: HTMLElement,
    onProgress: (time: Long, duration: Long, playing: Boolean) -> Unit,
): Int = js(
    """globalThis.setInterval(() => {
        const time = BigInt(Math.max(0, Math.round((element.currentTime || 0) * 1000)));
        const duration = BigInt(Math.max(0, Math.round(Number.isFinite(element.duration) ? element.duration * 1000 : 0)));
        onProgress(time, duration, !element.paused && !element.ended);
    }, 250)""",
)

private fun stopHtmlMediaProgressPolling(id: Int): Unit = js("{ globalThis.clearInterval(id); }")
