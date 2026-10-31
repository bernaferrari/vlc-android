package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import org.videolan.vlc.app.IosPlaybackService
import org.videolan.vlc.compose.player.PlayerArtworkFallback
import org.videolan.vlc.compose.player.PlayerSurface
import platform.UIKit.UIColor
import platform.UIKit.UIView

/**
 * iOS decoder island for the shared player route.
 *
 * VLCKit receives this exact UIView while a video or network stream is active;
 * it is explicitly cleared when Compose removes the surface. The surrounding
 * HUD and all player controls remain common Compose code.
 */
@OptIn(ExperimentalComposeUiApi::class)
val IosPlayerSurface: PlayerSurface = { state, _ ->
    if (!state.hasVideoOutput) {
        PlayerArtworkFallback(state)
    } else {
        UIKitView(
            factory = {
                UIView().apply { backgroundColor = UIColor.blackColor }
            },
            modifier = Modifier.fillMaxSize(),
            // Picture only: every player gesture (tap, seek, swipe, pinch) belongs to the Compose
            // HUD. An interactive interop view would take touches inside its full-screen bounds.
            properties = UIKitInteropProperties(isInteractive = false, isNativeAccessibilityEnabled = false),
            update = IosPlaybackService.shared::attachDrawable,
            onRelease = { IosPlaybackService.shared.attachDrawable(null) },
        )
    }
}
