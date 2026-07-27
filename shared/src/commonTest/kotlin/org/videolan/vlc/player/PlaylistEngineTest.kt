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

    @Test
    fun backendAttachedAfterSettingsRestorationReceivesCurrentAudioConfiguration() {
        val engine = PlaylistEngine()
        engine.setVolume(156)
        engine.setRate(1.25f)
        val backend = RecordingBackend()

        engine.setBackend(backend)

        assertEquals(156, backend.reportedVolume)
        assertEquals(1.25f, backend.reportedRate)
    }

    @Test
    fun playbackRateIsFiniteAndWithinDecoderRange() {
        val engine = PlaylistEngine()

        engine.setRate(Float.NaN)
        assertEquals(1f, engine.getRate())
        engine.setRate(20f)
        assertEquals(4f, engine.getRate())
        engine.setRate(0.01f)
        assertEquals(0.25f, engine.getRate())
    }

    private class RecordingBackend : PlayerBackend {
        var reportedVolume = -1
        var reportedRate = -1f

        override fun playUri(uri: String, title: String?) = Unit
        override fun pause() = Unit
        override fun resume() = Unit
        override fun stop() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun setVolume(volume: Int) { reportedVolume = volume }
        override fun getVolume(): Int = reportedVolume
        override fun setRate(rate: Float) { reportedRate = rate }
        override fun getRate(): Float = reportedRate
        override fun setListener(listener: PlayerBackend.Listener?) = Unit
        override fun release() = Unit
    }
}
