package org.videolan.vlc.app

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.vehicle.VehicleMediaCatalog

/**
 * Small Objective-C-exportable vehicle surface over the shared catalog and player.
 *
 * CarPlay owns its safety-constrained native templates, but it must never grow an
 * independent catalog or playback stack. Swift consumes this read-only snapshot and
 * sends selections straight back through [PlaybackController].
 */
object IosVehicleMediaBridge {
    private val catalog: VehicleMediaCatalog
        get() = VlcKoin.get().get()

    fun audioItems(): List<MediaItem> = catalog.snapshot()

    fun playAudioItem(id: Long): Boolean = catalog.play(id)
}
