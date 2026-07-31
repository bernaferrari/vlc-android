package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import org.videolan.vlc.compose.player.FallbackPlayerSurface
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.MainTab
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel

/**
 * Production entry point for the shared shell.
 *
 * This mirrors QuietGuard's Compose/Koin boundary: feature view models are
 * resolved from the platform Koin graph here, while [VlcMainShell] stays a
 * dependency-explicit, previewable renderer.
 */
@Composable
fun VlcKoinMainShell(
    modifier: Modifier = Modifier,
    initialTab: MainTab = MainTab.VIDEO,
    tab: MainTab? = null,
    onTabChange: ((MainTab) -> Unit)? = null,
    showBottomBar: Boolean = true,
    title: String = "VLC",
    hostCallbacks: ShellHostCallbacks = ShellHostCallbacks.NoOp,
    playerSurface: PlayerSurface = FallbackPlayerSurface,
) {
    val videoVm: VideoListViewModel = koinInject()
    val audioVm: AudioListViewModel = koinInject()
    val browserVm: BrowserViewModel = koinInject()
    val playlistsVm: PlaylistsViewModel = koinInject()
    val moreVm: MoreHubViewModel = koinInject()
    val playerVm: PlayerViewModel = koinInject()
    val settingsVm: SettingsViewModel = koinInject()

    VlcMainShell(
        modifier = modifier,
        initialTab = initialTab,
        tab = tab,
        onTabChange = onTabChange,
        showBottomBar = showBottomBar,
        videoVm = videoVm,
        audioVm = audioVm,
        browserVm = browserVm,
        playlistsVm = playlistsVm,
        moreVm = moreVm,
        playerVm = playerVm,
        settingsVm = settingsVm,
        title = title,
        hostCallbacks = hostCallbacks,
        playerSurface = playerSurface,
    )
}
