package org.videolan.vlc.app

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.player.PlaybackController

/**
 * Small Objective-C-exportable vehicle surface over the shared catalog and player.
 *
 * CarPlay owns its safety-constrained native templates, but it must never grow an
 * independent catalog or playback stack. Swift consumes this read-only snapshot and
 * sends selections straight back through [PlaybackController].
 */
object IosVehicleMediaBridge {
    fun audioItems(): List<MediaItem> = IosMediaLibrary.shared.snapshot()
        .filter { it.type == MediaType.AUDIO }

    fun playAudioItem(id: Long): Boolean {
        val queue = audioItems()
        val item = queue.firstOrNull { it.id == id } ?: return false
        PlaybackController.get().play(item, queue)
        return true
    }
}
