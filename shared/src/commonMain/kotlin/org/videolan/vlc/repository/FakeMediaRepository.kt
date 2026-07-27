package org.videolan.vlc.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.ABRepeat
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
        MediaItem(1, "Sunset Drive", "file:///demo/sunset.mp3", MediaType.AUDIO, 210_000, "Nova", "Night Roads", isFavorite = true, fileName = "sunset.mp3"),
        MediaItem(2, "City Lights", "file:///demo/city.mp4", MediaType.VIDEO, 360_000, width = 1920, height = 1080, seen = 1L, fileName = "city.mp4"),
        MediaItem(3, "Deep Focus", "file:///demo/focus.mp3", MediaType.AUDIO, 480_000, "Ambient Lab", "Work", fileName = "focus.mp3"),
        MediaItem(4, "Trailer", "file:///demo/trailer.mp4", MediaType.VIDEO, 120_000, width = 1280, height = 720, isFavorite = true, fileName = "trailer.mp4"),
        MediaItem(5, "Podcast #12", "file:///demo/pod.mp3", MediaType.AUDIO, 3_600_000, "VLC Talk", fileName = "pod.mp3"),
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
        val updated = item.copy(
            lastPlayed = item.lastPlayed.takeIf { it > 0 } ?: 1L,
            playedCount = item.playedCount + 1,
            seen = if (item.seen == 0L) 1L else item.seen,
        )
        items.value = items.value.map { if (it.id == id) updated else it }
        recent.value = listOf(updated) + recent.value.filterNot { it.id == id }
    }

    override suspend fun markAsUnplayed(id: Long) {
        items.value = items.value.map {
            if (it.id == id) it.copy(playedCount = 0, seen = 0L, lastPlayed = 0L) else it
        }
    }

    override suspend fun incrementPlayCount(id: Long) = markAsPlayed(id)

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isFavorite = favorite) else it
        }
        recent.value = recent.value.map {
            if (it.id == id) it.copy(isFavorite = favorite) else it
        }
    }
}

class FakePlaybackService : PlaybackService {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val _progress = MutableStateFlow(Progress())
    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    private val _abRepeat = MutableStateFlow(ABRepeat())
    private val _abRepeatEnabled = MutableStateFlow(false)
    private val _stopAfterCurrent = MutableStateFlow(false)
    private val observers = mutableListOf<PlaybackObserver>()
    private var volume = 100
    private var rate = 1f

    override val state: Flow<PlaybackState> = _state
    override val progress: Flow<Progress> = _progress
    override val currentPlaylist: Flow<Playlist> = _playlist
    override val abRepeat: Flow<ABRepeat> = _abRepeat
    override val abRepeatEnabled: Flow<Boolean> = _abRepeatEnabled
    override val stopAfterCurrent: Flow<Boolean> = _stopAfterCurrent

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

    override fun moveItem(from: Int, to: Int) {
        val playlist = _playlist.value
        if (from !in playlist.items.indices || to !in playlist.items.indices || from == to) return
        val items = playlist.items.toMutableList()
        val moved = items.removeAt(from)
        items.add(to, moved)
        val currentIndex = when {
            playlist.currentIndex == from -> to
            from < playlist.currentIndex && to >= playlist.currentIndex -> playlist.currentIndex - 1
            from > playlist.currentIndex && to <= playlist.currentIndex -> playlist.currentIndex + 1
            else -> playlist.currentIndex
        }
        _playlist.value = playlist.copy(items = items, currentIndex = currentIndex)
    }

    override fun removeAt(index: Int) {
        val playlist = _playlist.value
        if (index !in playlist.items.indices) return
        val items = playlist.items.toMutableList().also { it.removeAt(index) }
        if (items.isEmpty()) {
            stop()
            _playlist.value = playlist.copy(items = emptyList(), currentIndex = 0)
            return
        }
        val currentIndex = when {
            index < playlist.currentIndex -> playlist.currentIndex - 1
            playlist.currentIndex > items.lastIndex -> items.lastIndex
            else -> playlist.currentIndex
        }
        _playlist.value = playlist.copy(items = items, currentIndex = currentIndex)
        if (index == playlist.currentIndex) {
            val item = items[currentIndex]
            _state.value = PlaybackState.Playing(item, Progress(length = item.duration))
        }
    }

    override fun toggleABRepeat() {
        _abRepeatEnabled.value = !_abRepeatEnabled.value
        if (!_abRepeatEnabled.value) _abRepeat.value = ABRepeat()
    }

    override fun setABRepeatValue(timeMs: Long) {
        _abRepeat.value = when {
            _abRepeat.value.start < 0L -> ABRepeat(start = timeMs)
            timeMs < _abRepeat.value.start -> ABRepeat(start = timeMs, stop = _abRepeat.value.start)
            else -> _abRepeat.value.copy(stop = timeMs)
        }
        _abRepeatEnabled.value = true
    }

    override fun resetABRepeat() {
        _abRepeat.value = ABRepeat()
    }

    override fun clearABRepeat() {
        _abRepeat.value = ABRepeat()
        _abRepeatEnabled.value = false
    }

    override fun setStopAfterThis() {
        _stopAfterCurrent.value = _playlist.value.current != null
    }

    override fun clearStopAfter() {
        _stopAfterCurrent.value = false
    }
}
