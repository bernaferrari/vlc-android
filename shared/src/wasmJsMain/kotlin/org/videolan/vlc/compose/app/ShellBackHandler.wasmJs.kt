package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable

@Composable
actual fun HandleShellBackPress(enabled: Boolean, onBack: () -> Unit) {
    // Browser history is owned by the eventual web navigation host.
}
