package org.videolan.vlc.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.NoOpMediaSessionBridge
import org.videolan.vlc.platform.NoOpPipController
import org.videolan.vlc.platform.NoOpRendererBridge
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.platform.RendererBridge
import org.videolan.vlc.platform.SessionActions
import org.videolan.vlc.repository.HistoryRepository

/**
 * Thin façade over platform [PlaybackService] plus session / PiP / renderer hooks.
 *
 * UI / ViewModels should use this instead of Android `PlaylistManager` /
 * `org.videolan.vlc.PlaybackService` god-objects.
 */
class PlaybackController(
    private val service: PlaybackService = runCatching {
        VlcKoin.get().get<PlaybackService>()
    }.getOrElse { error("PlaybackService unavailable — start Koin first") },
    private val session: MediaSessionBridge = runCatching {
        VlcKoin.get().get<MediaSessionBridge>()
    }.getOrDefault(NoOpMediaSessionBridge),
    private val pip: PipController = runCatching {
        VlcKoin.get().get<PipController>()
    }.getOrDefault(NoOpPipController),
    private val renderers: RendererBridge = runCatching {
        VlcKoin.get().get<RendererBridge>()
    }.getOrDefault(NoOpRendererBridge),
    private val history: HistoryRepository? = runCatching {
        VlcKoin.get().get<HistoryRepository>()
    }.getOrNull(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sessionBound = false

    val state: Flow<PlaybackState> get() = service.state
    val progress: Flow<Progress> get() = service.progress
    val playlist: Flow<Playlist> get() = service.currentPlaylist
    val pipController: PipController get() = pip
    val rendererBridge: RendererBridge get() = renderers

    init {
        bindSession()
    }

    private fun bindSession() {
        if (sessionBound) return
        sessionBound = true
        session.activate()
        session.setActions(SessionActions())
        scope.launch {
            combine(service.state, service.progress) { st, prog -> st to prog }
                .collectLatest { (st, prog) ->
                    val item = when (st) {
                        is PlaybackState.Playing -> st.item
                        is PlaybackState.Paused -> st.item
                        is PlaybackState.Stopped -> st.item
                        is PlaybackState.Ended -> st.item
                        else -> null
                    }
                    session.updateMetadata(item)
                    session.updatePlayback(st is PlaybackState.Playing, prog)
                }
        }
    }

    fun play(item: MediaItem, queue: List<MediaItem> = emptyList()) {
        service.play(item, queue)
        scope.launch {
            runCatching { history?.addToHistory(item) }
        }
    }

    fun playFromIndex(queue: List<MediaItem>, index: Int) {
        service.playFromIndex(queue, index)
        val item = queue.getOrNull(index) ?: return
        scope.launch { runCatching { history?.addToHistory(item) } }
    }

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

    fun enterPip(): Boolean = pip.enterPip()
    fun exitPip() = pip.exitPip()
    fun startRendererDiscovery() = renderers.startDiscovery()
    fun stopRendererDiscovery() = renderers.stopDiscovery()
    fun selectRenderer(id: String?) = renderers.selectRenderer(id)

    fun addObserver(observer: PlaybackObserver) = service.addObserver(observer)
    fun removeObserver(observer: PlaybackObserver) = service.removeObserver(observer)

    companion object {
        fun get(): PlaybackController = runCatching {
            VlcKoin.get().get<PlaybackController>()
        }.getOrElse { PlaybackController() }

        fun getOrNull(): PlaybackController? = runCatching { get() }.getOrNull()
    }
}

data class PlaybackSnapshot(
    val state: PlaybackState = PlaybackState.Idle,
    val progress: Progress = Progress(),
    val playlist: Playlist = Playlist(0, "Current"),
)
