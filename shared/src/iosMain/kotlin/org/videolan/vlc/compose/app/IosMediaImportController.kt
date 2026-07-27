package org.videolan.vlc.compose.app

/**
 * Small Swift-owned import seam. The shared shell owns the visible action while
 * UIKit keeps ownership of Files/Photos privacy prompts and picker lifecycle.
 */
interface IosMediaImportHandler {
    fun presentMediaImport()
}

object IosMediaImportController {
    private var handler: IosMediaImportHandler? = null

    fun setHandler(handler: IosMediaImportHandler?) {
        this.handler = handler
    }

    fun presentMediaImport() {
        handler?.presentMediaImport()
    }
}
