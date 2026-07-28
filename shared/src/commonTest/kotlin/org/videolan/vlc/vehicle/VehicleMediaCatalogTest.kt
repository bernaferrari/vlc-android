@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.vehicle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.first
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.player.PlaybackState
import org.videolan.vlc.repository.FakeMediaRepository
import org.videolan.vlc.repository.FakePlaybackService

class VehicleMediaCatalogTest {
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
    fun exposesOnlyBoundedAudioButPlaysAgainstTheFullAudioQueue() = runTest {
        val audio = (1L..3L).map { id ->
            MediaItem(id, "Track $id", "file:///track$id.mp3", MediaType.AUDIO)
        }
        val video = MediaItem(9L, "Video", "file:///video.mp4", MediaType.VIDEO)
        val service = FakePlaybackService()
        val catalog = VehicleMediaCatalog(
            media = FakeMediaRepository(seed = audio + video),
            player = PlaybackController(
                service = service,
                isIncognito = { true },
                isHistoryEnabled = { false },
                defaultRateFor = { 1f },
            ),
            visibleLimit = 2,
            scope = backgroundScope,
        )

        runCurrent()

        assertEquals(listOf(1L, 2L), catalog.snapshot().map { it.id })
        assertTrue(catalog.play(3L))
        val playlist = service.currentPlaylist.first()
        assertEquals(listOf(1L, 2L, 3L), playlist.items.map { it.id })
        assertEquals(2, playlist.currentIndex)
        assertEquals(3L, (service.state.first() as PlaybackState.Playing).item.id)
        assertFalse(catalog.play(9L))
    }
}
