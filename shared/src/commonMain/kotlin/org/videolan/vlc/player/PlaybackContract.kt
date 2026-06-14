package org.videolan.vlc.player

import kotlinx.coroutines.flow.Flow
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

/**
 * Observer interface for playback events.
 * Platforms can wrap this to feed native callbacks into shared logic.
 */
interface PlaybackObserver {
    fun onStateChanged(state: PlaybackState)
    fun onProgressChanged(progress: Progress)
    fun onPlaylistChanged(playlist: Playlist)
}

/**
 * Playback service contract — the core player controller interface.
 *
 * Each platform implements this to bridge to its native player engine:
 *   - Android: wraps libVLC MediaPlayer + PlaylistManager
 *   - iOS: will wrap VLCKitMediaPlayer
 *
 * Shared UI code (Compose Multiplatform) and business logic interact only
 * with this interface, never with platform-specific player APIs.
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
}
