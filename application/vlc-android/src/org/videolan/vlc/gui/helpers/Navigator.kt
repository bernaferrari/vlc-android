/*
 * *************************************************************************
 *  Navigator.kt
 * **************************************************************************
 *  Copyright © 2018-2019 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.helpers

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.EXTRA_FOR_ESPRESSO
import org.videolan.resources.EXTRA_TARGET
import org.videolan.resources.ID_AUDIO
import org.videolan.resources.ID_DIRECTORIES
import org.videolan.resources.ID_VIDEO
import org.videolan.resources.util.parcelableList
import org.videolan.tools.KEY_FRAGMENT_ID
import org.videolan.tools.isStarted
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.gui.BaseFragment
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.MoreScreenController
import org.videolan.vlc.gui.PlaylistScreenController
import org.videolan.vlc.gui.audio.AudioScreenController
import org.videolan.vlc.gui.browser.BaseBrowserFragment
import org.videolan.vlc.gui.browser.MainBrowserScreenController
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.helpers.UiTools.isTablet
import org.videolan.vlc.gui.video.VideoScreenController
import org.videolan.vlc.util.getScreenWidth

private const val TAG = "Navigator"

class Navigator : NavigationBarView.OnItemSelectedListener, DefaultLifecycleObserver, INavigator {

    private val defaultFragmentId = R.id.nav_video
    override var currentFragmentId: Int = 0
    private var currentFragment: Fragment? = null
    private lateinit var activity: MainActivity
    private lateinit var settings: SharedPreferences
    override lateinit var navigationView: List<NavigationBarView>
    override lateinit var appbarLayout: AppBarLayout
    private var forExpresso: ArrayList<MediaLibraryItem>? = null
    private var moreController: MoreScreenController? = null
    private var mainBrowserController: MainBrowserScreenController? = null
    private var playlistController: PlaylistScreenController? = null
    private var videoController: VideoScreenController? = null
    private var audioController: AudioScreenController? = null


    override fun MainActivity.setupNavigation(state: Bundle?) {
        activity = this
        this@Navigator.settings = settings
        forExpresso = intent.parcelableList(EXTRA_FOR_ESPRESSO)
        currentFragmentId = intent.getIntExtra(EXTRA_TARGET, 0)
        if (state?.containsKey(KEY_CURRENT_FRAGMENT) == true) {
            currentFragment = supportFragmentManager.getFragment(state, KEY_CURRENT_FRAGMENT)
        }
        lifecycle.addObserver(this@Navigator)
        navigationView = listOf(findViewById(R.id.navigation), findViewById(R.id.navigation_rail))
        appbarLayout = findViewById(R.id.appbar)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (currentFragment === null && !currentIdIsExtension()) showScreen(if (currentFragmentId != 0) currentFragmentId else settings.getInt(KEY_FRAGMENT_ID, defaultFragmentId))
        navigationView.forEach { it.setOnItemSelectedListener(this) }
    }

    override fun onStop(owner: LifecycleOwner) {
        navigationView.forEach { it.setOnItemSelectedListener(null) }
    }

    private fun getNewFragment(id: Int): Fragment {
        return when (id) {
            else -> error("Unsupported main fragment id $id")
        }
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

    private fun showFragment(id: Int) {
        val tag = getTag(id)
        val fragment = getNewFragment(id)
        showFragment(fragment, id, tag)
    }

    private fun showFragment(fragment: Fragment, id: Int, tag: String = getTag(id)) {
        val fm = activity.supportFragmentManager
        if (currentFragment is BaseBrowserFragment) fm.popBackStackImmediate("root", FragmentManager.POP_BACK_STACK_INCLUSIVE)
        clearComposeScreenIfNeeded()
        val ft = fm.beginTransaction()
        ft.replace(R.id.fragment_placeholder, fragment, tag)
        if (BuildConfig.DEBUG) ft.commit()
        else ft.commitAllowingStateLoss()
        updateCheckedItem(id)
        currentFragment = fragment
        currentFragmentId = id
    }

    private fun clearCurrentFragmentNow() {
        currentFragment?.let { fragment ->
            val ft = activity.supportFragmentManager.beginTransaction().remove(fragment)
            if (BuildConfig.DEBUG) ft.commitNow()
            else ft.commitNowAllowingStateLoss()
        }
    }

    private fun showMoreScreen() {
        val fm = activity.supportFragmentManager
        if (currentFragment is BaseBrowserFragment) fm.popBackStackImmediate("root", FragmentManager.POP_BACK_STACK_INCLUSIVE)
        clearComposeScreenIfNeeded()
        clearCurrentFragmentNow()

        val controller = moreController ?: MoreScreenController(activity).also { moreController = it }
        val container = activity.findViewById<ViewGroup>(R.id.fragment_placeholder)
        container.removeAllViews()
        container.addView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme {
                        controller.Content()
                    }
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        controller.onVisible()
        updateCheckedItem(R.id.nav_more)
        activity.invalidateOptionsMenu()
        currentFragment = null
        currentFragmentId = R.id.nav_more
    }

    private fun showMainBrowserScreen() {
        val fm = activity.supportFragmentManager
        if (currentFragment is BaseBrowserFragment) fm.popBackStackImmediate("root", FragmentManager.POP_BACK_STACK_INCLUSIVE)
        clearComposeScreenIfNeeded()
        clearCurrentFragmentNow()

        val controller = mainBrowserController ?: MainBrowserScreenController(activity, forExpresso).also { mainBrowserController = it }
        val container = activity.findViewById<ViewGroup>(R.id.fragment_placeholder)
        container.removeAllViews()
        container.addView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme {
                        controller.Content()
                    }
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        controller.onVisible()
        updateCheckedItem(R.id.nav_directories)
        activity.invalidateOptionsMenu()
        currentFragment = null
        currentFragmentId = R.id.nav_directories
    }

    private fun showPlaylistScreen() {
        val fm = activity.supportFragmentManager
        if (currentFragment is BaseBrowserFragment) fm.popBackStackImmediate("root", FragmentManager.POP_BACK_STACK_INCLUSIVE)
        clearComposeScreenIfNeeded()
        clearCurrentFragmentNow()

        val controller = playlistController ?: PlaylistScreenController(activity).also { playlistController = it }
        val container = activity.findViewById<ViewGroup>(R.id.fragment_placeholder)
        container.removeAllViews()
        container.addView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme {
                        controller.Content()
                    }
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        controller.onVisible()
        updateCheckedItem(R.id.nav_playlists)
        activity.invalidateOptionsMenu()
        currentFragment = null
        currentFragmentId = R.id.nav_playlists
    }

    private fun showVideoScreen() {
        val fm = activity.supportFragmentManager
        if (currentFragment is BaseBrowserFragment) fm.popBackStackImmediate("root", FragmentManager.POP_BACK_STACK_INCLUSIVE)
        clearComposeScreenIfNeeded()
        clearCurrentFragmentNow()

        val controller = videoController ?: VideoScreenController(activity).also { videoController = it }
        val container = activity.findViewById<ViewGroup>(R.id.fragment_placeholder)
        container.removeAllViews()
        container.addView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme {
                        controller.Content()
                    }
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        controller.onVisible()
        updateCheckedItem(R.id.nav_video)
        currentFragment = null
        currentFragmentId = R.id.nav_video
        activity.invalidateOptionsMenu()
    }

    private fun showAudioScreen() {
        val fm = activity.supportFragmentManager
        if (currentFragment is BaseBrowserFragment) fm.popBackStackImmediate("root", FragmentManager.POP_BACK_STACK_INCLUSIVE)
        clearComposeScreenIfNeeded()
        clearCurrentFragmentNow()

        val controller = audioController ?: AudioScreenController(activity).also { audioController = it }
        val container = activity.findViewById<ViewGroup>(R.id.fragment_placeholder)
        container.removeAllViews()
        container.addView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme {
                        controller.Content()
                    }
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        controller.onVisible()
        updateCheckedItem(R.id.nav_audio)
        currentFragment = null
        currentFragmentId = R.id.nav_audio
        activity.invalidateOptionsMenu()
    }

    private fun clearComposeScreenIfNeeded() {
        if (currentFragmentId != R.id.nav_more && currentFragmentId != R.id.nav_directories && currentFragmentId != R.id.nav_playlists && currentFragmentId != R.id.nav_video && currentFragmentId != R.id.nav_audio) return
        moreController?.onHidden()
        mainBrowserController?.onHidden()
        playlistController?.onHidden()
        videoController?.onHidden()
        audioController?.onHidden()
        activity.findViewById<ViewGroup>(R.id.fragment_placeholder).removeAllViews()
    }

    override fun currentIdIsExtension() = idIsExtension(currentFragmentId)

    private fun idIsExtension(id: Int) = id in 1..100

    override fun reloadPreferences() {
        currentFragmentId = settings.getInt(KEY_FRAGMENT_ID, defaultFragmentId)
    }

    override fun configurationChanged(size: Int) {
        navigationView.forEach {
            when (it) {
                is BottomNavigationView -> if (activity.isTablet()) it.setGone() else it.setVisible()
                else -> if (!activity.isTablet()) it.setGone() else it.setVisible()
            }
        }
    }

    override fun getFragmentWidth(activity: Activity): Int {
        val screenWidth = activity.getScreenWidth()
        return screenWidth - activity.resources.getDimension(R.dimen.navigation_margin).toInt()
    }

    private fun getTag(id: Int) = when (id) {
        R.id.nav_audio -> ID_AUDIO
        R.id.nav_directories -> ID_DIRECTORIES
        else -> ID_VIDEO
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val current = currentFragment

        appbarLayout.setExpanded(true, true)

        if (current == null && !currentIdIsComposeScreen()) {
            return false
        }
        if (current is BaseFragment && current.actionMode != null) current.stopActionMode()

        if (currentFragmentId == id) { /* Already selected */
            // Go back at root level of current mProvider
            if ((current as? BaseBrowserFragment)?.isStarted() == false) {
                activity.supportFragmentManager.popBackStackImmediate("root", FragmentManager.POP_BACK_STACK_INCLUSIVE)
            } else {
                return false
            }
        } else {
            activity.slideDownAudioPlayer()
            showScreen(id)
        }
        return true
    }

    override fun refreshCurrentScreen(): Boolean {
        return when (currentFragmentId) {
            R.id.nav_more -> {
                moreController?.refresh()
                true
            }
            R.id.nav_directories -> {
                mainBrowserController?.refresh()
                true
            }
            R.id.nav_playlists -> {
                playlistController?.refresh()
                true
            }
            R.id.nav_video -> {
                videoController?.refresh()
                true
            }
            R.id.nav_audio -> {
                audioController?.refresh()
                true
            }
            else -> false
        }
    }

    override fun prepareCurrentScreenOptions(menu: Menu) {
        if (currentFragmentId == R.id.nav_directories) mainBrowserController?.prepareOptionsMenu(menu)
        if (currentFragmentId == R.id.nav_playlists) playlistController?.prepareOptionsMenu(menu)
        if (currentFragmentId == R.id.nav_video) videoController?.prepareOptionsMenu(menu)
        if (currentFragmentId == R.id.nav_audio) audioController?.prepareOptionsMenu(menu)
    }

    override fun onCurrentScreenOptionsItemSelected(item: MenuItem): Boolean {
        return when (currentFragmentId) {
            R.id.nav_directories -> mainBrowserController?.onOptionsItemSelected(item) == true
            R.id.nav_playlists -> playlistController?.onOptionsItemSelected(item) == true
            R.id.nav_video -> videoController?.onOptionsItemSelected(item) == true
            R.id.nav_audio -> audioController?.onOptionsItemSelected(item) == true
            else -> false
        }
    }

    private fun currentIdIsComposeScreen() = currentFragmentId == R.id.nav_more || currentFragmentId == R.id.nav_directories || currentFragmentId == R.id.nav_playlists || currentFragmentId == R.id.nav_video || currentFragmentId == R.id.nav_audio


    private fun updateCheckedItem(id: Int) {
        val currentId = currentFragmentId
        navigationView.forEach {
            val target = it.menu.findItem(id)
            if (id != it.selectedItemId && target != null) {
                val current = it.menu.findItem(currentId)
                if (current != null) current.isChecked = false
                target.isChecked = true
                /* Save the tab status in pref */
                settings.edit { putInt(KEY_FRAGMENT_ID, id) }
            }
        }
    }

}

interface INavigator {
    var navigationView: List<NavigationBarView>
    var appbarLayout: AppBarLayout
    var currentFragmentId: Int

    fun MainActivity.setupNavigation(state: Bundle?)
    fun currentIdIsExtension(): Boolean
    fun reloadPreferences()
    fun configurationChanged(size: Int)
    fun getFragmentWidth(activity: Activity): Int
    fun refreshCurrentScreen(): Boolean
    fun prepareCurrentScreenOptions(menu: Menu)
    fun onCurrentScreenOptionsItemSelected(item: MenuItem): Boolean
}

private const val KEY_CURRENT_FRAGMENT = "current_fragment"
