/*
 * ComposeVideoSurface — embeds libVLC VLCVideoLayout inside the shared HUD.
 */
package org.videolan.vlc.gui.video

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.util.VLCVideoLayout
import org.videolan.vlc.compose.player.VideoSurfaceWithHud
import org.videolan.vlc.viewmodel.PlayerViewModel

/**
 * Compose-first video surface + HUD for use outside the legacy
 * [VideoPlayerActivity] shell (e.g. SharedApp / future inline player).
 *
 * The libVLC drawable remains a platform [VLCVideoLayout]; chrome is the
 * shared [VideoSurfaceWithHud] (same design language as iOS CMP player).
 *
 * Legacy [VideoPlayerActivity] keeps its ID-stable shell for delegate wiring;
 * new surfaces should use this host so HUD and transport stay shared.
 */
@Composable
fun ComposeVideoSurface(
    modifier: Modifier = Modifier,
    playerVm: PlayerViewModel = remember { PlayerViewModel() },
    onAttachLayout: (VLCVideoLayout) -> Unit = {},
    onDetachLayout: (VLCVideoLayout) -> Unit = {},
    onClose: (() -> Unit)? = null,
) {
    val state by playerVm.state.collectAsState()

    VideoSurfaceWithHud(
        title = state.title,
        subtitle = state.subtitle,
        playing = state.playing,
        progress = state.progress,
        shuffle = state.shuffle,
        repeatMode = state.repeatMode,
        onTogglePlay = playerVm::togglePlayPause,
        onSeek = playerVm::seekTo,
        onNext = playerVm::next,
        onPrevious = playerVm::previous,
        onToggleShuffle = playerVm::toggleShuffle,
        onCycleRepeat = playerVm::cycleRepeat,
        onClose = onClose,
        modifier = modifier.fillMaxSize(),
    ) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    fitsSystemWindows = false
                    onAttachLayout(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { layout -> onDetachLayout(layout) },
        )
    }
}
