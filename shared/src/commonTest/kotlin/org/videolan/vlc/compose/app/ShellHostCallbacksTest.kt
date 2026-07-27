package org.videolan.vlc.compose.app

import org.videolan.vlc.model.MediaItem
import kotlin.test.Test
import kotlin.test.assertEquals

class ShellHostCallbacksTest {
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
