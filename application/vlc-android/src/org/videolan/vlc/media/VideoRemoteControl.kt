package org.videolan.vlc.media

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Remote-access player commands ("up", "center", "skip-next", …) independent from any one player
 * UI. The phone player route maps them to player commands; the TV player host replays them as
 * remote key presses.
 * A consumer resets the value to null once handled so the same command can repeat.
 */
object VideoRemoteControl {
    val actions = MutableStateFlow<String?>(null)
}
