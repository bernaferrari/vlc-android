package org.videolan.vlc.media

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Remote-access player commands ("up", "center", "skip-next", …) independent from any one player
 * UI. The shared player host collects them on phones; VideoPlayerActivity still does on TV.
 * A consumer resets the value to null once handled so the same command can repeat.
 */
object VideoRemoteControl {
    val actions = MutableStateFlow<String?>(null)
}
