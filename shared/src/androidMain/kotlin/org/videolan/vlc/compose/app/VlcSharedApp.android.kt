package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
fun VlcMainShellPreview() {
    VlcMainShell(title = "VLC")
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun VlcMainShellDarkPreview() {
    VlcMainShell(title = "VLC")
}

@Deprecated("Use VlcMainShellPreview", ReplaceWith("VlcMainShellPreview()"))
@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
fun VlcSharedAppLibraryPreview() {
    VlcMainShellPreview()
}

@Deprecated("Use VlcMainShellDarkPreview", ReplaceWith("VlcMainShellDarkPreview()"))
@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun VlcSharedAppDarkPreview() {
    VlcMainShellDarkPreview()
}
