package org.videolan.vlc.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackObserver
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState
import platform.Foundation.NSURL

/**
 * iOS [PlaybackService] that owns shared playlist/state machine and delegates
 * actual decoding to a [VlcKitPlayerBackend] (VLCKit / VLCMediaPlayer).
 *
 * Swift registers the backend once VLCKit is linked:
 * ```swift
 * IosPlaybackService.shared.setBackend(VlcKitBackend())
 * ```
 */
class IosPlaybackService : PlaybackService {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: Flow<PlaybackState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(Progress())
    override val progress: Flow<Progress> = _progress.asStateFlow()

    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    override val currentPlaylist: Flow<Playlist> = _playlist.asStateFlow()

    private val observers = mutableListOf<PlaybackObserver>()
    private var backend: VlcKitPlayerBackend? = null
    private var volume: Int = 100
    private var rate: Float = 1.0f

    fun setBackend(backend: VlcKitPlayerBackend?) {
        this.backend?.release()
        this.backend = backend
        backend?.setListener(object : VlcKitPlayerBackend.Listener {
            override fun onPlaying() = pushPlaying()
            override fun onPaused() = pushPaused()
            override fun onStopped() {
                val item = currentItem()
                updateState(PlaybackState.Stopped(item))
            }
            override fun onEnded() {
                val item = currentItem() ?: return
                when (_playlist.value.repeatMode) {
                    RepeatMode.ONE -> playFromIndex(_playlist.value.items, _playlist.value.currentIndex)
                    RepeatMode.ALL -> next()
                    RepeatMode.NONE -> {
                        updateState(PlaybackState.Ended(item))
                        next()
                    }
                }
            }
            override fun onError(message: String) = updateState(PlaybackState.Error(message))
            override fun onTimeChanged(timeMs: Long, lengthMs: Long) {
                updateProgress(timeMs, lengthMs)
            }
        })
    }

    override fun play(item: MediaItem, playlist: List<MediaItem>) {
        val list = if (playlist.isEmpty()) listOf(item) else playlist
        val index = list.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
        playFromIndex(list, index)
    }

    override fun playFromIndex(playlist: List<MediaItem>, index: Int) {
        if (playlist.isEmpty()) {
            updateState(PlaybackState.Error("Empty playlist"))
            return
        }
        val safeIndex = index.coerceIn(0, playlist.lastIndex)
        _playlist.value = _playlist.value.copy(
            items = playlist,
            currentIndex = safeIndex,
        )
        notifyPlaylist()
        val item = playlist[safeIndex]
        updateState(PlaybackState.Loading)
        val backend = backend
        if (backend == null) {
            // No VLCKit yet — keep shared state coherent for UI development.
            updateProgress(0L, item.duration.coerceAtLeast(0L))
            updateState(PlaybackState.Playing(item, _progress.value))
            return
        }
        try {
            backend.play(item.uri, item.title)
        } catch (t: Throwable) {
            updateState(PlaybackState.Error(t.message ?: "VLCKit play failed"))
        }
    }

    override fun pause() {
        backend?.pause()
        pushPaused()
    }

    override fun resume() {
        backend?.resume()
        pushPlaying()
    }

    override fun stop() {
        backend?.stop()
        updateState(PlaybackState.Stopped(currentItem()))
    }

    override fun seekTo(position: Long) {
        backend?.seekTo(position.coerceAtLeast(0L))
            ?: updateProgress(position.coerceAtLeast(0L), _progress.value.length)
    }

    override fun seekRelative(delta: Long) {
        val target = (_progress.value.time + delta).coerceAtLeast(0L)
        seekTo(target)
    }

    override fun next() {
        val pl = _playlist.value
        if (pl.items.isEmpty()) return
        val nextIndex = when {
            pl.shuffle && pl.items.size > 1 -> {
                var i = pl.currentIndex
                while (i == pl.currentIndex) i = pl.items.indices.random()
                i
            }
            pl.currentIndex + 1 < pl.items.size -> pl.currentIndex + 1
            pl.repeatMode == RepeatMode.ALL -> 0
            else -> return
        }
        playFromIndex(pl.items, nextIndex)
    }

    override fun previous() {
        val pl = _playlist.value
        if (pl.items.isEmpty()) return
        if (_progress.value.time > 3_000L) {
            seekTo(0L)
            return
        }
        val prev = if (pl.currentIndex > 0) pl.currentIndex - 1 else return
        playFromIndex(pl.items, prev)
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
        this.volume = volume.coerceIn(0, 200)
        backend?.setVolume(this.volume)
    }

    override fun getVolume(): Int = volume

    override fun setRate(rate: Float) {
        this.rate = rate.coerceIn(0.25f, 4.0f)
        backend?.setRate(this.rate)
    }

    override fun getRate(): Float = rate

    override fun addObserver(observer: PlaybackObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: PlaybackObserver) {
        observers.remove(observer)
    }

    private fun currentItem(): MediaItem? = _playlist.value.current

    private fun pushPlaying() {
        val item = currentItem() ?: return
        updateState(PlaybackState.Playing(item, _progress.value))
    }

    private fun pushPaused() {
        val item = currentItem() ?: return
        updateState(PlaybackState.Paused(item, _progress.value))
    }

    private fun updateState(newState: PlaybackState) {
        _state.value = newState
        observers.forEach { it.onStateChanged(newState) }
    }

    private fun updateProgress(time: Long, length: Long) {
        val p = Progress(time, length)
        _progress.value = p
        observers.forEach { it.onProgressChanged(p) }
        when (val s = _state.value) {
            is PlaybackState.Playing -> updateState(s.copy(progress = p))
            is PlaybackState.Paused -> updateState(s.copy(progress = p))
            else -> Unit
        }
    }

    private fun notifyPlaylist() {
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
    }

    companion object {
        /** Process-wide instance for Swift to attach a VLCKit backend. */
        val shared: IosPlaybackService by lazy { IosPlaybackService() }
    }
}

/**
 * Thin bridge Swift implements with VLCKit's `VLCMediaPlayer`.
 *
 * Kept free of UIKit so it can live in the shared framework; Swift supplies
 * the concrete player on the main thread.
 */
interface VlcKitPlayerBackend {
    fun play(uri: String, title: String?)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Int)
    fun setRate(rate: Float)
    fun setListener(listener: Listener?)
    fun release()

    interface Listener {
        fun onPlaying()
        fun onPaused()
        fun onStopped()
        fun onEnded()
        fun onError(message: String)
        fun onTimeChanged(timeMs: Long, lengthMs: Long)
    }
}

/** Helper for Swift when building NSURL-based media. */
fun mediaUrlOrNull(uri: String): NSURL? =
    NSURL.URLWithString(uri) ?: NSURL.fileURLWithPath(uri)
