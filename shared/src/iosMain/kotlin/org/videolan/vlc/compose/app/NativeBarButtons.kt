@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.videolan.vlc.compose.app

import org.videolan.vlc.compose.components.NativeBarActions
import org.videolan.vlc.compose.components.VLCConnectedIconAction
import platform.UIKit.UIAction
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.navigationItem

/**
 * Draws a pushed screen's Compose actions as navigation bar buttons, so on iOS 26 they join the
 * back button in Liquid Glass. Items are rebuilt only when the visible set changes (icon, label);
 * clicks go through [latest], so recomposed lambdas are always the ones invoked.
 */
internal class NativeBarButtons(private val controller: UIViewController) : NativeBarActions {
    private var latest: List<VLCConnectedIconAction> = emptyList()
    private var shownKey: List<Pair<String, String>>? = null

    override fun supports(actions: List<VLCConnectedIconAction>): Boolean =
        actions.all { symbolFor(it) != null }

    override fun publish(actions: List<VLCConnectedIconAction>) {
        latest = actions
        val key = actions.map { symbolFor(it).orEmpty() to it.contentDescription }
        if (key == shownKey) return
        shownKey = key
        // UIKit lays right items out from the trailing edge inward.
        controller.navigationItem.rightBarButtonItems = actions.indices.reversed().map { index ->
            val action = actions[index]
            // The action's title is the button's accessibility label; the bar shows only the symbol.
            val tap = UIAction.actionWithTitle(
                title = action.contentDescription,
                image = UIImage.systemImageNamed(symbolFor(action)!!),
                identifier = null,
                handler = { _ -> latest.getOrNull(index)?.onClick?.invoke() },
            )
            UIBarButtonItem(primaryAction = tap)
        }
    }

    private fun symbolFor(action: VLCConnectedIconAction): String? =
        SfSymbols[action.icon.imageVector.name]
}

/** Material Symbols used in page actions → their SF Symbols counterparts. */
private val SfSymbols = mapOf(
    "search" to "magnifyingglass",
    "close" to "xmark",
    "grid_view" to "square.grid.2x2",
    "view_list" to "list.bullet",
    "more_vert" to "ellipsis",
    "add" to "plus",
    "tune" to "slider.horizontal.3",
    "info" to "info.circle",
    "sort" to "arrow.up.arrow.down",
    "edit" to "pencil",
    "delete" to "trash",
    "refresh" to "arrow.clockwise",
    "ios_share" to "square.and.arrow.up",
    "shuffle" to "shuffle",
    "play_arrow" to "play.fill",
    "select_all" to "checklist",
    "star" to "star",
    "history" to "clock.arrow.circlepath",
    "settings" to "gearshape",
    "folder" to "folder",
    "open_in_new" to "arrow.up.right.square",
)
