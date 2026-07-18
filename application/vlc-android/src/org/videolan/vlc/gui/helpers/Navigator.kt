/*
 * *************************************************************************
 *  Navigator.kt
 * **************************************************************************
 *  Copyright © 2018-2019 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.helpers

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.EXTRA_FOR_ESPRESSO
import org.videolan.resources.EXTRA_TARGET
import org.videolan.resources.REMOTE_ACCESS_CLIENT_ACTIVITY
import org.videolan.resources.util.parcelableList
import org.videolan.tools.KEY_NAVIGATION_ID
import org.videolan.tools.KEY_USE_SHARED_MAIN_SHELL
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.compose.app.VlcMainShell
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.MainNavChromeState
import org.videolan.vlc.gui.MoreScreenController
import org.videolan.vlc.gui.PlaylistScreenController
import org.videolan.vlc.gui.audio.AudioScreenController
import org.videolan.vlc.gui.browser.MainBrowserScreenController
import org.videolan.vlc.gui.helpers.UiTools.isTablet
import org.videolan.vlc.gui.preferences.PreferencesActivity
import org.videolan.vlc.gui.video.VideoScreenController
import org.videolan.vlc.kmp.AndroidPipController
import org.videolan.vlc.kmp.VlcKmpInitializer
import org.videolan.vlc.platform.PipController
import org.videolan.vlc.util.getScreenWidth
import org.videolan.vlc.viewmodel.MainTab

class Navigator : DefaultLifecycleObserver, INavigator {

    private val defaultDestinationId = R.id.nav_video
    override var currentDestinationId: Int = 0
    private lateinit var activity: MainActivity
    private lateinit var settings: SharedPreferences
    override lateinit var navigationViews: List<View>
    override lateinit var appbarLayout: AppBarLayout
    private var navChrome: MainNavChromeState? = null
    private var forExpresso: ArrayList<MediaLibraryItem>? = null

    private var useSharedShell: Boolean = true
    private var sharedShellAttached = false
    private var sharedTab by mutableStateOf(MainTab.VIDEO)

    private var moreController: MoreScreenController? = null
    private var mainBrowserController: MainBrowserScreenController? = null
    private var playlistController: PlaylistScreenController? = null
    private var videoController: VideoScreenController? = null
    private var audioController: AudioScreenController? = null

    override fun MainActivity.setupNavigation(state: Bundle?) {
        activity = this
        this@Navigator.settings = settings
        forExpresso = intent.parcelableList(EXTRA_FOR_ESPRESSO)
        currentDestinationId = intent.getIntExtra(EXTRA_TARGET, 0)
        lifecycle.addObserver(this@Navigator)
        navigationViews = listOf(
            findViewById(R.id.navigation),
            findViewById(R.id.navigation_rail),
        )
        appbarLayout = findViewById(R.id.appbar)
        navChrome = mainNavChromeState
        navChrome?.onDestinationSelected = { id -> onDestinationSelected(id) }
        useSharedShell = settings.getBoolean(KEY_USE_SHARED_MAIN_SHELL, true)
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
        if (useSharedShell) {
            sharedTab = tabForNavId(initialId)
            attachSharedShell()
            updateCheckedItem(initialId)
            currentDestinationId = initialId
        } else {
            showScreen(initialId)
        }
    }

    override fun onStop(owner: LifecycleOwner) {}

    private fun onDestinationSelected(id: Int): Boolean {
        appbarLayout.setExpanded(true, true)
        if (currentDestinationId == id && !useSharedShell) return false
        activity.slideDownAudioPlayer()
        if (useSharedShell) {
            sharedTab = tabForNavId(id)
            updateCheckedItem(id)
            currentDestinationId = id
        } else {
            showScreen(id)
        }
        return true
    }

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
        container.removeAllViews()
        container.addView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme {
                        // Controlled by Android bottom chrome — hide internal bar.
                        VlcMainShell(
                            tab = sharedTab,
                            onTabChange = { t ->
                                sharedTab = t
                                val id = when (t) {
                                    MainTab.VIDEO -> R.id.nav_video
                                    MainTab.AUDIO -> R.id.nav_audio
                                    MainTab.BROWSER -> R.id.nav_directories
                                    MainTab.PLAYLISTS -> R.id.nav_playlists
                                    MainTab.MORE -> R.id.nav_more
                                }
                                updateCheckedItem(id)
                                currentDestinationId = id
                            },
                            showBottomBar = false,
                            title = activity.getString(R.string.app_name),
                            onOpenSettings = {
                                activity.startActivity(Intent(activity, PreferencesActivity::class.java))
                            },
                            onOpenRemoteClient = {
                                activity.startActivity(
                                    Intent().setClassName(activity, REMOTE_ACCESS_CLIENT_ACTIVITY)
                                )
                            },
                        )
                    }
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        sharedShellAttached = true
    }

    private fun showScreen(id: Int) {
        when (id) {
            R.id.nav_audio -> showAudioScreen()
            R.id.nav_more -> showMoreScreen()
            R.id.nav_directories -> showMainBrowserScreen()
            R.id.nav_playlists -> showPlaylistScreen()
            R.id.nav_video -> showVideoScreen()
            else -> showVideoScreen()
        }
    }

    private fun showMoreScreen() {
        clearComposeScreenIfNeeded()
        val controller = moreController ?: MoreScreenController(activity).also { moreController = it }
        attachCompose(controller::Content)
        controller.onVisible()
        updateCheckedItem(R.id.nav_more)
        activity.invalidateOptionsMenu()
        currentDestinationId = R.id.nav_more
    }

    private fun showMainBrowserScreen() {
        clearComposeScreenIfNeeded()
        val controller = mainBrowserController
            ?: MainBrowserScreenController(activity, forExpresso).also { mainBrowserController = it }
        attachCompose(controller::Content)
        controller.onVisible()
        updateCheckedItem(R.id.nav_directories)
        activity.invalidateOptionsMenu()
        currentDestinationId = R.id.nav_directories
    }

    private fun showPlaylistScreen() {
        clearComposeScreenIfNeeded()
        val controller = playlistController ?: PlaylistScreenController(activity).also { playlistController = it }
        attachCompose(controller::Content)
        controller.onVisible()
        updateCheckedItem(R.id.nav_playlists)
        activity.invalidateOptionsMenu()
        currentDestinationId = R.id.nav_playlists
    }

    private fun showVideoScreen() {
        clearComposeScreenIfNeeded()
        val controller = videoController ?: VideoScreenController(activity).also { videoController = it }
        attachCompose(controller::Content)
        controller.onVisible()
        updateCheckedItem(R.id.nav_video)
        currentDestinationId = R.id.nav_video
        activity.invalidateOptionsMenu()
    }

    private fun showAudioScreen() {
        clearComposeScreenIfNeeded()
        val controller = audioController ?: AudioScreenController(activity).also { audioController = it }
        attachCompose(controller::Content)
        controller.onVisible()
        updateCheckedItem(R.id.nav_audio)
        currentDestinationId = R.id.nav_audio
        activity.invalidateOptionsMenu()
    }

    private fun attachCompose(content: @androidx.compose.runtime.Composable () -> Unit) {
        val container = activity.findViewById<ViewGroup>(R.id.content_placeholder)
        container.removeAllViews()
        container.addView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme { content() }
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun clearComposeScreenIfNeeded() {
        if (currentDestinationId != R.id.nav_more &&
            currentDestinationId != R.id.nav_directories &&
            currentDestinationId != R.id.nav_playlists &&
            currentDestinationId != R.id.nav_video &&
            currentDestinationId != R.id.nav_audio
        ) return
        moreController?.onHidden()
        mainBrowserController?.onHidden()
        playlistController?.onHidden()
        videoController?.onHidden()
        audioController?.onHidden()
        activity.findViewById<ViewGroup>(R.id.content_placeholder).removeAllViews()
    }

    override fun currentIdIsExtension() = currentDestinationId in 1..100

    override fun reloadPreferences() {
        currentDestinationId = settings.getInt(KEY_NAVIGATION_ID, defaultDestinationId)
        useSharedShell = settings.getBoolean(KEY_USE_SHARED_MAIN_SHELL, true)
    }

    override fun configurationChanged(size: Int) {
        val bottom = activity.findViewById<View>(R.id.navigation)
        val rail = activity.findViewById<View>(R.id.navigation_rail)
        if (activity.isTablet()) {
            bottom.setGone()
            rail.setVisible()
        } else {
            bottom.setVisible()
            rail.setGone()
        }
    }

    override fun getContentWidth(activity: Activity): Int {
        val screenWidth = activity.getScreenWidth()
        return screenWidth - activity.resources.getDimension(R.dimen.navigation_margin).toInt()
    }

    override fun refreshCurrentScreen(): Boolean {
        if (useSharedShell) {
            // StateFlows refresh from repositories automatically.
            return true
        }
        return when (currentDestinationId) {
            R.id.nav_more -> { moreController?.refresh(); true }
            R.id.nav_directories -> { mainBrowserController?.refresh(); true }
            R.id.nav_playlists -> { playlistController?.refresh(); true }
            R.id.nav_video -> { videoController?.refresh(); true }
            R.id.nav_audio -> { audioController?.refresh(); true }
            else -> false
        }
    }

    override fun prepareCurrentScreenOptions(menu: Menu) {
        if (useSharedShell) return
        if (currentDestinationId == R.id.nav_directories) mainBrowserController?.prepareOptionsMenu(menu)
        if (currentDestinationId == R.id.nav_playlists) playlistController?.prepareOptionsMenu(menu)
        if (currentDestinationId == R.id.nav_video) videoController?.prepareOptionsMenu(menu)
        if (currentDestinationId == R.id.nav_audio) audioController?.prepareOptionsMenu(menu)
    }

    override fun onCurrentScreenOptionsItemSelected(item: MenuItem): Boolean {
        if (useSharedShell) return false
        return when (currentDestinationId) {
            R.id.nav_directories -> mainBrowserController?.onOptionsItemSelected(item) == true
            R.id.nav_playlists -> playlistController?.onOptionsItemSelected(item) == true
            R.id.nav_video -> videoController?.onOptionsItemSelected(item) == true
            R.id.nav_audio -> audioController?.onOptionsItemSelected(item) == true
            else -> false
        }
    }

    private fun updateCheckedItem(id: Int) {
        navChrome?.select(id, notify = false)
        settings.edit { putInt(KEY_NAVIGATION_ID, id) }
    }
}

interface INavigator {
    var navigationViews: List<View>
    var appbarLayout: AppBarLayout
    var currentDestinationId: Int

    fun MainActivity.setupNavigation(state: Bundle?)
    fun currentIdIsExtension(): Boolean
    fun reloadPreferences()
    fun configurationChanged(size: Int)
    fun getContentWidth(activity: Activity): Int
    fun refreshCurrentScreen(): Boolean
    fun prepareCurrentScreenOptions(menu: Menu)
    fun onCurrentScreenOptionsItemSelected(item: MenuItem): Boolean
}
