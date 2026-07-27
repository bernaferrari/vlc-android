package org.videolan.vlc.compose.app

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.viewmodel.MediaListUiState
import kotlin.test.Test
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
}
