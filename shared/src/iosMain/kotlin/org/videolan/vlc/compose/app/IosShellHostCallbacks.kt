package org.videolan.vlc.compose.app

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.util.ContextOption
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/** UIKit-backed actions that are safe to advertise from the shared iOS shell. */
class IosShellHostCallbacks(
    private val hostViewController: () -> UIViewController?,
) : ShellHostCallbacks {

    override fun supportsMediaImport(): Boolean = true

    override fun onImportMedia() {
        IosMediaImportController.presentMediaImport()
    }

    override fun onContextAction(item: MediaItem, option: ContextOption) = Unit

    override fun supportsContextAction(option: ContextOption): Boolean = when (option) {
        ContextOption.CTX_INFORMATION,
        ContextOption.CTX_SHARE,
        -> true
        else -> false
    }

    override fun onOpenInfo(item: MediaItem) {
        val details = listOfNotNull(
            item.artist?.takeIf(String::isNotBlank),
            item.album?.takeIf(String::isNotBlank),
            item.uri.takeIf(String::isNotBlank),
        ).joinToString("\n")
        presentAlert(title = item.displayTitle, message = details.ifBlank { "No additional information." })
    }

    override fun onShare(item: MediaItem) {
        val host = hostViewController() ?: return
        val activity = UIActivityViewController(
            activityItems = listOf(item.uri.ifBlank { item.displayTitle }),
            applicationActivities = null,
        )
        host.presentViewController(activity, animated = true, completion = null)
    }

    override fun onOpenAbout() {
        presentAlert(
            title = "VLC",
            message = "VLC for iOS uses the shared Compose media library shell.",
        )
    }

    override fun onOpenDonate() {
        NSURL.URLWithString("https://www.videolan.org/contribute.html")?.let {
            UIApplication.sharedApplication.openURL(it)
        }
    }

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
