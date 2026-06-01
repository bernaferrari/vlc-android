/*****************************************************************************
 * PreferencesActivity.java
 *
 * Copyright © 2011-2014 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.preferences

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.resources.ACTIVITY_RESULT_PREFERENCES
import org.videolan.resources.REMOTE_ACCESS_ONBOARDING
import org.videolan.resources.VLCInstance
import org.videolan.resources.util.parcelable
import org.videolan.resources.util.restartRemoteAccess
import org.videolan.resources.util.startRemoteAccess
import org.videolan.resources.util.stopRemoteAccess
import org.videolan.tools.AUDIO_RESUME_PLAYBACK
import org.videolan.tools.KEY_AUDIO_LAST_PLAYLIST
import org.videolan.tools.KEY_CURRENT_AUDIO
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_ARTIST
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_THUMB
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_TITLE
import org.videolan.tools.KEY_CURRENT_MEDIA
import org.videolan.tools.KEY_CURRENT_MEDIA_RESUME
import org.videolan.tools.KEY_MEDIA_LAST_PLAYLIST
import org.videolan.tools.KEY_MEDIA_LAST_PLAYLIST_RESUME
import org.videolan.tools.KEY_RESTRICT_SETTINGS
import org.videolan.tools.KEY_VIDEO_APP_SWITCH
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.RESULT_RESTART_APP
import org.videolan.tools.RESULT_UPDATE_ARTISTS
import org.videolan.tools.Settings
import org.videolan.tools.Settings.isPinCodeSet
import org.videolan.tools.VIDEO_RESUME_PLAYBACK
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.EqualizerSettingsActivity
import org.videolan.vlc.gui.PinCodeActivity
import org.videolan.vlc.gui.PinCodeReason
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.StartActivity
import org.videolan.vlc.gui.dialogs.showAutoInfoComposeDialog
import org.videolan.vlc.gui.dialogs.showAudioControlsSettingsComposeDialog
import org.videolan.vlc.gui.dialogs.showPermissionListComposeDialog
import org.videolan.vlc.gui.dialogs.showVideoControlsSettingsComposeDialog
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.restartMediaPlayer
import org.videolan.vlc.gui.preferences.search.PreferenceItem
import org.videolan.vlc.gui.preferences.search.PreferenceParser
import org.videolan.vlc.gui.preferences.search.PreferenceSearchActivity
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.widget.utils.refreshAllWidgets

const val EXTRA_PREF_END_POINT = "extra_pref_end_point"
private const val EXTRA_COMPOSE_PREF_DESTINATION = "extra_compose_pref_destination"
private const val EXTRA_COMPOSE_PREF_HIGHLIGHT = "extra_compose_pref_highlight"

class PreferencesActivity : BaseActivity() {

    private val searchRequestCode = 167
    private var mAppBarLayout: AppBarLayout? = null
    private var activeComposeDestination: PreferencesRootDestination? = null
    private var activeComposeHighlight: String? = null
    override val displayTitle = true
    private var pinCodeResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            finish()
        }
    }
    private var parentalPinCodeResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            showPreferenceSubpage(PreferencesRootDestination.ParentalControl)
        }
    }
    private var modifyPinCodeResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            UiTools.snacker(this, R.string.pin_code_modified)
        }
    }
    override fun getSnackAnchorView(overAudioPlayer:Boolean): View? = findViewById(android.R.id.content)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.getInstance(this).getBoolean(KEY_RESTRICT_SETTINGS, false)) {
            val intent = PinCodeActivity.getIntent(this, PinCodeReason.CHECK)
            pinCodeResult.launch(intent)
        }

        setContentView(R.layout.preferences_activity)
        setSupportActionBar(findViewById<View>(R.id.main_toolbar) as Toolbar)
        mAppBarLayout = findViewById(R.id.appbar)
        mAppBarLayout!!.post { ViewCompat.setElevation(mAppBarLayout!!, resources.getDimensionPixelSize(R.dimen.default_appbar_elevation).toFloat()) }
        onBackPressedDispatcher.addCallback(this) {
            if (!navigateUp()) finish()
        }
        if (savedInstanceState == null) {
            intent.parcelable<PreferenceItem>(EXTRA_PREF_END_POINT)?.let(::routePreferenceEndpoint) ?: showPreferencesRoot()
        } else {
            val restoredDestination = savedInstanceState.getString(EXTRA_COMPOSE_PREF_DESTINATION)?.let { destinationName ->
                runCatching { PreferencesRootDestination.valueOf(destinationName) }.getOrNull()
            }
            if (restoredDestination != null) {
                showPreferenceSubpage(
                    destination = restoredDestination,
                    highlightedKey = savedInstanceState.getString(EXTRA_COMPOSE_PREF_HIGHLIGHT)
                )
            } else if (supportFragmentManager.findFragmentById(R.id.fragment_placeholder) == null) {
                showPreferencesRoot()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        activeComposeDestination?.let { outState.putString(EXTRA_COMPOSE_PREF_DESTINATION, it.name) }
        activeComposeHighlight?.let { outState.putString(EXTRA_COMPOSE_PREF_HIGHLIGHT, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        refreshAllWidgets()
        super.onStop()
    }

    internal fun expandBar() {
        mAppBarLayout!!.setExpanded(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_prefs, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_android_auto_info)?.isVisible = activeComposeDestination == PreferencesRootDestination.AndroidAuto
        menu?.findItem(R.id.menu_remote_access_onboarding)?.isVisible = activeComposeDestination == PreferencesRootDestination.RemoteAccess
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (!navigateUp())
                    finish()
                return true
            }
            R.id.menu_pref_search -> {
                startActivityForResult(Intent(this, PreferenceSearchActivity::class.java), searchRequestCode)
            }
            R.id.menu_android_auto_info -> {
                if (activeComposeDestination == PreferencesRootDestination.AndroidAuto) {
                    showAutoInfoComposeDialog()
                    return true
                }
            }
            R.id.menu_remote_access_onboarding -> {
                if (activeComposeDestination == PreferencesRootDestination.RemoteAccess) {
                    openRemoteAccessOnboarding()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == searchRequestCode && resultCode == RESULT_OK) {
            data?.extras?.parcelable<PreferenceItem>(EXTRA_PREF_END_POINT)?.let {
                supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                routePreferenceEndpoint(it)
            }
        }

    }

    private fun showPreferencesRoot(highlightedKey: String? = null) {
        expandBar()
        activeComposeDestination = null
        activeComposeHighlight = highlightedKey
        supportActionBar?.title = getString(R.string.preferences)
        findViewById<ViewGroup>(R.id.fragment_placeholder).apply {
            removeAllViews()
            addView(
                ComposeView(this@PreferencesActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        val settings = Settings.getInstance(this@PreferencesActivity)
                        PreferencesRootScreen(
                            settings = settings,
                            highlightedKey = highlightedKey,
                            isVisible = { PreferenceVisibilityManager.isPreferenceVisible(it, settings, false) },
                            onDirectoriesClick = ::openDirectories,
                            onPermissionClick = { showPermissionListComposeDialog() },
                            onEqualizerClick = { startActivity(Intent(applicationContext, EqualizerSettingsActivity::class.java)) },
                            onCategoryClick = ::openRootDestination,
                            onPlaybackHistoryDisabled = ::disablePlaybackHistory,
                            onAudioResumeDisabled = ::disableAudioResumePlayback,
                            onVideoResumeChanged = ::onVideoResumePlaybackChanged,
                            onVideoActionSwitchChanged = ::onVideoActionSwitchChanged
                        )
                    }
                }
            )
        }
        invalidateOptionsMenu()
    }

    private fun openDirectories() {
        if (Medialibrary.getInstance().isWorking) {
            UiTools.snacker(this, getString(R.string.settings_ml_block_scan))
        } else {
            val intent = Intent(applicationContext, SecondaryActivity::class.java)
            intent.putExtra("fragment", SecondaryActivity.STORAGE_BROWSER)
            startActivity(intent)
            setResult(RESULT_RESTART)
        }
    }

    private fun openRootDestination(destination: PreferencesRootDestination) {
        when (destination) {
            PreferencesRootDestination.Ui -> openPreferenceFragment(PreferencesUi())
            PreferencesRootDestination.Video -> openPreferenceFragment(PreferencesVideo())
            PreferencesRootDestination.Subtitles -> openPreferenceFragment(PreferencesSubtitles())
            PreferencesRootDestination.Audio -> openPreferenceFragment(PreferencesAudio())
            PreferencesRootDestination.Casting -> showPreferenceSubpage(PreferencesRootDestination.Casting)
            PreferencesRootDestination.ParentalControl -> {
                if (isPinCodeSet()) {
                    showPreferenceSubpage(PreferencesRootDestination.ParentalControl)
                } else {
                    parentalPinCodeResult.launch(PinCodeActivity.getIntent(this, PinCodeReason.FIRST_CREATION))
                }
            }
            PreferencesRootDestination.RemoteAccess -> showPreferenceSubpage(PreferencesRootDestination.RemoteAccess)
            PreferencesRootDestination.AndroidAuto -> showPreferenceSubpage(PreferencesRootDestination.AndroidAuto)
            PreferencesRootDestination.Advanced -> openPreferenceFragment(PreferencesAdvanced())
        }
    }

    private fun routePreferenceEndpoint(endPoint: PreferenceItem) {
        when (endPoint.parentScreen) {
            PreferenceParser.VIDEO_CONTROLS_PARENT_SCREEN -> {
                showPreferencesRoot(endPoint.key)
                showVideoControlsSettingsComposeDialog()
            }
            PreferenceParser.AUDIO_CONTROLS_PARENT_SCREEN -> {
                showPreferencesRoot(endPoint.key)
                showAudioControlsSettingsComposeDialog()
            }
            R.xml.preferences_ui -> openPreferenceFragment(PreferencesUi().withEndpoint(endPoint))
            R.xml.preferences_video -> openPreferenceFragment(PreferencesVideo().withEndpoint(endPoint))
            R.xml.preferences_subtitles -> openPreferenceFragment(PreferencesSubtitles().withEndpoint(endPoint))
            R.xml.preferences_audio -> openPreferenceFragment(PreferencesAudio().withEndpoint(endPoint))
            R.xml.preferences_adv -> openPreferenceFragment(PreferencesAdvanced().withEndpoint(endPoint))
            R.xml.preferences_casting -> showPreferenceSubpage(PreferencesRootDestination.Casting, endPoint.key)
            R.xml.preferences_parental_control -> showPreferenceSubpage(PreferencesRootDestination.ParentalControl, endPoint.key)
            R.xml.preferences_remote_access -> showPreferenceSubpage(PreferencesRootDestination.RemoteAccess, endPoint.key)
            R.xml.preferences_android_auto -> showPreferenceSubpage(PreferencesRootDestination.AndroidAuto, endPoint.key)
            else -> showPreferencesRoot(endPoint.key)
        }
    }

    private fun BasePreferenceFragment.withEndpoint(endPoint: PreferenceItem): BasePreferenceFragment {
        arguments = bundleOf(EXTRA_PREF_END_POINT to endPoint)
        return this
    }

    private fun showPreferenceSubpage(destination: PreferencesRootDestination, highlightedKey: String? = null) {
        expandBar()
        activeComposeDestination = destination
        activeComposeHighlight = highlightedKey
        supportActionBar?.title = getString(destination.titleRes())
        if (destination == PreferencesRootDestination.RemoteAccess) showRemoteAccessOnboardingIfNeeded()
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        findViewById<ViewGroup>(R.id.fragment_placeholder).apply {
            removeAllViews()
            addView(
                ComposeView(this@PreferencesActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        val settings = Settings.getInstance(this@PreferencesActivity)
                        PreferencesComposeSubpageScreen(
                            settings = settings,
                            destination = destination,
                            highlightedKey = highlightedKey,
                            isPinCodeSet = { this@PreferencesActivity.isPinCodeSet() },
                            onModifyPinCodeClick = {
                                modifyPinCodeResult.launch(PinCodeActivity.getIntent(this@PreferencesActivity, PinCodeReason.MODIFY))
                            },
                            onSafeModeChanged = { Settings.safeMode = it },
                            onRestartAppRequired = ::setRestartApp,
                            onRestartCastingPipeline = ::restartCastingPipeline,
                            onAndroidAutoSettingChanged = ::updateAndroidAutoState,
                            onPlaybackSpeedGlobalChanged = ::onPlaybackSpeedGlobalChanged,
                            onRemoteAccessStatusClick = ::openRemoteAccessStatus,
                            onRemoteAccessEnabledChanged = ::onRemoteAccessEnabledChanged,
                            onRemoteAccessNetworkBrowserChanged = ::restartRemoteAccessServer
                        )
                    }
                }
            )
        }
        invalidateOptionsMenu()
    }

    private fun openPreferenceFragment(fragment: BasePreferenceFragment) {
        activeComposeDestination = null
        activeComposeHighlight = null
        invalidateOptionsMenu()
        findViewById<ViewGroup>(R.id.fragment_placeholder).removeAllViews()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_placeholder, fragment)
            .addToBackStack("main")
            .commit()
    }

    private fun navigateUp(): Boolean {
        if (activeComposeDestination != null) {
            showPreferencesRoot()
            return true
        }
        if (supportFragmentManager.popBackStackImmediate()) {
            if (supportFragmentManager.findFragmentById(R.id.fragment_placeholder) == null) {
                showPreferencesRoot()
            }
            return true
        }
        return false
    }

    private fun disablePlaybackHistory() {
        Settings.getInstance(this).edit()
            .putBoolean(PLAYBACK_HISTORY, false)
            .apply()
        setResult(RESULT_RESTART)
    }

    private fun disableAudioResumePlayback() {
        Settings.getInstance(this).edit()
            .remove(KEY_AUDIO_LAST_PLAYLIST)
            .remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
            .remove(KEY_CURRENT_AUDIO_RESUME_TITLE)
            .remove(KEY_CURRENT_AUDIO_RESUME_ARTIST)
            .remove(KEY_CURRENT_AUDIO_RESUME_THUMB)
            .remove(KEY_CURRENT_AUDIO)
            .remove(KEY_CURRENT_MEDIA)
            .remove(KEY_CURRENT_MEDIA_RESUME)
            .putBoolean(AUDIO_RESUME_PLAYBACK, false)
            .apply()
        setResult(RESULT_RESTART)
    }

    private fun onVideoResumePlaybackChanged(enabled: Boolean) {
        if (!enabled) {
            Settings.getInstance(this).edit()
                .remove(KEY_MEDIA_LAST_PLAYLIST)
                .remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
                .remove(KEY_CURRENT_MEDIA_RESUME)
                .remove(KEY_CURRENT_MEDIA)
                .putBoolean(VIDEO_RESUME_PLAYBACK, false)
                .apply()
            setResult(RESULT_RESTART)
        }
    }

    private fun onVideoActionSwitchChanged(value: String) {
        if (!AndroidUtil.isOOrLater && value == "2" && !Permissions.canDrawOverlays(this)) {
            Permissions.checkDrawOverlaysPermission(this)
        }
        Settings.getInstance(this).edit().putString(KEY_VIDEO_APP_SWITCH, value).apply()
    }

    private fun restartCastingPipeline() {
        lifecycleScope.launch {
            VLCInstance.restart()
            restartMediaPlayer()
        }
    }

    private fun updateAndroidAutoState() {
        PlaybackService.updateState()
    }

    private fun onPlaybackSpeedGlobalChanged() {
        PlaybackService.instance?.let { service ->
            service.playlistManager.getCurrentMedia()?.let { currentMedia ->
                service.playlistManager.restoreSpeed(currentMedia)
            }
        }
        PlaybackService.updateState()
    }

    private fun showRemoteAccessOnboardingIfNeeded() {
        val settings = Settings.getInstance(this)
        if (!settings.getBoolean(REMOTE_ACCESS_ONBOARDING, false)) {
            settings.edit().putBoolean(REMOTE_ACCESS_ONBOARDING, true).apply()
            openRemoteAccessOnboarding()
        }
    }

    private fun openRemoteAccessOnboarding() {
        startActivity(Intent(Intent.ACTION_VIEW).apply { setClassName(this@PreferencesActivity, REMOTE_ACCESS_ONBOARDING) })
    }

    private fun openRemoteAccessStatus() {
        startActivity(Intent(this, StartActivity::class.java).apply { action = "vlc.remoteaccess.share" })
    }

    private fun onRemoteAccessEnabledChanged(enabled: Boolean) {
        if (enabled) {
            startRemoteAccess()
        } else {
            stopRemoteAccess()
        }
    }

    private fun restartRemoteAccessServer() {
        restartRemoteAccess()
    }

    private fun PreferencesRootDestination.titleRes(): Int = when (this) {
        PreferencesRootDestination.Ui -> R.string.interface_prefs_screen
        PreferencesRootDestination.Video -> R.string.video_prefs_category
        PreferencesRootDestination.Subtitles -> R.string.subtitles_prefs_category
        PreferencesRootDestination.Audio -> R.string.audio_prefs_category
        PreferencesRootDestination.Casting -> R.string.casting_category
        PreferencesRootDestination.ParentalControl -> R.string.parental_control
        PreferencesRootDestination.RemoteAccess -> R.string.remote_access
        PreferencesRootDestination.AndroidAuto -> R.string.android_auto
        PreferencesRootDestination.Advanced -> R.string.advanced_prefs_category
    }

    fun exitAndRescan() {
        setRestart()
        val intent = intent
        finish()
        startActivity(intent)
    }

    fun setRestart() {
        setResult(RESULT_RESTART)
    }

    fun setRestartApp() {
        setResult(RESULT_RESTART_APP)
    }

    fun updateArtists() {
        setResult(RESULT_UPDATE_ARTISTS)
    }

    fun detectHeadset(detect: Boolean) {
        val le = PlaybackService.headSetDetection
        if (le.hasObservers()) le.value = detect
    }

    companion object {
        /**
         * Launch the preferences and redirect to a given preference
         * @param activity The calling activity
         * @param prefKey The preference key to redirect to
         * @throws NoSuchElementException if the key is not found
         */
        suspend fun launchWithPref(activity: FragmentActivity, prefKey:String) {
            val pref = withContext(Dispatchers.IO) {
                PreferenceParser.parsePreferences(activity, true)
            }.first { it.key == prefKey }
            when (pref.parentScreen) {
                PreferenceParser.VIDEO_CONTROLS_PARENT_SCREEN -> {
                    activity.showVideoControlsSettingsComposeDialog()
                    return
                }
                PreferenceParser.AUDIO_CONTROLS_PARENT_SCREEN -> {
                    activity.showAudioControlsSettingsComposeDialog()
                    return
                }
            }
            val intent = Intent(activity, PreferencesActivity::class.java)
            intent.putExtra(EXTRA_PREF_END_POINT, pref)
            activity.startActivityForResult(intent, ACTIVITY_RESULT_PREFERENCES)
        }
    }
}
