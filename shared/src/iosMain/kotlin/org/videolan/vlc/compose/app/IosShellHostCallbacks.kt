@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.videolan.vlc.compose.app

import org.videolan.vlc.app.IosMediaLibrary
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.ContextOption
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertActionStyleDestructive
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

/** UIKit-backed actions that are safe to advertise from the shared iOS shell. */
class IosShellHostCallbacks(
    private val hostViewController: () -> UIViewController?,
) : ShellHostCallbacks {

    override fun supportsMediaImport(): Boolean = true

    override fun onImportMedia() {
        IosMediaImportController.presentMediaImport()
    }

    override fun supportsSubtitleImport(): Boolean = true

    override fun onImportSubtitle(onPicked: (String) -> Unit) {
        IosMediaImportController.presentSubtitleImport(onPicked)
    }

    override fun onContextAction(item: MediaItem, option: ContextOption) = when (option) {
        ContextOption.CTX_DELETE -> confirmDelete(item)
        else -> Unit
    }

    override fun supportsContextAction(option: ContextOption): Boolean = when (option) {
        ContextOption.CTX_DELETE,
        ContextOption.CTX_INFORMATION,
        ContextOption.CTX_SHARE,
        -> true
        else -> false
    }

    override fun onOpenInfo(item: MediaItem) {
        val presentation = item.infoPresentation()
        presentAlert(
            title = presentation.title,
            message = presentation.details.ifBlank { "No additional information." },
        )
    }

    override fun onShare(item: MediaItem) {
        val host = hostViewController() ?: return
        val activity = UIActivityViewController(
            // Imported media lives in the app Documents folder. Hand UIKit the file URL so
            // receivers get the actual file rather than an unusable textual file:// address.
            activityItems = listOf<Any>(item.shareableActivityItem()),
            applicationActivities = null,
        )
        activity.popoverPresentationController()?.let { popover ->
            // A source anchor is required for a share sheet on iPad.
            popover.setSourceView(host.view)
            popover.setSourceRect(host.view.bounds)
        }
        host.presentViewController(activity, animated = true, completion = null)
    }

    override fun onOpenAbout() {
        presentAlert(
            title = "VLC",
            message = SHARED_ABOUT_MESSAGE,
        )
    }

    override fun onOpenDonate() {
        NSURL.URLWithString(VLC_DONATION_URL)?.let {
            UIApplication.sharedApplication.openURL(it)
        }
    }

    private fun confirmDelete(item: MediaItem) {
        val host = hostViewController() ?: return
        val alert = UIAlertController.alertControllerWithTitle(
            title = "Delete ${item.displayTitle}?",
            message = "This removes the file from VLC on this device.",
            preferredStyle = UIAlertControllerStyleAlert,
        )
        alert.addAction(
            UIAlertAction.actionWithTitle("Cancel", style = UIAlertActionStyleCancel, handler = null),
        )
        alert.addAction(
            UIAlertAction.actionWithTitle("Delete", style = UIAlertActionStyleDestructive) {
                if (!IosMediaLibrary.shared.deleteImportedMedia(item.id)) {
                    presentAlert(
                        title = "Could not delete file",
                        message = "VLC can only delete files it imported into its Documents library.",
                    )
                }
            },
        )
        host.presentViewController(alert, animated = true, completion = null)
    }

    private fun MediaItem.shareableActivityItem(): Any =
        uri.takeIf(String::isNotBlank)
            ?.let { NSURL.URLWithString(it) }
            ?.takeIf { it.isFileURL() }
            ?: uri.ifBlank { displayTitle }

    private fun presentAlert(title: String, message: String) {
        val host = hostViewController() ?: return
        val alert = UIAlertController.alertControllerWithTitle(
            title = title,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert,
        )
        alert.addAction(
            UIAlertAction.actionWithTitle("OK", style = UIAlertActionStyleDefault, handler = null),
        )
        host.presentViewController(alert, animated = true, completion = null)
    }
}
