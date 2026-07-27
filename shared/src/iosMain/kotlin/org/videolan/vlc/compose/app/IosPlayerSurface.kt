package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
val IosPlayerSurface: PlayerSurface = { state, _ ->
    if (!state.hasVideoOutput) {
        PlayerArtworkFallback()
    } else {
        UIKitView(
            factory = {
                UIView().apply { backgroundColor = UIColor.blackColor }
            },
            modifier = Modifier.fillMaxSize(),
            update = IosPlaybackService.shared::attachDrawable,
            onRelease = { IosPlaybackService.shared.attachDrawable(null) },
        )
    }
}
