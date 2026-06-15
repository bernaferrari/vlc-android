/*
 * *************************************************************************
 *  PreferencesComposeSubpages.kt
 * **************************************************************************
 *  Copyright © 2026 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.preferences

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import org.videolan.vlc.compose.components.vlcSelectionWash
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.videolan.medialibrary.Tools
import org.videolan.resources.AndroidDevices
import org.videolan.tools.AUDIO_DUCKING
import org.videolan.tools.DAV1D_THREAD_NUMBER
import org.videolan.tools.KEY_AOUT
import org.videolan.tools.KEY_APP_THEME
import org.videolan.tools.KEY_AUDIO_CONFIRM_RESUME
import org.videolan.tools.KEY_AUDIO_DIGITAL_OUTPUT
import org.videolan.tools.KEY_AUDIO_PREFERRED_LANGUAGE
import org.videolan.tools.KEY_AUDIO_REPLAY_GAIN_DEFAULT
import org.videolan.tools.KEY_AUDIO_REPLAY_GAIN_ENABLE
import org.videolan.tools.KEY_AUDIO_REPLAY_GAIN_MODE
import org.videolan.tools.KEY_AUDIO_REPLAY_GAIN_PEAK_PROTECTION
import org.videolan.tools.KEY_AUDIO_REPLAY_GAIN_PREAMP
import org.videolan.tools.KEY_AUDIO_RESUME_CARD
import org.videolan.tools.KEY_AUDIO_TASK_REMOVED
import org.videolan.tools.KEY_ANDROID_AUTO_QUEUE_INFO_POS_VAL
import org.videolan.tools.KEY_ALWAYS_FAST_SEEK
import org.videolan.tools.KEY_CASTING_AUDIO_ONLY
import org.videolan.tools.KEY_CASTING_PASSTHROUGH
import org.videolan.tools.KEY_CASTING_QUALITY
import org.videolan.tools.KEY_CUSTOM_LIBVLC_OPTIONS
import org.videolan.tools.KEY_DEBLOCKING
import org.videolan.tools.KEY_ENABLE_CASTING
import org.videolan.tools.KEY_ENABLE_CLONE_MODE
import org.videolan.tools.KEY_ENABLE_FRAME_SKIP
import org.videolan.tools.KEY_ENABLE_HEADSET_DETECTION
import org.videolan.tools.KEY_ENABLE_PLAY_ON_HEADSET_INSERTION
import org.videolan.tools.KEY_ENABLE_TIME_STRETCHING_AUDIO
import org.videolan.tools.KEY_ENABLE_VERBOSE_MODE
import org.videolan.tools.KEY_ENABLE_REMOTE_ACCESS
import org.videolan.tools.KEY_IGNORE_HEADSET_MEDIA_BUTTON_PRESSES
import org.videolan.tools.KEY_INCLUDE_MISSING
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.KEY_MEDIA_SEEN
import org.videolan.tools.KEY_OPENGL
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL
import org.videolan.tools.KEY_PERSISTENT_INCOGNITO
import org.videolan.tools.KEY_PREFER_SMBV1
import org.videolan.tools.KEY_PREFERRED_RESOLUTION
import org.videolan.tools.KEY_QUICK_PLAY
import org.videolan.tools.KEY_QUICK_PLAY_DEFAULT
import org.videolan.tools.KEY_REMOTE_ACCESS_ML_CONTENT
import org.videolan.tools.KEY_RESTRICT_SETTINGS
import org.videolan.tools.KEY_SAFE_MODE
import org.videolan.tools.KEY_SET_LOCALE
import org.videolan.tools.KEY_SHOW_HEADERS
import org.videolan.tools.KEY_SHOW_UPDATE
import org.videolan.tools.KEY_SUBTITLES_AUTOLOAD
import org.videolan.tools.KEY_SUBTITLES_BACKGROUND
import org.videolan.tools.KEY_SUBTITLES_BACKGROUND_COLOR
import org.videolan.tools.KEY_SUBTITLES_BACKGROUND_COLOR_OPACITY
import org.videolan.tools.KEY_SUBTITLES_BOLD
import org.videolan.tools.KEY_SUBTITLES_COLOR
import org.videolan.tools.KEY_SUBTITLES_COLOR_OPACITY
import org.videolan.tools.KEY_SUBTITLES_OUTLINE
import org.videolan.tools.KEY_SUBTITLES_OUTLINE_COLOR
import org.videolan.tools.KEY_SUBTITLES_OUTLINE_COLOR_OPACITY
import org.videolan.tools.KEY_SUBTITLES_OUTLINE_SIZE
import org.videolan.tools.KEY_SUBTITLES_SHADOW
import org.videolan.tools.KEY_SUBTITLES_SHADOW_COLOR
import org.videolan.tools.KEY_SUBTITLES_SHADOW_COLOR_OPACITY
import org.videolan.tools.KEY_SUBTITLES_SIZE
import org.videolan.tools.KEY_SUBTITLE_PREFERRED_LANGUAGE
import org.videolan.tools.KEY_SUBTITLE_TEXT_ENCODING
import org.videolan.tools.KEY_VIDEO_MATCH_FRAME_RATE
import org.videolan.tools.LIST_TITLE_ELLIPSIZE
import org.videolan.tools.LOCKSCREEN_COVER
import org.videolan.tools.PREF_TV_UI
import org.videolan.tools.POPUP_FORCE_LEGACY
import org.videolan.tools.REMOTE_ACCESS_FILE_BROWSER_CONTENT
import org.videolan.tools.REMOTE_ACCESS_HISTORY_CONTENT
import org.videolan.tools.REMOTE_ACCESS_LOGS
import org.videolan.tools.REMOTE_ACCESS_NETWORK_BROWSER_CONTENT
import org.videolan.tools.REMOTE_ACCESS_PLAYBACK_CONTROL
import org.videolan.tools.RESUME_PLAYBACK
import org.videolan.tools.RESTORE_BACKGROUND_VIDEO
import org.videolan.tools.LocaleUtils
import org.videolan.tools.LocaleUtils.getLocales
import org.videolan.tools.PLAYLIST_MODE_AUDIO
import org.videolan.tools.PLAYLIST_MODE_VIDEO
import org.videolan.tools.SHOW_SEEK_IN_COMPACT_NOTIFICATION
import org.videolan.tools.SHOW_VIDEO_THUMBNAILS
import org.videolan.tools.SLEEP_TIMER_DEFAULT_INTERVAL
import org.videolan.tools.SLEEP_TIMER_DEFAULT_RESET_INTERACTION
import org.videolan.tools.SLEEP_TIMER_DEFAULT_WAIT
import org.videolan.tools.Settings
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.isVLC4
import org.videolan.vlc.util.TextUtils
import org.videolan.vlc.util.FeatureFlag
import org.videolan.vlc.util.FeatureFlagManager
import org.videolan.vlc.util.LocaleUtil
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

private const val KEY_REMOTE_ACCESS_STATUS = "remote_access_status"
private const val KEY_ANDROID_AUTO_QUEUE_INFO_POS = "android_auto_queue_info_pos"
private const val KEY_ANDROID_AUTO_QUEUE_FORMAT = "android_auto_queue_format"
private const val KEY_ANDROID_AUTO_TITLE_SCALE = "android_auto_title_scale_val"
private const val KEY_ANDROID_AUTO_SUBTITLE_SCALE = "android_auto_subtitle_scale_val"
private const val KEY_ANDROID_AUTO_SPEED_BUTTONS = "enable_android_auto_speed_buttons"
private const val KEY_ANDROID_AUTO_SEEK_BUTTONS = "enable_android_auto_seek_buttons"
private const val KEY_DEFAULT_SLEEP_TIMER = "default_sleep_timer"
private const val KEY_SUBTITLES_PRESETS = "subtitles_presets"
private const val KEY_OPTIONAL_FEATURES = "optional_features"
private const val KEY_NETWORK_CACHING = "network_caching"
private const val KEY_HTTP_USER_AGENT = "http_user_agent"
private const val KEY_QUIT_APP = "quit_app"
private const val KEY_DUMP_MEDIA_DB = "dump_media_db"
private const val KEY_DUMP_APP_DB = "dump_app_db"
private const val KEY_CLEAR_MEDIA_DB = "clear_media_db"
private const val KEY_CLEAR_APP_DATA = "clear_app_data"
private const val KEY_CLEAR_HISTORY = "clear_history"
private const val KEY_EXPORT_SETTINGS = "export_settings"
private const val KEY_RESTORE_SETTINGS = "restore_settings"
private const val KEY_DEBUG_LOGS = "debug_logs"
private const val KEY_NIGHTLY_INSTALL = "nightly_install"

private data class SubpagePreferenceOption(
        val label: String,
        val value: String
)

/**
 * Compose replacement for the phone preference XML screens:
 * preferences_ui.xml, preferences_video.xml, preferences_audio.xml, preferences_subtitles.xml,
 * preferences_casting.xml, preferences_parental_control.xml, preferences_remote_access.xml,
 * preferences_android_auto.xml, and preferences_adv.xml.
 *
 * Those XML files stay parseable by PreferenceParser for search metadata where needed while this
 * screen owns the active phone rendering path, including Optional Features.
 */
@Composable
internal fun PreferencesComposeSubpageScreen(
        settings: SharedPreferences,
        destination: PreferencesRootDestination,
        highlightedKey: String?,
        isPinCodeSet: () -> Boolean,
        onModifyPinCodeClick: () -> Unit,
        onSafeModeChanged: (Boolean) -> Unit,
        onRestartAppRequired: () -> Unit,
        onRestartCastingPipeline: () -> Unit,
        onAndroidAutoSettingChanged: () -> Unit,
        onPlaybackSpeedGlobalChanged: () -> Unit,
        onRemoteAccessStatusClick: () -> Unit,
        onRemoteAccessEnabledChanged: (Boolean) -> Unit,
        onRemoteAccessNetworkBrowserChanged: () -> Unit,
        onPreferredResolutionChanged: () -> Unit,
        onPopupForceLegacyChanged: (Boolean) -> Unit,
        onHeadsetDetectionChanged: (Boolean) -> Unit,
        onAudioReplayGainChanged: () -> Unit,
        onSoundFontClick: () -> Unit,
        onRestartRequired: () -> Unit,
        onRestartDialogRequired: () -> Unit,
        onDefaultSleepTimerClick: (() -> Unit) -> Unit,
        onSeenMediaChanged: () -> Unit,
        onSubtitleSettingChanged: () -> Unit,
        onOptionalFeaturesClick: () -> Unit,
        onDebugLogsClick: () -> Unit,
        onInstallNightlyClick: () -> Unit,
        onClearPlaybackHistoryClick: () -> Unit,
        onClearMediaDatabaseClick: () -> Unit,
        onClearAppDataClick: () -> Unit,
        onQuitAppClick: () -> Unit,
        onDumpMediaDatabaseClick: () -> Unit,
        onDumpAppDatabaseClick: () -> Unit,
        onExportSettingsClick: () -> Unit,
        onRestoreSettingsClick: () -> Unit,
        onNetworkCachingChanged: (String) -> String,
        onAoutChanged: (String) -> Unit,
        onAdvancedRestartLibVlc: () -> Unit,
        onCustomLibVlcOptionsChanged: (String) -> Unit,
        onDav1dThreadNumberChanged: (String) -> String,
        onPreferSmbV1Changed: () -> Unit,
        onQuickPlayChanged: (Boolean) -> Unit,
        onFeatureFlagWarningRequired: (FeatureFlag, () -> Unit) -> Unit
) {
    VLCTheme {
        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .background(VLCThemeDefaults.colors.backgroundDefault)
        ) {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (destination) {
                    PreferencesRootDestination.Ui -> item {
                        UiPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onRestartAppRequired = onRestartAppRequired,
                                onRestartRequired = onRestartRequired,
                                onRestartDialogRequired = onRestartDialogRequired,
                                onDefaultSleepTimerClick = onDefaultSleepTimerClick,
                                onSeenMediaChanged = onSeenMediaChanged
                        )
                    }
                    PreferencesRootDestination.Video -> item {
                        VideoPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onPreferredResolutionChanged = onPreferredResolutionChanged,
                                onPopupForceLegacyChanged = onPopupForceLegacyChanged
                        )
                    }
                    PreferencesRootDestination.Audio -> item {
                        AudioPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onHeadsetDetectionChanged = onHeadsetDetectionChanged,
                                onReplayGainChanged = onAudioReplayGainChanged,
                                onSoundFontClick = onSoundFontClick
                        )
                    }
                    PreferencesRootDestination.Subtitles -> item {
                        SubtitlePreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onSubtitleSettingChanged = onSubtitleSettingChanged
                        )
                    }
                    PreferencesRootDestination.Casting -> item {
                        CastingPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onRestartAppRequired = onRestartAppRequired,
                                onRestartCastingPipeline = onRestartCastingPipeline
                        )
                    }
                    PreferencesRootDestination.ParentalControl -> item {
                        ParentalControlPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                isPinCodeSet = isPinCodeSet,
                                onModifyPinCodeClick = onModifyPinCodeClick,
                                onSafeModeChanged = onSafeModeChanged
                        )
                    }
                    PreferencesRootDestination.AndroidAuto -> item {
                        AndroidAutoPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onAndroidAutoSettingChanged = onAndroidAutoSettingChanged,
                                onPlaybackSpeedGlobalChanged = onPlaybackSpeedGlobalChanged
                        )
                    }
                    PreferencesRootDestination.RemoteAccess -> item {
                        RemoteAccessPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onStatusClick = onRemoteAccessStatusClick,
                                onRemoteAccessEnabledChanged = onRemoteAccessEnabledChanged,
                                onNetworkBrowserChanged = onRemoteAccessNetworkBrowserChanged
                        )
                    }
                    PreferencesRootDestination.Advanced -> item {
                        AdvancedPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onOptionalFeaturesClick = onOptionalFeaturesClick,
                                onDebugLogsClick = onDebugLogsClick,
                                onInstallNightlyClick = onInstallNightlyClick,
                                onClearPlaybackHistoryClick = onClearPlaybackHistoryClick,
                                onClearMediaDatabaseClick = onClearMediaDatabaseClick,
                                onClearAppDataClick = onClearAppDataClick,
                                onQuitAppClick = onQuitAppClick,
                                onDumpMediaDatabaseClick = onDumpMediaDatabaseClick,
                                onDumpAppDatabaseClick = onDumpAppDatabaseClick,
                                onExportSettingsClick = onExportSettingsClick,
                                onRestoreSettingsClick = onRestoreSettingsClick,
                                onNetworkCachingChanged = onNetworkCachingChanged,
                                onAoutChanged = onAoutChanged,
                                onAdvancedRestartLibVlc = onAdvancedRestartLibVlc,
                                onCustomLibVlcOptionsChanged = onCustomLibVlcOptionsChanged,
                                onDav1dThreadNumberChanged = onDav1dThreadNumberChanged,
                                onPreferSmbV1Changed = onPreferSmbV1Changed,
                                onQuickPlayChanged = onQuickPlayChanged
                        )
                    }
                    PreferencesRootDestination.OptionalFeatures -> item {
                        OptionalFeaturesPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onFeatureFlagWarningRequired = onFeatureFlagWarningRequired
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UiPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onRestartAppRequired: () -> Unit,
        onRestartRequired: () -> Unit,
        onRestartDialogRequired: () -> Unit,
        onDefaultSleepTimerClick: (() -> Unit) -> Unit,
        onSeenMediaChanged: () -> Unit
) {
    val context = LocalContext.current
    val isVisible = { key: String -> PreferenceVisibilityManager.isPreferenceVisible(key, settings, false) }
    val localePair = remember(context) {
        LocaleUtils.getLocalesUsedInProject(
                BuildConfig.TRANSLATION_ARRAY,
                context.getString(R.string.device_default)
        )
    }
    val themeEntries = if (AndroidDevices.canUseSystemNightMode()) {
        stringArrayResource(R.array.daynight_mode_entries).toList()
    } else {
        stringArrayResource(R.array.daynight_mode_legacy_entries).toList()
    }
    val themeValues = if (AndroidDevices.canUseSystemNightMode()) {
        stringArrayResource(R.array.daynight_mode_values).toList()
    } else {
        stringArrayResource(R.array.daynight_mode_legacy_values).toList()
    }
    var sleepTimerSummary by remember {
        mutableStateOf(defaultSleepTimerSummary(context, settings))
    }
    var incognitoMode by remember {
        mutableStateOf(settings.getBoolean(KEY_INCOGNITO, false))
    }

    if (isVisible(KEY_APP_THEME)) {
        ListPreferenceRow(
                key = KEY_APP_THEME,
                settings = settings,
                title = stringResource(R.string.daynight_title),
                defaultValue = "-1",
                entries = themeEntries,
                values = themeValues,
                highlighted = highlightedKey == KEY_APP_THEME,
                onValueChanged = { onRestartDialogRequired() }
        )
    }
    BooleanPreferenceRow(
            key = PREF_TV_UI,
            settings = settings,
            title = stringResource(R.string.tv_ui_title),
            summary = stringResource(R.string.tv_ui_summary),
            defaultValue = false,
            highlighted = highlightedKey == PREF_TV_UI,
            onAfterChange = { checked ->
                Settings.tvUI = checked
                onRestartAppRequired()
            }
    )
    ListPreferenceRow(
            key = KEY_SET_LOCALE,
            settings = settings,
            title = stringResource(R.string.set_locale),
            defaultValue = "",
            entries = localePair.localeEntries.toList(),
            values = localePair.localeEntryValues.toList(),
            highlighted = highlightedKey == KEY_SET_LOCALE,
            summary = "",
            onValueChanged = {
                onRestartRequired()
                onRestartDialogRequired()
            }
    )
    if (isVisible(LIST_TITLE_ELLIPSIZE)) {
        ListPreferenceRow(
                key = LIST_TITLE_ELLIPSIZE,
                settings = settings,
                title = stringResource(R.string.list_title_ellipsize),
                defaultValue = "0",
                entries = stringArrayResource(R.array.list_title_ellipsize_list).toList(),
                values = stringArrayResource(R.array.list_title_ellipsize_values).toList(),
                highlighted = highlightedKey == LIST_TITLE_ELLIPSIZE,
                onValueChanged = { value ->
                    Settings.listTitleEllipsize = value.toIntOrNull() ?: 0
                    onRestartRequired()
                }
        )
    }
    BooleanPreferenceRow(
            key = KEY_SHOW_HEADERS,
            settings = settings,
            title = stringResource(R.string.show_headers),
            summary = stringResource(R.string.show_headers_summary),
            defaultValue = true,
            highlighted = highlightedKey == KEY_SHOW_HEADERS,
            onAfterChange = { checked ->
                Settings.showHeaders = checked
                onRestartRequired()
            }
    )
    BooleanPreferenceRow(
            key = KEY_INCLUDE_MISSING,
            settings = settings,
            title = stringResource(R.string.browser_show_missing_media),
            summary = stringResource(R.string.browser_show_missing_media_summary),
            defaultValue = true,
            highlighted = highlightedKey == KEY_INCLUDE_MISSING,
            onAfterChange = { checked ->
                Settings.includeMissing = checked
                onRestartRequired()
            }
    )
    NavigationPreferenceRow(
            key = KEY_DEFAULT_SLEEP_TIMER,
            title = stringResource(R.string.sleep_title),
            summary = sleepTimerSummary,
            highlighted = highlightedKey == KEY_DEFAULT_SLEEP_TIMER,
            onClick = {
                onDefaultSleepTimerClick {
                    sleepTimerSummary = defaultSleepTimerSummary(context, settings)
                }
            }
    )
    BooleanPreferenceRow(
            key = KEY_INCOGNITO,
            settings = settings,
            title = stringResource(R.string.incognito_mode),
            defaultValue = false,
            highlighted = highlightedKey == KEY_INCOGNITO,
            checked = incognitoMode,
            onCheckedStateChange = { incognitoMode = it }
    )
    BooleanPreferenceRow(
            key = KEY_PERSISTENT_INCOGNITO,
            settings = settings,
            title = stringResource(R.string.persistent_incognito_mode),
            summary = stringResource(R.string.persistent_incognito_mode_summary),
            defaultValue = true,
            highlighted = highlightedKey == KEY_PERSISTENT_INCOGNITO,
            enabled = incognitoMode
    )

    PreferenceCategoryHeader(title = stringResource(R.string.video))
    BooleanPreferenceRow(
            key = KEY_MEDIA_SEEN,
            settings = settings,
            title = stringResource(R.string.media_seen),
            summary = stringResource(R.string.media_seen_summary),
            defaultValue = true,
            highlighted = highlightedKey == KEY_MEDIA_SEEN,
            onAfterChange = { onSeenMediaChanged() }
    )
    if (isVisible(PLAYLIST_MODE_VIDEO)) {
        BooleanPreferenceRow(
                key = PLAYLIST_MODE_VIDEO,
                settings = settings,
                title = stringResource(R.string.force_play_all_title),
                summary = stringResource(R.string.force_play_all_summary),
                defaultValue = false,
                highlighted = highlightedKey == PLAYLIST_MODE_VIDEO
        )
    }
    BooleanPreferenceRow(
            key = SHOW_VIDEO_THUMBNAILS,
            settings = settings,
            title = stringResource(R.string.show_video_thumbnails),
            summary = stringResource(R.string.show_video_thumbnails_summary),
            defaultValue = true,
            highlighted = highlightedKey == SHOW_VIDEO_THUMBNAILS,
            onAfterChange = { checked ->
                Settings.showVideoThumbs = checked
                onRestartRequired()
            }
    )

    PreferenceCategoryHeader(title = stringResource(R.string.audio))
    if (isVisible(KEY_AUDIO_RESUME_CARD)) {
        BooleanPreferenceRow(
                key = KEY_AUDIO_RESUME_CARD,
                settings = settings,
                title = stringResource(R.string.audio_resume_card_title),
                summary = stringResource(R.string.audio_resume_card_summary),
                defaultValue = true,
                highlighted = highlightedKey == KEY_AUDIO_RESUME_CARD
        )
    }
    if (isVisible(PLAYLIST_MODE_AUDIO)) {
        BooleanPreferenceRow(
                key = PLAYLIST_MODE_AUDIO,
                settings = settings,
                title = stringResource(R.string.force_play_all_audio_title),
                summary = stringResource(R.string.force_play_all_audio_summary),
                defaultValue = false,
                highlighted = highlightedKey == PLAYLIST_MODE_AUDIO
        )
    }
    if (isVisible(LOCKSCREEN_COVER)) {
        BooleanPreferenceRow(
                key = LOCKSCREEN_COVER,
                settings = settings,
                title = stringResource(R.string.lockscreen_cover_title),
                summary = stringResource(R.string.lockscreen_cover_summary),
                defaultValue = true,
                highlighted = highlightedKey == LOCKSCREEN_COVER
        )
    }
    if (isVisible(SHOW_SEEK_IN_COMPACT_NOTIFICATION)) {
        BooleanPreferenceRow(
                key = SHOW_SEEK_IN_COMPACT_NOTIFICATION,
                settings = settings,
                title = stringResource(R.string.show_seek_in_compact_notif_title),
                summary = stringResource(R.string.show_seek_in_compact_notif_summary),
                defaultValue = false,
                highlighted = highlightedKey == SHOW_SEEK_IN_COMPACT_NOTIFICATION
        )
    }
}

@Composable
private fun VideoPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onPreferredResolutionChanged: () -> Unit,
        onPopupForceLegacyChanged: (Boolean) -> Unit
) {
    BooleanPreferenceRow(
            key = KEY_ALWAYS_FAST_SEEK,
            settings = settings,
            title = stringResource(R.string.always_fast_seek),
            summary = stringResource(R.string.always_fast_seek_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_ALWAYS_FAST_SEEK
    )
    BooleanPreferenceRow(
            key = POPUP_FORCE_LEGACY,
            settings = settings,
            title = stringResource(R.string.popup_force_legacy_title),
            summary = stringResource(R.string.popup_force_legacy_summary),
            defaultValue = false,
            highlighted = highlightedKey == POPUP_FORCE_LEGACY,
            onAfterChange = onPopupForceLegacyChanged
    )
    BooleanPreferenceRow(
            key = RESTORE_BACKGROUND_VIDEO,
            settings = settings,
            title = stringResource(R.string.restore_background_video_title),
            summary = stringResource(R.string.restore_background_video_summary),
            defaultValue = false,
            highlighted = highlightedKey == RESTORE_BACKGROUND_VIDEO
    )
    BooleanPreferenceRow(
            key = KEY_VIDEO_MATCH_FRAME_RATE,
            settings = settings,
            title = stringResource(R.string.video_match_frame_rate_title),
            summary = stringResource(R.string.video_match_frame_rate_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_VIDEO_MATCH_FRAME_RATE
    )
    ListPreferenceRow(
            key = KEY_PREFERRED_RESOLUTION,
            settings = settings,
            title = stringResource(R.string.preferred_resolution),
            defaultValue = "-1",
            entries = stringArrayResource(R.array.preferred_resolution).toList(),
            values = stringArrayResource(R.array.preferred_resolution_values).toList(),
            highlighted = highlightedKey == KEY_PREFERRED_RESOLUTION,
            summaryFormatRes = R.string.preferred_resolution_summary,
            onValueChanged = { onPreferredResolutionChanged() }
    )

    PreferenceCategoryHeader(title = stringResource(R.string.interface_secondary_display_category_title))
    StaticPreferenceRow(
            summary = stringResource(R.string.interface_secondary_display_category_summary),
            highlighted = highlightedKey == "secondary_display_category_summary"
    )
    BooleanPreferenceRow(
            key = KEY_ENABLE_CLONE_MODE,
            settings = settings,
            title = stringResource(R.string.enable_clone_mode),
            summary = stringResource(R.string.enable_clone_mode_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_ENABLE_CLONE_MODE
    )
}

@Composable
private fun AudioPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onHeadsetDetectionChanged: (Boolean) -> Unit,
        onReplayGainChanged: () -> Unit,
        onSoundFontClick: () -> Unit
) {
    val context = LocalContext.current
    val localePair = remember(context) {
        LocaleUtils.getLocalesUsedInProject(
                BuildConfig.TRANSLATION_ARRAY,
                context.getString(R.string.no_track_preference),
                context.getLocales()
        )
    }
    var preferredAudioLanguage by remember {
        mutableStateOf(settings.getString(KEY_AUDIO_PREFERRED_LANGUAGE, "") ?: "")
    }
    var audioDigitalOutput by remember {
        mutableStateOf(settings.getBoolean(KEY_AUDIO_DIGITAL_OUTPUT, false))
    }
    var headsetDetectionEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_ENABLE_HEADSET_DETECTION, true))
    }
    var replayGainEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_AUDIO_REPLAY_GAIN_ENABLE, false))
    }
    val audioLanguageSummary = if (preferredAudioLanguage.isEmpty()) {
        stringResource(R.string.no_track_preference)
    } else {
        stringResource(R.string.track_preference, LocaleUtil.getLocaleName(preferredAudioLanguage))
    }
    val isVisible = { key: String -> PreferenceVisibilityManager.isPreferenceVisible(key, settings, false) }

    if (isVisible(RESUME_PLAYBACK)) {
        BooleanPreferenceRow(
                key = RESUME_PLAYBACK,
                settings = settings,
                title = stringResource(R.string.resume_playback_title),
                summary = stringResource(R.string.resume_playback_summary),
                defaultValue = true,
                highlighted = highlightedKey == RESUME_PLAYBACK
        )
    }
    if (isVisible(KEY_AUDIO_TASK_REMOVED)) {
        BooleanPreferenceRow(
                key = KEY_AUDIO_TASK_REMOVED,
                settings = settings,
                title = stringResource(R.string.audio_task_cleared_title),
                summary = stringResource(R.string.audio_task_cleared_summary),
                defaultValue = false,
                highlighted = highlightedKey == KEY_AUDIO_TASK_REMOVED
        )
    }
    if (isVisible(KEY_AUDIO_DIGITAL_OUTPUT)) {
        BooleanPreferenceRow(
                key = KEY_AUDIO_DIGITAL_OUTPUT,
                settings = settings,
                title = stringResource(R.string.audio_digital_title),
                summary = stringResource(if (audioDigitalOutput) R.string.audio_digital_output_enabled else R.string.audio_digital_output_disabled),
                defaultValue = false,
                highlighted = highlightedKey == KEY_AUDIO_DIGITAL_OUTPUT,
                checked = audioDigitalOutput,
                onCheckedStateChange = { audioDigitalOutput = it }
        )
    }
    ListPreferenceRow(
            key = KEY_AUDIO_PREFERRED_LANGUAGE,
            settings = settings,
            title = stringResource(R.string.audio_preferred_language),
            defaultValue = "",
            entries = localePair.localeEntries.toList(),
            values = localePair.localeEntryValues.toList(),
            highlighted = highlightedKey == KEY_AUDIO_PREFERRED_LANGUAGE,
            summary = audioLanguageSummary,
            onValueChanged = { preferredAudioLanguage = it }
    )
    ListPreferenceRow(
            key = KEY_AUDIO_CONFIRM_RESUME,
            settings = settings,
            title = stringResource(R.string.confirm_resume_audio_title),
            defaultValue = "0",
            entries = stringArrayResource(R.array.ask_confirmation_entries).toList(),
            values = stringArrayResource(R.array.ask_confirmation_values).toList(),
            highlighted = highlightedKey == KEY_AUDIO_CONFIRM_RESUME
    )

    if (isVisible("headset_prefs_category")) {
        PreferenceCategoryHeader(title = stringResource(R.string.headset_prefs_category))
        if (isVisible(KEY_ENABLE_HEADSET_DETECTION)) {
            BooleanPreferenceRow(
                    key = KEY_ENABLE_HEADSET_DETECTION,
                    settings = settings,
                    title = stringResource(R.string.enable_headset_detection),
                    summary = stringResource(R.string.enable_headset_detection_summary),
                    defaultValue = true,
                    highlighted = highlightedKey == KEY_ENABLE_HEADSET_DETECTION,
                    checked = headsetDetectionEnabled,
                    onCheckedStateChange = { headsetDetectionEnabled = it },
                    onAfterChange = onHeadsetDetectionChanged
            )
        }
        if (isVisible(KEY_ENABLE_PLAY_ON_HEADSET_INSERTION)) {
            BooleanPreferenceRow(
                    key = KEY_ENABLE_PLAY_ON_HEADSET_INSERTION,
                    settings = settings,
                    title = stringResource(R.string.enable_play_on_headset_insertion),
                    summary = stringResource(R.string.enable_play_on_headset_insertion_summary),
                    defaultValue = false,
                    highlighted = highlightedKey == KEY_ENABLE_PLAY_ON_HEADSET_INSERTION,
                    enabled = headsetDetectionEnabled
            )
        }
        if (isVisible(KEY_IGNORE_HEADSET_MEDIA_BUTTON_PRESSES)) {
            BooleanPreferenceRow(
                    key = KEY_IGNORE_HEADSET_MEDIA_BUTTON_PRESSES,
                    settings = settings,
                    title = stringResource(R.string.ignore_headset_media_button_presses),
                    summary = stringResource(R.string.ignore_headset_media_button_presses_summary),
                    defaultValue = false,
                    highlighted = highlightedKey == KEY_IGNORE_HEADSET_MEDIA_BUTTON_PRESSES
            )
        }
    }

    PreferenceCategoryHeader(title = stringResource(R.string.replaygain_prefs_category))
    BooleanPreferenceRow(
            key = KEY_AUDIO_REPLAY_GAIN_ENABLE,
            settings = settings,
            title = stringResource(R.string.replaygain_enable),
            summary = stringResource(R.string.replaygain_enable_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_AUDIO_REPLAY_GAIN_ENABLE,
            checked = replayGainEnabled,
            onCheckedStateChange = { replayGainEnabled = it },
            onAfterChange = { onReplayGainChanged() }
    )
    ListPreferenceRow(
            key = KEY_AUDIO_REPLAY_GAIN_MODE,
            settings = settings,
            title = stringResource(R.string.replaygain_mode),
            summary = stringResource(R.string.replaygain_mode_summary),
            defaultValue = "track",
            entries = stringArrayResource(R.array.replaygain).toList(),
            values = stringArrayResource(R.array.replaygain_values).toList(),
            highlighted = highlightedKey == KEY_AUDIO_REPLAY_GAIN_MODE,
            enabled = replayGainEnabled,
            onValueChanged = { onReplayGainChanged() }
    )
    ReplayGainNumberPreferenceRow(
            key = KEY_AUDIO_REPLAY_GAIN_PREAMP,
            settings = settings,
            title = stringResource(R.string.replaygain_preamp),
            summary = stringResource(R.string.replaygain_preamp_summary),
            defaultValue = "0.0",
            highlighted = highlightedKey == KEY_AUDIO_REPLAY_GAIN_PREAMP,
            enabled = replayGainEnabled,
            onValueChanged = onReplayGainChanged
    )
    ReplayGainNumberPreferenceRow(
            key = KEY_AUDIO_REPLAY_GAIN_DEFAULT,
            settings = settings,
            title = stringResource(R.string.replaygain_default),
            summary = stringResource(R.string.replaygain_default_summary),
            defaultValue = "-7.0",
            highlighted = highlightedKey == KEY_AUDIO_REPLAY_GAIN_DEFAULT,
            enabled = replayGainEnabled,
            onValueChanged = onReplayGainChanged
    )
    BooleanPreferenceRow(
            key = KEY_AUDIO_REPLAY_GAIN_PEAK_PROTECTION,
            settings = settings,
            title = stringResource(R.string.replaygain_peak_protection),
            summary = stringResource(R.string.replaygain_peak_protection_summary),
            defaultValue = true,
            highlighted = highlightedKey == KEY_AUDIO_REPLAY_GAIN_PEAK_PROTECTION,
            enabled = replayGainEnabled,
            onAfterChange = { onReplayGainChanged() }
    )

    PreferenceCategoryHeader(title = stringResource(R.string.advanced_prefs_category))
    if (isVisible(AUDIO_DUCKING)) {
        BooleanPreferenceRow(
                key = AUDIO_DUCKING,
                settings = settings,
                title = stringResource(R.string.audio_ducking_title),
                summary = stringResource(R.string.audio_ducking_summary),
                defaultValue = true,
                highlighted = highlightedKey == AUDIO_DUCKING
        )
    }
    NavigationPreferenceRow(
            key = KEY_SOUNDFONT,
            title = stringResource(R.string.soundfont),
            summary = stringResource(R.string.soundfont_summary),
            highlighted = highlightedKey == KEY_SOUNDFONT,
            onClick = onSoundFontClick
    )
}

@Composable
private fun SubtitlePreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onSubtitleSettingChanged: () -> Unit
) {
    val context = LocalContext.current
    val localePair = remember(context) {
        LocaleUtils.getLocalesUsedInProject(
                BuildConfig.TRANSLATION_ARRAY,
                context.getString(R.string.no_track_preference),
                context.getLocales()
        )
    }
    var preferredSubtitleLanguage by remember {
        mutableStateOf(settings.getString(KEY_SUBTITLE_PREFERRED_LANGUAGE, "") ?: "")
    }
    var subtitleBackgroundEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_SUBTITLES_BACKGROUND, false))
    }
    var subtitleShadowEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_SUBTITLES_SHADOW, true))
    }
    var subtitleOutlineEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_SUBTITLES_OUTLINE, true))
    }
    var resetVersion by remember { mutableIntStateOf(0) }
    val preferredSubtitleSummary = if (preferredSubtitleLanguage.isEmpty()) {
        stringResource(R.string.no_track_preference)
    } else {
        stringResource(R.string.track_preference, preferredSubtitleLanguage)
    }

    EphemeralListPreferenceRow(
            key = KEY_SUBTITLES_PRESETS,
            title = stringResource(R.string.subtitles_presets_title),
            entries = stringArrayResource(R.array.subtitles_presets_entries).toList(),
            values = stringArrayResource(R.array.subtitles_presets_values).toList(),
            highlighted = highlightedKey == KEY_SUBTITLES_PRESETS,
            onValueSelected = { preset ->
                applySubtitlePreset(
                        settings = settings,
                        preset = preset,
                        onBackgroundChanged = { subtitleBackgroundEnabled = it },
                        onShadowChanged = { subtitleShadowEnabled = it },
                        onOutlineChanged = { subtitleOutlineEnabled = it }
                )
                resetVersion++
                onSubtitleSettingChanged()
            }
    )
    BooleanPreferenceRow(
            key = KEY_SUBTITLES_AUTOLOAD,
            settings = settings,
            title = stringResource(R.string.subtitles_autoload_title),
            defaultValue = true,
            highlighted = highlightedKey == KEY_SUBTITLES_AUTOLOAD
    )
    ListPreferenceRow(
            key = KEY_SUBTITLE_TEXT_ENCODING,
            settings = settings,
            title = stringResource(R.string.subtitle_text_encoding),
            defaultValue = "",
            entries = stringArrayResource(R.array.subtitles_encoding_list).toList(),
            values = stringArrayResource(R.array.subtitles_encoding_values).toList(),
            highlighted = highlightedKey == KEY_SUBTITLE_TEXT_ENCODING,
            stateVersion = resetVersion,
            onValueChanged = { onSubtitleSettingChanged() }
    )
    ListPreferenceRow(
            key = KEY_SUBTITLE_PREFERRED_LANGUAGE,
            settings = settings,
            title = stringResource(R.string.subtitle_preferred_language),
            defaultValue = "",
            entries = localePair.localeEntries.toList(),
            values = localePair.localeEntryValues.toList(),
            highlighted = highlightedKey == KEY_SUBTITLE_PREFERRED_LANGUAGE,
            summary = preferredSubtitleSummary,
            stateVersion = resetVersion,
            onValueChanged = { preferredSubtitleLanguage = it }
    )

    PreferenceCategoryHeader(title = stringResource(R.string.subtitles_font_style))
    ListPreferenceRow(
            key = KEY_SUBTITLES_SIZE,
            settings = settings,
            title = stringResource(R.string.subtitles_size_title),
            defaultValue = "16",
            entries = stringArrayResource(R.array.subtitles_size_entries).toList(),
            values = stringArrayResource(R.array.subtitles_size_values).toList(),
            highlighted = highlightedKey == KEY_SUBTITLES_SIZE,
            stateVersion = resetVersion,
            onValueChanged = { onSubtitleSettingChanged() }
    )
    BooleanPreferenceRow(
            key = KEY_SUBTITLES_BOLD,
            settings = settings,
            title = stringResource(R.string.subtitles_bold_title),
            defaultValue = false,
            highlighted = highlightedKey == KEY_SUBTITLES_BOLD,
            onAfterChange = { onSubtitleSettingChanged() }
    )
    ColorPreferenceRow(
            key = KEY_SUBTITLES_COLOR,
            settings = settings,
            title = stringResource(R.string.subtitles_color),
            defaultValue = AndroidColor.WHITE,
            highlighted = highlightedKey == KEY_SUBTITLES_COLOR,
            stateVersion = resetVersion,
            onValueChanged = onSubtitleSettingChanged
    )
    IntSliderPreferenceRow(
            key = KEY_SUBTITLES_COLOR_OPACITY,
            settings = settings,
            title = stringResource(R.string.subtitles_opacity),
            defaultValue = 255,
            valueRange = 50f..255f,
            highlighted = highlightedKey == KEY_SUBTITLES_COLOR_OPACITY,
            stateVersion = resetVersion,
            onValueChanged = onSubtitleSettingChanged
    )

    PreferenceCategoryHeader(title = stringResource(R.string.subtitles_background_title))
    BooleanPreferenceRow(
            key = KEY_SUBTITLES_BACKGROUND,
            settings = settings,
            title = stringResource(R.string.subtitles_background_title),
            defaultValue = false,
            highlighted = highlightedKey == KEY_SUBTITLES_BACKGROUND,
            checked = subtitleBackgroundEnabled,
            onCheckedStateChange = { subtitleBackgroundEnabled = it },
            onAfterChange = { onSubtitleSettingChanged() }
    )
    if (subtitleBackgroundEnabled) {
        ColorPreferenceRow(
                key = KEY_SUBTITLES_BACKGROUND_COLOR,
                settings = settings,
                title = stringResource(R.string.subtitles_color),
                defaultValue = AndroidColor.BLACK,
                highlighted = highlightedKey == KEY_SUBTITLES_BACKGROUND_COLOR,
                stateVersion = resetVersion,
                onValueChanged = onSubtitleSettingChanged
        )
        IntSliderPreferenceRow(
                key = KEY_SUBTITLES_BACKGROUND_COLOR_OPACITY,
                settings = settings,
                title = stringResource(R.string.subtitles_opacity),
                defaultValue = 255,
                valueRange = 0f..255f,
                highlighted = highlightedKey == KEY_SUBTITLES_BACKGROUND_COLOR_OPACITY,
                stateVersion = resetVersion,
                onValueChanged = onSubtitleSettingChanged
        )
    }

    PreferenceCategoryHeader(title = stringResource(R.string.subtitles_shadow_title))
    BooleanPreferenceRow(
            key = KEY_SUBTITLES_SHADOW,
            settings = settings,
            title = stringResource(R.string.subtitles_shadow_title),
            defaultValue = true,
            highlighted = highlightedKey == KEY_SUBTITLES_SHADOW,
            checked = subtitleShadowEnabled,
            onCheckedStateChange = { subtitleShadowEnabled = it },
            onAfterChange = { onSubtitleSettingChanged() }
    )
    if (subtitleShadowEnabled) {
        ColorPreferenceRow(
                key = KEY_SUBTITLES_SHADOW_COLOR,
                settings = settings,
                title = stringResource(R.string.subtitles_color),
                defaultValue = AndroidColor.BLACK,
                highlighted = highlightedKey == KEY_SUBTITLES_SHADOW_COLOR,
                stateVersion = resetVersion,
                onValueChanged = onSubtitleSettingChanged
        )
        IntSliderPreferenceRow(
                key = KEY_SUBTITLES_SHADOW_COLOR_OPACITY,
                settings = settings,
                title = stringResource(R.string.subtitles_opacity),
                defaultValue = 128,
                valueRange = 0f..255f,
                highlighted = highlightedKey == KEY_SUBTITLES_SHADOW_COLOR_OPACITY,
                stateVersion = resetVersion,
                onValueChanged = onSubtitleSettingChanged
        )
    }

    PreferenceCategoryHeader(title = stringResource(R.string.subtitles_outline_title))
    BooleanPreferenceRow(
            key = KEY_SUBTITLES_OUTLINE,
            settings = settings,
            title = stringResource(R.string.subtitles_outline_title),
            defaultValue = true,
            highlighted = highlightedKey == KEY_SUBTITLES_OUTLINE,
            checked = subtitleOutlineEnabled,
            onCheckedStateChange = { subtitleOutlineEnabled = it },
            onAfterChange = { onSubtitleSettingChanged() }
    )
    if (subtitleOutlineEnabled) {
        ColorPreferenceRow(
                key = KEY_SUBTITLES_OUTLINE_COLOR,
                settings = settings,
                title = stringResource(R.string.subtitles_color),
                defaultValue = AndroidColor.BLACK,
                highlighted = highlightedKey == KEY_SUBTITLES_OUTLINE_COLOR,
                stateVersion = resetVersion,
                onValueChanged = onSubtitleSettingChanged
        )
        IntSliderPreferenceRow(
                key = KEY_SUBTITLES_OUTLINE_COLOR_OPACITY,
                settings = settings,
                title = stringResource(R.string.subtitles_opacity),
                defaultValue = 255,
                valueRange = 0f..255f,
                highlighted = highlightedKey == KEY_SUBTITLES_OUTLINE_COLOR_OPACITY,
                stateVersion = resetVersion,
                onValueChanged = onSubtitleSettingChanged
        )
    }
}

@Composable
private fun CastingPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onRestartAppRequired: () -> Unit,
        onRestartCastingPipeline: () -> Unit
) {
    var castingEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_ENABLE_CASTING, true))
    }
    PreferenceCategoryHeader(title = stringResource(R.string.casting_category))
    BooleanPreferenceRow(
            key = KEY_ENABLE_CASTING,
            settings = settings,
            title = stringResource(R.string.casting_switch_title),
            defaultValue = true,
            highlighted = highlightedKey == KEY_ENABLE_CASTING,
            checked = castingEnabled,
            onCheckedStateChange = { castingEnabled = it },
            onAfterChange = { onRestartAppRequired() }
    )
    BooleanPreferenceRow(
            key = KEY_CASTING_AUDIO_ONLY,
            settings = settings,
            title = stringResource(R.string.casting_audio_only),
            summary = stringResource(R.string.casting_audio_only_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_CASTING_AUDIO_ONLY,
            enabled = castingEnabled,
            onAfterChange = { onRestartCastingPipeline() }
    )
    BooleanPreferenceRow(
            key = KEY_CASTING_PASSTHROUGH,
            settings = settings,
            title = stringResource(R.string.casting_passthrough_title),
            summary = stringResource(R.string.casting_passthrough_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_CASTING_PASSTHROUGH,
            enabled = castingEnabled,
            onAfterChange = { onRestartCastingPipeline() }
    )
    ListPreferenceRow(
            key = KEY_CASTING_QUALITY,
            settings = settings,
            title = stringResource(R.string.casting_conversion_quality_title),
            summary = stringResource(R.string.casting_conversion_quality_summary),
            defaultValue = "2",
            entries = stringArrayResource(R.array.casting_quality).toList(),
            values = stringArrayResource(R.array.casting_quality_values).toList(),
            highlighted = highlightedKey == KEY_CASTING_QUALITY,
            enabled = castingEnabled,
            onValueChanged = { onRestartCastingPipeline() }
    )
}

@Composable
private fun ParentalControlPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        isPinCodeSet: () -> Boolean,
        onModifyPinCodeClick: () -> Unit,
        onSafeModeChanged: (Boolean) -> Unit
) {
    PreferenceCategoryHeader(title = stringResource(R.string.parental_control))
    NavigationPreferenceRow(
            key = "modify_pin_code",
            title = stringResource(R.string.pin_code_reason_modify),
            highlighted = highlightedKey == "modify_pin_code",
            onClick = onModifyPinCodeClick
    )
    BooleanPreferenceRow(
            key = KEY_RESTRICT_SETTINGS,
            settings = settings,
            title = stringResource(R.string.restrict_settings),
            defaultValue = false,
            highlighted = highlightedKey == KEY_RESTRICT_SETTINGS
    )
    BooleanPreferenceRow(
            key = KEY_SAFE_MODE,
            settings = settings,
            title = stringResource(R.string.safe_mode),
            summary = stringResource(R.string.safe_mode_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_SAFE_MODE,
            onAfterChange = { checked ->
                onSafeModeChanged(checked && isPinCodeSet())
            }
    )
}

@Composable
private fun RemoteAccessPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onStatusClick: () -> Unit,
        onRemoteAccessEnabledChanged: (Boolean) -> Unit,
        onNetworkBrowserChanged: () -> Unit
) {
    var remoteAccessEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_ENABLE_REMOTE_ACCESS, false))
    }
    BooleanPreferenceRow(
            key = KEY_ENABLE_REMOTE_ACCESS,
            settings = settings,
            title = stringResource(R.string.enable_remote_access),
            defaultValue = false,
            highlighted = highlightedKey == KEY_ENABLE_REMOTE_ACCESS,
            checked = remoteAccessEnabled,
            onCheckedStateChange = { remoteAccessEnabled = it },
            onAfterChange = onRemoteAccessEnabledChanged
    )
    NavigationPreferenceRow(
            key = KEY_REMOTE_ACCESS_STATUS,
            title = stringResource(R.string.remote_access_status),
            summary = stringResource(R.string.remote_access_status_summary),
            highlighted = highlightedKey == KEY_REMOTE_ACCESS_STATUS,
            enabled = remoteAccessEnabled,
            onClick = onStatusClick
    )

    PreferenceCategoryHeader(title = stringResource(R.string.remote_access_content))
    MultiSelectPreferenceRow(
            key = KEY_REMOTE_ACCESS_ML_CONTENT,
            settings = settings,
            title = stringResource(R.string.remote_access_medialibrary_content),
            dialogTitle = stringResource(R.string.remote_access_medialibrary_content),
            entries = stringArrayResource(R.array.remote_access_content_entries).toList(),
            values = stringArrayResource(R.array.remote_access_content_values).toList(),
            defaultValues = stringArrayResource(R.array.remote_access_content_values).toSet(),
            highlighted = highlightedKey == KEY_REMOTE_ACCESS_ML_CONTENT,
            enabled = remoteAccessEnabled
    )
    BooleanPreferenceRow(
            key = REMOTE_ACCESS_FILE_BROWSER_CONTENT,
            settings = settings,
            title = stringResource(R.string.remote_access_file_browser_content),
            defaultValue = false,
            highlighted = highlightedKey == REMOTE_ACCESS_FILE_BROWSER_CONTENT,
            enabled = remoteAccessEnabled
    )
    BooleanPreferenceRow(
            key = REMOTE_ACCESS_NETWORK_BROWSER_CONTENT,
            settings = settings,
            title = stringResource(R.string.remote_access_network_browser_content),
            defaultValue = false,
            highlighted = highlightedKey == REMOTE_ACCESS_NETWORK_BROWSER_CONTENT,
            enabled = remoteAccessEnabled,
            onAfterChange = { onNetworkBrowserChanged() }
    )
    BooleanPreferenceRow(
            key = REMOTE_ACCESS_HISTORY_CONTENT,
            settings = settings,
            title = stringResource(R.string.history),
            defaultValue = false,
            highlighted = highlightedKey == REMOTE_ACCESS_HISTORY_CONTENT,
            enabled = remoteAccessEnabled
    )
    BooleanPreferenceRow(
            key = REMOTE_ACCESS_PLAYBACK_CONTROL,
            settings = settings,
            title = stringResource(R.string.remote_access_playback_control),
            defaultValue = true,
            highlighted = highlightedKey == REMOTE_ACCESS_PLAYBACK_CONTROL,
            enabled = remoteAccessEnabled
    )
    BooleanPreferenceRow(
            key = REMOTE_ACCESS_LOGS,
            settings = settings,
            title = stringResource(R.string.remote_access_logs),
            defaultValue = false,
            highlighted = highlightedKey == REMOTE_ACCESS_LOGS,
            enabled = remoteAccessEnabled
    )
}

@Composable
private fun AndroidAutoPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onAndroidAutoSettingChanged: () -> Unit,
        onPlaybackSpeedGlobalChanged: () -> Unit
) {
    var queueInfoPosition by remember {
        mutableIntStateOf(settings.getString(KEY_ANDROID_AUTO_QUEUE_INFO_POS, "3")?.toIntOrNull() ?: 3)
    }
    PreferenceCategoryHeader(title = stringResource(R.string.interface_prefs_screen))
    SliderPreferenceRow(
            key = KEY_ANDROID_AUTO_TITLE_SCALE,
            settings = settings,
            title = stringResource(R.string.title_size),
            defaultValue = 100,
            valueRange = 50f..100f,
            highlighted = highlightedKey == KEY_ANDROID_AUTO_TITLE_SCALE,
            onValueChanged = onAndroidAutoSettingChanged
    )
    SliderPreferenceRow(
            key = KEY_ANDROID_AUTO_SUBTITLE_SCALE,
            settings = settings,
            title = stringResource(R.string.subtitle_size),
            defaultValue = 100,
            valueRange = 50f..100f,
            highlighted = highlightedKey == KEY_ANDROID_AUTO_SUBTITLE_SCALE,
            onValueChanged = onAndroidAutoSettingChanged
    )
    ListPreferenceRow(
            key = KEY_ANDROID_AUTO_QUEUE_INFO_POS,
            settings = settings,
            title = stringResource(R.string.queue_info),
            defaultValue = "3",
            entries = stringArrayResource(R.array.android_auto_queue_position).toList(),
            values = stringArrayResource(R.array.android_auto_queue_position_values).toList(),
            highlighted = highlightedKey == KEY_ANDROID_AUTO_QUEUE_INFO_POS,
            onValueChanged = { value ->
                queueInfoPosition = value.toIntOrNull() ?: 3
                settings.edit { putInt(KEY_ANDROID_AUTO_QUEUE_INFO_POS_VAL, queueInfoPosition) }
                onAndroidAutoSettingChanged()
            }
    )
    ListPreferenceRow(
            key = KEY_ANDROID_AUTO_QUEUE_FORMAT,
            settings = settings,
            title = stringResource(R.string.queue_format),
            defaultValue = "1",
            entries = stringArrayResource(R.array.android_auto_queue_format_entries).toList(),
            values = stringArrayResource(R.array.android_auto_queue_format_values).toList(),
            highlighted = highlightedKey == KEY_ANDROID_AUTO_QUEUE_FORMAT,
            enabled = queueInfoPosition > 0,
            onValueChanged = { value ->
                settings.edit { putInt("${KEY_ANDROID_AUTO_QUEUE_FORMAT}_val", value.toIntOrNull() ?: 1) }
                onAndroidAutoSettingChanged()
            }
    )

    PreferenceCategoryHeader(title = stringResource(R.string.controls_prefs_category))
    BooleanPreferenceRow(
            key = KEY_PLAYBACK_SPEED_AUDIO_GLOBAL,
            settings = settings,
            title = stringResource(R.string.audio_playback_speed_global),
            summary = stringResource(R.string.audio_playback_speed_global_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_PLAYBACK_SPEED_AUDIO_GLOBAL,
            onAfterChange = { onPlaybackSpeedGlobalChanged() }
    )
    BooleanPreferenceRow(
            key = KEY_ANDROID_AUTO_SPEED_BUTTONS,
            settings = settings,
            title = stringResource(R.string.enable_android_auto_speed_buttons),
            summary = stringResource(R.string.enable_android_auto_speed_buttons_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_ANDROID_AUTO_SPEED_BUTTONS,
            onAfterChange = { onAndroidAutoSettingChanged() }
    )
    BooleanPreferenceRow(
            key = KEY_ANDROID_AUTO_SEEK_BUTTONS,
            settings = settings,
            title = stringResource(R.string.enable_android_auto_seek_buttons),
            summary = stringResource(R.string.enable_android_auto_seek_buttons_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_ANDROID_AUTO_SEEK_BUTTONS,
            onAfterChange = { onAndroidAutoSettingChanged() }
    )
}

@Composable
private fun AdvancedPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onOptionalFeaturesClick: () -> Unit,
        onDebugLogsClick: () -> Unit,
        onInstallNightlyClick: () -> Unit,
        onClearPlaybackHistoryClick: () -> Unit,
        onClearMediaDatabaseClick: () -> Unit,
        onClearAppDataClick: () -> Unit,
        onQuitAppClick: () -> Unit,
        onDumpMediaDatabaseClick: () -> Unit,
        onDumpAppDatabaseClick: () -> Unit,
        onExportSettingsClick: () -> Unit,
        onRestoreSettingsClick: () -> Unit,
        onNetworkCachingChanged: (String) -> String,
        onAoutChanged: (String) -> Unit,
        onAdvancedRestartLibVlc: () -> Unit,
        onCustomLibVlcOptionsChanged: (String) -> Unit,
        onDav1dThreadNumberChanged: (String) -> String,
        onPreferSmbV1Changed: () -> Unit,
        onQuickPlayChanged: (Boolean) -> Unit
) {
    val isVisible = { key: String -> PreferenceVisibilityManager.isPreferenceVisible(key, settings, false) }
    var quickPlayEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_QUICK_PLAY, false))
    }
    var quickPlayDefaultEnabled by remember {
        mutableStateOf(settings.getBoolean(KEY_QUICK_PLAY_DEFAULT, false))
    }
    val useCompleteAout = isVLC4() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    if (isVisible(KEY_OPTIONAL_FEATURES)) {
        NavigationPreferenceRow(
                key = KEY_OPTIONAL_FEATURES,
                title = stringResource(R.string.optional_features),
                summary = stringResource(R.string.optional_features_summary),
                highlighted = highlightedKey == KEY_OPTIONAL_FEATURES,
                onClick = onOptionalFeaturesClick
        )
    }
    EditTextPreferenceRow(
            key = KEY_NETWORK_CACHING,
            settings = settings,
            title = stringResource(R.string.network_caching),
            summary = stringResource(R.string.network_caching_summary),
            defaultValue = "0",
            highlighted = highlightedKey == KEY_NETWORK_CACHING,
            maxLength = 5,
            keyboardType = KeyboardType.Number,
            onCommit = onNetworkCachingChanged
    )
    BooleanPreferenceRow(
            key = KEY_PREFER_SMBV1,
            settings = settings,
            title = stringResource(R.string.prefersmbv1),
            summary = stringResource(R.string.prefersmbv1_summary),
            defaultValue = true,
            highlighted = highlightedKey == KEY_PREFER_SMBV1,
            onAfterChange = { onPreferSmbV1Changed() }
    )
    EditTextPreferenceRow(
            key = KEY_HTTP_USER_AGENT,
            settings = settings,
            title = stringResource(R.string.http_user_agent),
            defaultValue = "",
            highlighted = highlightedKey == KEY_HTTP_USER_AGENT,
            singleLine = false,
            useValueSummary = true
    )
    NavigationPreferenceRow(
            key = KEY_QUIT_APP,
            title = stringResource(R.string.quit),
            highlighted = highlightedKey == KEY_QUIT_APP,
            onClick = onQuitAppClick
    )

    PreferenceCategoryHeader(title = stringResource(R.string.prefs_app_data))
    NavigationPreferenceRow(
            key = KEY_DUMP_MEDIA_DB,
            title = stringResource(R.string.dump_media_db),
            summary = stringResource(R.string.dump_media_db_summary),
            highlighted = highlightedKey == KEY_DUMP_MEDIA_DB,
            onClick = onDumpMediaDatabaseClick
    )
    NavigationPreferenceRow(
            key = KEY_DUMP_APP_DB,
            title = stringResource(R.string.dump_app_db),
            summary = stringResource(R.string.dump_media_db_summary),
            highlighted = highlightedKey == KEY_DUMP_APP_DB,
            onClick = onDumpAppDatabaseClick
    )
    NavigationPreferenceRow(
            key = KEY_CLEAR_MEDIA_DB,
            title = stringResource(R.string.clear_media_db),
            summary = stringResource(R.string.clear_media_database_summary),
            highlighted = highlightedKey == KEY_CLEAR_MEDIA_DB,
            onClick = onClearMediaDatabaseClick
    )
    NavigationPreferenceRow(
            key = KEY_CLEAR_APP_DATA,
            title = stringResource(R.string.clear_app_data),
            summary = stringResource(R.string.clear_app_data_summary),
            highlighted = highlightedKey == KEY_CLEAR_APP_DATA,
            onClick = onClearAppDataClick
    )
    NavigationPreferenceRow(
            key = KEY_CLEAR_HISTORY,
            title = stringResource(R.string.clear_playback_history),
            highlighted = highlightedKey == KEY_CLEAR_HISTORY,
            onClick = onClearPlaybackHistoryClick
    )
    NavigationPreferenceRow(
            key = KEY_EXPORT_SETTINGS,
            title = stringResource(R.string.export_settings),
            summary = stringResource(R.string.export_settings_summary),
            highlighted = highlightedKey == KEY_EXPORT_SETTINGS,
            onClick = onExportSettingsClick
    )
    NavigationPreferenceRow(
            key = KEY_RESTORE_SETTINGS,
            title = stringResource(R.string.restore_settings),
            summary = stringResource(R.string.restore_settings_summary),
            highlighted = highlightedKey == KEY_RESTORE_SETTINGS,
            onClick = onRestoreSettingsClick
    )

    PreferenceCategoryHeader(title = stringResource(R.string.performance_prefs_category))
    BooleanPreferenceRow(
            key = KEY_ENABLE_TIME_STRETCHING_AUDIO,
            settings = settings,
            title = stringResource(R.string.enable_time_stretching_audio),
            summary = stringResource(R.string.enable_time_stretching_audio_summary),
            defaultValue = LocalContext.current.resources.getBoolean(R.bool.time_stretching_default),
            highlighted = highlightedKey == KEY_ENABLE_TIME_STRETCHING_AUDIO,
            onAfterChange = { onAdvancedRestartLibVlc() }
    )
    ListPreferenceRow(
            key = KEY_OPENGL,
            settings = settings,
            title = stringResource(R.string.opengl_title),
            summary = stringResource(R.string.opengl_summary),
            defaultValue = "-1",
            entries = stringArrayResource(R.array.opengl_list).toList(),
            values = stringArrayResource(R.array.opengl_values).toList(),
            highlighted = highlightedKey == KEY_OPENGL,
            onValueChanged = { onAdvancedRestartLibVlc() }
    )
    if (isVisible(KEY_AOUT)) {
        ListPreferenceRow(
                key = KEY_AOUT,
                settings = settings,
                title = stringResource(R.string.aout),
                summary = stringResource(R.string.aout_summary),
                defaultValue = "0",
                entries = stringArrayResource(if (useCompleteAout) R.array.aouts_complete else R.array.aouts).toList(),
                values = stringArrayResource(if (useCompleteAout) R.array.aouts_complete_values else R.array.aouts_values).toList(),
                highlighted = highlightedKey == KEY_AOUT,
                onValueChanged = onAoutChanged
        )
    }
    ListPreferenceRow(
            key = KEY_DEBLOCKING,
            settings = settings,
            title = stringResource(R.string.deblocking),
            summary = stringResource(R.string.deblocking_summary),
            defaultValue = "-1",
            entries = stringArrayResource(R.array.deblocking_list).toList(),
            values = stringArrayResource(R.array.deblocking_values).toList(),
            highlighted = highlightedKey == KEY_DEBLOCKING,
            onValueChanged = { onAdvancedRestartLibVlc() }
    )
    BooleanPreferenceRow(
            key = KEY_ENABLE_FRAME_SKIP,
            settings = settings,
            title = stringResource(R.string.enable_frame_skip),
            summary = stringResource(R.string.enable_frame_skip_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_ENABLE_FRAME_SKIP,
            onAfterChange = { onAdvancedRestartLibVlc() }
    )
    EditTextPreferenceRow(
            key = DAV1D_THREAD_NUMBER,
            settings = settings,
            title = stringResource(R.string.dav1d_thread_number),
            defaultValue = "",
            highlighted = highlightedKey == DAV1D_THREAD_NUMBER,
            keyboardType = KeyboardType.Number,
            useValueSummary = true,
            onCommit = onDav1dThreadNumberChanged
    )
    BooleanPreferenceRow(
            key = KEY_QUICK_PLAY,
            settings = settings,
            title = stringResource(R.string.browser_quick_play),
            summary = stringResource(R.string.browser_quick_play_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_QUICK_PLAY,
            checked = quickPlayEnabled,
            onCheckedStateChange = { quickPlayEnabled = it },
            onAfterChange = { checked ->
                if (!checked) {
                    quickPlayDefaultEnabled = false
                    settings.edit { putBoolean(KEY_QUICK_PLAY_DEFAULT, false) }
                }
                onQuickPlayChanged(checked)
            }
    )
    BooleanPreferenceRow(
            key = KEY_QUICK_PLAY_DEFAULT,
            settings = settings,
            title = stringResource(R.string.browser_quick_play_default),
            summary = stringResource(R.string.browser_quick_play_default_summary),
            defaultValue = false,
            highlighted = highlightedKey == KEY_QUICK_PLAY_DEFAULT,
            enabled = quickPlayEnabled,
            checked = quickPlayDefaultEnabled,
            onCheckedStateChange = { quickPlayDefaultEnabled = it }
    )

    PreferenceCategoryHeader(title = stringResource(R.string.developer_prefs_category))
    BooleanPreferenceRow(
            key = KEY_ENABLE_VERBOSE_MODE,
            settings = settings,
            title = stringResource(R.string.enable_verbose_mode),
            summary = stringResource(R.string.enable_verbose_mode_summary),
            defaultValue = true,
            highlighted = highlightedKey == KEY_ENABLE_VERBOSE_MODE,
            onAfterChange = { onAdvancedRestartLibVlc() }
    )
    NavigationPreferenceRow(
            key = KEY_DEBUG_LOGS,
            title = stringResource(R.string.debug_logs),
            highlighted = highlightedKey == KEY_DEBUG_LOGS,
            onClick = onDebugLogsClick
    )
    NavigationPreferenceRow(
            key = KEY_NIGHTLY_INSTALL,
            title = stringResource(R.string.install_nightly),
            highlighted = highlightedKey == KEY_NIGHTLY_INSTALL,
            onClick = onInstallNightlyClick
    )
    if (isVisible(KEY_SHOW_UPDATE)) {
        BooleanPreferenceRow(
                key = KEY_SHOW_UPDATE,
                settings = settings,
                title = stringResource(R.string.update_nightly),
                summary = stringResource(R.string.update_nightly_summary),
                defaultValue = false,
                highlighted = highlightedKey == KEY_SHOW_UPDATE
        )
    }
    EditTextPreferenceRow(
            key = KEY_CUSTOM_LIBVLC_OPTIONS,
            settings = settings,
            title = stringResource(R.string.custom_libvlc_options),
            defaultValue = "",
            highlighted = highlightedKey == KEY_CUSTOM_LIBVLC_OPTIONS,
            singleLine = false,
            onCommit = { value ->
                settings.edit { putString(KEY_CUSTOM_LIBVLC_OPTIONS, value) }
                onCustomLibVlcOptionsChanged(value)
                value
            }
    )
}

@Composable
private fun OptionalFeaturesPreferencesContent(
        settings: SharedPreferences,
        highlightedKey: String?,
        onFeatureFlagWarningRequired: (FeatureFlag, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val featureStates = remember {
        mutableStateMapOf<FeatureFlag, Boolean>().apply {
            FeatureFlag.entries.forEach { put(it, FeatureFlagManager.isEnabled(context, it)) }
        }
    }
    if (FeatureFlag.entries.isEmpty()) {
        StaticPreferenceRow(
                summary = stringResource(R.string.optional_features_summary),
                highlighted = highlightedKey == KEY_OPTIONAL_FEATURES
        )
        return
    }
    FeatureFlag.entries.forEach { featureFlag ->
        val enabled = featureStates[featureFlag] ?: false
        val dependencyEnabled = featureFlag.dependsOn?.let { featureStates[it] ?: false } ?: true
        BooleanPreferenceRow(
                key = featureFlag.getKey(),
                settings = settings,
                title = stringResource(featureFlag.title),
                defaultValue = false,
                highlighted = highlightedKey == featureFlag.getKey(),
                enabled = dependencyEnabled,
                checked = enabled,
                onCheckedStateChange = { checked -> featureStates[featureFlag] = checked },
                onBeforeChange = { checked ->
                    if (checked && featureFlag.warning != null) {
                        onFeatureFlagWarningRequired(featureFlag) {
                            featureStates[featureFlag] = true
                            FeatureFlagManager.enable(context, featureFlag, true)
                        }
                        false
                    } else {
                        true
                    }
                },
                onAfterChange = { checked ->
                    FeatureFlagManager.enable(context, featureFlag, checked)
                }
        )
    }
}

@Composable
private fun EditTextPreferenceRow(
        key: String,
        settings: SharedPreferences,
        title: String,
        defaultValue: String,
        highlighted: Boolean,
        summary: String? = null,
        singleLine: Boolean = true,
        maxLength: Int? = null,
        keyboardType: KeyboardType = KeyboardType.Text,
        useValueSummary: Boolean = false,
        onCommit: (String) -> String = { value ->
            settings.edit { putString(key, value) }
            value
        }
) {
    var value by remember(key) {
        mutableStateOf(settings.getString(key, defaultValue) ?: defaultValue)
    }
    var dialogValue by remember(key) { mutableStateOf(value) }
    var expanded by remember(key) { mutableStateOf(false) }
    val resolvedSummary = if (useValueSummary) value.takeIf { it.isNotBlank() } else summary
    PreferenceRowFrame(
            highlighted = highlighted,
            role = Role.Button,
            onClick = {
                dialogValue = value
                expanded = true
            },
            textContent = {
                PreferenceText(
                        title = title,
                        summary = resolvedSummary,
                        enabled = true
                )
            },
            trailingContent = {
                Text(
                        text = ">",
                        color = VLCThemeDefaults.colors.fontLight,
                        style = MaterialTheme.typography.bodyLarge
                )
            }
    )
    if (expanded) {
        AlertDialog(
                onDismissRequest = { expanded = false },
                title = { Text(title) },
                text = {
                    OutlinedTextField(
                            value = dialogValue,
                            onValueChange = { newValue ->
                                dialogValue = maxLength?.let { newValue.take(it) } ?: newValue
                            },
                            singleLine = singleLine,
                            maxLines = if (singleLine) 1 else 5,
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                            modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                value = onCommit(dialogValue)
                                expanded = false
                            }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expanded = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
        )
    }
}

@Composable
private fun MultiSelectPreferenceRow(
        key: String,
        settings: SharedPreferences,
        title: String,
        dialogTitle: String,
        entries: List<String>,
        values: List<String>,
        defaultValues: Set<String>,
        highlighted: Boolean,
        enabled: Boolean
) {
    var selectedValues by remember(key) {
        mutableStateOf(settings.getStringSet(key, defaultValues)?.toSet() ?: defaultValues)
    }
    var pendingValues by remember(key) { mutableStateOf(selectedValues) }
    var expanded by remember(key) { mutableStateOf(false) }
    val summary = remoteAccessContentSummary(
            selectedValues = selectedValues,
            entries = entries,
            values = values
    )
    PreferenceRowFrame(
            highlighted = highlighted,
            enabled = enabled,
            role = Role.Button,
            onClick = {
                pendingValues = selectedValues
                expanded = true
            },
            textContent = {
                PreferenceText(
                        title = title,
                        summary = summary,
                        enabled = enabled
                )
            },
            trailingContent = {
                Text(
                        text = ">",
                        color = VLCThemeDefaults.colors.fontLight,
                        style = MaterialTheme.typography.bodyLarge
                )
            }
    )
    if (expanded) {
        AlertDialog(
                onDismissRequest = { expanded = false },
                title = { Text(dialogTitle) },
                text = {
                    Column {
                        entries.zip(values).forEach { (entry, value) ->
                            val checked = pendingValues.contains(value)
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(role = Role.Checkbox) {
                                                pendingValues = if (checked) {
                                                    pendingValues - value
                                                } else {
                                                    pendingValues + value
                                                }
                                            }
                                            .padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            pendingValues = if (isChecked) {
                                                pendingValues + value
                                            } else {
                                                pendingValues - value
                                            }
                                        }
                                )
                                Text(
                                        text = entry,
                                        color = VLCThemeDefaults.colors.fontDefault,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                selectedValues = pendingValues
                                settings.edit { putStringSet(key, pendingValues) }
                                expanded = false
                            }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expanded = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
        )
    }
}

@Composable
private fun remoteAccessContentSummary(
        selectedValues: Set<String>,
        entries: List<String>,
        values: List<String>
): String {
    val selectedEntries = values.zip(entries)
            .filter { (value, _) -> selectedValues.contains(value) }
            .map { (_, entry) -> entry }
    val disabledEntries = values.zip(entries)
            .filter { (value, _) -> !selectedValues.contains(value) }
            .map { (_, entry) -> entry }
    val selectedText = if (selectedEntries.isEmpty()) "-" else TextUtils.separatedString(*selectedEntries.toTypedArray())
    val disabledText = if (disabledEntries.isEmpty()) "-" else TextUtils.separatedString(*disabledEntries.toTypedArray())
    return stringResource(R.string.remote_access_medialibrary_content_summary, selectedText, disabledText)
}

private const val KEY_SOUNDFONT = "soundfont"

private fun defaultSleepTimerSummary(context: Context, settings: SharedPreferences): String {
    val interval = settings.getLong(SLEEP_TIMER_DEFAULT_INTERVAL, -1L)
    if (interval == -1L) return context.getString(R.string.disabled)
    val wait = settings.getBoolean(SLEEP_TIMER_DEFAULT_WAIT, false)
    val reset = settings.getBoolean(SLEEP_TIMER_DEFAULT_RESET_INTERACTION, false)
    return context.getString(
            R.string.default_sleep_timer_summary,
            Tools.millisToString(interval),
            if (wait) "true" else "false",
            if (reset) "true" else "false"
    )
}

private fun applySubtitlePreset(
        settings: SharedPreferences,
        preset: String,
        onBackgroundChanged: (Boolean) -> Unit,
        onShadowChanged: (Boolean) -> Unit,
        onOutlineChanged: (Boolean) -> Unit
) {
    var size = "16"
    var color = AndroidColor.WHITE
    var background = false
    var backgroundOpacity = 255
    var shadow = true
    var outline = true

    when (preset) {
        "1" -> size = "13"
        "2" -> {
            size = "10"
            background = true
            backgroundOpacity = 255
            shadow = false
            outline = false
        }
        "3" -> {
            background = true
            backgroundOpacity = 128
            shadow = false
        }
        "4" -> color = AndroidColor.YELLOW
        "5" -> {
            color = AndroidColor.YELLOW
            background = true
            backgroundOpacity = 128
            shadow = false
        }
    }

    settings.edit {
        putString(KEY_SUBTITLES_SIZE, size)
        putBoolean(KEY_SUBTITLES_BOLD, false)
        putInt(KEY_SUBTITLES_COLOR, color)
        putInt(KEY_SUBTITLES_COLOR_OPACITY, 255)
        putBoolean(KEY_SUBTITLES_BACKGROUND, background)
        putInt(KEY_SUBTITLES_BACKGROUND_COLOR, AndroidColor.BLACK)
        putInt(KEY_SUBTITLES_BACKGROUND_COLOR_OPACITY, backgroundOpacity)
        putBoolean(KEY_SUBTITLES_SHADOW, shadow)
        putInt(KEY_SUBTITLES_SHADOW_COLOR, AndroidColor.BLACK)
        putInt(KEY_SUBTITLES_SHADOW_COLOR_OPACITY, 128)
        putBoolean(KEY_SUBTITLES_OUTLINE, outline)
        putString(KEY_SUBTITLES_OUTLINE_SIZE, "4")
        putInt(KEY_SUBTITLES_OUTLINE_COLOR, AndroidColor.BLACK)
        putInt(KEY_SUBTITLES_OUTLINE_COLOR_OPACITY, 255)
    }
    onBackgroundChanged(background)
    onShadowChanged(shadow)
    onOutlineChanged(outline)
}

@Composable
private fun EphemeralListPreferenceRow(
        key: String,
        title: String,
        entries: List<String>,
        values: List<String>,
        highlighted: Boolean,
        onValueSelected: (String) -> Unit
) {
    val options = entries.zip(values).map { SubpagePreferenceOption(label = it.first, value = it.second) }
    var expanded by remember(key) { mutableStateOf(false) }
    PreferenceRowFrame(
            highlighted = highlighted,
            role = Role.Button,
            onClick = { expanded = true },
            textContent = {
                PreferenceText(
                        title = title,
                        summary = null,
                        enabled = true
                )
            },
            trailingContent = {
                Box {
                    Text(
                            text = ">",
                            color = VLCThemeDefaults.colors.fontLight,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable { expanded = true }
                    )
                    DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        expanded = false
                                        onValueSelected(option.value)
                                    }
                            )
                        }
                    }
                }
            }
    )
}

@Composable
private fun ColorPreferenceRow(
        key: String,
        settings: SharedPreferences,
        title: String,
        defaultValue: Int,
        highlighted: Boolean,
        stateVersion: Int,
        onValueChanged: () -> Unit
) {
    var colorValue by remember(key, stateVersion) {
        mutableIntStateOf(settings.getInt(key, defaultValue))
    }
    var dialogValue by remember(key, stateVersion) { mutableStateOf(colorToHex(colorValue)) }
    var expanded by remember(key) { mutableStateOf(false) }
    val parsedDialogColor = parseColorOrNull(dialogValue)
    PreferenceRowFrame(
            highlighted = highlighted,
            role = Role.Button,
            onClick = {
                dialogValue = colorToHex(colorValue)
                expanded = true
            },
            textContent = {
                PreferenceText(
                        title = title,
                        summary = colorToHex(colorValue),
                        enabled = true
                )
            },
            trailingContent = {
                Box(
                        modifier = Modifier
                                .size(28.dp)
                                .background(Color(colorValue))
                )
            }
    )
    if (expanded) {
        AlertDialog(
                onDismissRequest = { expanded = false },
                title = { Text(title) },
                text = {
                    Column {
                        OutlinedTextField(
                                value = dialogValue,
                                onValueChange = { dialogValue = it.take(9) },
                                singleLine = true,
                                isError = parsedDialogColor == null,
                                modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                                text = colorToHex(colorValue),
                                color = VLCThemeDefaults.colors.fontLight,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                            enabled = parsedDialogColor != null,
                            onClick = {
                                val newColor = parsedDialogColor ?: return@TextButton
                                colorValue = newColor
                                settings.edit { putInt(key, newColor) }
                                expanded = false
                                onValueChanged()
                            }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expanded = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
        )
    }
}

@Composable
private fun IntSliderPreferenceRow(
        key: String,
        settings: SharedPreferences,
        title: String,
        defaultValue: Int,
        valueRange: ClosedFloatingPointRange<Float>,
        highlighted: Boolean,
        stateVersion: Int,
        onValueChanged: () -> Unit
) {
    var value by remember(key, stateVersion) {
        mutableFloatStateOf(settings.getInt(key, defaultValue)
                .coerceIn(valueRange.start.roundToInt(), valueRange.endInclusive.roundToInt())
                .toFloat())
    }
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .vlcSelectionWash(highlighted)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            PreferenceText(
                    title = title,
                    summary = value.roundToInt().toString(),
                    enabled = true
            )
        }
        Slider(
                value = value,
                onValueChange = { newValue ->
                    value = newValue.roundToInt()
                            .coerceIn(valueRange.start.roundToInt(), valueRange.endInclusive.roundToInt())
                            .toFloat()
                    settings.edit { putInt(key, value.roundToInt()) }
                    onValueChanged()
                },
                valueRange = valueRange,
                modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

private fun colorToHex(color: Int): String = "#%06X".format(0xFFFFFF and color)

private fun parseColorOrNull(value: String): Int? {
    return runCatching {
        AndroidColor.parseColor(value.trim().let { if (it.startsWith("#")) it else "#$it" })
    }.getOrNull()
}

@Composable
private fun StaticPreferenceRow(
        summary: String,
        highlighted: Boolean
) {
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .vlcSelectionWash(highlighted)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            PreferenceText(
                    title = summary,
                    summary = null,
                    enabled = true
            )
        }
    }
}

@Composable
private fun ReplayGainNumberPreferenceRow(
        key: String,
        settings: SharedPreferences,
        title: String,
        summary: String,
        defaultValue: String,
        highlighted: Boolean,
        enabled: Boolean,
        onValueChanged: () -> Unit
) {
    var value by remember(key) {
        mutableStateOf(settings.getString(key, defaultValue) ?: defaultValue)
    }
    var dialogValue by remember(key) { mutableStateOf(value) }
    var expanded by remember(key) { mutableStateOf(false) }
    PreferenceRowFrame(
            highlighted = highlighted,
            enabled = enabled,
            role = Role.Button,
            onClick = {
                if (enabled) {
                    dialogValue = value
                    expanded = true
                }
            },
            textContent = {
                PreferenceText(
                        title = title,
                        summary = summary,
                        enabled = enabled
                )
            },
            trailingContent = {
                TextButton(
                        enabled = enabled,
                        onClick = {
                            dialogValue = value
                            expanded = true
                        }
                ) {
                    Text(value)
                }
            }
    )
    if (expanded) {
        AlertDialog(
                onDismissRequest = { expanded = false },
                title = { Text(title) },
                text = {
                    OutlinedTextField(
                            value = dialogValue,
                            onValueChange = { newValue ->
                                if (newValue.length <= 6) dialogValue = newValue
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                val formattedValue = formatReplayGainValue(dialogValue, defaultValue)
                                value = formattedValue
                                settings.edit { putString(key, formattedValue) }
                                expanded = false
                                onValueChanged()
                            }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expanded = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
        )
    }
}

private fun formatReplayGainValue(value: String, defaultValue: String): String {
    return try {
        DecimalFormat("###0.0###", DecimalFormatSymbols(Locale.ENGLISH)).format(value.toDouble())
    } catch (_: IllegalArgumentException) {
        defaultValue
    }
}

@Composable
private fun SliderPreferenceRow(
        key: String,
        settings: SharedPreferences,
        title: String,
        defaultValue: Int,
        valueRange: ClosedFloatingPointRange<Float>,
        highlighted: Boolean,
        onValueChanged: () -> Unit = {}
) {
    var value by remember(key) {
        mutableFloatStateOf(settings.getInt(key, defaultValue)
                .coerceIn(valueRange.start.roundToInt(), valueRange.endInclusive.roundToInt())
                .toFloat())
    }
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .vlcSelectionWash(highlighted)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            PreferenceText(
                    title = title,
                    summary = "${value.roundToInt()}%",
                    enabled = true
            )
        }
        Slider(
                value = value,
                onValueChange = { newValue ->
                    value = newValue.roundToInt()
                            .coerceIn(valueRange.start.roundToInt(), valueRange.endInclusive.roundToInt())
                            .toFloat()
                    settings.edit { putInt(key, value.roundToInt()) }
                    onValueChanged()
                },
                valueRange = valueRange,
                modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
