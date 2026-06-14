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

/**
 * Placeholder PlaybackService for iOS.
 *
 * Will be replaced by a VLCKit-backed implementation when the iOS player
 * integration is built. For now, provides the contract surface so the
 * Koin graph is complete and the iOS framework compiles.
 */
class IosPlaybackService : PlaybackService {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: Flow<PlaybackState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(Progress())
    override val progress: Flow<Progress> = _progress.asStateFlow()

    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    override val currentPlaylist: Flow<Playlist> = _playlist.asStateFlow()

    override fun play(item: MediaItem, playlist: List<MediaItem>) {}
    override fun playFromIndex(playlist: List<MediaItem>, index: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun stop() { _state.value = PlaybackState.Stopped(null) }
    override fun seekTo(position: Long) {}
    override fun seekRelative(delta: Long) {}
    override fun next() {}
    override fun previous() {}
    override fun setShuffle(enabled: Boolean) {}
    override fun setRepeatMode(mode: RepeatMode) {}
    override fun setVolume(volume: Int) {}
    override fun getVolume(): Int = 100
    override fun setRate(rate: Float) {}
    override fun getRate(): Float = 1.0f
    override fun addObserver(observer: PlaybackObserver) {}
    override fun removeObserver(observer: PlaybackObserver) {}
}
