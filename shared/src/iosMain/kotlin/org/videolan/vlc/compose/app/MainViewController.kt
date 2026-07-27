package org.videolan.vlc.compose.app

import androidx.compose.ui.window.ComposeUIViewController
import org.videolan.vlc.app.IosKoinBootstrap
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.app.VlcKoin
import platform.UIKit.UIViewController

/**
 * Compose Multiplatform root for iOS — full [VlcMainShell]
 * (Video / Audio / Browser / Playlists / More).
 */
fun MainViewController(): UIViewController {
    IosKoinBootstrap.start()
    runCatching {
        VlcKoin.get().get<MediaSessionBridge>().activate()
    }
    var hostViewController: UIViewController? = null
    val callbacks = IosShellHostCallbacks { hostViewController }
    return ComposeUIViewController {
        VlcKoinMainShell(
            title = "VLC",
            hostCallbacks = callbacks,
            playerSurface = IosPlayerSurface,
        )
    }.also { hostViewController = it }
}
