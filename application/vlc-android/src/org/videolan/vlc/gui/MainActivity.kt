/*****************************************************************************
 * MainActivity.java
 *
 * Copyright © 2011-2019 VLC authors and VideoLAN
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
 */

package org.videolan.vlc.gui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.resources.ACTIVITY_RESULT_OPEN
import org.videolan.resources.ACTIVITY_RESULT_PREFERENCES
import org.videolan.resources.ACTIVITY_RESULT_SECONDARY
import org.videolan.resources.AndroidDevices
import org.videolan.resources.CRASH_HAPPENED
import org.videolan.resources.EXPORT_SETTINGS_FILE
import org.videolan.resources.EXTRA_TARGET
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.KEY_LAST_SESSION_CRASHED
import org.videolan.tools.KEY_MEDIALIBRARY_AUTO_RESCAN
import org.videolan.tools.KEY_OBSOLETE_RESTORE_FILE_WARNED
import org.videolan.tools.KEY_SHOW_UPDATE
import org.videolan.tools.PERMISSION_NEVER_ASK
import org.videolan.tools.PERMISSION_NEXT_ASK
import org.videolan.tools.RESULT_RESCAN
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.RESULT_RESTART_APP
import org.videolan.tools.RESULT_UPDATE_ARTISTS
import org.videolan.tools.RESULT_UPDATE_SEEN_MEDIA
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.StartActivity
import org.videolan.vlc.gui.dialogs.NotificationPermissionManager
import org.videolan.vlc.gui.dialogs.showSimpleComposeDialog
import org.videolan.vlc.gui.dialogs.showPermissionListComposeDialog
import org.videolan.vlc.gui.dialogs.showUpdateComposeDialog
import org.videolan.vlc.gui.helpers.INavigator
import org.videolan.vlc.gui.helpers.Navigator
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.isTablet
import org.videolan.vlc.gui.preferences.PreferencesActivity
import org.videolan.vlc.gui.preferences.search.PreferenceParser
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.reloadLibrary
import org.videolan.vlc.util.AutoUpdate
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.Util
import org.videolan.vlc.util.WhatsNewManager
import org.videolan.vlc.util.getScreenWidth
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "VLC/MainActivity"

class MainActivity : ContentActivity(),
        INavigator by Navigator()
{
    private lateinit var backPressedCallback: OnBackPressedCallback
    var refreshing: Boolean = false
    private lateinit var mediaLibrary: Medialibrary
    private var scanNeeded = false
    private lateinit var toolbarIcon: ImageView

    override fun getSnackAnchorView(overAudioPlayer:Boolean): View? {
        val view = super.getSnackAnchorView(overAudioPlayer)
        return if (view?.id == android.R.id.content && !isTablet()) {if(overAudioPlayer) findViewById(android.R.id.content) else findViewById(R.id.appbar)} else view
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Util.checkCpuCompatibility(this)
        /*** Start initializing the UI  */
        setContentView(createMainActivityShell())
        initAudioPlayerContainerActivity()
        setupNavigation(savedInstanceState)

        /* Set up the action bar */
        prepareActionBar()
        /* Reload the latest preferences */
        scanNeeded = savedInstanceState == null && settings.getBoolean(KEY_MEDIALIBRARY_AUTO_RESCAN, true)
        mediaLibrary = Medialibrary.getInstance()

        if (!NotificationPermissionManager.launchIfNeeded(this)) {
            if (!Settings.firstRun) WhatsNewManager.launchIfNeeded(this) else WhatsNewManager.markAsShown(settings)
        }

        lifecycleScope.launch {
            if (!BuildConfig.DEBUG) return@launch
            AutoUpdate.clean(this@MainActivity.application)
            if (!settings.getBoolean(KEY_SHOW_UPDATE, true)) return@launch
            if (!settings.contains(KEY_SHOW_UPDATE)) {
                showSimpleComposeDialog(
                    title = getString(R.string.update_nightly),
                    message = getString(R.string.update_nightly_alert),
                    confirmText = getString(R.string.yes),
                    dismissText = getString(R.string.no),
                    onConfirm = {
                        settings.putSingle(KEY_SHOW_UPDATE, true)
                    },
                    onDismiss = {
                        settings.putSingle(KEY_SHOW_UPDATE, false)
                    }
                )
                return@launch
            }
            AutoUpdate.checkUpdate(this@MainActivity.application) { url, date ->
                showUpdateComposeDialog(url, date)
            }
        }
        if (settings.getBoolean(KEY_LAST_SESSION_CRASHED, false)) {
            settings.putSingle(KEY_LAST_SESSION_CRASHED, false)
            if (BuildConfig.BETA) {
                showSimpleComposeDialog(
                    title = getString(R.string.report_crash),
                    message = getString(R.string.serious_crash),
                    confirmText = getString(R.string.send_log),
                    dismissText = getString(R.string.cancel),
                    onConfirm = {
                        startActivity(
                            Intent(applicationContext, FeedbackActivity::class.java)
                                .apply {
                                    putExtra(CRASH_HAPPENED, true)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                        )
                    }
                )

            }
        }
        if (!settings.getBoolean(KEY_OBSOLETE_RESTORE_FILE_WARNED, false)) {
            lifecycleScope.launch {
                val file = File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + EXPORT_SETTINGS_FILE)
                val fileExists = withContext(Dispatchers.IO) {
                    file.exists()
                }
                if (!fileExists) return@launch
                //check if file is restorable
                try {
                    PreferenceParser.checkRestoreFile(file.path.toUri())
                } catch (_: Exception) {
                    UiTools.snackerConfirm(this@MainActivity, getString(R.string.obsolete_restore_settings), confirmMessage = R.string.ok, indefinite = true) {
                        lifecycleScope.launch {
                            PreferencesActivity.launchWithPref(this@MainActivity, "export_settings")
                        }
                    }
                }
            }
        }
        settings.putSingle(KEY_OBSOLETE_RESTORE_FILE_WARNED, true)
        backPressedCallback = onBackPressedDispatcher.addCallback(enabled = true) {
            if (AndroidUtil.isNougatOrLater && isInMultiWindowMode) {
                UiTools.confirmExit(this@MainActivity)
                return@addCallback
            }
        }
        backPressedCallback.isEnabled = AndroidUtil.isNougatOrLater && isInMultiWindowMode
    }


    override fun onResume() {
        super.onResume()
        //Only the partial permission is granted for Android 11+
        if (!settings.getBoolean(PERMISSION_NEVER_ASK, false) && settings.getLong(PERMISSION_NEXT_ASK, 0L) < System.currentTimeMillis() && Permissions.canReadStorage(this) && !Permissions.hasAllAccess(this)) {
            UiTools.snackerMessageInfinite(this, getString(R.string.partial_content))?.setAction(R.string.more) {
                showPermissionListComposeDialog()
            }?.show()
            settings.putSingle(PERMISSION_NEXT_ASK, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2))
        }
        updateIncognitoModeIcon()
        configurationChanged(getScreenWidth())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Permissions.FINE_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                forceRefresh()
            }
        }
    }


    private fun prepareActionBar() {
        toolbarIcon = findViewById(R.id.toolbar_icon)
        updateIncognitoModeIcon()
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
            setDisplayShowTitleEnabled(false)
        }
    }

    override fun onStart() {
        super.onStart()
        if (mediaLibrary.isInitiated) {
            /* Load media items from database and storage */
            if (scanNeeded && Permissions.canReadStorage(this) && !mediaLibrary.isWorking) this.reloadLibrary()
        }
    }

    override fun onStop() {
        super.onStop()
        if (changingConfigurations == 0) {
            /* Check for an ongoing scan that needs to be resumed during onResume */
            scanNeeded = mediaLibrary.isWorking
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_TARGET, currentDestinationId)
        super.onSaveInstanceState(outState)
    }

    override fun onRestart() {
        super.onRestart()
        /* Reload the latest preferences */
        reloadPreferences()
    }

    override fun startSupportActionMode(callback: ActionMode.Callback): ActionMode? {
        appBarLayout.setExpanded(true)
        return super.startSupportActionMode(callback)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.ml_menu_refresh)?.isVisible = Permissions.canReadStorage(this)
        menu?.findItem(R.id.incognito_mode)?.isChecked = Settings.getInstance(this).getBoolean(KEY_INCOGNITO, false)
        menu?.let { prepareCurrentScreenOptions(it) }
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Handle onClick form menu buttons
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.ml_menu_filter) UiTools.setKeyboardVisibility(appBarLayout, false)
        if (onCurrentScreenOptionsItemSelected(item)) return true

        // Handle item selection
        return when (item.itemId) {
            // Refresh
            R.id.ml_menu_refresh -> {
                if (Permissions.canReadStorage(this)) forceRefresh()
                true
            }
            R.id.incognito_mode -> {
                lifecycleScope.launch {
                    if (!UiTools.updateIncognitoMode(this@MainActivity, item)) return@launch
                    updateIncognitoModeIcon()
                }
                true
            }
            android.R.id.home ->
                // Slide down the audio player or toggle the sidebar
                slideDownAudioPlayer()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateIncognitoModeIcon() {
        val incognito = Settings.getInstance (this).getBoolean(KEY_INCOGNITO, false)
        toolbarIcon.setImageDrawable(ContextCompat.getDrawable(this, if (incognito) R.drawable.ic_incognito else if (BuildConfig.DEBUG && BuildConfig.VLC_MAJOR_VERSION == 4) R.drawable.ic_icon_vlc4 else R.drawable.ic_icon))

    }

    fun forceRefresh() {
        if (refreshCurrentScreen()) return
        if (!mediaLibrary.isWorking) {
            reloadLibrary()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if (VLCBilling.getInstance(this.application).iabHelper.handleActivityResult(requestCode, resultCode, data)) return
        if (requestCode == ACTIVITY_RESULT_PREFERENCES) {
            when (resultCode) {
                RESULT_RESCAN -> this.reloadLibrary()
                RESULT_RESTART, RESULT_RESTART_APP -> {
                    val intent = Intent(this@MainActivity, if (resultCode == RESULT_RESTART_APP) StartActivity::class.java else MainActivity::class.java)
                    finish()
                    startActivity(intent)
                }
                RESULT_UPDATE_SEEN_MEDIA -> if (currentDestinationId == R.id.nav_video) refreshCurrentScreen()
                RESULT_UPDATE_ARTISTS -> {
                    if (currentDestinationId == R.id.nav_audio) refreshCurrentScreen()
                }
            }
        } else if (requestCode == ACTIVITY_RESULT_OPEN && resultCode == RESULT_OK) {
            MediaUtils.openUri(this, data!!.data)
        } else if (requestCode == ACTIVITY_RESULT_SECONDARY) {
            if (resultCode == RESULT_RESCAN) {
                forceRefresh()
            } else {
                scanNeeded = false
            }
        }
    }

    // Note. onKeyDown will not occur while moving within a list
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            val filterItem = toolbar.menu.findItem(R.id.ml_menu_filter)
            if (filterItem?.isVisible == true) filterItem.expandActionView()
            else startActivity(Intent(Intent.ACTION_SEARCH, null, this, SearchActivity::class.java))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
