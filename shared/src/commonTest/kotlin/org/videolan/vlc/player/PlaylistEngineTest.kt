package org.videolan.vlc.player

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.RepeatMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaylistEngineTest {

    private fun item(id: Long, title: String = "t$id") =
        MediaItem(id, title, "file:///$id.mp3", MediaType.AUDIO, duration = 60_000)

    @Test
    fun loadAndNext() {
        val engine = PlaylistEngine()
        val items = listOf(item(1), item(2), item(3))
        engine.playFromIndex(items, 0)
        assertEquals(0, engine.snapshot().currentIndex)
        engine.next()
        assertEquals(1, engine.snapshot().currentIndex)
        engine.next()
        assertEquals(2, engine.snapshot().currentIndex)
    }

    @Test
    fun appendAndInsertNext() {
        val engine = PlaylistEngine()
        engine.playFromIndex(listOf(item(1)), 0)
        engine.append(listOf(item(2), item(3)))
        assertEquals(3, engine.snapshot().size)
        engine.insertNext(listOf(item(9)))
        assertEquals(item(9).uri, engine.snapshot().items[1].uri)
    }

    @Test
    fun removeAdjustsIndex() {
        val engine = PlaylistEngine()
        engine.playFromIndex(listOf(item(1), item(2), item(3)), 1)
        engine.removeAt(0)
        assertEquals(0, engine.snapshot().currentIndex)
        assertEquals(2, engine.snapshot().size)
    }

    @Test
    fun shuffleAndRepeatFlags() {
        val engine = PlaylistEngine()
        engine.playFromIndex(listOf(item(1), item(2)), 0)
        engine.setShuffle(true)
        engine.setRepeatMode(RepeatMode.ALL)
        assertTrue(engine.snapshot().shuffle)
        assertEquals(RepeatMode.ALL, engine.snapshot().repeatMode)
    }

    @Test
    fun indexHelperMove() {
        assertEquals(2, PlaylistIndexHelper.adjustCurrentOnMove(1, 1, 3))
        assertEquals(1, PlaylistIndexHelper.adjustCurrentOnAdd(0, 0, false))
    }
}
