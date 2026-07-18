package org.videolan.vlc.app

import kotlinx.coroutines.flow.Flow
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackObserver
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState
import org.videolan.vlc.player.PlayerBackend
import org.videolan.vlc.player.PlaylistEngine
import platform.Foundation.NSURL

/**
 * iOS [PlaybackService] backed by shared [PlaylistEngine] + optional VLCKit backend.
 *
 * Swift:
 * ```swift
 * IosPlaybackService.shared.setBackend(VlcKitBackend())
 * ```
 */
class IosPlaybackService : PlaybackService {

    private val engine = PlaylistEngine()

    override val state: Flow<PlaybackState> get() = engine.state
    override val progress: Flow<Progress> get() = engine.progress
    override val currentPlaylist: Flow<Playlist> get() = engine.currentPlaylist

    /**
     * Accepts the legacy VlcKitPlayerBackend and adapts it to [PlayerBackend].
     */
    fun setBackend(backend: VlcKitPlayerBackend?) {
        if (backend == null) {
            engine.setBackend(null)
            return
        }
        engine.setBackend(VlcKitPlayerBackendAdapter(backend))
    }

    override fun play(item: MediaItem, playlist: List<MediaItem>) = engine.play(item, playlist)
    override fun playFromIndex(playlist: List<MediaItem>, index: Int) = engine.playFromIndex(playlist, index)
    override fun pause() = engine.pause()
    override fun resume() = engine.resume()
    override fun stop() = engine.stop()
    override fun seekTo(position: Long) = engine.seekTo(position)
    override fun seekRelative(delta: Long) = engine.seekRelative(delta)
    override fun next() = engine.next()
    override fun previous() = engine.previous()
    override fun setShuffle(enabled: Boolean) = engine.setShuffle(enabled)
    override fun setRepeatMode(mode: RepeatMode) = engine.setRepeatMode(mode)
    override fun setVolume(volume: Int) = engine.setVolume(volume)
    override fun getVolume(): Int = engine.getVolume()
    override fun setRate(rate: Float) = engine.setRate(rate)
    override fun getRate(): Float = engine.getRate()
    override fun addObserver(observer: PlaybackObserver) = engine.addObserver(observer)
    override fun removeObserver(observer: PlaybackObserver) = engine.removeObserver(observer)

    override fun append(items: List<MediaItem>) = engine.append(items)
    override fun insertNext(items: List<MediaItem>) = engine.insertNext(items)
    override fun insertAt(index: Int, item: MediaItem) = engine.insertAt(index, item)
    override fun moveItem(from: Int, to: Int) = engine.moveItem(from, to)
    override fun removeAt(index: Int) = engine.removeAt(index)
    override fun removeByUri(uri: String) = engine.removeByUri(uri)
    override fun clearQueue() = engine.clearQueue()
    override fun setStopAfterThis() = engine.setStopAfterThis()
    override fun clearStopAfter() = engine.clearStopAfter()
    override fun toggleABRepeat() = engine.toggleABRepeat()
    override fun setABRepeatValue(timeMs: Long) = engine.setABRepeatValue(timeMs)
    override fun resetABRepeat() = engine.resetABRepeat()
    override fun clearABRepeat() = engine.clearABRepeat()

    companion object {
        val shared: IosPlaybackService by lazy { IosPlaybackService() }
    }
}

/** Adapts Swift VlcKitPlayerBackend to shared PlayerBackend. */
private class VlcKitPlayerBackendAdapter(
    private val kit: VlcKitPlayerBackend,
) : PlayerBackend {
    override fun playUri(uri: String, title: String?) = kit.play(uri, title)
    override fun pause() = kit.pause()
    override fun resume() = kit.resume()
    override fun stop() = kit.stop()
    override fun seekTo(positionMs: Long) = kit.seekTo(positionMs)
    override fun setVolume(volume: Int) = kit.setVolume(volume)
    override fun getVolume(): Int = 100
    override fun setRate(rate: Float) = kit.setRate(rate)
    override fun getRate(): Float = 1f
    override fun setListener(listener: PlayerBackend.Listener?) {
        if (listener == null) {
            kit.setListener(null)
            return
        }
        kit.setListener(object : VlcKitPlayerBackend.Listener {
            override fun onPlaying() = listener.onPlaying()
            override fun onPaused() = listener.onPaused()
            override fun onStopped() = listener.onStopped()
            override fun onEnded() = listener.onEnded()
            override fun onError(message: String) = listener.onError(message)
            override fun onTimeChanged(timeMs: Long, lengthMs: Long) =
                listener.onTimeChanged(timeMs, lengthMs)
        })
    }
    override fun release() = kit.release()
}

/**
 * Thin bridge Swift implements with VLCKit's VLCMediaPlayer.
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

fun mediaUrlOrNull(uri: String): NSURL? =
    NSURL.URLWithString(uri) ?: NSURL.fileURLWithPath(uri)
