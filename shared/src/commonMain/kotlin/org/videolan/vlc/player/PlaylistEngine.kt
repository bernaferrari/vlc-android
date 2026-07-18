package org.videolan.vlc.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.videolan.vlc.model.ABRepeat
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import kotlin.random.Random

/**
 * Decode-only backend. Platforms implement with libVLC / VLCKit.
 */
interface PlayerBackend {
    fun playUri(uri: String, title: String?)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Int)
    fun getVolume(): Int
    fun setRate(rate: Float)
    fun getRate(): Float
    fun setListener(listener: Listener?)
    fun release()

    interface Listener {
        fun onPlaying()
        fun onPaused()
        fun onStopped()
        fun onEnded()
        fun onError(message: String)
        fun onTimeChanged(timeMs: Long, lengthMs: Long)
    }
}

/**
 * Shared playlist manager — KMP port of Android PlaylistManager queue logic.
 *
 * Owns the queue, shuffle/repeat, previous-stack, A/B repeat, and stop-after.
 * Delegates actual decoding to [PlayerBackend].
 */
class PlaylistEngine(
    private var backend: PlayerBackend? = null,
) : PlaybackService {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(Progress())
    override val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    override val currentPlaylist: StateFlow<Playlist> = _playlist.asStateFlow()

    private val _abRepeat = MutableStateFlow(ABRepeat())
    val abRepeat: StateFlow<ABRepeat> = _abRepeat.asStateFlow()

    private val _abRepeatOn = MutableStateFlow(false)
    val abRepeatOn: StateFlow<Boolean> = _abRepeatOn.asStateFlow()

    private val observers = mutableListOf<PlaybackObserver>()
    private val previousStack = ArrayDeque<Int>()
    private var expanding = false
    private var stopAfterIndex: Int = -1
    private var volume: Int = 100
    private var rate: Float = 1f
    private val random = Random.Default

    fun setBackend(backend: PlayerBackend?) {
        this.backend?.release()
        this.backend = backend
        backend?.setListener(object : PlayerBackend.Listener {
            override fun onPlaying() = pushPlaying()
            override fun onPaused() = pushPaused()
            override fun onStopped() {
                updateState(PlaybackState.Stopped(currentItem()))
            }
            override fun onEnded() = handleEnded()
            override fun onError(message: String) = updateState(PlaybackState.Error(message))
            override fun onTimeChanged(timeMs: Long, lengthMs: Long) {
                updateProgress(timeMs, lengthMs)
                maybeApplyAbRepeat(timeMs)
            }
        })
    }

    // --- PlaybackService ---

    override fun play(item: MediaItem, playlist: List<MediaItem>) {
        val list = playlist.ifEmpty { listOf(item) }
        val index = list.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
        playFromIndex(list, index)
    }

    override fun playFromIndex(playlist: List<MediaItem>, index: Int) {
        if (playlist.isEmpty()) {
            updateState(PlaybackState.Error("Empty playlist"))
            return
        }
        previousStack.clear()
        stopAfterIndex = -1
        val safe = index.coerceIn(0, playlist.lastIndex)
        _playlist.value = _playlist.value.copy(items = playlist.toList(), currentIndex = safe)
        notifyPlaylist()
        startCurrent()
    }

    override fun pause() {
        backend?.pause()
        pushPaused()
    }

    override fun resume() {
        backend?.resume()
        pushPlaying()
    }

    override fun stop() {
        backend?.stop()
        stopAfterIndex = -1
        clearABRepeat()
        updateState(PlaybackState.Stopped(currentItem()))
    }

    override fun seekTo(position: Long) {
        val pos = position.coerceAtLeast(0L)
        backend?.seekTo(pos) ?: updateProgress(pos, _progress.value.length)
    }

    override fun seekRelative(delta: Long) {
        seekTo(_progress.value.time + delta)
    }

    override fun next() {
        val pl = _playlist.value
        if (pl.items.isEmpty()) return
        val nextIdx = nextIndex(pl) ?: run {
            updateState(PlaybackState.Ended(currentItem() ?: return))
            return
        }
        if (pl.currentIndex >= 0) previousStack.addLast(pl.currentIndex)
        setCurrentIndex(nextIdx)
        startCurrent()
    }

    override fun previous() {
        val pl = _playlist.value
        if (pl.items.isEmpty()) return
        // If >3s into track, restart current
        if (_progress.value.time > 3_000L) {
            seekTo(0)
            return
        }
        val prev = if (previousStack.isNotEmpty()) previousStack.removeLast()
        else PlaylistIndexHelper.determineSequentialPrevNext(
            pl.currentIndex, pl.size, pl.repeatMode == RepeatMode.ALL
        ).first.takeIf { it >= 0 }
        if (prev == null || prev < 0) {
            seekTo(0)
            return
        }
        setCurrentIndex(prev)
        startCurrent()
    }

    override fun setShuffle(enabled: Boolean) {
        _playlist.update { it.copy(shuffle = enabled) }
        if (!enabled) previousStack.clear()
        notifyPlaylist()
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _playlist.update { it.copy(repeatMode = mode) }
        notifyPlaylist()
    }

    override fun setVolume(volume: Int) {
        this.volume = volume.coerceIn(0, 200)
        backend?.setVolume(this.volume)
    }

    override fun getVolume(): Int = backend?.getVolume() ?: volume

    override fun setRate(rate: Float) {
        this.rate = rate
        backend?.setRate(rate)
    }

    override fun getRate(): Float = backend?.getRate() ?: rate

    override fun addObserver(observer: PlaybackObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: PlaybackObserver) {
        observers.remove(observer)
    }

    // --- Queue mutations (PlaylistManager parity) ---

    override fun append(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val pl = _playlist.value
        if (pl.isEmpty) {
            playFromIndex(items, 0)
            return
        }
        _playlist.update { it.copy(items = it.items + items) }
        notifyPlaylist()
    }

    override fun insertNext(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val pl = _playlist.value
        if (pl.isEmpty) {
            playFromIndex(items, 0)
            return
        }
        val insertAt = (pl.currentIndex + 1).coerceAtMost(pl.items.size)
        val mutable = pl.items.toMutableList()
        mutable.addAll(insertAt, items)
        _playlist.update { it.copy(items = mutable) }
        notifyPlaylist()
    }

    override fun insertAt(index: Int, item: MediaItem) {
        val pl = _playlist.value
        val mutable = pl.items.toMutableList()
        val at = index.coerceIn(0, mutable.size)
        mutable.add(at, item)
        val newCurrent = PlaylistIndexHelper.adjustCurrentOnAdd(pl.currentIndex, at, expanding)
        _playlist.update { it.copy(items = mutable, currentIndex = newCurrent) }
        notifyPlaylist()
    }

    override fun moveItem(from: Int, to: Int) {
        val pl = _playlist.value
        if (from !in pl.items.indices) return
        val mutable = pl.items.toMutableList()
        val item = mutable.removeAt(from)
        val dest = to.coerceIn(0, mutable.size)
        mutable.add(dest, item)
        val newCurrent = PlaylistIndexHelper.adjustCurrentOnMove(pl.currentIndex, from, dest)
        _playlist.update { it.copy(items = mutable, currentIndex = newCurrent) }
        notifyPlaylist()
    }

    override fun removeAt(index: Int) {
        val pl = _playlist.value
        if (index !in pl.items.indices) return
        val (newCurrent, wasCurrent) = PlaylistIndexHelper.adjustCurrentOnRemove(
            pl.currentIndex, index, expanding
        )
        val mutable = pl.items.toMutableList()
        mutable.removeAt(index)
        if (mutable.isEmpty()) {
            stop()
            _playlist.update { it.copy(items = emptyList(), currentIndex = 0) }
            notifyPlaylist()
            return
        }
        _playlist.update {
            it.copy(
                items = mutable,
                currentIndex = newCurrent.coerceIn(0, mutable.lastIndex),
            )
        }
        notifyPlaylist()
        if (wasCurrent) startCurrent()
    }

    override fun removeByUri(uri: String) {
        val idx = _playlist.value.items.indexOfFirst { it.uri == uri }
        if (idx >= 0) removeAt(idx)
    }

    override fun clearQueue() {
        stop()
        previousStack.clear()
        _playlist.value = Playlist(0, "Current")
        notifyPlaylist()
    }

    override fun setStopAfterThis() {
        stopAfterIndex = _playlist.value.currentIndex
    }

    override fun clearStopAfter() {
        stopAfterIndex = -1
    }

    // --- A/B repeat ---

    override fun toggleABRepeat() {
        _abRepeatOn.value = !_abRepeatOn.value
        if (!_abRepeatOn.value) _abRepeat.value = ABRepeat()
    }

    override fun setABRepeatValue(timeMs: Long) {
        val cur = _abRepeat.value
        _abRepeat.value = when {
            cur.start < 0 -> cur.copy(start = timeMs)
            cur.stop < 0 -> cur.copy(stop = timeMs)
            else -> ABRepeat(start = timeMs, stop = -1)
        }
        _abRepeatOn.value = true
    }

    override fun resetABRepeat() {
        _abRepeat.value = ABRepeat()
    }

    override fun clearABRepeat() {
        _abRepeat.value = ABRepeat()
        _abRepeatOn.value = false
    }

    fun snapshot(): Playlist = _playlist.value

    fun currentItem(): MediaItem? = _playlist.value.current

    // --- internals ---

    private fun startCurrent() {
        val item = currentItem() ?: run {
            updateState(PlaybackState.Idle)
            return
        }
        updateState(PlaybackState.Loading)
        clearABRepeat()
        val b = backend
        if (b == null) {
            updateProgress(0L, item.duration.coerceAtLeast(0L))
            updateState(PlaybackState.Playing(item, _progress.value))
            return
        }
        try {
            b.setVolume(volume)
            b.setRate(rate)
            b.playUri(item.uri, item.title)
        } catch (t: Throwable) {
            updateState(PlaybackState.Error(t.message ?: "Play failed"))
        }
    }

    private fun handleEnded() {
        val item = currentItem()
        if (stopAfterIndex >= 0 && _playlist.value.currentIndex == stopAfterIndex) {
            stopAfterIndex = -1
            if (item != null) updateState(PlaybackState.Ended(item))
            stop()
            return
        }
        when (_playlist.value.repeatMode) {
            RepeatMode.ONE -> startCurrent()
            RepeatMode.ALL, RepeatMode.NONE -> {
                if (item != null) updateState(PlaybackState.Ended(item))
                val next = nextIndex(_playlist.value)
                if (next == null) {
                    stop()
                } else {
                    if (_playlist.value.currentIndex >= 0) previousStack.addLast(_playlist.value.currentIndex)
                    setCurrentIndex(next)
                    startCurrent()
                }
            }
        }
    }

    private fun nextIndex(pl: Playlist): Int? {
        if (pl.items.isEmpty()) return null
        return when {
            pl.repeatMode == RepeatMode.ONE -> pl.currentIndex
            pl.shuffle -> {
                if (pl.size == 1) if (pl.repeatMode == RepeatMode.ALL) 0 else null
                else {
                    var idx: Int
                    do {
                        idx = random.nextInt(pl.size)
                    } while (idx == pl.currentIndex && pl.size > 1)
                    idx
                }
            }
            else -> {
                val (prev, next) = PlaylistIndexHelper.determineSequentialPrevNext(
                    pl.currentIndex, pl.size, pl.repeatMode == RepeatMode.ALL
                )
                next.takeIf { it >= 0 }
            }
        }
    }

    private fun setCurrentIndex(index: Int) {
        _playlist.update { it.copy(currentIndex = index.coerceIn(0, (it.items.size - 1).coerceAtLeast(0))) }
        notifyPlaylist()
    }

    private fun maybeApplyAbRepeat(timeMs: Long) {
        if (!_abRepeatOn.value) return
        val ab = _abRepeat.value
        if (ab.start >= 0 && ab.stop > ab.start && timeMs >= ab.stop) {
            seekTo(ab.start)
        }
    }

    private fun pushPlaying() {
        val item = currentItem() ?: return
        updateState(PlaybackState.Playing(item, _progress.value))
    }

    private fun pushPaused() {
        val item = currentItem() ?: return
        updateState(PlaybackState.Paused(item, _progress.value))
    }

    private fun updateProgress(time: Long, length: Long) {
        val p = Progress(time.coerceAtLeast(0L), length.coerceAtLeast(0L))
        _progress.value = p
        observers.forEach { it.onProgressChanged(p) }
        // keep progress inside Playing/Paused
        when (val s = _state.value) {
            is PlaybackState.Playing -> _state.value = s.copy(progress = p)
            is PlaybackState.Paused -> _state.value = s.copy(progress = p)
            else -> Unit
        }
    }

    private fun updateState(state: PlaybackState) {
        _state.value = state
        observers.forEach { it.onStateChanged(state) }
    }

    private fun notifyPlaylist() {
        val pl = _playlist.value
        observers.forEach { it.onPlaylistChanged(pl) }
    }
}
