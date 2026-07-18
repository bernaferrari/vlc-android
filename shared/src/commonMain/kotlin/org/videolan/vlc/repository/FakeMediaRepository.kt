package org.videolan.vlc.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackObserver
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState

/** Deterministic sample library for previews and unit tests. */
object FakeCatalog {
    val items: List<MediaItem> = listOf(
        MediaItem(1, "Sunset Drive", "file:///demo/sunset.mp3", MediaType.AUDIO, 210_000, "Nova", "Night Roads"),
        MediaItem(2, "City Lights", "file:///demo/city.mp4", MediaType.VIDEO, 360_000, width = 1920, height = 1080),
        MediaItem(3, "Deep Focus", "file:///demo/focus.mp3", MediaType.AUDIO, 480_000, "Ambient Lab", "Work"),
        MediaItem(4, "Trailer", "file:///demo/trailer.mp4", MediaType.VIDEO, 120_000, width = 1280, height = 720),
        MediaItem(5, "Podcast #12", "file:///demo/pod.mp3", MediaType.AUDIO, 3_600_000, "VLC Talk"),
    )
}

class FakeMediaRepository(
    seed: List<MediaItem> = FakeCatalog.items,
) : MediaRepository {
    private val items = MutableStateFlow(seed)
    private val recent = MutableStateFlow(seed.take(3))

    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> =
        items.map { list -> if (type == MediaType.ALL) list else list.filter { it.type == type } }

    override suspend fun getMedia(id: Long): MediaItem? = items.value.firstOrNull { it.id == id }

    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> {
        val set = ids.toSet()
        return items.value.filter { it.id in set }
    }

    override fun search(query: String, type: MediaType): Flow<List<MediaItem>> =
        items.map { list ->
            list.filter {
                (type == MediaType.ALL || it.type == type) &&
                    it.title.contains(query, true)
            }
        }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> =
        recent.map { it.take(limit) }

    override suspend fun count(type: MediaType): Int =
        if (type == MediaType.ALL) items.value.size else items.value.count { it.type == type }

    override suspend fun markAsPlayed(id: Long) {
        val item = getMedia(id) ?: return
        recent.value = listOf(item) + recent.value.filterNot { it.id == id }
    }

    override suspend fun incrementPlayCount(id: Long) = markAsPlayed(id)
}

class FakePlaybackService : PlaybackService {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val _progress = MutableStateFlow(Progress())
    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    private val observers = mutableListOf<PlaybackObserver>()
    private var volume = 100
    private var rate = 1f

    override val state: Flow<PlaybackState> = _state
    override val progress: Flow<Progress> = _progress
    override val currentPlaylist: Flow<Playlist> = _playlist

    override fun play(item: MediaItem, playlist: List<MediaItem>) {
        val list = playlist.ifEmpty { listOf(item) }
        val idx = list.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
        playFromIndex(list, idx)
    }

    override fun playFromIndex(playlist: List<MediaItem>, index: Int) {
        if (playlist.isEmpty()) return
        val i = index.coerceIn(0, playlist.lastIndex)
        _playlist.value = _playlist.value.copy(items = playlist, currentIndex = i)
        val item = playlist[i]
        val p = Progress(0, item.duration)
        _progress.value = p
        _state.value = PlaybackState.Playing(item, p)
        observers.forEach {
            it.onPlaylistChanged(_playlist.value)
            it.onStateChanged(_state.value)
            it.onProgressChanged(p)
        }
    }

    override fun pause() {
        val item = _playlist.value.current ?: return
        _state.value = PlaybackState.Paused(item, _progress.value)
        observers.forEach { it.onStateChanged(_state.value) }
    }

    override fun resume() {
        val item = _playlist.value.current ?: return
        _state.value = PlaybackState.Playing(item, _progress.value)
        observers.forEach { it.onStateChanged(_state.value) }
    }

    override fun stop() {
        _state.value = PlaybackState.Stopped(_playlist.value.current)
        observers.forEach { it.onStateChanged(_state.value) }
    }

    override fun seekTo(position: Long) {
        _progress.value = _progress.value.copy(time = position.coerceAtLeast(0))
        observers.forEach { it.onProgressChanged(_progress.value) }
    }

    override fun seekRelative(delta: Long) = seekTo(_progress.value.time + delta)
    override fun next() {
        val pl = _playlist.value
        if (pl.currentIndex < pl.items.lastIndex) playFromIndex(pl.items, pl.currentIndex + 1)
    }
    override fun previous() {
        val pl = _playlist.value
        if (pl.currentIndex > 0) playFromIndex(pl.items, pl.currentIndex - 1)
    }
    override fun setShuffle(enabled: Boolean) {
        _playlist.value = _playlist.value.copy(shuffle = enabled)
    }
    override fun setRepeatMode(mode: RepeatMode) {
        _playlist.value = _playlist.value.copy(repeatMode = mode)
    }
    override fun setVolume(volume: Int) { this.volume = volume }
    override fun getVolume(): Int = volume
    override fun setRate(rate: Float) { this.rate = rate }
    override fun getRate(): Float = rate
    override fun addObserver(observer: PlaybackObserver) { observers.add(observer) }
    override fun removeObserver(observer: PlaybackObserver) { observers.remove(observer) }
}
