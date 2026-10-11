package org.videolan.vlc.kmp

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.util.VLCVideoLayout
import org.videolan.vlc.compose.player.PlayerArtworkFallback
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.compose.player.TvPlayerArtworkStage

/** Android decoder island for the shared Compose player route (phones). */
val AndroidPlayerSurface: PlayerSurface = androidPlayerSurface(tv = false)

/** Decoder island of the full-screen TV / external player host. */
val AndroidTvPlayerSurface: PlayerSurface = androidPlayerSurface(tv = true)

private fun androidPlayerSurface(tv: Boolean): PlayerSurface {
    // See SharedVideoSurfaceRegistry.SurfaceOptions for why the phone route uses a TextureView.
    val options = SharedVideoSurfaceRegistry.SurfaceOptions(textureView = !tv, followDisplayRoutes = tv)
    return { state, _ ->
        DisposableEffect(Unit) {
            SharedVideoSurfaceRegistry.activateHost()
            onDispose { SharedVideoSurfaceRegistry.deactivateHost() }
        }
        if (!state.hasVideoOutput) {
            if (tv) TvPlayerArtworkStage(state) else PlayerArtworkFallback(state)
        } else {
            AndroidView(
                factory = { context ->
                    VLCVideoLayout(context).apply {
                        clipChildren = true
                        clipToPadding = true
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        fitsSystemWindows = false
                        SharedVideoSurfaceRegistry.attachSurface(this, options)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = SharedVideoSurfaceRegistry::releaseSurface,
            )
        }
    }
}
