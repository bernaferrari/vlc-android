package org.videolan.vlc.compose.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.videolan.vlc.compose.components.VLCAuthorsScreen
import org.videolan.vlc.compose.components.VLCLibrariesScreen
import org.videolan.vlc.compose.components.VLCLibraryLicense

/** About details remain in the same Nav3 stack on Android, iOS, and Wasm. */
@Composable
internal fun AboutLibrariesDestination(
    hostCallbacks: ShellHostCallbacks,
    modifier: Modifier = Modifier,
) {
    var libraries by remember(hostCallbacks) { mutableStateOf<List<VLCLibraryLicense>?>(null) }
    LaunchedEffect(hostCallbacks) {
        libraries = hostCallbacks.loadAboutLibraries()
    }
    VLCUtilityPane(modifier = modifier) {
        VLCLibrariesScreen(
            title = ShellStrings.libraries(),
            libraries = libraries.orEmpty(),
            closeContentDescription = ShellStrings.back(),
            openLinkContentDescription = ShellStrings.openInBrowser(),
            onClose = {},
            onOpenLicenseLink = hostCallbacks::onOpenExternalUrl,
            showHeader = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun AboutAuthorsDestination(
    hostCallbacks: ShellHostCallbacks,
    modifier: Modifier = Modifier,
) {
    var authors by remember(hostCallbacks) { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(hostCallbacks) {
        authors = hostCallbacks.loadAboutAuthors()
    }
    VLCUtilityPane(modifier = modifier) {
        VLCAuthorsScreen(
            title = ShellStrings.authors(),
            authors = authors.orEmpty(),
            closeContentDescription = ShellStrings.back(),
            onClose = {},
            showHeader = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
