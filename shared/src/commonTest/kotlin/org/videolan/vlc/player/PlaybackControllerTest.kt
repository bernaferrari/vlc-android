@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.videolan.vlc.model.HistoryEntry
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.repository.FakeCatalog
import org.videolan.vlc.repository.FakePlaybackService
import org.videolan.vlc.repository.HistoryRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackControllerTest {

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
    fun recordsHistoryOnlyForPlaybackNotQueueChanges() = runTest {
        val history = RecordingHistoryRepository()
        val controller = PlaybackController(
            service = FakePlaybackService(),
            history = history,
        )
        val first = FakeCatalog.items.first()
        val second = FakeCatalog.items[1]

        controller.play(first)
        controller.append(listOf(second))
        controller.insertNext(listOf(second))
        controller.playFromIndex(listOf(first, second), 1)

        assertEquals(listOf(first.id, second.id), history.playedIds)
    }

    private class RecordingHistoryRepository : HistoryRepository {
        val playedIds = mutableListOf<Long>()

        override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> = flowOf(emptyList())

        override suspend fun addToHistory(item: MediaItem) {
            playedIds += item.id
        }

        override suspend fun clearHistory() = Unit

        override suspend fun removeHistoryEntry(id: Long) = Unit
    }
}
