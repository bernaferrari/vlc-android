package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.videolan.vlc.compose.components.VLCAboutScreen
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols

/**
 * The About journey belongs to the shared Nav3 back stack, not to an Android activity or an iOS
 * alert.  Hosts supply only build metadata, packaged license text, and the destinations behind
 * each link; copy, layout, sheets, accessibility, and back behavior stay identical everywhere.
 */
@Composable
internal fun AboutDestination(
    hostCallbacks: ShellHostCallbacks,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var licenseText by remember(hostCallbacks) { mutableStateOf(VLC_DEFAULT_LICENSE_TEXT) }
    LaunchedEffect(hostCallbacks) {
        licenseText = hostCallbacks.loadAboutLicenseText()
    }

    VLCAboutScreen(
        title = ShellStrings.about(),
        appName = ShellStrings.appName(),
        description = ShellStrings.aboutDescription(),
        versionInfo = hostCallbacks.aboutVersionInfo(),
        copyright = ShellStrings.aboutCopyright(),
        licenseTitle = ShellStrings.aboutLicense(),
        licenseText = licenseText,
        websiteTitle = ShellStrings.officialWebsite(),
        feedbackTitle = ShellStrings.sendFeedback(),
        sourcesTitle = ShellStrings.sources(),
        librariesTitle = ShellStrings.libraries(),
        authorsTitle = ShellStrings.authors(),
        closeContentDescription = ShellStrings.close(),
        openLinkContentDescription = ShellStrings.openInBrowser(),
        onClose = onBack,
        onOpenWebsite = { hostCallbacks.onOpenAboutAction(AboutAction.WEBSITE) },
        onSendFeedback = { hostCallbacks.onOpenAboutAction(AboutAction.FEEDBACK) },
        onOpenSources = { hostCallbacks.onOpenAboutAction(AboutAction.SOURCES) },
        onOpenLibraries = { hostCallbacks.onOpenAboutAction(AboutAction.LIBRARIES) },
        onOpenAuthors = { hostCallbacks.onOpenAboutAction(AboutAction.AUTHORS) },
        onOpenLicenseLink = { hostCallbacks.onOpenAboutAction(AboutAction.LICENSE) },
        // This is a Nav3 detail, so use a spatial Back affordance rather than a dialog close.
        closeIconContent = {
            Icon(
                icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
            )
        },
        modifier = modifier.fillMaxSize(),
    )
}
