package org.videolan.vlc.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

/**
 * Playback-rate contract shared by common UI, persisted defaults, and platform adapters.
 * VLC for Android and VLC for iOS both expose rates through 8×.
 */
object PlaybackRate {
    const val MIN = 0.25f
    const val MAX = 8f

    fun normalize(rate: Float): Float = rate.takeIf(Float::isFinite)?.coerceIn(MIN, MAX) ?: 1f
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
    val abRepeat: Flow<ABRepeat> get() = flowOf(ABRepeat())
    val abRepeatEnabled: Flow<Boolean> get() = flowOf(false)
    val stopAfterCurrent: Flow<Boolean> get() = flowOf(false)
    val videoScaleMode: Flow<VideoScaleMode> get() = flowOf(VideoScaleMode.BEST_FIT)
    val tracks: Flow<PlaybackTracks> get() = flowOf(PlaybackTracks())
    val delays: Flow<PlaybackDelays> get() = flowOf(PlaybackDelays())
    val sleepTimer: Flow<SleepTimerState> get() = flowOf(SleepTimerState())
    val chapters: Flow<PlaybackChapters> get() = flowOf(PlaybackChapters())
    val equalizer: Flow<PlaybackEqualizer> get() = flowOf(PlaybackEqualizer())
    val videoCrop: Flow<PlaybackVideoCrop> get() = flowOf(PlaybackVideoCrop())
    val videoAdjust: Flow<PlaybackVideoAdjust> get() = flowOf(PlaybackVideoAdjust())
    val bookmarks: Flow<PlaybackBookmarks> get() = flowOf(PlaybackBookmarks())

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
    /**
     * Sets a decoder-safe rate that must be in effect when the next item is loaded.
     *
     * Most services can retain it through [setRate]. Android overrides this because its
     * PlaylistManager loads asynchronously and otherwise restores the previous media rate.
     */
    fun setRateForNextPlayback(rate: Float) = setRate(rate)
    fun setRate(rate: Float)
    fun getRate(): Float
    fun setVideoScaleMode(mode: VideoScaleMode) {}
    fun selectAudioTrack(id: String) {}
    fun selectSubtitleTrack(id: String) {}
    fun setAudioDelay(delayUs: Long) {}
    fun setSubtitleDelay(delayUs: Long) {}
    fun setSleepTimer(durationMillis: Long, waitForCurrentItem: Boolean = false) {}
    fun clearSleepTimer() {}
    fun selectChapter(index: Int) {}
    fun loadExternalSubtitle(uri: String): Boolean = false
    fun setEqualizerEnabled(enabled: Boolean) {}
    fun selectEqualizerPreset(id: String) {}
    fun setEqualizerPreamp(preampDb: Float) {}
    fun setEqualizerBand(index: Int, amplificationDb: Float) {}
    fun setVideoCrop(mode: VideoCropMode) {}
    fun setVideoAdjustEnabled(enabled: Boolean) {}
    fun setVideoAdjust(parameter: VideoAdjustParameter, value: Float) {}
    fun resetVideoAdjust() {}
    fun addBookmark() {}
    fun removeBookmark(id: String) {}
    fun renameBookmark(id: String, title: String) {}
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
