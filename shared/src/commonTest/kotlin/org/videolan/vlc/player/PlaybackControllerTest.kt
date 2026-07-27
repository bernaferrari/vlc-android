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
import org.videolan.vlc.model.Progress
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.SessionActions
import org.videolan.vlc.repository.FakeCatalog
import org.videolan.vlc.repository.FakePlaybackService
import org.videolan.vlc.repository.HistoryRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun incognitoPlaybackNeverTouchesSharedHistory() = runTest {
        val history = RecordingHistoryRepository()
        val controller = PlaybackController(
            service = FakePlaybackService(),
            history = history,
            isIncognito = { true },
        )

        controller.play(FakeCatalog.items.first())
        controller.playFromIndex(FakeCatalog.items, 1)

        assertTrue(history.playedIds.isEmpty())
    }

    @Test
    fun clearsNowPlayingMetadataWhenPlaybackStops() = runTest {
        val service = FakePlaybackService()
        val session = RecordingMediaSessionBridge()
        val controller = PlaybackController(service = service, session = session)
        val item = FakeCatalog.items.first()

        service.setRate(1.25f)
        controller.play(item)
        controller.stop()

        assertEquals(item, session.metadata.last { it != null })
        assertEquals(null, session.metadata.last())
        assertTrue(session.playingStates.last().not())
        assertEquals(0f, session.playbackRates.last())
        assertTrue(session.playbackRates.contains(1.25f))
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

    private class RecordingMediaSessionBridge : MediaSessionBridge {
        val metadata = mutableListOf<MediaItem?>()
        val playingStates = mutableListOf<Boolean>()
        val playbackRates = mutableListOf<Float>()

        override fun activate() = Unit
        override fun deactivate() = Unit
        override fun updateMetadata(item: MediaItem?) { metadata += item }
        override fun updatePlayback(playing: Boolean, progress: Progress, rate: Float) {
            playingStates += playing
            playbackRates += rate
        }
        override fun setActions(actions: SessionActions) = Unit
    }
}
