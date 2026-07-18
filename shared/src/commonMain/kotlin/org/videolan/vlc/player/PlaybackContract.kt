package org.videolan.vlc.player

import kotlinx.coroutines.flow.Flow
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode

/**
 * Playback state shared across platforms.
 */
sealed class PlaybackState {
    data object Idle : PlaybackState()
    data object Loading : PlaybackState()
    data class Playing(val item: MediaItem, val progress: Progress) : PlaybackState()
    data class Paused(val item: MediaItem, val progress: Progress) : PlaybackState()
    data class Stopped(val item: MediaItem?) : PlaybackState()
    data class Error(val message: String) : PlaybackState()
    data class Ended(val item: MediaItem) : PlaybackState()
}

interface PlaybackObserver {
    fun onStateChanged(state: PlaybackState)
    fun onProgressChanged(progress: Progress)
    fun onPlaylistChanged(playlist: Playlist)
}

/**
 * Core player + queue controller (KMP PlaylistManager surface).
 *
 * Platforms implement via [PlaylistEngine] + decode backend, or bridge to
 * legacy Android PlaylistManager.
 */
interface PlaybackService {
    val state: Flow<PlaybackState>
    val progress: Flow<Progress>
    val currentPlaylist: Flow<Playlist>

    fun play(item: MediaItem, playlist: List<MediaItem> = emptyList())
    fun playFromIndex(playlist: List<MediaItem>, index: Int)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(position: Long)
    fun seekRelative(delta: Long)
    fun next()
    fun previous()
    fun setShuffle(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun setVolume(volume: Int)
    fun getVolume(): Int
    fun setRate(rate: Float)
    fun getRate(): Float
    fun addObserver(observer: PlaybackObserver)
    fun removeObserver(observer: PlaybackObserver)

    // Queue mutations — ported from PlaylistManager
    fun append(items: List<MediaItem>) {}
    fun insertNext(items: List<MediaItem>) {}
    fun insertAt(index: Int, item: MediaItem) {}
    fun moveItem(from: Int, to: Int) {}
    fun removeAt(index: Int) {}
    fun removeByUri(uri: String) {}
    fun clearQueue() {}
    fun setStopAfterThis() {}
    fun clearStopAfter() {}

    // A/B repeat
    fun toggleABRepeat() {}
    fun setABRepeatValue(timeMs: Long) {}
    fun resetABRepeat() {}
    fun clearABRepeat() {}
}
