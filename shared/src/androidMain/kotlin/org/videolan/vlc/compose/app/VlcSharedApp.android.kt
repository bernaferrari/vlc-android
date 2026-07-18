package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.videolan.vlc.repository.FakeMediaRepository
import org.videolan.vlc.repository.FakePlaybackService
import org.videolan.vlc.viewmodel.LibraryViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
fun VlcSharedAppLibraryPreview() {
    val media = FakeMediaRepository()
    val playback = FakePlaybackService()
    VlcSharedApp(
        libraryVm = LibraryViewModel(media, playback),
        playerVm = PlayerViewModel(playback),
        settingsVm = SettingsViewModel(prefs = null),
        title = "VLC",
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VlcSharedAppDarkPreview() {
    VlcSharedAppLibraryPreview()
}
