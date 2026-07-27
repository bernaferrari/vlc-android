package org.videolan.vlc.kmp

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.util.VLCVideoLayout
import org.videolan.vlc.compose.player.PlayerArtworkFallback
import org.videolan.vlc.compose.player.PlayerSurface

/** Android decoder island for the shared Compose player route. */
val AndroidPlayerSurface: PlayerSurface = { state ->
    DisposableEffect(Unit) {
        SharedVideoSurfaceRegistry.activateHost()
        onDispose { SharedVideoSurfaceRegistry.deactivateHost() }
    }
    if (!state.hasVideoOutput) {
        PlayerArtworkFallback()
    } else {
        AndroidView(
            factory = { context ->
                VLCVideoLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    fitsSystemWindows = false
                    SharedVideoSurfaceRegistry.attachSurface(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = SharedVideoSurfaceRegistry::releaseSurface,
        )
    }
}
