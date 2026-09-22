package org.videolan.vlc.gui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import org.videolan.tools.Settings
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCAppTheme
import org.videolan.vlc.compose.theme.VLCTheme

/**
 * Separate ComposeViews (including dialogs and legacy overlays) do not inherit composition
 * locals. Install the saved appearance at every platform root, retaining dark TV
 * surfaces even when Android's configuration is light. Explicit nested themes still win.
 */
fun ComposeView.setVlcContent(content: @Composable () -> Unit) {
    val darkSurface = Settings.showTvUi
    setContent { PlatformAppTheme(darkSurface, content) }
}

fun VLCComposeView.setVlcContent(content: @Composable () -> Unit) {
    val darkSurface = Settings.showTvUi
    setContent { PlatformAppTheme(darkSurface, content) }
}

@Composable
private fun PlatformAppTheme(darkSurface: Boolean, content: @Composable () -> Unit) {
    VLCAppTheme {
        if (darkSurface) VLCTheme(darkTheme = true, content = content) else content()
    }
}
