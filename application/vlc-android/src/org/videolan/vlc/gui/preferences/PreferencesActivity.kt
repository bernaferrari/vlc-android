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
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.resources.ACTIVITY_RESULT_PREFERENCES
import org.videolan.resources.AndroidDevices
import org.videolan.resources.EXPORT_SETTINGS_FILE
import org.videolan.resources.REMOTE_ACCESS_ONBOARDING
import org.videolan.resources.ROOM_DATABASE
import org.videolan.resources.SCHEME_PACKAGE
import org.videolan.resources.VLCInstance
import org.videolan.resources.util.parcelable
import org.videolan.resources.util.restartRemoteAccess
import org.videolan.resources.util.startRemoteAccess
import org.videolan.resources.util.stopRemoteAccess
import org.videolan.tools.AUDIO_RESUME_PLAYBACK
import org.videolan.tools.BitmapCache
import org.videolan.tools.DAV1D_THREAD_NUMBER
import org.videolan.tools.KEY_APP_THEME
import org.videolan.tools.KEY_AUDIO_DIGITAL_OUTPUT
import org.videolan.tools.KEY_AUDIO_LAST_PLAYLIST
import org.videolan.tools.KEY_BLACK_THEME
import org.videolan.tools.KEY_CUSTOM_LIBVLC_OPTIONS
import org.videolan.tools.KEY_CURRENT_AUDIO
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_ARTIST
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_THUMB
import org.videolan.tools.KEY_CURRENT_AUDIO_RESUME_TITLE
import org.videolan.tools.KEY_CURRENT_MEDIA
import org.videolan.tools.KEY_CURRENT_MEDIA_RESUME
import org.videolan.tools.KEY_DAYNIGHT
import org.videolan.tools.KEY_MEDIA_LAST_PLAYLIST
import org.videolan.tools.KEY_MEDIA_LAST_PLAYLIST_RESUME
import org.videolan.tools.KEY_NETWORK_CACHING_VALUE
import org.videolan.tools.KEY_RESTRICT_SETTINGS
import org.videolan.tools.KEY_VIDEO_APP_SWITCH
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.PREF_SHOW_VIDEO_SETTINGS_DISCLAIMER
import org.videolan.tools.POPUP_FORCE_LEGACY
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.RESULT_RESTART_APP
import org.videolan.tools.RESULT_UPDATE_ARTISTS
import org.videolan.tools.RESULT_UPDATE_SEEN_MEDIA
import org.videolan.tools.Settings
import org.videolan.tools.Settings.isPinCodeSet
import org.videolan.tools.VIDEO_RESUME_PLAYBACK
import org.videolan.tools.putSingle
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.StartActivity
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.DebugLogActivity
import org.videolan.vlc.gui.EqualizerSettingsActivity
import org.videolan.vlc.gui.PinCodeActivity
import org.videolan.vlc.gui.PinCodeReason
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.browser.EXTRA_MRL
import org.videolan.vlc.gui.browser.FilePickerActivity
import org.videolan.vlc.gui.browser.KEY_PICKER_TYPE
import org.videolan.vlc.gui.dialogs.showAutoInfoComposeDialog
import org.videolan.vlc.gui.dialogs.showAudioControlsSettingsComposeDialog
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.dialogs.showFeatureFlagWarningComposeDialog
import org.videolan.vlc.gui.dialogs.showPermissionListComposeDialog
import org.videolan.vlc.gui.dialogs.showSleepTimerComposeDialog
import org.videolan.vlc.gui.dialogs.showUpdateComposeDialog
import org.videolan.vlc.gui.dialogs.showVideoControlsSettingsComposeDialog
import org.videolan.vlc.gui.helpers.MedialibraryUtils
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.hf.StoragePermissionsDelegate.Companion.getWritePermission
import org.videolan.vlc.gui.helpers.restartMediaPlayer
import org.videolan.vlc.gui.preferences.search.PreferenceItem
import org.videolan.vlc.gui.preferences.search.PreferenceParser
import org.videolan.vlc.gui.preferences.search.PreferenceSearchActivity
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.providers.PickerType
import org.videolan.vlc.util.AutoUpdate
import org.videolan.vlc.util.FeatureFlag
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.share
import org.videolan.vlc.widget.utils.refreshAllWidgets
import java.io.File
import java.io.IOException

const val EXTRA_PREF_END_POINT = "extra_pref_end_point"
private const val EXTRA_COMPOSE_PREF_DESTINATION = "extra_compose_pref_destination"
private const val EXTRA_COMPOSE_PREF_HIGHLIGHT = "extra_compose_pref_highlight"
private const val KEY_NETWORK_CACHING = "network_caching"
private const val RESULT_VALUE_CLEAR_HISTORY = 1
private const val RESULT_VALUE_CLEAR_MEDIA_DATABASE = 2
private const val RESULT_VALUE_CLEAR_APP_DATA = 3

open class PreferencesActivity : BaseActivity() {

    private val searchRequestCode = 167
    private var mAppBarLayout: AppBarLayout? = null
    private var activeComposeDestination: PreferencesRootDestination? = null
    private var activeComposeHighlight: String? = null
    private var pendingSubtitlesRestart = false
    private var needToRestartOnResume = false
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
    private var soundFontResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val soundFontMrl = result.data?.getStringExtra(EXTRA_MRL) ?: return@registerForActivityResult
        lifecycleScope.launch {
            MediaUtils.useAsSoundFont(this@PreferencesActivity, soundFontMrl.toUri())
            VLCInstance.restart()
        }
        UiTools.restartDialog(this)
    }
    private var settingsRestoreResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val settingsMrl = result.data?.getStringExtra(EXTRA_MRL) ?: return@registerForActivityResult
        restoreSettingsFrom(settingsMrl.toUri())
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
            intent.parcelable<PreferenceItem>(EXTRA_PREF_END_POINT)?.let(::routePreferenceEndpoint)
                ?: intent.getStringExtra(EXTRA_PREF_END_POINT)?.let(::routeLegacyPreferenceEndpoint)
                ?: showPreferencesRoot()
        } else {
            val restoredDestination = savedInstanceState.getString(EXTRA_COMPOSE_PREF_DESTINATION)?.let { destinationName ->
                runCatching { PreferencesRootDestination.valueOf(destinationName) }.getOrNull()
            }
            if (restoredDestination != null) {
                showPreferenceSubpage(
                    destination = restoredDestination,
                    highlightedKey = savedInstanceState.getString(EXTRA_COMPOSE_PREF_HIGHLIGHT)
                )
            } else {
                showPreferencesRoot()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        activeComposeDestination?.let { outState.putString(EXTRA_COMPOSE_PREF_DESTINATION, it.name) }
        activeComposeHighlight?.let { outState.putString(EXTRA_COMPOSE_PREF_HIGHLIGHT, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        if (needToRestartOnResume) {
            lifecycleScope.launch {
                VLCInstance.restart()
                UiTools.restartDialog(this@PreferencesActivity)
            }
            needToRestartOnResume = false
        }
        super.onResume()
    }

    override fun onStop() {
        flushComposeDestinationSideEffects(activeComposeDestination)
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
                routePreferenceEndpoint(it)
            }
        }

    }

    private fun showPreferencesRoot(highlightedKey: String? = null) {
        flushComposeDestinationSideEffects(activeComposeDestination)
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
            PreferencesRootDestination.Ui -> showPreferenceSubpage(PreferencesRootDestination.Ui)
            PreferencesRootDestination.Video -> showPreferenceSubpage(PreferencesRootDestination.Video)
            PreferencesRootDestination.Subtitles -> showPreferenceSubpage(PreferencesRootDestination.Subtitles)
            PreferencesRootDestination.Audio -> showPreferenceSubpage(PreferencesRootDestination.Audio)
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
            PreferencesRootDestination.Advanced -> showPreferenceSubpage(PreferencesRootDestination.Advanced)
            PreferencesRootDestination.OptionalFeatures -> showPreferenceSubpage(PreferencesRootDestination.OptionalFeatures)
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
            R.xml.preferences_ui -> showPreferenceSubpage(PreferencesRootDestination.Ui, endPoint.key)
            R.xml.preferences_video -> showPreferenceSubpage(PreferencesRootDestination.Video, endPoint.key)
            R.xml.preferences_subtitles -> showPreferenceSubpage(PreferencesRootDestination.Subtitles, endPoint.key)
            R.xml.preferences_audio -> showPreferenceSubpage(PreferencesRootDestination.Audio, endPoint.key)
            R.xml.preferences_adv -> showPreferenceSubpage(PreferencesRootDestination.Advanced, endPoint.key)
            R.xml.preferences_casting -> showPreferenceSubpage(PreferencesRootDestination.Casting, endPoint.key)
            R.xml.preferences_parental_control -> showPreferenceSubpage(PreferencesRootDestination.ParentalControl, endPoint.key)
            R.xml.preferences_remote_access -> showPreferenceSubpage(PreferencesRootDestination.RemoteAccess, endPoint.key)
            R.xml.preferences_android_auto -> showPreferenceSubpage(PreferencesRootDestination.AndroidAuto, endPoint.key)
            else -> showPreferencesRoot(endPoint.key)
        }
    }

    private fun routeLegacyPreferenceEndpoint(endPoint: String) {
        when (endPoint) {
            "remote_access_category" -> showPreferenceSubpage(PreferencesRootDestination.RemoteAccess, endPoint)
            "enable_remote_access" -> showPreferenceSubpage(PreferencesRootDestination.RemoteAccess, endPoint)
            else -> showPreferencesRoot(endPoint)
        }
    }

    private fun showPreferenceSubpage(destination: PreferencesRootDestination, highlightedKey: String? = null) {
        if (activeComposeDestination != destination) flushComposeDestinationSideEffects(activeComposeDestination)
        expandBar()
        activeComposeDestination = destination
        activeComposeHighlight = highlightedKey
        supportActionBar?.title = getString(destination.titleRes())
        if (destination == PreferencesRootDestination.Ui) ensureUiThemePreferenceDefaults(Settings.getInstance(this))
        if (destination == PreferencesRootDestination.Video) showVideoSettingsDisclaimerIfNeeded()
        if (destination == PreferencesRootDestination.RemoteAccess) showRemoteAccessOnboardingIfNeeded()
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
                            onRemoteAccessNetworkBrowserChanged = ::restartRemoteAccessServer,
                            onPreferredResolutionChanged = ::restartMediaPipeline,
                            onPopupForceLegacyChanged = ::onPopupForceLegacyChanged,
                            onHeadsetDetectionChanged = ::detectHeadset,
                            onAudioReplayGainChanged = ::restartMediaPipeline,
                            onSoundFontClick = ::openSoundFontPicker,
                            onRestartRequired = ::setRestart,
                            onRestartDialogRequired = ::showRestartDialog,
                            onDefaultSleepTimerClick = ::openDefaultSleepTimer,
                            onSeenMediaChanged = ::updateSeenMedia,
                            onSubtitleSettingChanged = ::markSubtitlesRestartPending,
                            onOptionalFeaturesClick = { showPreferenceSubpage(PreferencesRootDestination.OptionalFeatures) },
                            onDebugLogsClick = ::openDebugLogs,
                            onInstallNightlyClick = ::installNightly,
                            onClearPlaybackHistoryClick = ::confirmClearPlaybackHistory,
                            onClearMediaDatabaseClick = ::confirmClearMediaDatabase,
                            onClearAppDataClick = ::confirmClearAppData,
                            onQuitAppClick = ::quitApp,
                            onDumpMediaDatabaseClick = ::dumpMediaDatabase,
                            onDumpAppDatabaseClick = ::dumpAppDatabase,
                            onExportSettingsClick = ::exportSettings,
                            onRestoreSettingsClick = ::openSettingsRestorePicker,
                            onNetworkCachingChanged = ::onNetworkCachingChanged,
                            onAoutChanged = ::onAoutChanged,
                            onAdvancedRestartLibVlc = ::restartMediaPipeline,
                            onCustomLibVlcOptionsChanged = ::onCustomLibVlcOptionsChanged,
                            onDav1dThreadNumberChanged = ::onDav1dThreadNumberChanged,
                            onPreferSmbV1Changed = ::onPreferSmbV1Changed,
                            onQuickPlayChanged = ::onQuickPlayChanged,
                            onFeatureFlagWarningRequired = ::showFeatureFlagWarning
                        )
                    }
                }
            )
        }
        invalidateOptionsMenu()
    }

    private fun navigateUp(): Boolean {
        if (activeComposeDestination == PreferencesRootDestination.OptionalFeatures) {
            showPreferenceSubpage(PreferencesRootDestination.Advanced)
            return true
        }
        if (activeComposeDestination != null) {
            showPreferencesRoot()
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
        restartMediaPipeline()
    }

    private fun restartMediaPipeline() {
        lifecycleScope.launch {
            restartMediaPipelineNow()
        }
    }

    private suspend fun restartMediaPipelineNow() {
        VLCInstance.restart()
        restartMediaPlayer()
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

    private fun showVideoSettingsDisclaimerIfNeeded() {
        val settings = Settings.getInstance(this)
        if (settings.getBoolean(PREF_SHOW_VIDEO_SETTINGS_DISCLAIMER, false)) {
            UiTools.snackerConfirm(this, getString(R.string.video_settings_disclaimer), indefinite = true) {
                settings.edit().putBoolean(PREF_SHOW_VIDEO_SETTINGS_DISCLAIMER, false).apply()
            }
        }
    }

    private fun onPopupForceLegacyChanged(forceLegacy: Boolean) {
        if (forceLegacy && !Permissions.canDrawOverlays(this)) Permissions.checkDrawOverlaysPermission(this)
        if (!forceLegacy && !Permissions.isPiPAllowed(this)) Permissions.checkPiPPermission(this)
    }

    private fun ensureUiThemePreferenceDefaults(settings: android.content.SharedPreferences) {
        if (settings.contains(KEY_APP_THEME)) return
        var theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        if (settings.getBoolean(KEY_DAYNIGHT, false) && !AndroidDevices.canUseSystemNightMode()) {
            theme = AppCompatDelegate.MODE_NIGHT_AUTO
        } else if (settings.contains(KEY_BLACK_THEME)) {
            theme = if (settings.getBoolean(KEY_BLACK_THEME, false)) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        }
        settings.edit { putString(KEY_APP_THEME, theme.toString()) }
    }

    private fun showRestartDialog() {
        UiTools.restartDialog(this)
    }

    private fun openDefaultSleepTimer(onTimerChanged: () -> Unit) {
        showSleepTimerComposeDialog(forDefault = true, onDismiss = onTimerChanged)
    }

    private fun updateSeenMedia() {
        setResult(RESULT_UPDATE_SEEN_MEDIA)
    }

    private fun openDebugLogs() {
        startActivity(Intent(this, DebugLogActivity::class.java))
    }

    private fun installNightly() {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_nightly))
            .setMessage(getString(R.string.install_nightly_alert))
            .setPositiveButton(R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    AutoUpdate.checkUpdate(application, true) { url, date ->
                        showUpdateComposeDialog(url, date, newInstall = true)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmClearPlaybackHistory() {
        showConfirmDeleteComposeDialog(
            title = getString(R.string.clear_playback_history),
            description = getString(R.string.clear_history_message),
            buttonText = getString(R.string.clear_history),
            resultType = RESULT_VALUE_CLEAR_HISTORY
        ) {
            onConfirmAdvancedDelete(RESULT_VALUE_CLEAR_HISTORY)
        }
    }

    private fun confirmClearMediaDatabase() {
        val medialibrary = Medialibrary.getInstance()
        if (medialibrary.isWorking) {
            Toast.makeText(this, R.string.settings_ml_block_scan, Toast.LENGTH_LONG).show()
        } else {
            showConfirmDeleteComposeDialog(
                title = getString(R.string.clear_media_db),
                description = getString(R.string.clear_media_db_message),
                buttonText = getString(R.string.clear),
                resultType = RESULT_VALUE_CLEAR_MEDIA_DATABASE
            ) {
                onConfirmAdvancedDelete(RESULT_VALUE_CLEAR_MEDIA_DATABASE)
            }
        }
    }

    private fun confirmClearAppData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            showConfirmDeleteComposeDialog(
                title = getString(R.string.clear_app_data),
                description = getString(R.string.clear_app_data_message),
                buttonText = getString(R.string.clear),
                resultType = RESULT_VALUE_CLEAR_APP_DATA
            ) {
                onConfirmAdvancedDelete(RESULT_VALUE_CLEAR_APP_DATA)
            }
        } else {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = Uri.fromParts(SCHEME_PACKAGE, applicationContext.packageName, null)
            })
        }
    }

    private fun onConfirmAdvancedDelete(reason: Int) {
        when (reason) {
            RESULT_VALUE_CLEAR_HISTORY -> {
                Medialibrary.getInstance().clearHistory(Medialibrary.HISTORY_TYPE_GLOBAL)
                Settings.getInstance(this).edit()
                    .remove(KEY_AUDIO_LAST_PLAYLIST)
                    .remove(KEY_MEDIA_LAST_PLAYLIST)
                    .remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
                    .remove(KEY_CURRENT_AUDIO)
                    .remove(KEY_CURRENT_MEDIA)
                    .remove(KEY_CURRENT_MEDIA_RESUME)
                    .remove(KEY_CURRENT_AUDIO_RESUME_TITLE)
                    .remove(KEY_CURRENT_AUDIO_RESUME_ARTIST)
                    .remove(KEY_CURRENT_AUDIO_RESUME_THUMB)
                    .apply()
            }
            RESULT_VALUE_CLEAR_MEDIA_DATABASE -> clearMediaDatabase()
            RESULT_VALUE_CLEAR_APP_DATA -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                }
            }
        }
    }

    private fun clearMediaDatabase() {
        val medialibrary = Medialibrary.getInstance()
        if (medialibrary.isWorking) {
            Toast.makeText(this, R.string.settings_ml_block_scan, Toast.LENGTH_LONG).show()
        } else {
            lifecycleScope.launch {
                val roots = medialibrary.foldersList
                withContext(Dispatchers.IO) {
                    medialibrary.clearDatabase(false)
                    try {
                        getExternalFilesDir(null)?.let {
                            val files = File(it.absolutePath + Medialibrary.MEDIALIB_FOLDER_NAME).listFiles()
                            files?.forEach { file ->
                                if (file.isFile) FileUtils.deleteFile(file)
                            }
                        }
                        BitmapCache.clear()
                    } catch (e: IOException) {
                        Log.e(this::class.java.simpleName, e.message, e)
                    }
                }
                roots.forEach { root ->
                    MedialibraryUtils.addDir(root.removePrefix("file://"), this@PreferencesActivity)
                }
            }
        }
    }

    private fun quitApp() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun dumpMediaDatabase() {
        if (Medialibrary.getInstance().isWorking) {
            UiTools.snacker(this, getString(R.string.settings_ml_block_scan))
        } else {
            val dst = File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + Medialibrary.VLC_MEDIA_DB_NAME)
            lifecycleScope.launch {
                if (getWritePermission(Uri.fromFile(dst))) {
                    val copied = withContext(Dispatchers.IO) {
                        val db = File(getDir("db", Context.MODE_PRIVATE).toString() + Medialibrary.VLC_MEDIA_DB_NAME)
                        FileUtils.copyFile(db, dst)
                    }
                    if (copied) {
                        UiTools.snackerConfirm(this@PreferencesActivity, getString(R.string.dump_db_succes), confirmMessage = R.string.share, overAudioPlayer = false) {
                            share(dst)
                        }
                    } else {
                        Toast.makeText(this@PreferencesActivity, getString(R.string.dump_db_failure), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun dumpAppDatabase() {
        val dst = File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + ROOM_DATABASE)
        lifecycleScope.launch {
            if (getWritePermission(Uri.fromFile(dst))) {
                val copied = withContext(Dispatchers.IO) {
                    val db = File(getDir("db", Context.MODE_PRIVATE).parent!! + "/databases")
                    val files = db.listFiles()?.map { it.path }?.toTypedArray()
                    if (files == null) false else FileUtils.zip(files, dst.path)
                }
                if (copied) {
                    UiTools.snackerConfirm(this@PreferencesActivity, getString(R.string.dump_db_succes), confirmMessage = R.string.share, overAudioPlayer = false) {
                        share(dst)
                    }
                } else {
                    Toast.makeText(this@PreferencesActivity, getString(R.string.dump_db_failure), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun exportSettings() {
        val dst = File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + EXPORT_SETTINGS_FILE)
        lifecycleScope.launch(Dispatchers.IO) {
            if (getWritePermission(Uri.fromFile(dst))) {
                PreferenceParser.exportPreferences(this@PreferencesActivity, dst)
            }
        }
    }

    private fun openSettingsRestorePicker() {
        settingsRestoreResult.launch(Intent(this, FilePickerActivity::class.java).apply {
            putExtra(KEY_PICKER_TYPE, PickerType.SETTINGS.ordinal)
        })
    }

    private fun restoreSettingsFrom(file: Uri) {
        lifecycleScope.launch {
            try {
                PreferenceParser.restoreSettings(this@PreferencesActivity, file)
                var continueRestart = true
                if (Settings.getInstance(this@PreferencesActivity).getBoolean(POPUP_FORCE_LEGACY, false) && !Permissions.canDrawOverlays(this@PreferencesActivity)) {
                    continueRestart = !Permissions.checkDrawOverlaysPermission(this@PreferencesActivity)
                }
                if (continueRestart) {
                    VLCInstance.restart()
                    UiTools.restartDialog(this@PreferencesActivity)
                } else {
                    needToRestartOnResume = true
                }
            } catch (e: Exception) {
                Log.e("EqualizerSettings", "restoreSettingsFrom: ${e.message}", e)
                UiTools.snacker(this@PreferencesActivity, getString(R.string.invalid_settings_file))
            }
        }
    }

    private fun onNetworkCachingChanged(value: String): String {
        val settings = Settings.getInstance(this)
        val originalValue = value.toIntOrNull()
        val newValue = originalValue?.coerceIn(0, 60000) ?: 0
        settings.edit {
            putString(KEY_NETWORK_CACHING, newValue.toString())
            putInt(KEY_NETWORK_CACHING_VALUE, newValue)
        }
        if (originalValue == null || originalValue != newValue) {
            UiTools.snacker(this, R.string.network_caching_popup)
        }
        restartMediaPipeline()
        return newValue.toString()
    }

    private fun onAoutChanged(value: String) {
        restartMediaPipeline()
        if (value == "2") Settings.getInstance(this).putSingle(KEY_AUDIO_DIGITAL_OUTPUT, false)
    }

    private fun onCustomLibVlcOptionsChanged(@Suppress("UNUSED_PARAMETER") value: String) {
        lifecycleScope.launch {
            try {
                VLCInstance.restart()
            } catch (e: IllegalStateException) {
                UiTools.snacker(this@PreferencesActivity, R.string.custom_libvlc_options_invalid)
                Settings.getInstance(this@PreferencesActivity).putSingle(KEY_CUSTOM_LIBVLC_OPTIONS, "")
            } finally {
                restartMediaPlayer()
            }
            restartMediaPipelineNow()
        }
    }

    private fun onDav1dThreadNumberChanged(value: String): String {
        if (value.isNotEmpty() && (!value.isDigitsOnly() || value.toInt() < 1)) {
            UiTools.snacker(this, R.string.dav1d_thread_number_invalid)
            Settings.getInstance(this).putSingle(DAV1D_THREAD_NUMBER, "")
            return ""
        }
        Settings.getInstance(this).putSingle(DAV1D_THREAD_NUMBER, value)
        return value
    }

    private fun onPreferSmbV1Changed() {
        lifecycleScope.launch { VLCInstance.restart() }
        UiTools.restartDialog(this)
    }

    private fun onQuickPlayChanged(@Suppress("UNUSED_PARAMETER") enabled: Boolean) {
        setResult(RESULT_RESTART)
    }

    private fun showFeatureFlagWarning(featureFlag: FeatureFlag, onAccepted: () -> Unit) {
        showFeatureFlagWarningComposeDialog(featureFlag, onAccepted)
    }

    private fun markSubtitlesRestartPending() {
        pendingSubtitlesRestart = true
    }

    private fun flushComposeDestinationSideEffects(destination: PreferencesRootDestination?) {
        if (destination == PreferencesRootDestination.Subtitles && pendingSubtitlesRestart) {
            pendingSubtitlesRestart = false
            restartMediaPipeline()
        }
    }

    private fun openSoundFontPicker() {
        soundFontResult.launch(Intent(this, FilePickerActivity::class.java).apply {
            putExtra(KEY_PICKER_TYPE, PickerType.SOUNDFONT.ordinal)
        })
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
        PreferencesRootDestination.OptionalFeatures -> R.string.optional_features
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
