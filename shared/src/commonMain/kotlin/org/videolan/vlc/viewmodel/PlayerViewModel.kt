package org.videolan.vlc.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.Progress
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackService
import org.videolan.vlc.player.PlaybackState

data class PlayerUiState(
    val title: String = "",
    val subtitle: String = "",
    val artworkUri: String? = null,
    val playing: Boolean = false,
    val progress: Progress = Progress(),
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val hasMedia: Boolean = false,
    val error: String? = null,
)

class PlayerViewModel(
    private val playback: PlaybackService = runCatching {
        VlcKoin.get().get<PlaybackService>()
    }.getOrElse { error("PlaybackService unavailable") },
) : VlcViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        launch {
            combine(playback.state, playback.progress, playback.currentPlaylist) { st, prog, pl ->
                Triple(st, prog, pl)
            }.collect { (st, prog, pl) ->
                val item = when (st) {
                    is PlaybackState.Playing -> st.item
                    is PlaybackState.Paused -> st.item
                    is PlaybackState.Stopped -> st.item
                    is PlaybackState.Ended -> st.item
                    else -> pl.current
                }
                _state.update {
                    it.copy(
                        title = item?.displayTitle.orEmpty(),
                        subtitle = listOfNotNull(item?.artist, item?.album).joinToString(" · "),
                        artworkUri = item?.artworkUri,
                        playing = st is PlaybackState.Playing,
                        progress = when (st) {
                            is PlaybackState.Playing -> st.progress
                            is PlaybackState.Paused -> st.progress
                            else -> prog
                        },
                        shuffle = pl.shuffle,
                        repeatMode = pl.repeatMode,
                        hasMedia = item != null || pl.items.isNotEmpty(),
                        error = (st as? PlaybackState.Error)?.message,
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        if (_state.value.playing) playback.pause() else playback.resume()
    }

    fun play(item: MediaItem, playlist: List<MediaItem> = emptyList()) {
        playback.play(item, playlist)
    }

    fun next() = playback.next()
    fun previous() = playback.previous()
    fun stop() = playback.stop()
    fun seekTo(position: Long) = playback.seekTo(position)
    fun seekRelative(delta: Long) = playback.seekRelative(delta)

    fun cycleRepeat() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        playback.setRepeatMode(next)
    }

    fun toggleShuffle() {
        playback.setShuffle(!_state.value.shuffle)
    }
}
