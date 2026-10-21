@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.videolan.vlc.compose.app

import org.videolan.vlc.app.IosKoinBootstrap
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.platform.MediaSessionBridge
import platform.UIKit.UIViewController

/**
 * iOS root: native tab bar and navigation stacks (Liquid Glass on iOS 26+) around the same shared
 * screens Android shows. See [IosNativeShell].
 */
fun MainViewController(): UIViewController {
    IosKoinBootstrap.start()
    runCatching { VlcKoin.get().get<MediaSessionBridge>().activate() }
    return IosNativeShell(playerSurface = IosPlayerSurface).build()
}
