/*
 * *************************************************************************
 *  SecondaryActivity.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
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

package org.videolan.vlc.gui

import org.videolan.vlc.gui.helpers.setVlcContent
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.launch
import org.videolan.libvlc.Dialog
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.resources.KEY_ANIMATED
import org.videolan.resources.TAG_ITEM
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.resources.util.parcelable
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.RESULT_RESCAN
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.Settings
import org.videolan.tools.copy
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.audio.SecondaryAudioScreenController
import org.videolan.vlc.gui.browser.SecondaryFileBrowserScreenController
import org.videolan.vlc.gui.browser.KEY_IN_MEDIALIB
import org.videolan.vlc.gui.browser.KEY_JUMP_TO
import org.videolan.vlc.gui.browser.KEY_MEDIA
import org.videolan.vlc.gui.browser.SecondaryStorageBrowserScreenController
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.reloadLibrary
import org.videolan.vlc.util.DialogDelegate
import org.videolan.vlc.util.IDialogManager
import org.videolan.vlc.util.Permissions

class SecondaryActivity : ContentActivity(), IDialogManager {

    private var audioAlbumsSongsController: SecondaryAudioScreenController? = null
    private var storageBrowserController: SecondaryStorageBrowserScreenController? = null
    private var fileBrowserController: SecondaryFileBrowserScreenController? = null
    override val displayTitle = true
    private val dialogsDelegate = DialogDelegate()
    val isOnboarding:Boolean
    get() {
        return intent.getStringExtra(KEY_DESTINATION) == STORAGE_BROWSER_ONBOARDING
    }


    override fun forcedTheme() =
        if (intent.getStringExtra(KEY_DESTINATION) == STORAGE_BROWSER_ONBOARDING) R.style.Theme_VLC_Onboarding
        else null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(android.R.id.content)) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
        setContentView(createSecondaryActivityShell())
        initAudioPlayerContainerActivity()

        if (isOnboarding) WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val contentContainer = findViewById<ViewGroup>(R.id.content_placeholder)
        val params = contentContainer.layoutParams as CoordinatorLayout.LayoutParams

        if (AndroidDevices.isTv) {
            applyOverscanMargin(this)
            params.topMargin = resources.getDimensionPixelSize(UiTools.getResourceFromAttribute(this, R.attr.actionBarSize))
        } else
            params.behavior = AppBarLayout.ScrollingViewBehavior()
        contentContainer.requestLayout()

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val destinationId = intent.getStringExtra(KEY_DESTINATION)
        if (destinationId == ALBUMS_SONGS) {
            setupAudioAlbumsSongsContent(contentContainer)
        } else if (destinationId == STORAGE_BROWSER || destinationId == STORAGE_BROWSER_ONBOARDING) {
            setupStorageBrowserContent(contentContainer, destinationId == STORAGE_BROWSER_ONBOARDING)
        } else if (destinationId == FILE_BROWSER) {
            setupFileBrowserContent(contentContainer)
        } else {
            finish()
            return
        }
        dialogsDelegate.observeDialogs(this, this)
        if (intent.getBooleanExtra(KEY_ANIMATED, false)) supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_up)
    }

    override fun onStart() {
        super.onStart()
        audioAlbumsSongsController?.onVisible()
        storageBrowserController?.onVisible()
        fileBrowserController?.onVisible()
    }

    override fun onStop() {
        audioAlbumsSongsController?.onHidden()
        storageBrowserController?.onHidden()
        fileBrowserController?.onHidden()
        super.onStop()
    }

    override fun fireDialog(dialog: Dialog) {
        DialogActivity.dialog = dialog
        startActivity(Intent(DialogActivity.KEY_DIALOG, null, this, DialogActivity::class.java))
    }

    override fun dialogCanceled(dialog: Dialog?) {}

    override fun onResume() {
        if (!intent.getBooleanExtra(KEY_ANIMATED, false)) overridePendingTransition(0, 0)
        super.onResume()
    }

    override fun onPause() {
        if (!intent.getBooleanExtra(KEY_ANIMATED, false) && isFinishing)
            overridePendingTransition(0, 0)
        super.onPause()
    }

    override fun finish() {
        super.finish()
        if (intent.getBooleanExtra(KEY_ANIMATED, false)) overridePendingTransition(R.anim.no_animation, R.anim.slide_out_bottom)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_RESULT_SECONDARY) {
            if (resultCode == RESULT_RESCAN) {
                this.reloadLibrary()
                audioAlbumsSongsController?.refresh()
                storageBrowserController?.refresh()
                fileBrowserController?.refresh()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        if (fileBrowserController != null) addNetworkOptionsMenu(menu)
        audioAlbumsSongsController?.prepareOptionsMenu(menu)
        storageBrowserController?.prepareOptionsMenu(menu)
        fileBrowserController?.prepareOptionsMenu(menu)
        return result
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.ml_menu_refresh)?.isVisible = Permissions.canReadStorage(this)
        menu?.findItem(R.id.incognito_mode)?.isChecked = Settings.getInstance(this).getBoolean(KEY_INCOGNITO, false)
        menu?.let { audioAlbumsSongsController?.prepareOptionsMenu(it) }
        menu?.let { storageBrowserController?.prepareOptionsMenu(it) }
        menu?.let { fileBrowserController?.prepareOptionsMenu(it) }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.ml_menu_refresh -> {
                if (Permissions.canReadStorage(this)) {
                    val ml = Medialibrary.getInstance()
                    if (!ml.isWorking) reloadLibrary()
                    audioAlbumsSongsController?.refresh()
                    storageBrowserController?.refresh()
                    fileBrowserController?.refresh()
                }
                return true
            }
            R.id.incognito_mode -> {
                lifecycleScope.launch {
                    if (!UiTools.updateIncognitoMode(this@SecondaryActivity, item)) return@launch
                }
                return true
            }
        }
        if (audioAlbumsSongsController?.onOptionsItemSelected(item) == true) return true
        if (storageBrowserController?.onOptionsItemSelected(item) == true) return true
        if (fileBrowserController?.onOptionsItemSelected(item) == true) return true
        return super.onOptionsItemSelected(item)
    }

    override fun hideRenderers() = intent.getStringExtra(KEY_DESTINATION) == STORAGE_BROWSER ||
            intent.getStringExtra(KEY_DESTINATION) == STORAGE_BROWSER_ONBOARDING ||
            fileBrowserController?.hideRenderers() == true

    private fun setupAudioAlbumsSongsContent(container: ViewGroup) {
        val media = intent.parcelable<MediaLibraryItem>(TAG_ITEM)
        if (media == null) {
            finish()
            return
        }
        val controller = audioAlbumsSongsController ?: SecondaryAudioScreenController(
            activity = this,
            parent = media,
            fromAlbums = intent.getBooleanExtra(HeaderMediaListActivity.ARTIST_FROM_ALBUM, false)
        ).also { audioAlbumsSongsController = it }

        container.removeAllViews()
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setVlcContent {
                VLCTheme {
                    controller.Content()
                }
            }
            container.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        controller.onVisible()
    }

    private fun setupStorageBrowserContent(container: ViewGroup, onboarding: Boolean) {
        setResult(RESULT_RESTART)
        val controller = storageBrowserController ?: SecondaryStorageBrowserScreenController(
            activity = this,
            onboarding = onboarding
        ).also { storageBrowserController = it }

        container.removeAllViews()
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setVlcContent {
                VLCTheme {
                    controller.Content()
                }
            }
            container.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        controller.onVisible()
    }

    private fun setupFileBrowserContent(container: ViewGroup) {
        val media = intent.parcelable<MediaWrapper>(KEY_MEDIA)
        if (media == null) {
            finish()
            return
        }
        val controller = fileBrowserController ?: SecondaryFileBrowserScreenController(
            activity = this,
            currentMedia = media,
            jumpTo = intent.parcelable(KEY_JUMP_TO),
            storageBrowser = intent.hasExtra(KEY_IN_MEDIALIB),
            scannedDirectory = intent.getBooleanExtra(KEY_IN_MEDIALIB, false)
        ).also { fileBrowserController = it }

        container.removeAllViews()
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setVlcContent {
                VLCTheme {
                    controller.Content()
                }
            }
            container.addView(this, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        controller.onVisible()
    }

    private fun addNetworkOptionsMenu(menu: Menu): MenuItem {
        return menu.findItem(R.id.ml_menu_save) ?: menu.add(Menu.NONE, R.id.ml_menu_save, 2, R.string.favorites_add).apply {
            setIcon(R.drawable.ic_fav_add)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }

    companion object {
        const val TAG = "VLC/SecondaryActivity"

        const val ACTIVITY_RESULT_SECONDARY = 3

        const val KEY_DESTINATION = "destination"

        const val ALBUMS_SONGS = "albumsSongs"
        const val STORAGE_BROWSER = "storage_browser"
        const val STORAGE_BROWSER_ONBOARDING = "storage_browser_onboarding"
        const val FILE_BROWSER = "file_browser"
    }
}
