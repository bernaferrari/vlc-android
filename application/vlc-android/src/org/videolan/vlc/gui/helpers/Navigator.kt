/*
 * *************************************************************************
 *  Navigator.kt
 * **************************************************************************
 *  Copyright © 2018-2019 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.helpers

import org.videolan.vlc.gui.helpers.setVlcContent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import org.videolan.resources.EXTRA_TARGET
import org.videolan.tools.KEY_NAVIGATION_ID
import org.videolan.tools.setGone
import org.videolan.vlc.R
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.compose.app.VlcKoinMainShell
import org.videolan.vlc.kmp.AndroidShellHostCallbacks
import org.videolan.vlc.kmp.AndroidPlayerSurface
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.kmp.AndroidPipController
import org.videolan.vlc.kmp.VlcKmpInitializer
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.viewmodel.MainTab

class Navigator : DefaultLifecycleObserver, INavigator {

    private val defaultDestinationId = R.id.nav_video
    override var currentDestinationId: Int = 0
    private lateinit var activity: MainActivity
    private lateinit var settings: SharedPreferences
    override lateinit var appbarLayout: AppBarLayout

    private var sharedShellAttached = false
    private var sharedTab by mutableStateOf(MainTab.VIDEO)

    override fun MainActivity.setupNavigation(state: Bundle?) {
        activity = this
        this@Navigator.settings = settings
        currentDestinationId = intent.getIntExtra(EXTRA_TARGET, 0)
        lifecycle.addObserver(this@Navigator)
        appbarLayout = findViewById(R.id.appbar)
        if (!VlcKmpInitializer.isInitialized) {
            VlcKmpInitializer.initialize(applicationContext)
        }
        runCatching {
            (VlcKoin.get().get<PipController>() as? AndroidPipController)?.attachActivity(this)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (currentIdIsExtension()) return
        val initialId = if (currentDestinationId != 0) currentDestinationId
        else settings.getInt(KEY_NAVIGATION_ID, defaultDestinationId)
        sharedTab = tabForNavId(initialId)
        attachSharedShell()
        updateCheckedItem(initialId)
        currentDestinationId = initialId
    }

    override fun onStop(owner: LifecycleOwner) {}

    private fun tabForNavId(id: Int): MainTab = when (id) {
        R.id.nav_audio -> MainTab.AUDIO
        R.id.nav_directories -> MainTab.BROWSER
        R.id.nav_playlists -> MainTab.PLAYLISTS
        R.id.nav_more -> MainTab.MORE
        else -> MainTab.VIDEO
    }

    private fun attachSharedShell() {
        if (sharedShellAttached) return
        val container = activity.findViewById<ViewGroup>(R.id.content_placeholder)
        hideLegacyChromeForSharedShell(container)
        container.removeAllViews()
        container.addView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setVlcContent {
                    // The host owns an Activity Result launcher; keep it stable across Compose
                    // recompositions so importing media never registers duplicate callbacks.
                    val hostCallbacks = remember(activity) { AndroidShellHostCallbacks(activity) }
                    // VlcMainShell is the product chrome on both Android and
                    // iOS. Keep Android here strictly as the system/LibVLC host.
                    VlcKoinMainShell(
                        tab = sharedTab,
                        onTabChange = { t ->
                            sharedTab = t
                            currentDestinationId = when (t) {
                                MainTab.VIDEO -> R.id.nav_video
                                MainTab.AUDIO -> R.id.nav_audio
                                MainTab.BROWSER -> R.id.nav_directories
                                MainTab.PLAYLISTS -> R.id.nav_playlists
                                MainTab.MORE -> R.id.nav_more
                            }
                        },
                        title = activity.getString(R.string.app_name),
                        hostCallbacks = hostCallbacks,
                        playerSurface = AndroidPlayerSurface,
                    )
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        sharedShellAttached = true
    }

    /**
     * MainActivity still creates these views for legacy activity/service code,
     * but the shared Compose shell is their visual owner on this path. Hiding
     * rather than removing the islands keeps Android system hooks stable while
     * avoiding duplicate toolbars, navigation, FABs, and mini-players.
     */
    private fun hideLegacyChromeForSharedShell(content: ViewGroup) {
        appbarLayout.setGone()
        activity.findViewById<View>(R.id.fab).setGone()
        activity.findViewById<View>(R.id.audio_player_container).setGone()
        activity.findViewById<View>(R.id.scan_progress_layout).setGone()
        (content.layoutParams as? CoordinatorLayout.LayoutParams)?.let { params ->
            params.marginStart = 0
            params.marginEnd = 0
            params.topMargin = 0
            params.bottomMargin = 0
            params.behavior = null
            content.layoutParams = params
        }
    }

    override fun currentIdIsExtension() = currentDestinationId in 1..100

    override fun reloadPreferences() {
        currentDestinationId = settings.getInt(KEY_NAVIGATION_ID, defaultDestinationId)
    }

    private fun updateCheckedItem(id: Int) {
        settings.edit { putInt(KEY_NAVIGATION_ID, id) }
    }
}

interface INavigator {
    var appbarLayout: AppBarLayout
    var currentDestinationId: Int

    fun MainActivity.setupNavigation(state: Bundle?)
    fun currentIdIsExtension(): Boolean
    fun reloadPreferences()
}
