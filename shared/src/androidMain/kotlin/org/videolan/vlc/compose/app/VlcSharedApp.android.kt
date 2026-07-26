package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.videolan.vlc.player.PlaybackController
import org.videolan.vlc.repository.FakeMediaRepository
import org.videolan.vlc.repository.FakePlaybackService
import org.videolan.vlc.repository.StubHistoryRepository
import org.videolan.vlc.repository.StubPlaylistRepository
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
fun VlcMainShellPreview() {
    PreviewShell()
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun VlcMainShellDarkPreview() {
    PreviewShell()
}

/**
 * Android Studio previews do not initialize the platform Koin graph.  Supplying
 * the same deterministic fakes used by common tests keeps these previews
 * interactive and avoids masking composition failures behind missing DI.
 */
@Composable
private fun PreviewShell() {
    val media = remember { FakeMediaRepository() }
    val playbackService = remember { FakePlaybackService() }
    val playback = remember { PlaybackController(service = playbackService) }
    val playlists = remember { StubPlaylistRepository() }
    val history = remember { StubHistoryRepository() }
    VlcMainShell(
        title = "VLC",
        videoVm = remember { VideoListViewModel(repo = media, player = playback) },
        audioVm = remember { AudioListViewModel(repo = media, playlists = playlists, player = playback) },
        browserVm = remember { BrowserViewModel(repo = media, player = playback) },
        playlistsVm = remember { PlaylistsViewModel(repo = playlists, player = playback) },
        moreVm = remember { MoreHubViewModel(history = history, media = media, player = playback) },
        playerVm = remember { PlayerViewModel(playback = playbackService) },
        settingsVm = remember { SettingsViewModel() },
    )
}

@Deprecated("Use VlcMainShellPreview", ReplaceWith("VlcMainShellPreview()"))
@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
fun VlcSharedAppLibraryPreview() {
    VlcMainShellPreview()
}

@Deprecated("Use VlcMainShellDarkPreview", ReplaceWith("VlcMainShellDarkPreview()"))
@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun VlcSharedAppDarkPreview() {
    VlcMainShellDarkPreview()
}
