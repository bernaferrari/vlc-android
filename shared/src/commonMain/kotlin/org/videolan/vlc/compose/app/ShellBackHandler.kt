package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable

/** Installs the platform's system-back bridge when one exists. */
@Composable
expect fun HandleShellBackPress(enabled: Boolean, onBack: () -> Unit)
