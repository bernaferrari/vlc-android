@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.FakePlaybackService
import org.videolan.vlc.repository.HistoryRepository
import org.videolan.vlc.repository.StreamRepository
import org.videolan.vlc.repository.StubMediaRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoreHubViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun retryReplacesFailedHistoryAndStreamCollectors() = runTest {
        val history = ToggleHistoryRepository()
        val streams = ToggleStreamRepository()
        val viewModel = MoreHubViewModel(
            history = history,
            media = StubMediaRepository(),
            streamsRepo = streams,
            player = PlaybackController(service = FakePlaybackService()),
        )

        val failed = viewModel.state.value
        assertNotNull(failed.historyError)
        assertNotNull(failed.streamsError)
        assertFalse(failed.historyError.contains("history backend"))
        assertFalse(failed.streamsError.contains("stream backend"))

        history.fail = false
        streams.fail = false
        viewModel.retryHistory()
        viewModel.retryStreams()

        val recovered = viewModel.state.value
        assertEquals(listOf(history.entry), recovered.history)
        assertEquals(listOf(streams.item), recovered.streams)
        assertNull(recovered.historyError)
        assertNull(recovered.streamsError)
        assertFalse(recovered.loading)
        assertFalse(recovered.streamsLoading)

        viewModel.onCleared()
    }

    @Test
    fun networkStreamValidationAndPlaybackAreShared() = runTest {
        assertTrue(isPlayableStreamUri("https://media.example.test/movie.mp4"))
        assertTrue(isPlayableStreamUri("rtsp://media.example.test/live"))
        assertFalse(isPlayableStreamUri("media.example.test/live"))
        assertFalse(isPlayableStreamUri("://broken"))

        val playback = FakePlaybackService()
        val viewModel = MoreHubViewModel(
            history = ToggleHistoryRepository().apply { fail = false },
            media = StubMediaRepository(),
            streamsRepo = ToggleStreamRepository().apply { fail = false },
            player = PlaybackController(service = playback),
        )

        viewModel.playStream("", " https://media.example.test/movie.mp4 ")

        val playing = playback.currentPlaylist.first { it.items.isNotEmpty() }.current
        assertNotNull(playing)
        assertEquals("https://media.example.test/movie.mp4", playing.uri)
        assertEquals(playing.uri, playing.title)
        viewModel.onCleared()
    }

    private class ToggleHistoryRepository : HistoryRepository {
        var fail = true
        val entry = HistoryEntry(MediaItem(id = 1L, title = "Recent", uri = "file:///recent.mp3"))

        override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> = flow {
            if (fail) error("history backend unavailable")
            emit(listOf(entry))
        }

        override suspend fun addToHistory(item: MediaItem) = Unit
        override suspend fun clearHistory() = Unit
        override suspend fun removeHistoryEntry(id: Long) = Unit
    }

    private class ToggleStreamRepository : StreamRepository {
        var fail = true
        val item = MediaItem(id = 2L, title = "Radio", uri = "https://example.invalid/radio")

        override fun observeStreams(): Flow<List<MediaItem>> = flow {
            if (fail) error("stream backend unavailable")
            emit(listOf(item))
        }

        override suspend fun addStream(title: String, uri: String): MediaItem? = null
        override suspend fun renameStream(id: Long, title: String) = Unit
        override suspend fun deleteStream(id: Long) = Unit
    }
}
