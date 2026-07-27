package org.videolan.vlc.compose.app

import org.videolan.vlc.model.MediaItem
import kotlin.test.Test
import kotlin.test.assertEquals

class ShellHostCallbacksTest {

    @Test
    fun localDeletionIsNeverOfferedForStreamsOrSyntheticRows() {
        assertEquals(
            true,
            MediaItem(id = 1, title = "Downloaded", uri = "file:///media/movie.mp4").isLocallyDeletable(),
        )
        assertEquals(
            true,
            MediaItem(id = 2, title = "SAF", uri = "content://media/video/2").isLocallyDeletable(),
        )
        assertEquals(
            false,
            MediaItem(id = 3, title = "Stream", uri = "https://example.test/live.m3u8").isLocallyDeletable(),
        )
        assertEquals(
            false,
            MediaItem(id = 4, title = "Artist", uri = "artist://4").isLocallyDeletable(),
        )
    }

    @Test
    fun mediaInformationPresentation_is_shared_and_keeps_all_available_metadata() {
        val presentation = MediaItem(
            id = 7L,
            title = "Live recording",
            uri = "file:///Documents/live.mkv",
            artist = "VLC",
            album = "Concerts",
        ).infoPresentation()

        assertEquals("Live recording", presentation.title)
        assertEquals("VLC\nConcerts\nfile:///Documents/live.mkv", presentation.details)
        assertEquals("Live recording\nVLC\nConcerts\nfile:///Documents/live.mkv", presentation.dialogMessage())
    }

    @Test
    fun empty_media_title_uses_the_uri_filename_not_its_parent_folder() {
        val item = MediaItem(
            id = 8L,
            title = "",
            uri = "https://media.example.org/concerts/finale.webm?token=1",
        )

        assertEquals("finale.webm", item.displayTitle)
    }
}
