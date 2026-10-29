package org.videolan.vlc.app

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.player.PlaybackState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserPlaybackServiceTest {
    private fun item(id: Long) = MediaItem(id, "Track $id", "blob:track-$id", MediaType.AUDIO)

    @Test
    fun queueActionsMutateRealQueueAndKeepCurrentItem() = runTest {
        val player = BrowserPlaybackService()
        player.append(listOf(item(1)))
        player.append(listOf(item(3)))
        player.insertNext(listOf(item(2)))
        player.insertAt(0, item(4))
        val queue = player.currentPlaylist.first()
        assertEquals(listOf(4L, 1L, 2L, 3L), queue.items.map { it.id })
        assertEquals(1L, queue.current?.id)
        player.removeByUri(item(4).uri)
        assertEquals(listOf(1L, 2L, 3L), player.currentPlaylist.first().items.map { it.id })
        player.clearQueue()
        assertTrue(player.currentPlaylist.first().isEmpty)
        assertTrue(player.state.first() is PlaybackState.Stopped)
    }

    @Test
    fun nextAtQueueEndStopsPublishedPlayback() = runTest {
        val player = BrowserPlaybackService()
        player.playFromIndex(listOf(item(1)), 0)
        player.resume()
        player.next()
        assertTrue(player.state.first() is PlaybackState.Stopped)
    }

    @Test
    fun shuffleVisitsEachEntryThenStopsWithRepeatOff() = runTest {
        val player = BrowserPlaybackService()
        player.playFromIndex((1L..8L).map(::item), 0)
        player.setShuffle(true)
        val visited = mutableSetOf(player.currentPlaylist.first().currentIndex)
        repeat(7) {
            player.next()
            assertTrue(visited.add(player.currentPlaylist.first().currentIndex))
        }
        player.next()
        assertTrue(player.state.first() is PlaybackState.Stopped)
        player.setRepeatMode(RepeatMode.ALL)
        player.next()
        assertTrue(player.state.first() is PlaybackState.Loading)
    }

    @Test
    fun changingTracksClearsAbMarkers() = runTest {
        val player = BrowserPlaybackService()
        player.playFromIndex(listOf(item(1), item(2)), 0)
        player.setABRepeatValue(1_000)
        player.setABRepeatValue(2_000)
        player.next()
        assertFalse(player.abRepeatEnabled.first())
        assertFalse(player.abRepeat.first().isActive)
        player.setABRepeatValue(3_000)
        player.setABRepeatValue(4_000)
        player.playFromIndex(listOf(item(3)), 0)
        assertFalse(player.abRepeatEnabled.first())
    }
}
