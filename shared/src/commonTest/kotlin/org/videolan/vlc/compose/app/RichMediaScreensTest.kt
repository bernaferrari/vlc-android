package org.videolan.vlc.compose.app

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.compose.components.VLCListItemPosition
import org.videolan.vlc.compose.components.vlcIndexLabel
import org.videolan.vlc.viewmodel.MediaListUiState
import org.videolan.vlc.viewmodel.SortMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RichMediaScreensTest {

    @Test
    fun initial_empty_library_uses_the_calm_shared_loading_presentation() {
        assertTrue(
            shouldUseEmptyMediaPresentation(
                state = MediaListUiState(loading = true),
                sections = emptyList(),
                groups = emptyList(),
                pagingItemCount = 0,
            ),
        )
    }

    @Test
    fun authoritative_empty_library_ignores_stale_paging_placeholders() {
        // Paging can still expose a stale placeholder count after MediaLibrary has reported an
        // empty library. The shared state is the source of truth for whether these controls have
        // anything to operate on.
        assertTrue(
            shouldUseEmptyMediaPresentation(
                state = MediaListUiState(loading = false, count = 0),
                sections = emptyList(),
                groups = emptyList(),
                pagingItemCount = 12,
            ),
        )
    }

    @Test
    fun content_and_active_list_states_keep_their_controls() {
        val media = MediaItem(id = 1L, title = "Clip", uri = "file:///clip.mp4")

        assertFalse(
            shouldUseEmptyMediaPresentation(
                state = MediaListUiState(loading = false, items = listOf(media), count = 1),
                sections = emptyList(),
                groups = emptyList(),
                pagingItemCount = 1,
            ),
        )
        assertFalse(
            shouldUseEmptyMediaPresentation(
                state = MediaListUiState(loading = false, query = "clip"),
                sections = emptyList(),
                groups = emptyList(),
                pagingItemCount = 0,
            ),
        )
        assertFalse(
            shouldUseEmptyMediaPresentation(
                state = MediaListUiState(loading = false, error = "Library unavailable"),
                sections = emptyList(),
                groups = emptyList(),
                pagingItemCount = 0,
            ),
        )
    }

    @Test
    fun query_refresh_keeps_existing_content_visible_while_loading() {
        val media = MediaItem(id = 1L, title = "Clip", uri = "file:///clip.mp4")

        assertTrue(
            hasVisibleMediaContent(
                state = MediaListUiState(loading = true, count = 1, items = listOf(media)),
                sections = emptyList(),
                groups = emptyList(),
                pagingItemCount = 0,
            ),
        )
        assertFalse(
            hasVisibleMediaContent(
                state = MediaListUiState(loading = true),
                sections = emptyList(),
                groups = emptyList(),
                pagingItemCount = 0,
            ),
        )
    }

    @Test
    fun audio_section_selector_is_only_shown_for_a_filterable_audio_root() {
        val media = MediaItem(id = 1L, title = "Track", uri = "file:///track.mp3")

        assertTrue(shouldShowAudioSectionSelector(MediaListUiState(items = listOf(media), count = 1)))
        assertTrue(shouldShowAudioSectionSelector(MediaListUiState(sections = listOf("T" to listOf(media)))))
        assertFalse(shouldShowAudioSectionSelector(MediaListUiState()))
        assertFalse(
            shouldShowAudioSectionSelector(
                MediaListUiState(items = listOf(media), count = 1, openedEntityTitle = "Album"),
            ),
        )
    }

    @Test
    fun alphabetic_fast_scroll_targets_account_for_visible_section_headers() {
        val alpha = MediaItem(id = 1L, title = "Alpha", uri = "file:///alpha.mp3")
        val beta = MediaItem(id = 2L, title = "2 Fast", uri = "file:///two.mp3")

        val targets = mediaIndexScrollTargets(
            listOf("A" to listOf(alpha), "#" to listOf(beta)),
        )

        assertTrue(targets[0].itemIndex == 1)
        assertTrue(targets[1].itemIndex == 3)
        assertTrue(vlcIndexLabel(targets[0].labelSource) == "A")
        assertTrue(vlcIndexLabel(targets[1].labelSource) == "#")
    }

    @Test
    fun fast_scroll_uses_the_active_sort_field_and_disables_numeric_sorts() {
        val item = MediaItem(
            id = 1L,
            title = "Title",
            uri = "file:///z-file.mp3",
            artist = "Artist",
            album = "Collection",
            fileName = "z-file.mp3",
        )

        assertEquals("Title", mediaFastScrollLabelSource(item, SortMode.TITLE))
        assertEquals("z-file.mp3", mediaFastScrollLabelSource(item, SortMode.FILENAME))
        assertEquals("Artist", mediaFastScrollLabelSource(item, SortMode.ARTIST))
        assertEquals("Collection", mediaFastScrollLabelSource(item, SortMode.ALBUM))
        assertEquals(null, mediaFastScrollLabelSource(item, SortMode.DURATION))
        assertTrue(mediaIndexScrollTargets(listOf("" to listOf(item)), SortMode.RECENT).isEmpty())
    }

    @Test
    fun segmented_library_rows_keep_soft_outer_corners_and_compact_joins() {
        assertEquals(VLCListItemPosition.Single, sectionListItemPosition(index = 0, size = 1))
        assertEquals(VLCListItemPosition.First, sectionListItemPosition(index = 0, size = 3))
        assertEquals(VLCListItemPosition.Middle, sectionListItemPosition(index = 1, size = 3))
        assertEquals(VLCListItemPosition.Last, sectionListItemPosition(index = 2, size = 3))
    }

    @Test
    fun paging_uses_alphabetic_neighbours_without_waiting_for_the_entire_library() {
        assertEquals(
            VLCListItemPosition.First,
            pagedListItemPosition(current = "Albatross", previous = "9 Lives", next = "Album"),
        )
        assertEquals(
            VLCListItemPosition.Last,
            pagedListItemPosition(current = "Album", previous = "Albatross", next = "Beta"),
        )
        assertEquals(
            VLCListItemPosition.Single,
            pagedListItemPosition(current = "Beta", previous = "Album", next = "1 More"),
        )
    }

    @Test
    fun paging_does_not_round_page_tails_before_append_reaches_end() {
        assertEquals(
            VLCListItemPosition.Middle,
            pagedListItemPosition(
                current = "Audio 24",
                previous = "Audio 23",
                next = null,
                isLastItem = false,
            ),
        )
        assertEquals(
            VLCListItemPosition.Last,
            pagedListItemPosition(
                current = "Audio 24",
                previous = "Audio 23",
                next = null,
                isLastItem = true,
            ),
        )
    }
}
