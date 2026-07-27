package org.videolan.vlc.app

import kotlinx.coroutines.flow.Flow
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.VideoScaleMode
import org.videolan.vlc.player.PlaybackObserver
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState
import org.videolan.vlc.player.PlayerBackend
import org.videolan.vlc.player.PlaylistEngine
import platform.Foundation.NSURL
import platform.UIKit.UIView

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
    private var kitBackend: VlcKitPlayerBackend? = null

    override val state: Flow<PlaybackState> get() = engine.state
    override val progress: Flow<Progress> get() = engine.progress
    override val currentPlaylist: Flow<Playlist> get() = engine.currentPlaylist
    override val abRepeat: Flow<ABRepeat> get() = engine.abRepeat
    override val abRepeatEnabled: Flow<Boolean> get() = engine.abRepeatEnabled
    override val stopAfterCurrent: Flow<Boolean> get() = engine.stopAfterCurrent
    override val videoScaleMode: Flow<VideoScaleMode> get() = engine.videoScaleMode

    /**
     * Accepts the legacy VlcKitPlayerBackend and adapts it to [PlayerBackend].
     */
    fun setBackend(backend: VlcKitPlayerBackend?) {
        kitBackend = backend
        if (backend == null) {
            engine.setBackend(null)
            return
        }
        engine.setBackend(VlcKitPlayerBackendAdapter(backend))
    }

    /** Routes the Compose-owned drawable to the native VLCKit backend. */
    fun attachDrawable(view: UIView?) {
        kitBackend?.attachDrawable(view)
    }

    /** Writes a paused-only session snapshot at UIKit lifecycle boundaries. */
    fun saveSession() {
        val playlist = engine.snapshot()
        if (playlist.items.isEmpty()) {
            IosMediaLibrary.shared.clearPlaybackSession()
            return
        }
        IosMediaLibrary.shared.savePlaybackSession(
            playlist = playlist,
            positionMs = engine.progress.value.time,
            volume = engine.getVolume(),
            rate = engine.getRate(),
        )
    }

    /**
     * Recreates the last queue in a paused state after launch. Missing local files are removed
     * before handing it to the shared engine, and no decoder is asked to auto-play.
     */
    fun restoreSession(): Boolean {
        val saved = IosMediaLibrary.shared.playbackSession() ?: return false
        val libraryByUri = IosMediaLibrary.shared.snapshot().associateBy(MediaItem::uri)
        val savedCurrentUri = saved.playlist.current?.uri
        val items = saved.playlist.items.mapNotNull { libraryByUri[it.uri] }
        if (items.isEmpty()) {
            IosMediaLibrary.shared.clearPlaybackSession()
            return false
        }
        val index = items.indexOfFirst { it.uri == savedCurrentUri }.coerceAtLeast(0)
        val playlist = saved.playlist.copy(items = items, currentIndex = index)
        engine.setVolume(saved.volume)
        engine.setRate(saved.rate)
        return engine.restorePaused(playlist, saved.positionMs).also { restored ->
            if (restored) {
                IosMediaLibrary.shared.savePlaybackSession(
                    playlist = playlist,
                    positionMs = saved.positionMs,
                    volume = engine.getVolume(),
                    rate = engine.getRate(),
                )
            }
        }
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
    override fun setVideoScaleMode(mode: VideoScaleMode) = engine.setVideoScaleMode(mode)
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
    override fun preparePaused(uri: String, title: String?, positionMs: Long): Boolean =
        kit.preparePaused(uri, title, positionMs)
    override fun pause() = kit.pause()
    override fun resume() = kit.resume()
    override fun stop() = kit.stop()
    override fun seekTo(positionMs: Long) = kit.seekTo(positionMs)
    override fun setVolume(volume: Int) = kit.setVolume(volume)
    override fun getVolume(): Int = kit.getVolume()
    override fun setRate(rate: Float) = kit.setRate(rate)
    override fun getRate(): Float = kit.getRate()
    override fun setVideoOutput(aspectRatio: String?, scale: Float) = kit.setVideoOutput(aspectRatio, scale)
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
    override fun release() = kit.dispose()
}

/**
 * Thin bridge Swift implements with VLCKit's VLCMediaPlayer.
 */
interface VlcKitPlayerBackend {
    /** Sets the UIView VLCKit should draw video into, or clears it on disposal. */
    fun attachDrawable(view: UIView?)
    fun play(uri: String, title: String?)
    /** Loads the item and start position without starting playback. */
    fun preparePaused(uri: String, title: String?, positionMs: Long): Boolean
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Int)
    fun getVolume(): Int
    fun setRate(rate: Float)
    fun getRate(): Float
    fun setVideoOutput(aspectRatio: String?, scale: Float)
    fun setListener(listener: Listener?)
    /** Named to avoid colliding with Objective-C NSObject.release in Swift implementations. */
    fun dispose()

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
