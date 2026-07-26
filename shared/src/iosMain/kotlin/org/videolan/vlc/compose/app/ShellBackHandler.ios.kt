package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable

@Composable
actual fun HandleShellBackPress(enabled: Boolean, onBack: () -> Unit) {
    // iOS navigation is currently provided by the visible shell controls.
}
