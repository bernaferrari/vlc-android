/*
 * *************************************************************************
 *  AndroidSettingsContributor.kt
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

package org.videolan.vlc.kmp

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.resources.AndroidDevices
import org.videolan.resources.AppContextProvider
import org.videolan.resources.EXPORT_SETTINGS_FILE
import org.videolan.resources.REMOTE_ACCESS_ONBOARDING
import org.videolan.resources.ROOM_DATABASE
import org.videolan.resources.SCHEME_PACKAGE
import org.videolan.resources.VLCInstance
import org.videolan.resources.util.restartRemoteAccess
import org.videolan.resources.util.startRemoteAccess
import org.videolan.resources.util.stopRemoteAccess
import org.videolan.tools.*
import org.videolan.tools.LocaleUtils.getLocales
import org.videolan.tools.Settings.isPinCodeSet
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.StartActivity
import org.videolan.vlc.VlcMigrationHelper
import org.videolan.vlc.gui.DebugLogActivity
import org.videolan.vlc.gui.EqualizerSettingsActivity
import org.videolan.vlc.gui.PinCodeActivity
import org.videolan.vlc.gui.PinCodeReason
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.browser.EXTRA_MRL
import org.videolan.vlc.gui.browser.FilePickerActivity
import org.videolan.vlc.gui.browser.KEY_PICKER_TYPE
import org.videolan.vlc.gui.dialogs.showAutoInfoComposeDialog
import org.videolan.vlc.gui.dialogs.showFeatureFlagWarningComposeDialog
import org.videolan.vlc.gui.dialogs.showPermissionListComposeDialog
import org.videolan.vlc.gui.dialogs.showSleepTimerComposeDialog
import org.videolan.vlc.gui.dialogs.showUpdateComposeDialog
import org.videolan.vlc.gui.helpers.MedialibraryUtils
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.hf.StoragePermissionsDelegate.Companion.getWritePermission
import org.videolan.vlc.gui.helpers.restartMediaPlayer
import org.videolan.vlc.gui.preferences.search.PreferenceParser
import org.videolan.vlc.isVLC4
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.providers.PickerType
import org.videolan.vlc.settings.SettingsAction
import org.videolan.vlc.settings.SettingsChoice
import org.videolan.vlc.settings.SettingsColor
import org.videolan.vlc.settings.SettingsConfirmation
import org.videolan.vlc.settings.SettingsContributor
import org.videolan.vlc.settings.SettingsGroup
import org.videolan.vlc.settings.SettingsGroupIds
import org.videolan.vlc.settings.SettingsIcons
import org.videolan.vlc.settings.SettingsItem
import org.videolan.vlc.settings.SettingsLink
import org.videolan.vlc.settings.SettingsMessage
import org.videolan.vlc.settings.SettingsMultiChoice
import org.videolan.vlc.settings.SettingsNote
import org.videolan.vlc.settings.SettingsOption
import org.videolan.vlc.settings.SettingsSection
import org.videolan.vlc.settings.SettingsSlider
import org.videolan.vlc.settings.SettingsTextInput
import org.videolan.vlc.settings.SettingsToggle
import org.videolan.vlc.util.FeatureFlag
import org.videolan.vlc.util.FeatureFlagManager
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.LocaleUtil
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.AutoUpdate
import org.videolan.vlc.util.share
import org.videolan.vlc.widget.utils.refreshAllWidgets
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Section ids contributed by Android. */
object AndroidSettingsSections {
    const val INTERFACE = "interface"
    const val VIDEO = "video"
    const val SUBTITLES = "subtitles"
    const val AUDIO = "audio"
    const val CASTING = "casting"
    const val PARENTAL_CONTROL = "parental_control"
    const val REMOTE_ACCESS = "remote_access"
    const val ANDROID_AUTO = "android_auto"
    const val ADVANCED = "advanced"
    const val OPTIONAL_FEATURES = "optional_features"
}

private const val KEY_DIRECTORIES = "directories"
private const val KEY_PERMISSIONS = "permissions"
private const val KEY_EQUALIZER = "equalizer"
private const val KEY_SOUNDFONT = "soundfont"
private const val KEY_DEFAULT_SLEEP_TIMER = "default_sleep_timer"
private const val KEY_SUBTITLES_PRESETS = "subtitles_presets"
private const val KEY_MODIFY_PIN = "modify_pin_code"
private const val KEY_REMOTE_ACCESS_STATUS = "remote_access_status"
private const val KEY_REMOTE_ACCESS_INFO_ACTION = "remote_access_onboarding"
private const val KEY_ANDROID_AUTO_INFO_ACTION = "android_auto_info"
private const val KEY_ANDROID_AUTO_QUEUE_INFO_POS = "android_auto_queue_info_pos"
private const val KEY_ANDROID_AUTO_QUEUE_FORMAT = "android_auto_queue_format"
private const val KEY_ANDROID_AUTO_TITLE_SCALE = "android_auto_title_scale_val"
private const val KEY_ANDROID_AUTO_SUBTITLE_SCALE = "android_auto_subtitle_scale_val"
private const val KEY_ANDROID_AUTO_SPEED_BUTTONS = "enable_android_auto_speed_buttons"
private const val KEY_ANDROID_AUTO_SEEK_BUTTONS = "enable_android_auto_seek_buttons"
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

/**
 * Android's contribution to the shared settings surface: every preference that used to live in
 * the forked PreferencesActivity screens, described with the shared model, plus the platform side
 * effects (libVLC restarts, pickers, PIN codes, Android Auto refreshes, …).
 *
 * SharedPreferences stays the single source of truth on Android; writes go through [putSingle],
 * which mirrors them into the shared DataStore.
 */
class AndroidSettingsContributor(private val appContext: Context) : SettingsContributor {

    private val prefs: SharedPreferences = Settings.getInstance(appContext)
    private val context: Context get() = AppContextProvider.appContext
    private val isTv: Boolean get() = Settings.showTvUi
    private var pendingSubtitlesRestart = false

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val changes: SharedFlow<Unit> = _changes

    private val _messages = MutableSharedFlow<SettingsMessage>(extraBufferCapacity = 4)
    override val messages: SharedFlow<SettingsMessage> = _messages

    // Held strongly: SharedPreferences only keeps weak references to its listeners.
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _changes.tryEmit(Unit) }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    // --- Small builders ------------------------------------------------------------------------

    private fun str(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun options(@ArrayRes entries: Int, @ArrayRes values: Int): List<SettingsOption> =
        context.resources.getStringArray(entries).zip(context.resources.getStringArray(values))
            .map { (label, value) -> SettingsOption(value = value, label = label) }

    private fun bool(key: String, default: Boolean) = prefs.getBoolean(key, default)
    private fun string(key: String, default: String) = prefs.getString(key, default) ?: default
    private fun int(key: String, default: Int) = prefs.getInt(key, default)

    private fun toggle(key: String, @StringRes title: Int, default: Boolean, @StringRes summary: Int? = null, enabled: Boolean = true) =
        SettingsToggle(key, str(title), bool(key, default), summary?.let(::str), enabled)

    private fun choice(key: String, @StringRes title: Int, default: String, @ArrayRes entries: Int, @ArrayRes values: Int, summary: String? = null, enabled: Boolean = true) =
        SettingsChoice(key, str(title), options(entries, values), string(key, default), summary, enabled)

    private fun action(key: String, @StringRes title: Int, @StringRes summary: Int? = null, external: Boolean = false, destructive: Boolean = false, confirmation: SettingsConfirmation? = null, enabled: Boolean = true) =
        SettingsAction(key, str(title), summary?.let(::str), enabled = enabled, external = external, destructive = destructive, confirmation = confirmation)

    private fun group(id: String, @StringRes title: Int?, vararg items: SettingsItem?) =
        SettingsGroup(id, title?.let(::str), items.filterNotNull())

    private fun localeOptions(@StringRes defaultLabel: Int, prependDeviceLocales: Boolean): List<SettingsOption> {
        val pair = if (prependDeviceLocales) {
            LocaleUtils.getLocalesUsedInProject(BuildConfig.TRANSLATION_ARRAY, str(defaultLabel), context.getLocales())
        } else {
            LocaleUtils.getLocalesUsedInProject(BuildConfig.TRANSLATION_ARRAY, str(defaultLabel))
        }
        return pair.localeEntries.zip(pair.localeEntryValues).map { (label, value) -> SettingsOption(value, label) }
    }

    // --- Visibility (formerly PreferenceVisibilityManager) -------------------------------------

    private val castingAvailable get() = !isTv
    private val androidAutoAvailable get() = !isTv
    private val remoteAccessAvailable get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
    private val aoutSelectable get() = VlcMigrationHelper.getAudioOutputFromDevice() == VlcMigrationHelper.AudioOutput.ALL

    // --- Root ----------------------------------------------------------------------------------

    override fun rootGroups(): List<SettingsGroup> = listOf(
        group(
            SettingsGroupIds.PLAYBACK, null,
            toggle(KEY_PERSISTENT_INCOGNITO, R.string.persistent_incognito_mode, true, R.string.persistent_incognito_mode_summary, enabled = bool(KEY_INCOGNITO, false)),
        ),
        group(
            SettingsGroupIds.LIBRARY, null,
            action(KEY_DIRECTORIES, R.string.medialibrary_directories, R.string.directories_summary, external = true),
            toggle(KEY_MEDIALIBRARY_AUTO_RESCAN, R.string.auto_rescan, true, R.string.auto_rescan_summary),
            toggle(KEY_INCLUDE_MISSING, R.string.browser_show_missing_media, true, R.string.browser_show_missing_media_summary),
            toggle(KEY_MEDIA_SEEN, R.string.media_seen, true, R.string.media_seen_summary),
            action(KEY_PERMISSIONS, R.string.permissions, R.string.permissions_summary).takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 },
        ),
        group(
            SettingsGroupIds.NETWORK, null,
            choice(KEY_METERED_CONNECTION, R.string.metered_connection, "0", R.array.metered_connection_list, R.array.metered_connection_values),
        ),
    )

    override fun sections(): List<SettingsSection> = listOfNotNull(
        interfaceSection(),
        videoSection(),
        subtitlesSection(),
        audioSection(),
        castingSection().takeIf { castingAvailable },
        parentalSection(),
        remoteAccessSection().takeIf { remoteAccessAvailable },
        androidAutoSection().takeIf { androidAutoAvailable },
        advancedSection(),
        optionalFeaturesSection().takeIf { FeatureFlag.entries.isNotEmpty() },
    )

    private fun interfaceSection() = SettingsSection(
        id = AndroidSettingsSections.INTERFACE,
        title = str(R.string.interface_prefs_screen),
        summary = str(R.string.settings_section_interface_summary),
        icon = SettingsIcons.Interface,
        groups = listOf(
            group(
                "general", null,
                toggle(PREF_TV_UI, R.string.tv_ui_title, false, R.string.tv_ui_summary),
                SettingsChoice(KEY_SET_LOCALE, str(R.string.set_locale), localeOptions(R.string.device_default, false), string(KEY_SET_LOCALE, "")),
                choice(LIST_TITLE_ELLIPSIZE, R.string.list_title_ellipsize, "0", R.array.list_title_ellipsize_list, R.array.list_title_ellipsize_values).takeIf { !isTv },
                SettingsAction(KEY_DEFAULT_SLEEP_TIMER, str(R.string.sleep_title), defaultSleepTimerSummary()),
            ),
            group(
                "video", R.string.video,
                toggle(PLAYLIST_MODE_VIDEO, R.string.force_play_all_title, false, R.string.force_play_all_summary).takeIf { isTv },
            ),
            group(
                "audio", R.string.audio,
                toggle(KEY_AUDIO_RESUME_CARD, R.string.audio_resume_card_title, true, R.string.audio_resume_card_summary).takeIf { !isTv },
                toggle(PLAYLIST_MODE_AUDIO, R.string.force_play_all_audio_title, false, R.string.force_play_all_audio_summary).takeIf { isTv },
                toggle(LOCKSCREEN_COVER, R.string.lockscreen_cover_title, true, R.string.lockscreen_cover_summary).takeIf { !isTv },
                toggle(SHOW_SEEK_IN_COMPACT_NOTIFICATION, R.string.show_seek_in_compact_notif_title, false, R.string.show_seek_in_compact_notif_summary).takeIf { !isTv },
            ),
        ),
    )

    private fun videoSection() = SettingsSection(
        id = AndroidSettingsSections.VIDEO,
        title = str(R.string.video_prefs_category),
        summary = str(R.string.settings_section_video_summary),
        icon = SettingsIcons.Video,
        groups = listOf(
            group(
                "decoding", null,
                choice(KEY_HARDWARE_ACCELERATION, R.string.hardware_acceleration, "-1", R.array.hardware_acceleration_list, R.array.hardware_acceleration_values, str(R.string.hardware_acceleration_summary)),
                SettingsChoice(
                    KEY_PREFERRED_RESOLUTION, str(R.string.preferred_resolution),
                    options(R.array.preferred_resolution, R.array.preferred_resolution_values),
                    string(KEY_PREFERRED_RESOLUTION, "-1"),
                ),
                toggle(KEY_ALWAYS_FAST_SEEK, R.string.always_fast_seek, false, R.string.always_fast_seek_summary),
                toggle(KEY_VIDEO_MATCH_FRAME_RATE, R.string.video_match_frame_rate_title, false, R.string.video_match_frame_rate_summary),
            ),
            group(
                "behaviour", null,
                choice(KEY_VIDEO_APP_SWITCH, R.string.video_app_switch_title, "0", R.array.video_app_switch_action_titles, R.array.video_app_switch_action_values, str(R.string.video_app_switch_summary))
                    .takeIf { !isTv || AndroidDevices.hasPiP },
                choice(SCREEN_ORIENTATION, R.string.screen_orientation, "99", R.array.screen_orientation_list, R.array.screen_orientation_values).takeIf { !isTv },
                toggle(POPUP_FORCE_LEGACY, R.string.popup_force_legacy_title, false, R.string.popup_force_legacy_summary).takeIf { AndroidDevices.pipAllowed },
                toggle(RESTORE_BACKGROUND_VIDEO, R.string.restore_background_video_title, false, R.string.restore_background_video_summary),
            ),
            group(
                "secondary_display", R.string.interface_secondary_display_category_title,
                SettingsNote("secondary_display_category_summary", str(R.string.interface_secondary_display_category_summary)).takeIf { !isTv },
                toggle(KEY_ENABLE_CLONE_MODE, R.string.enable_clone_mode, false, R.string.enable_clone_mode_summary).takeIf { !isTv },
            ),
        ),
    )

    private fun subtitlesSection(): SettingsSection {
        val preferredLanguage = string(KEY_SUBTITLE_PREFERRED_LANGUAGE, "")
        val opacityLabel: (Int) -> String = { "${(it * 100 + 127) / 255}%" }
        return SettingsSection(
            id = AndroidSettingsSections.SUBTITLES,
            title = str(R.string.subtitles_prefs_category),
            summary = str(R.string.settings_section_subtitles_summary),
            icon = SettingsIcons.Subtitles,
            groups = listOf(
                group(
                    "general", null,
                    // A preset applies several values at once; it has no persisted selection of its own.
                    SettingsChoice(KEY_SUBTITLES_PRESETS, str(R.string.subtitles_presets_title), options(R.array.subtitles_presets_entries, R.array.subtitles_presets_values), null),
                    toggle(KEY_SUBTITLES_AUTOLOAD, R.string.subtitles_autoload_title, true),
                    choice(KEY_SUBTITLE_TEXT_ENCODING, R.string.subtitle_text_encoding, "", R.array.subtitles_encoding_list, R.array.subtitles_encoding_values),
                    SettingsChoice(
                        KEY_SUBTITLE_PREFERRED_LANGUAGE, str(R.string.subtitle_preferred_language),
                        localeOptions(R.string.no_track_preference, true), preferredLanguage,
                    ),
                ),
                group(
                    "font", R.string.subtitles_font_style,
                    choice(KEY_SUBTITLES_SIZE, R.string.subtitles_size_title, "16", R.array.subtitles_size_entries, R.array.subtitles_size_values),
                    toggle(KEY_SUBTITLES_BOLD, R.string.subtitles_bold_title, false),
                    SettingsColor(KEY_SUBTITLES_COLOR, str(R.string.subtitles_color), int(KEY_SUBTITLES_COLOR, android.graphics.Color.WHITE)),
                    SettingsSlider(KEY_SUBTITLES_COLOR_OPACITY, str(R.string.subtitles_opacity), int(KEY_SUBTITLES_COLOR_OPACITY, 255), 50..255, valueLabel = opacityLabel),
                ),
                subtitleLayerGroup("background", R.string.subtitles_background_title, KEY_SUBTITLES_BACKGROUND, false, KEY_SUBTITLES_BACKGROUND_COLOR, KEY_SUBTITLES_BACKGROUND_COLOR_OPACITY, 255, opacityLabel),
                subtitleLayerGroup("shadow", R.string.subtitles_shadow_title, KEY_SUBTITLES_SHADOW, true, KEY_SUBTITLES_SHADOW_COLOR, KEY_SUBTITLES_SHADOW_COLOR_OPACITY, 128, opacityLabel),
                subtitleLayerGroup("outline", R.string.subtitles_outline_title, KEY_SUBTITLES_OUTLINE, true, KEY_SUBTITLES_OUTLINE_COLOR, KEY_SUBTITLES_OUTLINE_COLOR_OPACITY, 255, opacityLabel),
            ),
        )
    }

    private fun subtitleLayerGroup(
        id: String,
        @StringRes title: Int,
        enabledKey: String,
        enabledDefault: Boolean,
        colorKey: String,
        opacityKey: String,
        opacityDefault: Int,
        opacityLabel: (Int) -> String,
    ): SettingsGroup {
        val enabled = bool(enabledKey, enabledDefault)
        return group(
            id, title,
            toggle(enabledKey, title, enabledDefault),
            SettingsColor(colorKey, str(R.string.subtitles_color), int(colorKey, android.graphics.Color.BLACK)).takeIf { enabled },
            SettingsSlider(opacityKey, str(R.string.subtitles_opacity), int(opacityKey, opacityDefault), 0..255, valueLabel = opacityLabel).takeIf { enabled },
        )
    }

    private fun audioSection(): SettingsSection {
        val preferredLanguage = string(KEY_AUDIO_PREFERRED_LANGUAGE, "")
        val digitalOutput = bool(KEY_AUDIO_DIGITAL_OUTPUT, false)
        val headsetDetection = bool(KEY_ENABLE_HEADSET_DETECTION, true)
        val replayGain = bool(KEY_AUDIO_REPLAY_GAIN_ENABLE, false)
        return SettingsSection(
            id = AndroidSettingsSections.AUDIO,
            title = str(R.string.audio_prefs_category),
            summary = str(R.string.settings_section_audio_summary),
            icon = SettingsIcons.Audio,
            groups = listOf(
                group(
                    "general", null,
                    action(KEY_EQUALIZER, R.string.equalizer, external = true),
                    toggle(RESUME_PLAYBACK, R.string.resume_playback_title, true, R.string.resume_playback_summary).takeIf { AndroidDevices.isPhone && !isTv },
                    toggle(KEY_AUDIO_TASK_REMOVED, R.string.audio_task_cleared_title, false, R.string.audio_task_cleared_summary).takeIf { !isTv },
                    SettingsToggle(
                        KEY_AUDIO_DIGITAL_OUTPUT, str(R.string.audio_digital_title), digitalOutput,
                        str(if (digitalOutput) R.string.audio_digital_output_enabled else R.string.audio_digital_output_disabled),
                    ).takeIf { string(KEY_AOUT, "0") != "2" },
                    SettingsChoice(
                        KEY_AUDIO_PREFERRED_LANGUAGE, str(R.string.audio_preferred_language),
                        localeOptions(R.string.no_track_preference, true), preferredLanguage,
                        summary = if (preferredLanguage.isEmpty()) null else str(R.string.track_preference, LocaleUtil.getLocaleName(preferredLanguage)),
                    ),
                    choice(KEY_AUDIO_CONFIRM_RESUME, R.string.confirm_resume_audio_title, "0", R.array.ask_confirmation_entries, R.array.ask_confirmation_values),
                ),
                group(
                    "headset", R.string.headset_prefs_category,
                    toggle(KEY_ENABLE_HEADSET_DETECTION, R.string.enable_headset_detection, true, R.string.enable_headset_detection_summary).takeIf { !isTv },
                    toggle(KEY_ENABLE_PLAY_ON_HEADSET_INSERTION, R.string.enable_play_on_headset_insertion, false, R.string.enable_play_on_headset_insertion_summary, enabled = headsetDetection).takeIf { !isTv },
                    toggle(KEY_IGNORE_HEADSET_MEDIA_BUTTON_PRESSES, R.string.ignore_headset_media_button_presses, false, R.string.ignore_headset_media_button_presses_summary).takeIf { !isTv },
                ),
                group(
                    "replaygain", R.string.replaygain_prefs_category,
                    toggle(KEY_AUDIO_REPLAY_GAIN_ENABLE, R.string.replaygain_enable, false, R.string.replaygain_enable_summary),
                    choice(KEY_AUDIO_REPLAY_GAIN_MODE, R.string.replaygain_mode, "track", R.array.replaygain, R.array.replaygain_values, str(R.string.replaygain_mode_summary), enabled = replayGain),
                    SettingsTextInput(KEY_AUDIO_REPLAY_GAIN_PREAMP, str(R.string.replaygain_preamp), string(KEY_AUDIO_REPLAY_GAIN_PREAMP, "0.0"), str(R.string.replaygain_preamp_summary), numeric = true, maxLength = 6, enabled = replayGain),
                    SettingsTextInput(KEY_AUDIO_REPLAY_GAIN_DEFAULT, str(R.string.replaygain_default), string(KEY_AUDIO_REPLAY_GAIN_DEFAULT, "-7.0"), str(R.string.replaygain_default_summary), numeric = true, maxLength = 6, enabled = replayGain),
                    toggle(KEY_AUDIO_REPLAY_GAIN_PEAK_PROTECTION, R.string.replaygain_peak_protection, true, R.string.replaygain_peak_protection_summary, enabled = replayGain),
                ),
                group(
                    "advanced", R.string.advanced_prefs_category,
                    toggle(AUDIO_DUCKING, R.string.audio_ducking_title, true, R.string.audio_ducking_summary).takeIf { !AndroidUtil.isOOrLater },
                    action(KEY_SOUNDFONT, R.string.soundfont, R.string.soundfont_summary, external = true),
                ),
            ),
        )
    }

    private fun castingSection(): SettingsSection {
        val casting = bool(KEY_ENABLE_CASTING, true)
        return SettingsSection(
            id = AndroidSettingsSections.CASTING,
            title = str(R.string.casting_category),
            summary = str(R.string.settings_section_casting_summary),
            icon = SettingsIcons.Casting,
            groups = listOf(
                group(
                    "casting", null,
                    toggle(KEY_ENABLE_CASTING, R.string.casting_switch_title, true),
                    toggle(KEY_CASTING_AUDIO_ONLY, R.string.casting_audio_only, false, R.string.casting_audio_only_summary, enabled = casting),
                    toggle(KEY_CASTING_PASSTHROUGH, R.string.casting_passthrough_title, false, R.string.casting_passthrough_summary, enabled = casting),
                    choice(KEY_CASTING_QUALITY, R.string.casting_conversion_quality_title, "2", R.array.casting_quality, R.array.casting_quality_values, str(R.string.casting_conversion_quality_summary), enabled = casting),
                ),
            ),
        )
    }

    private fun parentalSection() = SettingsSection(
        id = AndroidSettingsSections.PARENTAL_CONTROL,
        title = str(R.string.parental_control),
        summary = str(R.string.settings_section_parental_control_summary),
        icon = SettingsIcons.ParentalControl,
        groups = listOf(
            group(
                "parental", null,
                action(KEY_MODIFY_PIN, R.string.pin_code_reason_modify),
                toggle(KEY_RESTRICT_SETTINGS, R.string.restrict_settings, false),
                toggle(KEY_SAFE_MODE, R.string.safe_mode, false, R.string.safe_mode_summary),
            ),
        ),
    )

    private fun remoteAccessSection(): SettingsSection {
        val enabled = bool(KEY_ENABLE_REMOTE_ACCESS, false)
        val contentEntries = context.resources.getStringArray(R.array.remote_access_content_entries)
        val contentValues = context.resources.getStringArray(R.array.remote_access_content_values)
        val selectedContent = prefs.getStringSet(KEY_REMOTE_ACCESS_ML_CONTENT, contentValues.toSet())?.toSet() ?: contentValues.toSet()
        return SettingsSection(
            id = AndroidSettingsSections.REMOTE_ACCESS,
            title = str(R.string.remote_access),
            summary = str(R.string.settings_section_remote_access_summary),
            icon = SettingsIcons.RemoteAccess,
            infoActionKey = KEY_REMOTE_ACCESS_INFO_ACTION,
            infoActionLabel = str(R.string.remote_access_info),
            groups = listOf(
                group(
                    "server", null,
                    toggle(KEY_ENABLE_REMOTE_ACCESS, R.string.enable_remote_access, false),
                    action(KEY_REMOTE_ACCESS_STATUS, R.string.remote_access_status, R.string.remote_access_status_summary, external = true, enabled = enabled),
                ),
                group(
                    "content", R.string.remote_access_content,
                    SettingsMultiChoice(
                        KEY_REMOTE_ACCESS_ML_CONTENT, str(R.string.remote_access_medialibrary_content),
                        contentEntries.zip(contentValues).map { (label, value) -> SettingsOption(value, label) },
                        selectedContent, enabled = enabled,
                    ),
                    toggle(REMOTE_ACCESS_FILE_BROWSER_CONTENT, R.string.remote_access_file_browser_content, false, enabled = enabled),
                    toggle(REMOTE_ACCESS_NETWORK_BROWSER_CONTENT, R.string.remote_access_network_browser_content, false, enabled = enabled),
                    toggle(REMOTE_ACCESS_HISTORY_CONTENT, R.string.history, false, enabled = enabled),
                    toggle(REMOTE_ACCESS_PLAYBACK_CONTROL, R.string.remote_access_playback_control, true, enabled = enabled),
                    toggle(REMOTE_ACCESS_LOGS, R.string.remote_access_logs, false, enabled = enabled),
                ),
            ),
        )
    }

    private fun androidAutoSection(): SettingsSection {
        val queuePosition = string(KEY_ANDROID_AUTO_QUEUE_INFO_POS, "3").toIntOrNull() ?: 3
        val percent: (Int) -> String = { "$it%" }
        return SettingsSection(
            id = AndroidSettingsSections.ANDROID_AUTO,
            title = str(R.string.android_auto),
            summary = str(R.string.settings_section_android_auto_summary),
            icon = SettingsIcons.AndroidAuto,
            infoActionKey = KEY_ANDROID_AUTO_INFO_ACTION,
            infoActionLabel = str(R.string.info),
            groups = listOf(
                group(
                    "interface", R.string.interface_prefs_screen,
                    SettingsSlider(KEY_ANDROID_AUTO_TITLE_SCALE, str(R.string.title_size), int(KEY_ANDROID_AUTO_TITLE_SCALE, 100).coerceIn(50, 100), 50..100, step = 5, valueLabel = percent),
                    SettingsSlider(KEY_ANDROID_AUTO_SUBTITLE_SCALE, str(R.string.subtitle_size), int(KEY_ANDROID_AUTO_SUBTITLE_SCALE, 100).coerceIn(50, 100), 50..100, step = 5, valueLabel = percent),
                    choice(KEY_ANDROID_AUTO_QUEUE_INFO_POS, R.string.queue_info, "3", R.array.android_auto_queue_position, R.array.android_auto_queue_position_values),
                    choice(KEY_ANDROID_AUTO_QUEUE_FORMAT, R.string.queue_format, "1", R.array.android_auto_queue_format_entries, R.array.android_auto_queue_format_values, enabled = queuePosition > 0),
                ),
                group(
                    "controls", R.string.controls_prefs_category,
                    toggle(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL, R.string.audio_playback_speed_global, false, R.string.audio_playback_speed_global_summary),
                    toggle(KEY_ANDROID_AUTO_SPEED_BUTTONS, R.string.enable_android_auto_speed_buttons, false, R.string.enable_android_auto_speed_buttons_summary),
                    toggle(KEY_ANDROID_AUTO_SEEK_BUTTONS, R.string.enable_android_auto_seek_buttons, false, R.string.enable_android_auto_seek_buttons_summary),
                ),
            ),
        )
    }

    private fun advancedSection(): SettingsSection {
        val quickPlay = bool(KEY_QUICK_PLAY, false)
        val completeAout = isVLC4() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val destructive = { title: Int, message: Int, confirm: Int -> SettingsConfirmation(str(title), str(message), str(confirm)) }
        return SettingsSection(
            id = AndroidSettingsSections.ADVANCED,
            title = str(R.string.advanced_prefs_category),
            summary = str(R.string.settings_section_advanced_summary),
            icon = SettingsIcons.Advanced,
            groups = listOf(
                group(
                    "general", null,
                    SettingsLink(AndroidSettingsSections.OPTIONAL_FEATURES, str(R.string.optional_features), AndroidSettingsSections.OPTIONAL_FEATURES, str(R.string.optional_features_summary))
                        .takeIf { FeatureFlag.entries.isNotEmpty() },
                    SettingsTextInput(KEY_NETWORK_CACHING, str(R.string.network_caching), string(KEY_NETWORK_CACHING, "0"), str(R.string.network_caching_summary), numeric = true, maxLength = 5),
                    toggle(KEY_PREFER_SMBV1, R.string.prefersmbv1, true, R.string.prefersmbv1_summary),
                    SettingsTextInput(KEY_HTTP_USER_AGENT, str(R.string.http_user_agent), string(KEY_HTTP_USER_AGENT, ""), multiline = true),
                    action(KEY_QUIT_APP, R.string.quit),
                ),
                group(
                    "app_data", R.string.prefs_app_data,
                    action(KEY_DUMP_MEDIA_DB, R.string.dump_media_db, R.string.dump_media_db_summary),
                    action(KEY_DUMP_APP_DB, R.string.dump_app_db, R.string.dump_media_db_summary),
                    action(KEY_EXPORT_SETTINGS, R.string.export_settings, R.string.export_settings_summary),
                    action(KEY_RESTORE_SETTINGS, R.string.restore_settings, R.string.restore_settings_summary, external = true),
                    action(KEY_CLEAR_HISTORY, R.string.clear_playback_history, destructive = true,
                        confirmation = destructive(R.string.clear_playback_history, R.string.clear_history_message, R.string.clear_history)),
                    action(KEY_CLEAR_MEDIA_DB, R.string.clear_media_db, R.string.clear_media_database_summary, destructive = true,
                        confirmation = destructive(R.string.clear_media_db, R.string.clear_media_db_message, R.string.clear)),
                    action(KEY_CLEAR_APP_DATA, R.string.clear_app_data, R.string.clear_app_data_summary, destructive = true,
                        confirmation = destructive(R.string.clear_app_data, R.string.clear_app_data_message, R.string.clear)),
                ),
                group(
                    "performance", R.string.performance_prefs_category,
                    toggle(KEY_ENABLE_TIME_STRETCHING_AUDIO, R.string.enable_time_stretching_audio, context.resources.getBoolean(R.bool.time_stretching_default), R.string.enable_time_stretching_audio_summary),
                    choice(KEY_OPENGL, R.string.opengl_title, "-1", R.array.opengl_list, R.array.opengl_values, str(R.string.opengl_summary)),
                    choice(
                        KEY_AOUT, R.string.aout, "0",
                        if (completeAout) R.array.aouts_complete else R.array.aouts,
                        if (completeAout) R.array.aouts_complete_values else R.array.aouts_values,
                        str(R.string.aout_summary),
                    ).takeIf { aoutSelectable },
                    choice(KEY_DEBLOCKING, R.string.deblocking, "-1", R.array.deblocking_list, R.array.deblocking_values, str(R.string.deblocking_summary)),
                    toggle(KEY_ENABLE_FRAME_SKIP, R.string.enable_frame_skip, false, R.string.enable_frame_skip_summary),
                    SettingsTextInput(DAV1D_THREAD_NUMBER, str(R.string.dav1d_thread_number), string(DAV1D_THREAD_NUMBER, ""), numeric = true, maxLength = 3),
                    toggle(KEY_QUICK_PLAY, R.string.browser_quick_play, false, R.string.browser_quick_play_summary).takeIf { !isTv },
                    toggle(KEY_QUICK_PLAY_DEFAULT, R.string.browser_quick_play_default, false, R.string.browser_quick_play_default_summary, enabled = quickPlay).takeIf { !isTv },
                ),
                group(
                    "developer", R.string.developer_prefs_category,
                    toggle(KEY_ENABLE_VERBOSE_MODE, R.string.enable_verbose_mode, true, R.string.enable_verbose_mode_summary),
                    action(KEY_DEBUG_LOGS, R.string.debug_logs, external = true),
                    action(KEY_NIGHTLY_INSTALL, R.string.install_nightly,
                        confirmation = SettingsConfirmation(str(R.string.install_nightly), str(R.string.install_nightly_alert), str(R.string.ok), destructive = false)),
                    toggle(KEY_SHOW_UPDATE, R.string.update_nightly, false, R.string.update_nightly_summary).takeIf { !isTv && BuildConfig.DEBUG },
                    SettingsTextInput(KEY_CUSTOM_LIBVLC_OPTIONS, str(R.string.custom_libvlc_options), string(KEY_CUSTOM_LIBVLC_OPTIONS, ""), multiline = true),
                ),
            ),
        )
    }

    private fun optionalFeaturesSection() = SettingsSection(
        id = AndroidSettingsSections.OPTIONAL_FEATURES,
        title = str(R.string.optional_features),
        icon = SettingsIcons.OptionalFeatures,
        parentId = AndroidSettingsSections.ADVANCED,
        groups = listOf(
            SettingsGroup("flags", null, FeatureFlag.entries.map { flag ->
                SettingsToggle(
                    key = flag.getKey(),
                    title = str(flag.title),
                    checked = FeatureFlagManager.isEnabled(appContext, flag),
                    enabled = flag.dependsOn?.let { FeatureFlagManager.isEnabled(appContext, it) } ?: true,
                )
            }),
        ),
    )

    private fun defaultSleepTimerSummary(): String {
        val interval = prefs.getLong(SLEEP_TIMER_DEFAULT_INTERVAL, -1L)
        if (interval == -1L) return str(R.string.disabled)
        val wait = prefs.getBoolean(SLEEP_TIMER_DEFAULT_WAIT, false)
        val reset = prefs.getBoolean(SLEEP_TIMER_DEFAULT_RESET_INTERACTION, false)
        return str(R.string.default_sleep_timer_summary, Tools.millisToString(interval), wait.toString(), reset.toString())
    }

    // --- Writes and side effects ---------------------------------------------------------------

    override fun onValueChange(key: String, value: Any) {
        when (key) {
            KEY_SUBTITLES_PRESETS -> {
                applySubtitlePreset(value as String)
                pendingSubtitlesRestart = true
                return
            }
            KEY_NETWORK_CACHING -> {
                onNetworkCachingChanged(value as String)
                return
            }
            DAV1D_THREAD_NUMBER -> {
                val threads = value as String
                if (threads.isNotEmpty() && (!threads.isDigitsOnly() || threads.toInt() < 1)) {
                    message(R.string.dav1d_thread_number_invalid)
                    put(key, "")
                } else {
                    put(key, threads)
                }
                return
            }
            KEY_AUDIO_REPLAY_GAIN_PREAMP, KEY_AUDIO_REPLAY_GAIN_DEFAULT -> {
                val fallback = if (key == KEY_AUDIO_REPLAY_GAIN_PREAMP) "0.0" else "-7.0"
                put(key, formatReplayGain(value as String, fallback))
                restartMediaPipeline()
                return
            }
        }
        FeatureFlagManager.getByKey(key)?.let { flag ->
            onFeatureFlagChanged(flag, value as Boolean)
            return
        }
        put(key, value)
        sideEffects(key, value)
    }

    /** Shared rows (history, resume, remote access…) still need Android's extra bookkeeping. */
    override fun afterValueChange(key: String, value: Any) = sideEffects(key, value)

    override fun confirmationFor(key: String, value: Any): SettingsConfirmation? = when {
        key == PLAYBACK_HISTORY && value == false ->
            SettingsConfirmation(str(R.string.playback_history_title), str(R.string.playback_history_warning), str(R.string.ok), destructive = false)
        key == AUDIO_RESUME_PLAYBACK && value == false ->
            SettingsConfirmation(str(R.string.audio_resume_playback_title), str(R.string.audio_resume_playback_warning), str(R.string.ok), destructive = false)
        else -> null
    }

    private fun put(key: String, value: Any) {
        prefs.putSingle(key, value)
    }

    private fun sideEffects(key: String, value: Any) {
        when (key) {
            // Shared rows
            PLAYBACK_HISTORY -> {
                if (value == true) {
                    prefs.edit {
                        putBoolean(AUDIO_RESUME_PLAYBACK, true)
                        putBoolean(VIDEO_RESUME_PLAYBACK, true)
                    }
                }
            }
            AUDIO_RESUME_PLAYBACK -> if (value == false) {
                prefs.edit {
                    remove(KEY_AUDIO_LAST_PLAYLIST)
                    remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
                    remove(KEY_CURRENT_AUDIO_RESUME_TITLE)
                    remove(KEY_CURRENT_AUDIO_RESUME_ARTIST)
                    remove(KEY_CURRENT_AUDIO_RESUME_THUMB)
                    remove(KEY_CURRENT_AUDIO)
                    remove(KEY_CURRENT_MEDIA)
                    remove(KEY_CURRENT_MEDIA_RESUME)
                }
            }
            VIDEO_RESUME_PLAYBACK -> if (value == false) {
                prefs.edit {
                    remove(KEY_MEDIA_LAST_PLAYLIST)
                    remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
                    remove(KEY_CURRENT_MEDIA_RESUME)
                    remove(KEY_CURRENT_MEDIA)
                }
            }
            KEY_ENABLE_REMOTE_ACCESS -> if (value == true) appContext.startRemoteAccess() else appContext.stopRemoteAccess()
            KEY_INCOGNITO -> Settings.incognitoMode = value as Boolean

            // Interface
            PREF_TV_UI -> {
                Settings.tvUI = value as Boolean
                AndroidSettingsEffects.request(AndroidSettingsEffect.RESTART_APP)
            }
            KEY_SET_LOCALE -> {
                AndroidSettingsEffects.request(AndroidSettingsEffect.RESTART)
                settingsHostActivity?.let { UiTools.restartDialog(it) }
            }
            LIST_TITLE_ELLIPSIZE -> {
                Settings.listTitleEllipsize = (value as String).toIntOrNull() ?: 0
                AndroidSettingsEffects.request(AndroidSettingsEffect.RESTART)
            }
            KEY_INCLUDE_MISSING -> {
                Settings.includeMissing = value as Boolean
                AndroidSettingsEffects.request(AndroidSettingsEffect.RESTART)
            }
            KEY_MEDIA_SEEN -> AndroidSettingsEffects.request(AndroidSettingsEffect.UPDATE_SEEN_MEDIA)

            // Video
            KEY_VIDEO_APP_SWITCH -> settingsHostActivity?.let { activity ->
                if (!AndroidUtil.isOOrLater && value == "2" && !Permissions.canDrawOverlays(activity)) Permissions.checkDrawOverlaysPermission(activity)
            }
            POPUP_FORCE_LEGACY -> settingsHostActivity?.let { activity ->
                if (value == true && !Permissions.canDrawOverlays(activity)) Permissions.checkDrawOverlaysPermission(activity)
                if (value == false && !Permissions.isPiPAllowed(activity)) Permissions.checkPiPPermission(activity)
            }
            KEY_PREFERRED_RESOLUTION -> restartMediaPipeline()

            // Subtitles
            KEY_SUBTITLE_TEXT_ENCODING, KEY_SUBTITLES_SIZE, KEY_SUBTITLES_BOLD, KEY_SUBTITLES_COLOR, KEY_SUBTITLES_COLOR_OPACITY,
            KEY_SUBTITLES_BACKGROUND, KEY_SUBTITLES_BACKGROUND_COLOR, KEY_SUBTITLES_BACKGROUND_COLOR_OPACITY,
            KEY_SUBTITLES_SHADOW, KEY_SUBTITLES_SHADOW_COLOR, KEY_SUBTITLES_SHADOW_COLOR_OPACITY,
            KEY_SUBTITLES_OUTLINE, KEY_SUBTITLES_OUTLINE_COLOR, KEY_SUBTITLES_OUTLINE_COLOR_OPACITY -> pendingSubtitlesRestart = true

            // Audio
            KEY_ENABLE_HEADSET_DETECTION -> {
                val detection = PlaybackService.headSetDetection
                if (detection.hasObservers()) detection.value = value as Boolean
            }
            KEY_AUDIO_REPLAY_GAIN_ENABLE, KEY_AUDIO_REPLAY_GAIN_MODE, KEY_AUDIO_REPLAY_GAIN_PEAK_PROTECTION -> restartMediaPipeline()

            // Casting
            KEY_ENABLE_CASTING -> AndroidSettingsEffects.request(AndroidSettingsEffect.RESTART_APP)
            KEY_CASTING_AUDIO_ONLY, KEY_CASTING_PASSTHROUGH, KEY_CASTING_QUALITY -> restartMediaPipeline()

            // Parental control
            KEY_SAFE_MODE -> Settings.safeMode = value == true && appContext.isPinCodeSet()

            // Remote access content
            REMOTE_ACCESS_NETWORK_BROWSER_CONTENT -> appContext.restartRemoteAccess()

            // Android Auto
            KEY_ANDROID_AUTO_QUEUE_INFO_POS -> {
                prefs.edit { putInt(KEY_ANDROID_AUTO_QUEUE_INFO_POS_VAL, (value as String).toIntOrNull() ?: 3) }
                PlaybackService.updateState()
            }
            KEY_ANDROID_AUTO_QUEUE_FORMAT -> {
                prefs.edit { putInt("${KEY_ANDROID_AUTO_QUEUE_FORMAT}_val", (value as String).toIntOrNull() ?: 1) }
                PlaybackService.updateState()
            }
            KEY_ANDROID_AUTO_TITLE_SCALE, KEY_ANDROID_AUTO_SUBTITLE_SCALE, KEY_ANDROID_AUTO_SPEED_BUTTONS, KEY_ANDROID_AUTO_SEEK_BUTTONS ->
                PlaybackService.updateState()
            KEY_PLAYBACK_SPEED_AUDIO_GLOBAL -> {
                PlaybackService.instance?.let { service ->
                    service.playlistManager.getCurrentMedia()?.let { service.playlistManager.restoreSpeed(it) }
                }
                PlaybackService.updateState()
            }

            // Advanced
            KEY_PREFER_SMBV1 -> {
                AppScope.launch { VLCInstance.restart() }
                settingsHostActivity?.let { UiTools.restartDialog(it) }
            }
            KEY_ENABLE_TIME_STRETCHING_AUDIO, KEY_OPENGL, KEY_DEBLOCKING, KEY_ENABLE_FRAME_SKIP, KEY_ENABLE_VERBOSE_MODE -> restartMediaPipeline()
            KEY_AOUT -> {
                restartMediaPipeline()
                if (value == "2") put(KEY_AUDIO_DIGITAL_OUTPUT, false)
            }
            KEY_QUICK_PLAY -> {
                if (value == false) put(KEY_QUICK_PLAY_DEFAULT, false)
                AndroidSettingsEffects.request(AndroidSettingsEffect.RESTART)
            }
            KEY_CUSTOM_LIBVLC_OPTIONS -> AppScope.launch {
                try {
                    VLCInstance.restart()
                } catch (e: IllegalStateException) {
                    message(R.string.custom_libvlc_options_invalid)
                    put(KEY_CUSTOM_LIBVLC_OPTIONS, "")
                } finally {
                    restartMediaPlayer()
                }
                restartMediaPipelineNow()
            }
        }
        Settings.hydrateVlcSettingsCache()
    }

    override fun onAction(key: String) {
        val activity = settingsHostActivity
        when (key) {
            KEY_DIRECTORIES -> {
                if (Medialibrary.getInstance().isWorking) return message(R.string.settings_ml_block_scan)
                activity?.startActivity(Intent(activity, SecondaryActivity::class.java).putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.STORAGE_BROWSER))
                AndroidSettingsEffects.request(AndroidSettingsEffect.RESTART)
            }
            KEY_PERMISSIONS -> activity?.showPermissionListComposeDialog()
            KEY_EQUALIZER -> activity?.startActivity(Intent(activity, EqualizerSettingsActivity::class.java))
            KEY_SOUNDFONT -> pickFile(PickerType.SOUNDFONT) { mrl ->
                MediaUtils.useAsSoundFont(appContext, mrl.toUri())
                VLCInstance.restart()
                settingsHostActivity?.let { UiTools.restartDialog(it) }
            }
            KEY_DEFAULT_SLEEP_TIMER -> activity?.showSleepTimerComposeDialog(forDefault = true) { _changes.tryEmit(Unit) }
            KEY_MODIFY_PIN -> activity?.let {
                AppScope.launch {
                    if (it.awaitActivityResult(PinCodeActivity.getIntent(it, PinCodeReason.MODIFY)).resultCode == android.app.Activity.RESULT_OK) {
                        message(R.string.pin_code_modified)
                    }
                }
            }
            KEY_REMOTE_ACCESS_STATUS -> activity?.startActivity(Intent(activity, StartActivity::class.java).apply { action = "vlc.remoteaccess.share" })
            KEY_REMOTE_ACCESS_INFO_ACTION -> openRemoteAccessOnboarding()
            KEY_ANDROID_AUTO_INFO_ACTION -> activity?.showAutoInfoComposeDialog()
            KEY_QUIT_APP -> android.os.Process.killProcess(android.os.Process.myPid())
            KEY_DUMP_MEDIA_DB -> {
                if (Medialibrary.getInstance().isWorking) return message(R.string.settings_ml_block_scan)
                dump(File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + Medialibrary.VLC_MEDIA_DB_NAME)) { dst ->
                    FileUtils.copyFile(File(appContext.getDir("db", Context.MODE_PRIVATE).toString() + Medialibrary.VLC_MEDIA_DB_NAME), dst)
                }
            }
            KEY_DUMP_APP_DB -> dump(File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + ROOM_DATABASE)) { dst ->
                val files = File(appContext.getDir("db", Context.MODE_PRIVATE).parent!! + "/databases").listFiles()?.map { it.path }?.toTypedArray()
                files != null && FileUtils.zip(files, dst.path)
            }
            KEY_CLEAR_HISTORY -> {
                Medialibrary.getInstance().clearHistory(Medialibrary.HISTORY_TYPE_GLOBAL)
                prefs.edit {
                    remove(KEY_AUDIO_LAST_PLAYLIST)
                    remove(KEY_MEDIA_LAST_PLAYLIST)
                    remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
                    remove(KEY_CURRENT_AUDIO)
                    remove(KEY_CURRENT_MEDIA)
                    remove(KEY_CURRENT_MEDIA_RESUME)
                    remove(KEY_CURRENT_AUDIO_RESUME_TITLE)
                    remove(KEY_CURRENT_AUDIO_RESUME_ARTIST)
                    remove(KEY_CURRENT_AUDIO_RESUME_THUMB)
                }
            }
            KEY_CLEAR_MEDIA_DB -> clearMediaDatabase()
            KEY_CLEAR_APP_DATA -> (appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            KEY_EXPORT_SETTINGS -> activity?.let {
                val dst = File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + EXPORT_SETTINGS_FILE)
                AppScope.launch {
                    if (it.getWritePermission(Uri.fromFile(dst))) PreferenceParser.exportPreferences(it, dst)
                }
            }
            KEY_RESTORE_SETTINGS -> pickFile(PickerType.SETTINGS) { mrl -> restoreSettings(mrl.toUri()) }
            KEY_DEBUG_LOGS -> activity?.startActivity(Intent(activity, DebugLogActivity::class.java))
            KEY_NIGHTLY_INSTALL -> activity?.let {
                AppScope.launch {
                    AutoUpdate.checkUpdate(it.application, true) { url, date -> it.showUpdateComposeDialog(url, date, newInstall = true) }
                }
            }
        }
    }

    // --- Gates and lifecycle -------------------------------------------------------------------

    override suspend fun canOpenSettings(): Boolean {
        if (!prefs.getBoolean(KEY_RESTRICT_SETTINGS, false)) return true
        val activity = settingsHostActivity ?: return false
        return activity.awaitActivityResult(PinCodeActivity.getIntent(activity, PinCodeReason.CHECK)).resultCode == android.app.Activity.RESULT_OK
    }

    override suspend fun canOpenSection(id: String): Boolean {
        if (id != AndroidSettingsSections.PARENTAL_CONTROL || appContext.isPinCodeSet()) return true
        val activity = settingsHostActivity ?: return false
        return activity.awaitActivityResult(PinCodeActivity.getIntent(activity, PinCodeReason.FIRST_CREATION)).resultCode == android.app.Activity.RESULT_OK
    }

    override fun onSectionOpened(id: String) {
        when (id) {
            AndroidSettingsSections.VIDEO -> if (prefs.getBoolean(PREF_SHOW_VIDEO_SETTINGS_DISCLAIMER, false)) {
                _messages.tryEmit(SettingsMessage(str(R.string.video_settings_disclaimer), str(R.string.ok)) {
                    prefs.edit { putBoolean(PREF_SHOW_VIDEO_SETTINGS_DISCLAIMER, false) }
                })
            }
            AndroidSettingsSections.REMOTE_ACCESS -> if (!prefs.getBoolean(REMOTE_ACCESS_ONBOARDING, false)) {
                prefs.edit { putBoolean(REMOTE_ACCESS_ONBOARDING, true) }
                openRemoteAccessOnboarding()
            }
        }
    }

    override fun onSectionClosed(id: String) {
        if (id == AndroidSettingsSections.SUBTITLES) flushSubtitlesRestart()
    }

    override fun onSettingsClosed() {
        flushSubtitlesRestart()
        appContext.refreshAllWidgets()
        AndroidSettingsEffects.dispatchPending()
    }

    private fun flushSubtitlesRestart() {
        if (!pendingSubtitlesRestart) return
        pendingSubtitlesRestart = false
        restartMediaPipeline()
    }

    // --- Helpers -------------------------------------------------------------------------------

    private fun message(@StringRes text: Int) {
        _messages.tryEmit(SettingsMessage(str(text)))
    }

    private fun restartMediaPipeline() {
        AppScope.launch { restartMediaPipelineNow() }
    }

    private suspend fun restartMediaPipelineNow() {
        VLCInstance.restart()
        restartMediaPlayer()
    }

    private fun openRemoteAccessOnboarding() {
        val activity = settingsHostActivity ?: return
        activity.startActivity(Intent(Intent.ACTION_VIEW).apply { setClassName(activity, REMOTE_ACCESS_ONBOARDING) })
    }

    private fun pickFile(type: PickerType, onPicked: suspend (String) -> Unit) {
        val activity = settingsHostActivity ?: return
        AppScope.launch {
            val result = activity.awaitActivityResult(Intent(activity, FilePickerActivity::class.java).putExtra(KEY_PICKER_TYPE, type.ordinal))
            if (result.resultCode != android.app.Activity.RESULT_OK) return@launch
            result.data?.getStringExtra(EXTRA_MRL)?.let { onPicked(it) }
        }
    }

    private suspend fun restoreSettings(file: Uri) {
        val activity = settingsHostActivity ?: return
        try {
            PreferenceParser.restoreSettings(activity, file)
            if (prefs.getBoolean(POPUP_FORCE_LEGACY, false) && !Permissions.canDrawOverlays(activity)) {
                Permissions.checkDrawOverlaysPermission(activity)
            }
            VLCInstance.restart()
            UiTools.restartDialog(activity)
        } catch (e: Exception) {
            Log.e("AndroidSettings", "restoreSettings: ${e.message}", e)
            message(R.string.invalid_settings_file)
        }
    }

    private fun dump(dst: File, copy: (File) -> Boolean) {
        val activity = settingsHostActivity ?: return
        AppScope.launch {
            if (!activity.getWritePermission(Uri.fromFile(dst))) return@launch
            val copied = withContext(Dispatchers.IO) { copy(dst) }
            if (copied) {
                _messages.tryEmit(SettingsMessage(str(R.string.dump_db_succes), str(R.string.share)) { activity.share(dst) })
            } else {
                message(R.string.dump_db_failure)
            }
        }
    }

    private fun clearMediaDatabase() {
        val medialibrary = Medialibrary.getInstance()
        if (medialibrary.isWorking) return message(R.string.settings_ml_block_scan)
        AppScope.launch {
            val roots = medialibrary.foldersList
            withContext(Dispatchers.IO) {
                medialibrary.clearDatabase(false)
                try {
                    appContext.getExternalFilesDir(null)?.let { dir ->
                        File(dir.absolutePath + Medialibrary.MEDIALIB_FOLDER_NAME).listFiles()?.forEach { file ->
                            if (file.isFile) FileUtils.deleteFile(file)
                        }
                    }
                    BitmapCache.clear()
                } catch (e: IOException) {
                    Log.e("AndroidSettings", e.message, e)
                }
            }
            roots.forEach { root -> MedialibraryUtils.addDir(root.removePrefix("file://"), appContext) }
        }
    }

    private fun onNetworkCachingChanged(value: String) {
        val original = value.toIntOrNull()
        val normalized = original?.coerceIn(0, 60000) ?: 0
        prefs.edit {
            putString(KEY_NETWORK_CACHING, normalized.toString())
            putInt(KEY_NETWORK_CACHING_VALUE, normalized)
        }
        if (original == null || original != normalized) message(R.string.network_caching_popup)
        restartMediaPipeline()
    }

    private fun onFeatureFlagChanged(flag: FeatureFlag, enabled: Boolean) {
        val warning = flag.warning
        val activity = settingsHostActivity
        if (enabled && warning != null && activity != null) {
            activity.showFeatureFlagWarningComposeDialog(flag) { FeatureFlagManager.enable(appContext, flag, true) }
        } else {
            FeatureFlagManager.enable(appContext, flag, enabled)
        }
    }

    private fun formatReplayGain(value: String, fallback: String): String = try {
        DecimalFormat("###0.0###", DecimalFormatSymbols(Locale.ENGLISH)).format(value.toDouble())
    } catch (_: IllegalArgumentException) {
        fallback
    }

    private fun applySubtitlePreset(preset: String) {
        var size = "16"
        var color = android.graphics.Color.WHITE
        var background = false
        var backgroundOpacity = 255
        var shadow = true
        var outline = true
        when (preset) {
            "1" -> size = "13"
            "2" -> {
                size = "10"
                background = true
                shadow = false
                outline = false
            }
            "3" -> {
                background = true
                backgroundOpacity = 128
                shadow = false
            }
            "4" -> color = android.graphics.Color.YELLOW
            "5" -> {
                color = android.graphics.Color.YELLOW
                background = true
                backgroundOpacity = 128
                shadow = false
            }
        }
        prefs.edit {
            putString(KEY_SUBTITLES_SIZE, size)
            putBoolean(KEY_SUBTITLES_BOLD, false)
            putInt(KEY_SUBTITLES_COLOR, color)
            putInt(KEY_SUBTITLES_COLOR_OPACITY, 255)
            putBoolean(KEY_SUBTITLES_BACKGROUND, background)
            putInt(KEY_SUBTITLES_BACKGROUND_COLOR, android.graphics.Color.BLACK)
            putInt(KEY_SUBTITLES_BACKGROUND_COLOR_OPACITY, backgroundOpacity)
            putBoolean(KEY_SUBTITLES_SHADOW, shadow)
            putInt(KEY_SUBTITLES_SHADOW_COLOR, android.graphics.Color.BLACK)
            putInt(KEY_SUBTITLES_SHADOW_COLOR_OPACITY, 128)
            putBoolean(KEY_SUBTITLES_OUTLINE, outline)
            putString(KEY_SUBTITLES_OUTLINE_SIZE, "4")
            putInt(KEY_SUBTITLES_OUTLINE_COLOR, android.graphics.Color.BLACK)
            putInt(KEY_SUBTITLES_OUTLINE_COLOR_OPACITY, 255)
        }
    }
}
