/*
 * ************************************************************************
 *  PhoneActivityShell.kt
 * *************************************************************************
 * Copyright © 2026 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 */

package org.videolan.vlc.gui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.videolan.tools.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.gui.helpers.BottomNavigationBehavior
import org.videolan.vlc.gui.helpers.FloatingActionButtonBehavior
import org.videolan.vlc.gui.helpers.PlayerBehavior
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.view.installScanProgressHost

internal fun MainActivity.createMainActivityShell(): View {
    val root = CoordinatorLayout(this).apply {
        id = R.id.coordinator
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    val navigationRail = NavigationRailView(this).apply {
        id = R.id.navigation_rail
        labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        elevation = 4.dp.toFloat()
        menu.clear()
        inflateMenu(R.menu.bottom_navigation)
        addHeaderView(createShellFab(large = true))
    }
    root.addView(
        navigationRail,
        CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.MATCH_PARENT).apply {
            behavior = AppBarLayout.ScrollingViewBehavior()
        }
    )

    root.addView(createPhoneToolbarShell())

    val content = FrameLayout(this).apply {
        id = R.id.content_placeholder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) isKeyboardNavigationCluster = true
    }
    root.addView(
        content,
        CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT).apply {
            marginStart = resources.getDimensionPixelSize(R.dimen.navigation_margin)
            behavior = AppBarLayout.ScrollingViewBehavior()
        }
    )

    root.addView(
        createScanProgressComposeHost(),
        CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT).apply {
            anchorId = R.id.navigation
            anchorGravity = Gravity.TOP
            gravity = Gravity.TOP
        }
    )

    root.addView(createAudioPlayerContainerShell())

    root.addView(
        createShellFab(large = false),
        CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT).apply {
            anchorId = R.id.navigation
            anchorGravity = Gravity.TOP or Gravity.END
            gravity = Gravity.TOP or Gravity.END
            dodgeInsetEdges = Gravity.BOTTOM
            behavior = FloatingActionButtonBehavior(this@createMainActivityShell, null)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.default_margin)
        }
    )

    val bottomNavigation = BottomNavigationView(this).apply {
        id = R.id.navigation
        setBackgroundResource(resolveResourceAttr(R.attr.bottom_navigation_background))
        elevation = 16.dp.toFloat()
        itemIconTintList = ContextCompat.getColorStateList(context, R.color.bottom_navigation_selector)
        itemRippleColor = ColorStateList.valueOf(resolveColorAttr(R.attr.bottom_navigation_focus))
        itemTextColor = ContextCompat.getColorStateList(context, R.color.bottom_navigation_selector)
        labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        menu.clear()
        inflateMenu(R.menu.bottom_navigation)
    }
    root.addView(
        bottomNavigation,
        CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM
            behavior = BottomNavigationBehavior<BottomNavigationView>(this@createMainActivityShell, null)
            insetEdge = Gravity.BOTTOM
        }
    )

    root.addView(createAudioPlayerTipsComposeHost())

    return root
}

internal fun SecondaryActivity.createSecondaryActivityShell(): View {
    val root = FrameLayout(this).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
    val coordinator = CoordinatorLayout(this).apply {
        id = R.id.coordinator
    }
    root.addView(
        coordinator,
        FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    )

    coordinator.addView(createPhoneToolbarShell())

    val content = FrameLayout(this).apply {
        id = R.id.content_placeholder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) isKeyboardNavigationCluster = true
    }
    coordinator.addView(
        content,
        CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT)
    )

    coordinator.addView(
        createScanProgressComposeHost(),
        CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM
        }
    )

    coordinator.addView(createAudioPlayerContainerShell())

    coordinator.addView(
        createShellFab(large = false).apply {
            setImageResource(R.drawable.ic_fab_add)
            visibility = View.INVISIBLE
        },
        CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp, 16.dp, 16.dp, 16.dp)
            anchorId = R.id.content_placeholder
            anchorGravity = Gravity.BOTTOM or Gravity.END
            behavior = FloatingActionButtonBehavior(this@createSecondaryActivityShell, null)
        }
    )

    root.addView(
        createAudioPlaylistTipsComposeHost(),
        FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    )
    root.addView(
        createAudioPlayerTipsComposeHost(),
        FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    )

    return root
}

private fun Context.createPhoneToolbarShell(): AppBarLayout {
    val appBar = AppBarLayout(this).apply {
        id = R.id.appbar
        setBackgroundResource(resolveResourceAttr(R.attr.background_actionbar))
        elevation = 0f
        ViewCompat.setElevation(this, 0f)
    }

    val toolbar = MaterialToolbar(ContextThemeWrapper(this, R.style.Toolbar_VLC)).apply {
        id = R.id.main_toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) isKeyboardNavigationCluster = true
        navigationContentDescription = getString(R.string.abc_action_bar_up_description)
        navigationIcon = AppCompatResources.getDrawable(context, resolveResourceAttr(androidx.appcompat.R.attr.homeAsUpIndicator))
        popupTheme = resolveResourceAttr(R.attr.toolbar_popup_style)
        setTitleTextColor(resolveColorAttr(R.attr.colorPrimary))
        titleMarginStart = resources.getDimensionPixelSize(R.dimen.default_margin)
    }
    appBar.addView(
        toolbar,
        AppBarLayout.LayoutParams(AppBarLayout.LayoutParams.MATCH_PARENT, actionBarSize()).apply {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
        }
    )

    toolbar.addView(
        ImageView(this).apply {
            id = R.id.toolbar_icon
            setImageResource(R.drawable.ic_incognito)
        },
        androidx.appcompat.widget.Toolbar.LayoutParams(32.dp, 32.dp)
    )
    toolbar.addView(
        TextView(this).apply {
            id = R.id.toolbar_vlc_title
            setPadding(16.dp, 0, 16.dp, 0)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setTextAppearance(R.style.ToolbarTitleText)
            setText(R.string.app_name)
        },
        androidx.appcompat.widget.Toolbar.LayoutParams(
            androidx.appcompat.widget.Toolbar.LayoutParams.WRAP_CONTENT,
            androidx.appcompat.widget.Toolbar.LayoutParams.WRAP_CONTENT
        )
    )

    appBar.addView(
        TabLayout(ContextThemeWrapper(this, R.style.TabLayout_VLC)).apply {
            id = R.id.sliding_tabs
            isInlineLabel = true
            visibility = View.GONE
        },
        AppBarLayout.LayoutParams(AppBarLayout.LayoutParams.MATCH_PARENT, AppBarLayout.LayoutParams.WRAP_CONTENT).apply {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        }
    )

    return appBar
}

private fun Context.createAudioPlayerContainerShell(): FrameLayout {
    return FrameLayout(this).apply {
        id = R.id.audio_player_container
        elevation = resources.getDimension(R.dimen.audio_player_elevation)
        visibility = View.GONE
        addView(
            createAudioPlayerHostView(),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        addView(
            createAudioPlaylistTipsComposeHost(),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        layoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT).apply {
            behavior = PlayerBehavior<FrameLayout>()
        }
    }
}

private fun Context.createShellFab(large: Boolean): FloatingActionButton {
    return FloatingActionButton(this).apply {
        id = if (large) R.id.fab_large else R.id.fab
        useCompatPadding = true
        setImageResource(R.drawable.ic_fab_add)
        supportImageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey50))
        rippleColor = ContextCompat.getColor(context, R.color.orange50)
        visibility = if (large) View.VISIBLE else View.INVISIBLE
    }
}

private fun Context.createScanProgressComposeHost() = VLCComposeView(this).apply {
    id = R.id.scan_progress_layout
    visibility = View.GONE
    isClickable = false
    isFocusable = false
    installScanProgressHost(this@createScanProgressComposeHost)
}

private fun Context.actionBarSize(): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
    return if (typedValue.resourceId != 0) resources.getDimensionPixelSize(typedValue.resourceId)
    else TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
}

private fun Context.resolveResourceAttr(attrId: Int): Int = UiTools.getResourceFromAttribute(this, attrId)

private fun Context.resolveColorAttr(attrId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrId, typedValue, true)
    return if (typedValue.resourceId != 0) ContextCompat.getColor(this, typedValue.resourceId) else typedValue.data
}
