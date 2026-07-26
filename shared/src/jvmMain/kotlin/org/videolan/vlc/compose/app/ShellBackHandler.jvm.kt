package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable

@Composable
actual fun HandleShellBackPress(enabled: Boolean, onBack: () -> Unit) {
    // Desktop host integration is supplied by the embedding application.
}
