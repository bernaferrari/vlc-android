package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.videolan.vlc.viewmodel.MainTab

/**
 * Deprecated thin wrapper around [VlcMainShell].
 *
 * Prefer [VlcKoinMainShell] in a host and [VlcMainShell] in previews/tests.
 * Kept for binary/source compatibility with older lab callers.
 */
@Deprecated(
    message = "Use VlcKoinMainShell instead",
    replaceWith = ReplaceWith(
        "VlcKoinMainShell(modifier = modifier, title = title, hostCallbacks = hostCallbacks)",
        "org.videolan.vlc.compose.app.VlcKoinMainShell",
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
    VlcKoinMainShell(
        modifier = modifier,
        title = title,
        initialTab = initialTab,
        hostCallbacks = hostCallbacks,
        onOpenSettings = onOpenSettings,
        onOpenRemoteClient = onOpenRemoteClient,
    )
}
