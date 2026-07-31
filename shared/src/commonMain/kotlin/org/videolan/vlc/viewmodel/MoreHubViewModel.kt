package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.MediaRepository
import org.videolan.vlc.repository.StreamRepository

/** Independent UI state for the More hub's history and stream sources. */
data class MoreUiState(
    val history: List<HistoryEntry> = emptyList(),
    val platformName: String = "",
    /** History's initial loading state, retained for the existing empty-state UI. */
    val loading: Boolean = true,
    val historyError: String? = null,
    val streams: List<MediaItem> = emptyList(),
    val streamsLoading: Boolean = true,
    val streamsError: String? = null,
    val streamActionError: String? = null,
    val historySelection: Set<String> = emptySet(),
    val hasStreamRepository: Boolean = false,
)

/**
 * Feature-scoped state owner for history and named streams.
 *
 * Each retry replaces its previous collector, so a failing source cannot leave
 * duplicate collectors running or turn a transient failure into a silent empty
 * section. Existing data remains visible while a retry is in flight.
 */
class MoreHubViewModel(
    private val history: HistoryRepository = historyRepo(),
    private val media: MediaRepository = mediaRepo(),
    private val streamsRepo: StreamRepository? = streamRepoOrNull(),
    private val player: PlaybackController = playback(),
) : VlcViewModel() {
    private val _state = MutableStateFlow(
        MoreUiState(hasStreamRepository = streamsRepo != null),
    )
    val state: StateFlow<MoreUiState> = _state.asStateFlow()

    private var historyJob: Job? = null
    private var streamsJob: Job? = null

    init {
        retryHistory()
        retryStreams()
        launch {
            val info = runCatching { org.videolan.vlc.platform.PlatformInfoProvider.current }.getOrNull()
            val name = info?.let { "${it.platform} ${it.osVersion}" }.orEmpty()
            _state.update { it.copy(platformName = name) }
        }
    }

    fun retryHistory() {
        historyJob?.cancel()
        _state.update { current ->
            current.copy(loading = current.history.isEmpty(), historyError = null)
        }
        historyJob = launch {
            history.observeHistory(50)
                .catch {
                    _state.update { current ->
                        current.copy(loading = false, historyError = HISTORY_LOAD_ERROR)
                    }
                }
                .collectLatest { entries ->
                    _state.update {
                        it.copy(history = entries, loading = false, historyError = null)
                    }
                }
        }
    }

    fun retryStreams() {
        streamsJob?.cancel()
        _state.update { current ->
            current.copy(streamsLoading = current.streams.isEmpty(), streamsError = null)
        }
        streamsJob = launch {
            val streamFlow = streamsRepo?.observeStreams() ?: media.observeMedia(MediaType.STREAM)
            streamFlow
                .catch {
                    _state.update { current ->
                        current.copy(streamsLoading = false, streamsError = STREAMS_LOAD_ERROR)
                    }
                }
                .collectLatest { streams ->
                    _state.update {
                        it.copy(streams = streams, streamsLoading = false, streamsError = null)
                    }
                }
        }
    }

    fun clearHistory() = launchIo {
        runCatching { history.clearHistory() }
        _state.update { it.copy(historySelection = emptySet()) }
    }

    fun playHistory(entry: HistoryEntry) = player.play(entry.item)
    fun playStream(item: MediaItem) = player.play(item)
    fun playStream(title: String, uri: String) {
        val cleanUri = uri.trim()
        if (!isPlayableStreamUri(cleanUri)) return
        player.play(
            MediaItem(
                id = cleanUri.hashCode().toLong(),
                title = title.trim().ifBlank { cleanUri },
                uri = cleanUri,
                type = MediaType.STREAM,
            )
        )
    }

    fun renameStream(id: Long, title: String) = launchIo {
        if (title.isBlank()) return@launchIo
        runCatching { streamsRepo?.renameStream(id, title.trim()) }
    }

    fun deleteStream(id: Long) = launchIo {
        runCatching { streamsRepo?.deleteStream(id) }
            .onSuccess { _state.update { state -> state.copy(streamActionError = null) } }
            .onFailure { error ->
                _state.update { state ->
                    state.copy(
                        streamActionError = error.message?.takeIf { it.isNotBlank() }
                            ?: "Couldn’t delete stream. Try again.",
                    )
                }
            }
    }

    fun addStream(title: String, uri: String) = launchIo {
        runCatching { streamsRepo?.addStream(title, uri) }
    }

    fun clearStreamActionError() = _state.update { it.copy(streamActionError = null) }

    fun moveUp(entry: HistoryEntry) = launchIo {
        runCatching { history.moveUp(entry.item.id) }
    }

    fun removeHistory(entry: HistoryEntry) = launchIo {
        runCatching { history.removeHistoryEntry(entry.item.id) }
    }

    fun toggleHistorySelect(entry: HistoryEntry) {
        val key = entry.historyKey()
        _state.update {
            val selected = it.historySelection.toMutableSet()
            if (!selected.add(key)) selected.remove(key)
            it.copy(historySelection = selected)
        }
    }

    fun clearHistorySelection() = _state.update { it.copy(historySelection = emptySet()) }

    fun removeSelectedHistory() = launchIo {
        val selected = _state.value.history.filter { it.historyKey() in _state.value.historySelection }
        selected.forEach { entry ->
            runCatching { history.removeHistoryEntry(entry.item.id) }
        }
        _state.update { it.copy(historySelection = emptySet()) }
    }

    private companion object {
        const val HISTORY_LOAD_ERROR = "Couldn’t load history. Try again."
        const val STREAMS_LOAD_ERROR = "Couldn’t load streams. Try again."
    }
}

private fun historyRepo(): HistoryRepository = runCatching { VlcKoin.get().get<HistoryRepository>() }
    .getOrElse { error("HistoryRepository unavailable") }

private fun mediaRepo(): MediaRepository = runCatching { VlcKoin.get().get<MediaRepository>() }
    .getOrElse { error("MediaRepository unavailable") }

private fun streamRepoOrNull(): StreamRepository? =
    runCatching { VlcKoin.get().get<StreamRepository>() }.getOrNull()

private fun playback(): PlaybackController = runCatching { PlaybackController.get() }
    .getOrElse { error("PlaybackController unavailable") }

private fun HistoryEntry.historyKey(): String = "${item.id}:${playedAt}:${item.uri}"

internal fun isPlayableStreamUri(value: String): Boolean {
    val uri = value.trim()
    val separator = uri.indexOf("://")
    if (separator <= 0 || separator >= uri.lastIndex - 2) return false
    val scheme = uri.substring(0, separator)
    return scheme.first().isLetter() && scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }
}
