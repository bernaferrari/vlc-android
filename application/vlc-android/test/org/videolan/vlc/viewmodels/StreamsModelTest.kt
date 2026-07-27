package org.videolan.vlc.viewmodels

import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.stubs.StubDataSource
import org.videolan.vlc.BaseTest

/**
 * Android adapter contract for [StreamsModel]. Shared queue and library behavior
 * lives in commonTest; this only verifies the native medialibrary shape it reads.
 */
class StreamsModelTest : BaseTest() {
    override fun beforeTest() {
        super.beforeTest()
        StubDataSource.getInstance().resetData()
        medialibrary.clearHistory(Medialibrary.HISTORY_TYPE_NETWORK)
    }

    @Test
    fun networkHistoryExposesStreamsWithoutMixingLocalPlaybackHistory() {
        val location = "https://example.test/live.m3u8"

        assertTrue(medialibrary.addToHistory(location, "Live radio"))

        val streams = medialibrary.history(Medialibrary.HISTORY_TYPE_NETWORK)
        assertEquals(1, streams.size)
        assertEquals(MediaWrapper.TYPE_STREAM, streams.single().type)
        assertEquals(location.toUri(), streams.single().uri)
        assertTrue(medialibrary.history(Medialibrary.HISTORY_TYPE_GLOBAL).isEmpty())
    }
}
