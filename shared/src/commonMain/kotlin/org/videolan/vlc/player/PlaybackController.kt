package org.videolan.vlc.player

import kotlinx.coroutines.flow.Flow
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode

/**
 * Thin façade over platform [PlaybackService].
 *
 * UI / ViewModels should prefer this over reaching into Android
 * `PlaylistManager` / `org.videolan.vlc.PlaybackService` god-objects.
 * Platform adapters remain the only place that talks to those engines.
 */
class PlaybackController(
    private val service: PlaybackService = runCatching {
        VlcKoin.get().get<PlaybackService>()
    }.getOrElse { error("PlaybackService unavailable — start Koin first") },
) {
    val state: Flow<PlaybackState> get() = service.state
    val progress: Flow<Progress> get() = service.progress
    val playlist: Flow<Playlist> get() = service.currentPlaylist

    fun play(item: MediaItem, queue: List<MediaItem> = emptyList()) =
        service.play(item, queue)

    fun playFromIndex(queue: List<MediaItem>, index: Int) =
        service.playFromIndex(queue, index)

    fun pause() = service.pause()
    fun resume() = service.resume()
    fun stop() = service.stop()
    fun next() = service.next()
    fun previous() = service.previous()
    fun seekTo(positionMs: Long) = service.seekTo(positionMs)
    fun seekRelative(deltaMs: Long) = service.seekRelative(deltaMs)
    fun setShuffle(enabled: Boolean) = service.setShuffle(enabled)
    fun setRepeatMode(mode: RepeatMode) = service.setRepeatMode(mode)
    fun setVolume(volume: Int) = service.setVolume(volume)
    fun getVolume(): Int = service.getVolume()
    fun setRate(rate: Float) = service.setRate(rate)
    fun getRate(): Float = service.getRate()

    fun addObserver(observer: PlaybackObserver) = service.addObserver(observer)
    fun removeObserver(observer: PlaybackObserver) = service.removeObserver(observer)

    companion object {
        /** Resolve from Koin or fail fast. */
        fun get(): PlaybackController = PlaybackController()

        fun getOrNull(): PlaybackController? = runCatching { get() }.getOrNull()
    }
}

/**
 * Snapshot helper for platforms that still need a synchronous peek
 * (e.g. Android notification builders). Prefer [Flow] collection in new code.
 */
data class PlaybackSnapshot(
    val state: PlaybackState = PlaybackState.Idle,
    val progress: Progress = Progress(),
    val playlist: Playlist = Playlist(0, "Current"),
)
