package org.videolan.vlc.compose.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.videolan.vlc.model.Progress
import org.videolan.vlc.viewmodel.MediaListUiState
import org.videolan.vlc.viewmodel.PlayerUiState
import org.videolan.vlc.viewmodel.VideoGroupingMode

class ContinueWatchingTest {
    private val library = MediaListUiState(loading = false, count = 1)
    private val video = PlayerUiState(
        uri = "file:///video.mp4",
        title = "Video",
        hasMedia = true,
        hasVideoOutput = true,
        progress = Progress(time = 30_000, length = 120_000),
    )

    @Test
    fun unfinished_video_can_reopen_whether_paused_or_playing() {
        assertTrue(shouldShowContinueWatching(library, video.copy(playing = false)))
        assertTrue(shouldShowContinueWatching(library, video.copy(playing = true)))
    }

    @Test
    fun does_not_claim_unknown_unstarted_or_completed_media_is_resumable() {
        val ineligible = listOf(
            video.copy(hasMedia = false),
            video.copy(hasVideoOutput = false),
            video.copy(uri = ""),
            video.copy(progress = Progress(time = 0, length = 120_000)),
            video.copy(progress = Progress(time = -1, length = 120_000)),
            video.copy(progress = Progress(time = 30_000, length = 0)),
            video.copy(progress = Progress(time = 120_000, length = 120_000)),
            video.copy(progress = Progress(time = 130_000, length = 120_000)),
        )
        ineligible.forEach { player ->
            assertFalse(shouldShowContinueWatching(library, player), player.toString())
        }
    }

    @Test
    fun active_video_does_not_interrupt_search_selection_or_drilled_in_browsing() {
        val focusedLibraryStates = listOf(
            library.copy(query = "another video"),
            library.copy(onlyFavorites = true),
            library.copy(selection = setOf("file:///another.mp4")),
            library.copy(containerId = 12),
            library.copy(containerTitle = "Downloads"),
            library.copy(openedEntityTitle = "Album"),
            library.copy(groupingMode = VideoGroupingMode.FOLDER),
        )
        focusedLibraryStates.forEach { state ->
            assertFalse(shouldShowContinueWatching(state, video), state.toString())
        }
    }
}
