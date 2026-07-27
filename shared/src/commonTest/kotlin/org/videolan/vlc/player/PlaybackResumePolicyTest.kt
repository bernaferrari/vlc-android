package org.videolan.vlc.player

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackResumePolicyTest {
    @Test
    fun audioVideoAndStreamsHonorTheirIndependentResumePreferences() {
        val audio = MediaItem(1, "Song", "file:///song.mp3", MediaType.AUDIO)
        val video = MediaItem(2, "Film", "file:///film.mp4", MediaType.VIDEO)
        val stream = MediaItem(3, "Live TV", "https://example.test/live", MediaType.STREAM)

        assertTrue(shouldPersistPlaybackSession(audio, audioResumeEnabled = true, videoResumeEnabled = false))
        assertFalse(shouldPersistPlaybackSession(video, audioResumeEnabled = true, videoResumeEnabled = false))
        assertFalse(shouldPersistPlaybackSession(stream, audioResumeEnabled = true, videoResumeEnabled = false))
    }
}
