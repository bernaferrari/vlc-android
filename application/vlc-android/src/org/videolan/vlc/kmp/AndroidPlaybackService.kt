package org.videolan.vlc.kmp

import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.vlc.media.PlaylistManager
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackObserver
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState
import org.videolan.vlc.PlaybackService as AndroidPlaybackHost

/**
 * Android implementation of [PlaybackService] that delegates to the existing
 * [PlaylistManager] / [org.videolan.vlc.media.PlayerController] stack.
 *
 * [PlaylistManager] only exists while the Android [AndroidPlaybackHost] is alive,
 * so it is resolved lazily via [playlistManagerProvider] (defaults to the live service).
 *
 * Prefer [org.videolan.vlc.player.PlaybackController] from UI/shared code.
 * This adapter is the only Android type allowed to touch PlaylistManager.
 *
 * State/progress: observes [PlaylistManager.playingState] / [PlaylistManager.currentPlayedMedia]
 * and the active player progress LiveData when a manager is available. Full MediaPlayer event
 * parity is partial — ended/error paths still rely on host service lifecycle.
 */
class AndroidPlaybackService(
    private val playlistManagerProvider: () -> PlaylistManager? = {
        AndroidPlaybackHost.instance?.playlistManager
    }
) : PlaybackService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: Flow<PlaybackState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(Progress())
    override val progress: Flow<Progress> = _progress.asStateFlow()

    private val _playlist = MutableStateFlow(Playlist(0, "Current"))
    override val currentPlaylist: Flow<Playlist> = _playlist.asStateFlow()

    private val observers = mutableListOf<PlaybackObserver>()

    private var boundManager: PlaylistManager? = null
    private val playingObserver = Observer<Boolean> { playing ->
        pushStateFromHost(playing == true)
    }
    private val mediaObserver = Observer<MediaWrapper?> { mw ->
        pushStateFromHost(PlaylistManager.playingState.value == true, mw)
    }
    private val progressObserver = Observer<org.videolan.vlc.media.Progress> { p ->
        updateProgress(p.time, p.length)
    }

    init {
        // Bridge host LiveData into KMP flows for as long as this adapter lives.
        PlaylistManager.playingState.observeForever(playingObserver)
        PlaylistManager.currentPlayedMedia.observeForever(mediaObserver)
        ensurePlayerProgressBound()
    }

    private fun manager(): PlaylistManager? {
        val current = playlistManagerProvider()
        if (current !== boundManager) {
            boundManager?.player?.progress?.removeObserver(progressObserver)
            boundManager = current
            current?.player?.progress?.observeForever(progressObserver)
        }
        return current
    }

    private fun ensurePlayerProgressBound() {
        manager()
    }

    override fun play(item: MediaItem, playlist: List<MediaItem>) {
        _state.value = PlaybackState.Loading
        val list = if (playlist.isEmpty()) listOf(item) else playlist
        val index = list.indexOfFirst { it.id == item.id && it.uri == item.uri }
            .takeIf { it >= 0 } ?: list.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
        playFromIndex(list, index)
    }

    override fun playFromIndex(playlist: List<MediaItem>, index: Int) {
        _state.value = PlaybackState.Loading
        val pm = manager()
        if (pm == null) {
            _state.value = PlaybackState.Error("PlaybackService not running")
            return
        }
        val wrappers = playlist.map { it.toMediaWrapper() }
        val safeIndex = index.coerceIn(0, (wrappers.size - 1).coerceAtLeast(0))
        _playlist.value = Playlist(
            id = 0,
            name = "Current",
            items = playlist,
            currentIndex = safeIndex,
            shuffle = pm.shuffling,
            repeatMode = repeatModeFromHost()
        )
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
        // PlaylistManager.load is @MainThread + suspend
        pm.launch {
            try {
                pm.load(wrappers, safeIndex, mlUpdate = true)
                pushStateFromHost(playing = true)
            } catch (t: Throwable) {
                _state.value = PlaybackState.Error(t.message ?: "Failed to load playlist")
                observers.forEach { it.onStateChanged(_state.value) }
            }
        }
    }

    override fun pause() {
        manager()?.pause()
        pushStateFromHost(playing = false)
    }

    override fun resume() {
        manager()?.play()
        pushStateFromHost(playing = true)
    }

    override fun stop() {
        manager()?.stop()
        val item = currentItem()
        val stopped = PlaybackState.Stopped(item)
        _state.value = stopped
        observers.forEach { it.onStateChanged(stopped) }
    }

    override fun seekTo(position: Long) {
        manager()?.player?.setTime(position.coerceAtLeast(0L))
    }

    override fun seekRelative(delta: Long) {
        val player = manager()?.player ?: return
        val target = (player.getCurrentTime() + delta).coerceAtLeast(0L)
        player.setTime(target)
    }

    override fun next() {
        manager()?.next()
    }

    override fun previous() {
        manager()?.previous(true)
    }

    override fun setShuffle(enabled: Boolean) {
        val pm = manager() ?: return
        if (pm.shuffling != enabled) {
            pm.shuffle()
        }
        _playlist.value = _playlist.value.copy(shuffle = enabled)
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        val pm = manager() ?: return
        when (mode) {
            RepeatMode.NONE -> pm.setRepeatType(PlaybackStateCompat.REPEAT_MODE_NONE)
            RepeatMode.ONE -> pm.setRepeatType(PlaybackStateCompat.REPEAT_MODE_ONE)
            RepeatMode.ALL -> pm.setRepeatType(PlaybackStateCompat.REPEAT_MODE_ALL)
        }
        _playlist.value = _playlist.value.copy(repeatMode = mode)
        observers.forEach { it.onPlaylistChanged(_playlist.value) }
    }

    override fun setVolume(volume: Int) {
        manager()?.player?.setVolume(volume.coerceIn(0, 200))
    }

    override fun getVolume(): Int {
        return manager()?.player?.getVolume() ?: 100
    }

    override fun setRate(rate: Float) {
        manager()?.player?.setRate(rate, true)
    }

    override fun getRate(): Float {
        return manager()?.player?.getRate() ?: 1.0f
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
        // Keep Playing/Paused progress in sync when we already have an item
        when (val current = _state.value) {
            is PlaybackState.Playing -> {
                val updated = current.copy(progress = p)
                _state.value = updated
                observers.forEach { it.onStateChanged(updated) }
            }
            is PlaybackState.Paused -> {
                val updated = current.copy(progress = p)
                _state.value = updated
                observers.forEach { it.onStateChanged(updated) }
            }
            else -> Unit
        }
    }

    private fun pushStateFromHost(playing: Boolean, media: MediaWrapper? = PlaylistManager.currentPlayedMedia.value) {
        ensurePlayerProgressBound()
        val item = media?.toMediaItem() ?: currentItem()
        val progress = _progress.value
        val newState = when {
            item == null && !PlaylistManager.hasMedia() -> PlaybackState.Idle
            item == null -> PlaybackState.Stopped(null)
            playing -> PlaybackState.Playing(item, progress)
            PlayerControllerState.isStopped() && !PlaylistManager.hasMedia() -> PlaybackState.Stopped(item)
            else -> PlaybackState.Paused(item, progress)
        }
        if (_state.value != newState) {
            _state.value = newState
            observers.forEach { it.onStateChanged(newState) }
        }
    }

    private fun currentItem(): MediaItem? =
        PlaylistManager.currentPlayedMedia.value?.toMediaItem()
            ?: _playlist.value.current

    private fun repeatModeFromHost(): RepeatMode = when (PlaylistManager.repeating.value) {
        PlaybackStateCompat.REPEAT_MODE_ONE -> RepeatMode.ONE
        PlaybackStateCompat.REPEAT_MODE_ALL,
        PlaybackStateCompat.REPEAT_MODE_GROUP -> RepeatMode.ALL
        else -> RepeatMode.NONE
    }

    private fun MediaItem.toMediaWrapper(): MediaWrapper {
        if (id != 0L) {
            Medialibrary.getInstance().getMedia(id)?.let { return it }
        }
        val parsedUri = Uri.parse(uri)
        val itemType = type
        return MLServiceLocator.getAbstractMediaWrapper(parsedUri).apply {
            if (title.isNotBlank()) this.title = title
            this.type = when (itemType) {
                MediaType.VIDEO -> MediaWrapper.TYPE_VIDEO
                MediaType.AUDIO -> MediaWrapper.TYPE_AUDIO
                MediaType.STREAM -> MediaWrapper.TYPE_STREAM
                MediaType.DIR -> MediaWrapper.TYPE_DIR
                MediaType.SUBTITLE -> MediaWrapper.TYPE_SUBTITLE
                MediaType.PLAYLIST -> MediaWrapper.TYPE_PLAYLIST
                MediaType.GROUP -> MediaWrapper.TYPE_GROUP
                MediaType.ALL -> MediaWrapper.TYPE_ALL
            }
            if (duration > 0L) length = duration
        }
    }

    private fun MediaWrapper.toMediaItem(): MediaItem {
        return MediaItem(
            id = this.id,
            title = this.title ?: "",
            uri = this.uri?.toString() ?: "",
            type = when (this.type) {
                MediaWrapper.TYPE_VIDEO -> MediaType.VIDEO
                MediaWrapper.TYPE_AUDIO -> MediaType.AUDIO
                MediaWrapper.TYPE_STREAM -> MediaType.STREAM
                MediaWrapper.TYPE_DIR -> MediaType.DIR
                MediaWrapper.TYPE_SUBTITLE -> MediaType.SUBTITLE
                MediaWrapper.TYPE_PLAYLIST -> MediaType.PLAYLIST
                MediaWrapper.TYPE_GROUP -> MediaType.GROUP
                else -> MediaType.ALL
            },
            duration = this.length,
            artist = this.artistName,
            album = this.albumName,
            albumArtist = this.albumArtistName,
            genre = this.genre,
            year = this.releaseYear,
            trackNumber = this.trackNumber,
            discNumber = this.discNumber,
            artworkUri = this.artworkURL,
            width = this.width,
            height = this.height,
            lastModified = this.lastModified,
            rating = this.rating?.toFloatOrNull() ?: 0f,
            playedCount = this.playCount.toInt(),
            lastPlayed = this.seen,
        )
    }
}

/**
 * Small helper so we don't import PlayerController companion just for stopped checks
 * without holding a player instance.
 */
private object PlayerControllerState {
    fun isStopped(): Boolean =
        org.videolan.vlc.media.PlayerController.playbackState == PlaybackStateCompat.STATE_STOPPED ||
            org.videolan.vlc.media.PlayerController.playbackState == PlaybackStateCompat.STATE_NONE
}
