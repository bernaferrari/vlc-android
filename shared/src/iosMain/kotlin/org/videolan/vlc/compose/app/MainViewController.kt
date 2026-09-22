@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.videolan.vlc.compose.app

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.videolan.tools.VlcSettings
import org.videolan.vlc.compose.theme.resolveVLCThemePreference
import org.videolan.vlc.compose.theme.VLCThemeAppearance
import platform.UIKit.UIColor
import platform.UIKit.UIUserInterfaceStyle.UIUserInterfaceStyleLight
import platform.UIKit.UIUserInterfaceStyle.UIUserInterfaceStyleDark
import platform.UIKit.UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
import org.videolan.vlc.app.IosKoinBootstrap
import org.videolan.vlc.platform.MediaSessionBridge
import org.videolan.vlc.platform.IosAppLockController
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
    var hostViewController by mutableStateOf<UIViewController?>(null)
    IosAppLockController.attachHost { hostViewController }
    val callbacks = IosShellHostCallbacks { hostViewController }
    return ComposeUIViewController {
        NativeHostAppearance(hostViewController)
        VlcKoinMainShell(
            title = "VLC",
            hostCallbacks = callbacks,
            playerSurface = IosPlayerSurface,
        )
    }.also { hostViewController = it }
}

/** Native alerts and pickers inherit the same chosen appearance and accent as Compose. */
@Composable
private fun NativeHostAppearance(host: UIViewController?) {
    val appearance by VlcSettings.themeAppearance.collectAsState()
    val accent by VlcSettings.themeAccent.collectAsState()
    val preference = resolveVLCThemePreference(appearance, accent)
    val dark = preference.appearance.resolveDarkTheme(isSystemInDarkTheme())
    val primary = preference.accent.primary(dark)
    SideEffect {
        host?.overrideUserInterfaceStyle = when (preference.appearance) {
            VLCThemeAppearance.System -> UIUserInterfaceStyleUnspecified
            VLCThemeAppearance.Light -> UIUserInterfaceStyleLight
            VLCThemeAppearance.Dark -> UIUserInterfaceStyleDark
        }
        host?.view?.tintColor = UIColor(
            red = primary.red.toDouble(),
            green = primary.green.toDouble(),
            blue = primary.blue.toDouble(),
            alpha = primary.alpha.toDouble(),
        )
    }
}
