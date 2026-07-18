/*
 * MainNavChrome — Compose Multiplatform-style bottom bar + navigation rail
 * replacing Material BottomNavigationView / NavigationRailView widgets while
 * preserving R.id.navigation / R.id.navigation_rail for layout behaviors.
 */
package org.videolan.vlc.gui

import android.content.Context
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/** Holds selection + click callback for both bottom bar and rail hosts. */
class MainNavChromeState {
    var selectedId by mutableIntStateOf(R.id.nav_video)
    var onDestinationSelected: ((Int) -> Unit)? = null

    fun select(id: Int, notify: Boolean = true) {
        if (selectedId == id) return
        selectedId = id
        if (notify) onDestinationSelected?.invoke(id)
    }
}

private val NavItems = listOf(
    Triple(R.id.nav_video, R.string.video, R.drawable.ic_video),
    Triple(R.id.nav_audio, R.string.audio, R.drawable.ic_menu_audio),
    Triple(R.id.nav_directories, R.string.browse, R.drawable.ic_folder),
    Triple(R.id.nav_playlists, R.string.playlists, R.drawable.ic_playlist),
    Triple(R.id.nav_more, R.string.more, R.drawable.ic_nav_more),
)

fun Context.createComposeBottomNav(state: MainNavChromeState): ComposeView =
    ComposeView(this).apply {
        id = R.id.navigation
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            VLCTheme {
                ComposeBottomBar(state)
            }
        }
    }

fun Context.createComposeNavRail(state: MainNavChromeState, onFabClick: (() -> Unit)? = null): ComposeView =
    ComposeView(this).apply {
        id = R.id.navigation_rail
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            VLCTheme {
                ComposeNavRail(state, onFabClick)
            }
        }
    }

@Composable
private fun ComposeBottomBar(state: MainNavChromeState) {
    val colors = VLCThemeDefaults.colors
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.backgroundDefault,
    ) {
        NavItems.forEach { (id, label, icon) ->
            NavigationBarItem(
                selected = state.selectedId == id,
                onClick = { state.select(id) },
                icon = {
                    Icon(painter = painterResource(icon), contentDescription = stringResource(label))
                },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun ComposeNavRail(state: MainNavChromeState, onFabClick: (() -> Unit)?) {
    val colors = VLCThemeDefaults.colors
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = colors.backgroundDefault,
        header = {
            if (onFabClick != null) {
                FloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier.padding(8.dp),
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fab_add),
                        contentDescription = stringResource(R.string.add),
                    )
                }
            }
        }
    ) {
        NavItems.forEach { (id, label, icon) ->
            NavigationRailItem(
                selected = state.selectedId == id,
                onClick = { state.select(id) },
                icon = {
                    Icon(painter = painterResource(icon), contentDescription = stringResource(label))
                },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

/** Still used by SecondaryActivity / menu inflation helpers that build Menu objects. */
fun addPhoneNavigationItems(menu: Menu) {
    menu.add(Menu.NONE, R.id.nav_video, 0, R.string.video).setIcon(R.drawable.ic_video)
    menu.add(Menu.NONE, R.id.nav_audio, 1, R.string.audio).setIcon(R.drawable.ic_menu_audio)
    menu.add(Menu.NONE, R.id.nav_directories, 2, R.string.browse).setIcon(R.drawable.ic_folder)
    menu.add(Menu.NONE, R.id.nav_playlists, 3, R.string.playlists).setIcon(R.drawable.ic_playlist)
    menu.add(Menu.NONE, R.id.nav_more, 4, R.string.more).setIcon(R.drawable.ic_nav_more)
}

fun ViewGroup.findMainNavChromeState(): MainNavChromeState? =
    getTag(R.id.navigation) as? MainNavChromeState
        ?: (parent as? View)?.getTag(R.id.navigation) as? MainNavChromeState
