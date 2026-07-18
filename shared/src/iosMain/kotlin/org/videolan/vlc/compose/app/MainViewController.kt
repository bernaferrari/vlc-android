package org.videolan.vlc.compose.app

import androidx.compose.ui.window.ComposeUIViewController
import org.videolan.vlc.app.IosKoinBootstrap
import org.videolan.vlc.app.IosMediaLibrary
import org.videolan.vlc.app.VlcSharedApi
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
        if (IosMediaLibrary.shared.snapshot().isEmpty()) {
            VlcSharedApi().seedDemoLibrary()
        }
    }
    runCatching {
        VlcKoin.get().get<MediaSessionBridge>().activate()
    }
    return ComposeUIViewController {
        VlcMainShell(title = "VLC")
    }
}
