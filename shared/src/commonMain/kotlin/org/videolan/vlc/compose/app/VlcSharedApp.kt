package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.videolan.vlc.viewmodel.MainTab

/**
 * Deprecated thin wrapper around [VlcMainShell].
 *
 * Prefer [VlcMainShell] directly (Android main path, iOS [MainViewController], previews).
 * Kept for binary/source compatibility with older lab callers.
 */
@Deprecated(
    message = "Use VlcMainShell instead",
    replaceWith = ReplaceWith(
        "VlcMainShell(modifier = modifier, title = title, hostCallbacks = hostCallbacks)",
        "org.videolan.vlc.compose.app.VlcMainShell",
        "org.videolan.vlc.compose.app.ShellHostCallbacks",
    ),
)
@Composable
fun VlcSharedApp(
    modifier: Modifier = Modifier,
    title: String = "VLC",
    initialTab: MainTab = MainTab.VIDEO,
    hostCallbacks: ShellHostCallbacks = ShellHostCallbacks.NoOp,
    onOpenSettings: (() -> Unit)? = null,
    onOpenRemoteClient: (() -> Unit)? = null,
) {
    VlcMainShell(
        modifier = modifier,
        title = title,
        initialTab = initialTab,
        hostCallbacks = hostCallbacks,
        onOpenSettings = onOpenSettings,
        onOpenRemoteClient = onOpenRemoteClient,
    )
}
