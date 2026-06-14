package org.videolan.vlc.kmp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackObserver
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState

/**
 * Android implementation of [PlaybackService] that delegates to the existing
 * [PlaylistManager] singleton.
 *
 * This adapter bridges the shared KMP playback contract to the Android-specific
 * VLC playback engine (libVLC + PlaylistManager + PlayerController).
 */
class AndroidPlaybackService(
    private val playlistManager: PlaylistManager
) : PlaybackService {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: Flow<PlaybackState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(Progress())
    override val progress: Flow<Progress> = _progress.asStateFlow()

    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    override val currentPlaylist: Flow<Playlist> = _playlist.asStateFlow()

    private val observers = mutableListOf<PlaybackObserver>()

    override fun play(item: MediaItem, playlist: List<MediaItem>) {
        _state.value = PlaybackState.Loading
        // TODO: Convert MediaItem → MediaWrapper and load into PlaylistManager
    }

    override fun playFromIndex(playlist: List<MediaItem>, index: Int) {
        _state.value = PlaybackState.Loading
        // TODO: Convert and call playlistManager.playIndex(index)
    }

    override fun pause() {
        playlistManager.pause()
    }

    override fun resume() {
        playlistManager.play()
    }

    override fun stop() {
        playlistManager.stop()
        _state.value = PlaybackState.Stopped(null)
    }

    override fun seekTo(position: Long) {
        // PlaylistManager's player controller handles seeking
        // TODO: Wire to PlayerController.setTime(position)
    }

    override fun seekRelative(delta: Long) {
        // TODO: Wire to PlayerController.setTime(current + delta)
    }

    override fun next() {
        playlistManager.next()
    }

    override fun previous() {
        playlistManager.previous(true)
    }

    override fun setShuffle(enabled: Boolean) {
        playlistManager.shuffle()
    }

    override fun setRepeatMode(mode: RepeatMode) {
        when (mode) {
            RepeatMode.NONE -> playlistManager.setRepeatType(0)
            RepeatMode.ONE -> playlistManager.setRepeatType(1)
            RepeatMode.ALL -> playlistManager.setRepeatType(2)
        }
    }

    override fun setVolume(volume: Int) {
        // TODO: Wire to PlayerController.setVolume(volume)
    }

    override fun getVolume(): Int {
        // TODO: Wire to PlayerController.getVolume()
        return 100
    }

    override fun setRate(rate: Float) {
        // TODO: Wire to PlayerController.setRate(rate)
    }

    override fun getRate(): Float {
        // TODO: Wire to PlayerController.getRate()
        return 1.0f
    }

    override fun addObserver(observer: PlaybackObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: PlaybackObserver) {
        observers.remove(observer)
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
    }
}
