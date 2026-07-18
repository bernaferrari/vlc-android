package org.videolan.vlc.compose.app

import androidx.compose.ui.window.ComposeUIViewController
import org.videolan.vlc.app.IosKoinBootstrap
import org.videolan.vlc.app.IosMediaRepository
import org.videolan.vlc.app.VlcSharedApi
import platform.UIKit.UIViewController

/**
 * Compose Multiplatform root for iOS — parity shell with Android [VlcSharedApp].
 *
 * Swift:
 * ```swift
 * let vc = MainViewControllerKt.MainViewController()
 * ```
 */
fun MainViewController(): UIViewController {
    IosKoinBootstrap.start()
    // Documents scan may leave library empty — seed streaming demos for first run.
    runCatching {
        if (IosMediaRepository.shared.snapshot().isEmpty()) {
            VlcSharedApi().seedDemoLibrary()
        }
    }
    return ComposeUIViewController {
        VlcSharedApp(title = "VLC")
    }
}
