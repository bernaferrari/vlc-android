/*
 * ************************************************************************
 *  PreferenceParser.kt
 * *************************************************************************
 * Copyright © 2021 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 *
 *
 */

package org.videolan.vlc.gui.preferences.search

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.XmlResourceParser
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.annotation.XmlRes
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER
import org.videolan.tools.AUDIO_DELAY_GLOBAL
import org.videolan.tools.AUDIO_PLAY_PROGRESS_MODE
import org.videolan.tools.BROWSER_SHOW_HIDDEN_FILES
import org.videolan.tools.BROWSER_SHOW_ONLY_MULTIMEDIA
import org.videolan.tools.CloseableUtils
import org.videolan.tools.DISPLAY_UNDER_NOTCH
import org.videolan.tools.ENABLE_BRIGHTNESS_GESTURE
import org.videolan.tools.ENABLE_DOUBLE_TAP_PLAY
import org.videolan.tools.ENABLE_DOUBLE_TAP_SEEK
import org.videolan.tools.ENABLE_FASTPLAY
import org.videolan.tools.ENABLE_SCALE_GESTURE
import org.videolan.tools.ENABLE_SEEK_BUTTONS
import org.videolan.tools.ENABLE_SWIPE_SEEK
import org.videolan.tools.ENABLE_VOLUME_GESTURE
import org.videolan.tools.FASTPLAY_SPEED
import org.videolan.tools.KEY_AUDIO_BOOST
import org.videolan.tools.KEY_AUDIO_FORCE_SHUFFLE
import org.videolan.tools.KEY_AUDIO_JUMP_DELAY
import org.videolan.tools.KEY_AUDIO_LONG_JUMP_DELAY
import org.videolan.tools.KEY_AUDIO_SHOW_BOOkMARK_BUTTONS
import org.videolan.tools.KEY_AUDIO_SHOW_BOOKMARK_MARKERS
import org.videolan.tools.KEY_AUDIO_SHOW_CHAPTER_BUTTONS
import org.videolan.tools.KEY_AUDIO_SHOW_TRACK_NUMBERS
import org.videolan.tools.KEY_BLURRED_COVER_BACKGROUND
import org.videolan.tools.KEY_CURRENT_EQUALIZER_ID
import org.videolan.tools.KEY_ARTISTS_SHOW_ALL
import org.videolan.tools.KEY_CUSTOM_LIBVLC_OPTIONS
import org.videolan.tools.KEY_INCOGNITO_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE
import org.videolan.tools.KEY_INCOGNITO_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE
import org.videolan.tools.KEY_PLAYBACK_SPEED_VIDEO_GLOBAL
import org.videolan.tools.KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE
import org.videolan.tools.KEY_SAVE_INDIVIDUAL_AUDIO_DELAY
import org.videolan.tools.KEY_SHOW_WHATS_NEW
import org.videolan.tools.KEY_VIDEO_CONFIRM_RESUME
import org.videolan.tools.KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY
import org.videolan.tools.KEY_VIDEO_JUMP_DELAY
import org.videolan.tools.KEY_VIDEO_LONG_JUMP_DELAY
import org.videolan.tools.LOCK_USE_SENSOR
import org.videolan.tools.POPUP_KEEPSCREEN
import org.videolan.tools.PREF_RESTORE_VIDEO_TIPS_SHOWN
import org.videolan.tools.PREF_SHOW_VIDEO_SETTINGS_DISCLAIMER
import org.videolan.tools.PREF_TIPS_SHOWN
import org.videolan.tools.PREF_WIDGETS_TIPS_SHOWN
import org.videolan.tools.SAVE_BRIGHTNESS
import org.videolan.tools.SCREENSHOT_MODE
import org.videolan.tools.SCREEN_ORIENTATION
import org.videolan.tools.Settings
import org.videolan.tools.VIDEO_HUD_TIMEOUT
import org.videolan.tools.VIDEO_TRANSITION_SHOW
import org.videolan.tools.deleteSharedPreferences
import org.videolan.tools.getContextWithLocale
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.gui.helpers.DefaultPlaybackAction
import org.videolan.vlc.gui.helpers.DefaultPlaybackActionMediaType
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.providers.medialibrary.AlbumsProvider
import org.videolan.vlc.providers.medialibrary.ArtistsProvider
import org.videolan.vlc.providers.medialibrary.FoldersProvider
import org.videolan.vlc.providers.medialibrary.GenresProvider
import org.videolan.vlc.providers.medialibrary.PlaylistsProvider
import org.videolan.vlc.providers.medialibrary.TracksProvider
import org.videolan.vlc.providers.medialibrary.VideoGroupsProvider
import org.videolan.vlc.providers.medialibrary.VideosProvider
import org.videolan.vlc.util.EqualizerExport
import org.videolan.vlc.util.EqualizerUtil
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.VersionMigration
import org.videolan.vlc.util.share
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

object PreferenceParser {
    const val VIDEO_CONTROLS_PARENT_SCREEN = -1001
    const val AUDIO_CONTROLS_PARENT_SCREEN = -1002

    // Other settings that should be backed up and restored
    val additionalSettings = arrayOf(
        KEY_SHOW_WHATS_NEW,
        AUDIO_DELAY_GLOBAL,
        AUDIO_PLAY_PROGRESS_MODE,
        KEY_PLAYBACK_SPEED_VIDEO_GLOBAL,
        KEY_PLAYBACK_SPEED_AUDIO_GLOBAL,
        KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE,
        KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE,
        KEY_INCOGNITO_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE,
        KEY_INCOGNITO_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE,
        SCREEN_ORIENTATION,
        PREF_TIPS_SHOWN,
        PREF_WIDGETS_TIPS_SHOWN,
        PREF_RESTORE_VIDEO_TIPS_SHOWN,
        PREF_SHOW_VIDEO_SETTINGS_DISCLAIMER,
        DISPLAY_UNDER_NOTCH,
        "equalizer_enabled",
        "equalizer_set",
        "equalizer_values",
        "equalizer_saved"
        )

    /**
     * Parses all the preferences available in the app.
     * @param context the context to be used to retrieve the preferences
     * @param parseUIPrefs whether to parse the UI preferences or not
     *
     * @return a list of [PreferenceItem]
     */
    fun parsePreferences(context: Context, parseUIPrefs: Boolean = false): ArrayList<PreferenceItem> {
        val result = ArrayList<PreferenceItem>()
        arrayListOf(R.xml.preferences, R.xml.preferences_adv, R.xml.preferences_audio, R.xml.preferences_parental_control, R.xml.preferences_casting, R.xml.preferences_subtitles, R.xml.preferences_ui, R.xml.preferences_video, R.xml.preferences_remote_access, R.xml.preferences_android_auto)
            .forEach {
                result.addAll(parsePreferences(context, it))
            }
        if (parseUIPrefs) {
            result.addAll(buildControlPreferenceItems(context, forVideo = true))
            result.addAll(buildControlPreferenceItems(context, forVideo = false))
        }
        return result
    }

    /**
     * Parses all the preferences available in the app.
     * @param context the context to be used to retrieve the preferences
     * @param forVideo if true, parses the video controls else the audio controls
     *
     * @return a list of [PreferenceItem]
     */
    fun parseControlPreferences(context: Context, forVideo: Boolean): ArrayList<PreferenceItem> {
        return buildControlPreferenceItems(context, forVideo)
    }

    private fun buildControlPreferenceItems(context: Context, forVideo: Boolean): ArrayList<PreferenceItem> {
        val englishContext = context.applicationContext.getContextWithLocale("en")
        val definitions = if (forVideo) videoControlPreferenceDefinitions() else audioControlPreferenceDefinitions()
        val parentScreen = if (forVideo) VIDEO_CONTROLS_PARENT_SCREEN else AUDIO_CONTROLS_PARENT_SCREEN
        return ArrayList(definitions.map { definition ->
            val summary = definition.summaryRes?.let { summaryRes ->
                definition.summaryArg?.let { context.getString(summaryRes, it) } ?: context.getString(summaryRes)
            } ?: ""
            val summaryEng = definition.summaryRes?.let { summaryRes ->
                definition.summaryArg?.let { englishContext.getString(summaryRes, it) } ?: englishContext.getString(summaryRes)
            } ?: ""
            PreferenceItem(
                key = definition.key,
                parentScreen = parentScreen,
                title = context.getString(definition.titleRes),
                summary = summary,
                titleEng = englishContext.getString(definition.titleRes),
                summaryEng = summaryEng,
                category = context.getString(definition.categoryRes),
                categoryEng = englishContext.getString(definition.categoryRes),
                defaultValue = definition.defaultValue
            )
        })
    }

    private fun videoControlPreferenceDefinitions() = listOf(
        ControlPreferenceDefinition(KEY_AUDIO_BOOST, R.string.audio_boost_title, R.string.audio_boost_summary, R.string.controls_setting, "true"),
        ControlPreferenceDefinition(KEY_SAVE_INDIVIDUAL_AUDIO_DELAY, R.string.save_audiodelay_title, R.string.save_audiodelay_summary, R.string.controls_setting, "true"),
        ControlPreferenceDefinition(KEY_VIDEO_CONFIRM_RESUME, R.string.confirm_resume_title, null, R.string.controls_setting, "0"),
        ControlPreferenceDefinition(ENABLE_VOLUME_GESTURE, R.string.enable_volume_gesture_title, R.string.enable_volume_gesture_summary, R.string.gestures, "true"),
        ControlPreferenceDefinition(ENABLE_BRIGHTNESS_GESTURE, R.string.enable_brightness_gesture_title, R.string.enable_brightness_gesture_summary, R.string.gestures, "true"),
        ControlPreferenceDefinition(SAVE_BRIGHTNESS, R.string.save_brightness_title, R.string.save_brightness_summary, R.string.gestures, "false"),
        ControlPreferenceDefinition(ENABLE_SWIPE_SEEK, R.string.enable_swipe_seek_title, R.string.enable_swipe_seek_summary, R.string.gestures, "true"),
        ControlPreferenceDefinition(ENABLE_SCALE_GESTURE, R.string.enable_scale_gesture_title, R.string.enable_scale_gesture_summary, R.string.gestures, "true"),
        ControlPreferenceDefinition(ENABLE_DOUBLE_TAP_SEEK, R.string.enable_double_tap_seek_title, R.string.enable_double_tap_seek_summary, R.string.gestures, "true"),
        ControlPreferenceDefinition(KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY, R.string.video_double_tap_jump_delay, R.string.jump_delay_summary, R.string.gestures, "10", "10"),
        ControlPreferenceDefinition(ENABLE_DOUBLE_TAP_PLAY, R.string.enable_double_tap_play_title, R.string.enable_double_tap_play_summary, R.string.gestures, "true"),
        ControlPreferenceDefinition(SCREENSHOT_MODE, R.string.enable_video_screenshot, null, R.string.gestures, "0"),
        ControlPreferenceDefinition(ENABLE_FASTPLAY, R.string.enable_tap_and_hold_fastplay_title, R.string.enable_tap_and_hold_fastplay_summary, R.string.gestures, "false"),
        ControlPreferenceDefinition(FASTPLAY_SPEED, R.string.fastplay_speed_title, null, R.string.gestures, "20"),
        ControlPreferenceDefinition(ENABLE_SEEK_BUTTONS, R.string.enable_seek_buttons, R.string.enable_seek_buttons_summary, R.string.player_controls, "false"),
        ControlPreferenceDefinition(KEY_VIDEO_JUMP_DELAY, R.string.jump_delay, R.string.jump_delay_summary, R.string.player_controls, "10", "10"),
        ControlPreferenceDefinition(KEY_VIDEO_LONG_JUMP_DELAY, R.string.long_jump_delay, R.string.jump_delay_summary, R.string.player_controls, "20", "20"),
        ControlPreferenceDefinition(POPUP_KEEPSCREEN, R.string.popup_keepscreen_title, R.string.popup_keepscreen_summary, R.string.player_controls, "false"),
        ControlPreferenceDefinition(VIDEO_HUD_TIMEOUT, R.string.video_hud_timeout, null, R.string.player_controls, "4"),
        ControlPreferenceDefinition(VIDEO_TRANSITION_SHOW, R.string.video_transition_title, R.string.video_transition_summary, R.string.player_controls, "true"),
        ControlPreferenceDefinition(LOCK_USE_SENSOR, R.string.lock_use_sensor_title, R.string.lock_use_sensor_summary, R.string.player_controls, "true")
    )

    private fun audioControlPreferenceDefinitions() = listOf(
        ControlPreferenceDefinition(KEY_AUDIO_JUMP_DELAY, R.string.jump_delay, R.string.jump_delay_summary, R.string.controls_prefs_category, "10", "10"),
        ControlPreferenceDefinition(KEY_AUDIO_LONG_JUMP_DELAY, R.string.long_jump_delay, R.string.jump_delay_summary, R.string.controls_prefs_category, "20", "20"),
        ControlPreferenceDefinition(KEY_AUDIO_FORCE_SHUFFLE, R.string.force_shuffle_title, R.string.force_shuffle_summary, R.string.controls_prefs_category, "false"),
        ControlPreferenceDefinition(KEY_BLURRED_COVER_BACKGROUND, R.string.blurred_cover_background_title, R.string.blurred_cover_background_summary, R.string.interface_prefs_screen, "true"),
        ControlPreferenceDefinition(KEY_AUDIO_SHOW_TRACK_NUMBERS, R.string.albums_show_track_numbers, null, R.string.interface_prefs_screen, "false"),
        ControlPreferenceDefinition(KEY_AUDIO_SHOW_CHAPTER_BUTTONS, R.string.show_chapter_buttons, R.string.show_chapter_buttons_summary, R.string.interface_prefs_screen, "true"),
        ControlPreferenceDefinition(KEY_AUDIO_SHOW_BOOkMARK_BUTTONS, R.string.show_bookmark_buttons, R.string.show_bookmark_buttons_summary, R.string.interface_prefs_screen, "true"),
        ControlPreferenceDefinition(KEY_AUDIO_SHOW_BOOKMARK_MARKERS, R.string.show_bookmark_markers, R.string.show_bookmark_markers_summary, R.string.interface_prefs_screen, "true")
    )

    private data class ControlPreferenceDefinition(
        val key: String,
        val titleRes: Int,
        val summaryRes: Int?,
        val categoryRes: Int,
        val defaultValue: String,
        val summaryArg: String? = null
    )

    /**
     * Compares the preference list with the set settings to get the list of the changed settings by the user
     * @param context the context to be used to retrieve the preferences
     * @param parseUIPrefs whether to parse the UI preferences or not
     * @param showTitle whether to show the title of the preference or not
     *
     * @return a list of changed settings in the form a of pair of the key and the value
     */
    private fun getAllChangedPrefs(context: Context, parseUIPrefs: Boolean = true, showTitle: Boolean = false): ArrayList<Pair<String, Any>> {
        val allPrefs = parsePreferences(context, parseUIPrefs = parseUIPrefs)
        val allSettings = Settings.getInstance(context).all
        val changedSettings = ArrayList<Pair<String, Any>>()
        allPrefs.forEach { pref ->
            allSettings.forEach { setting ->
                if (pref.key == setting.key && pref.key != KEY_CUSTOM_LIBVLC_OPTIONS) {
                    setting.value?.let {
                        if (!isSame(it, pref.defaultValue)) {
                            val first = if (showTitle) "${pref.key} (${pref.titleEng})" else pref.key
                            changedSettings.add(Pair(first, it))
                        }
                    }
                }
            }
        }
        return changedSettings
    }

    /**
     * Get all changed control prefs
     *
     * @param context the context to be used to retrieve the preferences
     * @param forVideo if true, returns the video controls else the audio controls
     * @param showTitle whether to show the title of the preference or not
     * @return a list of changed settings in the form a of pair of the key and the value
     */
    private fun getAllChangedControlPrefs(context: Context, forVideo: Boolean = false, showTitle: Boolean = false): ArrayList<Pair<String, Any>> {
        val allPrefs = parseControlPreferences(context, forVideo = forVideo)
        val allSettings = Settings.getInstance(context).all
        val changedSettings = ArrayList<Pair<String, Any>>()
        allPrefs.forEach { pref ->
            allSettings.forEach { setting ->
                if (pref.key == setting.key && pref.key != KEY_CUSTOM_LIBVLC_OPTIONS) {
                    setting.value?.let {
                        val first = if (showTitle) "${pref.key} (${pref.titleEng})" else pref.key
                        if (!isSame(it, pref.defaultValue)) changedSettings.add(Pair(first, it))
                    }
                }
            }
        }
        return changedSettings
    }

    /**
     * Compares a [SharedPreferences] item value to a retrieved String from the preference parsing
     * @param settingValue the found preference value
     * @param defaultValue the defaultValue [String] found by parsing the pref xml
     *
     * @return true if values are considered to be the same
     */
    private fun isSame(settingValue: Any, defaultValue: String?) = when {
        defaultValue == null -> false
        settingValue is Boolean -> settingValue.toString() == defaultValue
        else -> settingValue == defaultValue
    }

    /**
     * Get a string describing the preferences changed by the user
     * @param context the context to be used to retrieve the preferences
     *
     * @return a string of all the changed preferences
     */
    fun getChangedPrefsString(context: Context) = buildString {
        append("\r\nMain settings:\r\n")
        getAllChangedPrefs(context, parseUIPrefs = false, showTitle = true).forEach { append("* ${it.first} -> ${it.second}\r\n") }
        val videoControls = buildString {
            getAllChangedControlPrefs(context, forVideo = true, showTitle = true).forEach { append("* ${it.first} -> ${it.second}\r\n") }
        }
        if (videoControls.isNotBlank()) {
            append("\r\nVideo controls:\r\n")
            append(videoControls)
        }
        val audioControls = buildString {
            getAllChangedControlPrefs(context, forVideo = false, showTitle = true).forEach { append("* ${it.first} -> ${it.second}\r\n") }
        }
        if (audioControls.isNotBlank()) {
            append("\r\nAudio controls:\r\n")
            append(audioControls)
        }
        //display settings
        val displaySettings = buildString {
            val settings = Settings.getInstance(context)
            val englishContext = context.applicationContext.getContextWithLocale("en")
            DefaultPlaybackActionMediaType.entries.forEach {
                val currentPlaybackAction = it.getCurrentPlaybackAction(settings)
                if (currentPlaybackAction != DefaultPlaybackAction.PLAY) {
                    append("* ${it.defaultActionKey} -> ${englishContext.getString(currentPlaybackAction.title)}\r\n")
                }
            }

            for ((key) in settings.all) {
                if (key.startsWith("display_mode_")) {
                    append("* $key -> ${settings.getBoolean(key, false)}\r\n")
                }
            }

            for ((key) in settings.all) {
                if (key.endsWith("_only_favs")) {
                    append("* $key -> ${settings.getBoolean(key, false)}\r\n")
                }
            }

            if (settings.getBoolean(KEY_ARTISTS_SHOW_ALL, false))
                append("* $KEY_ARTISTS_SHOW_ALL -> true\r\n")

            if (settings.getBoolean(BROWSER_SHOW_ONLY_MULTIMEDIA, false))
                append("* $BROWSER_SHOW_ONLY_MULTIMEDIA -> true\r\n")

            if (!Settings.showTrackNumber)
                append("* $ALBUMS_SHOW_TRACK_NUMBER -> false\r\n")

            if (!settings.getBoolean(BROWSER_SHOW_HIDDEN_FILES, true))
                append("* $BROWSER_SHOW_HIDDEN_FILES -> false\r\n")

            //sorts
            arrayOf(
                AlbumsProvider::class.java, ArtistsProvider::class.java, FoldersProvider::class.java, GenresProvider::class.java, PlaylistsProvider::class.java, TracksProvider::class.java,
                VideoGroupsProvider::class.java, VideosProvider::class.java
            ).forEach {
                for ((key) in settings.all) {
                    if (key.startsWith(it.simpleName) && !key.endsWith("_desc") && !key.endsWith("_only_favs")) {
                        append("* Sort ${it.simpleName} -> ${settings.getInt(key, 0)} (${getSortName(settings.getInt(key, 0))}) - ${if (settings.getBoolean("${key}_desc", false)) "DESC" else "ASC"}\r\n")
                    }
                }
            }

        }
        if (displaySettings.isNotBlank()) {
            append("\r\nDisplay settings:\r\n")
            append(displaySettings)
        }
    }

    fun getSortName(index:Int) = when(index) {
        Medialibrary.SORT_DEFAULT -> "Default"
        Medialibrary.SORT_ALPHA -> "Alphabetical"
        Medialibrary.SORT_DURATION -> "Duration"
        Medialibrary.SORT_INSERTIONDATE -> "Insertion date"
        Medialibrary.SORT_LASTMODIFICATIONDATE -> "Last modification date"
        Medialibrary.SORT_RELEASEDATE -> "Release date"
        Medialibrary.SORT_FILESIZE -> "File size"
        Medialibrary.SORT_ARTIST -> "Artist"
        Medialibrary.SORT_PLAYCOUNT -> "Play count"
        Medialibrary.SORT_ALBUM -> "Album"
        Medialibrary.SORT_FILENAME -> "Filename"
        else -> "Unknown"
    }

    /**
     * Get a string describing the preferences changed by the user in json format
     * @param context the context to be used to retrieve the preferences
     *
     * @return a string of all the changed preferences
     */
    fun getChangedPrefsJson(context: Context):String  {
        val settingsEntries = arrayListOf<SettingEntry>()
        Settings.getInstance(context).all.forEach { setting ->
            if (setting.key !in Settings.getRestoreBlacklist())
                setting.value?.let {
                    settingsEntries.add(SettingEntry(setting.key, it, SettingType.getFromAny(it)))
                }
        }
        val settingsBackup = SettingsBackup(settingsEntries, EqualizerUtil.getEqualizerExport(context), VersionMigration.getCurrentVersion())
        val moshi = Moshi.Builder().build()
        val jsonAdapter: JsonAdapter<SettingsBackup> = moshi.adapter(SettingsBackup::class.java)
        return jsonAdapter.toJson(settingsBackup)!!
    }

    /**
     * Parse a preference xml resource to get a list of [PreferenceItem]
     * @param context the context to be used to retrieve the preferences
     * @param id the xml resource id to parse
     *
     * @return all the parsed items in the form of a [PreferenceItem] list
     */
    private fun parsePreferences(context: Context, @XmlRes id: Int): ArrayList<PreferenceItem> {
        var category = ""
        var categoryEng = ""
        val result = ArrayList<PreferenceItem>()
        val parser = context.resources.getXml(id)
        var eventType = -1
        val namespace = "http://schemas.android.com/apk/res/android"
        val appNamespace = "http://schemas.android.com/apk/res-auto"
        var firstPrefScreeFound = false
        val englishContext = context.applicationContext.getContextWithLocale("en")
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            if (eventType == XmlResourceParser.START_TAG) {
                val element = parser.name

                if (element != "PreferenceScreen" && !firstPrefScreeFound) {
                    firstPrefScreeFound = true
                    category = getValue(context, parser, namespace, "title")
                    categoryEng = getValue(englishContext, parser, namespace, "title")
                }
                if (element != "PreferenceCategory" && element != "Preference") {
                    var key = getValue(context, parser, namespace, "key")
                    if (key.isBlank()) key = getValue(context, parser, appNamespace, "key")
                    var title = getValue(context, parser, namespace, "title")
                    if (title.isBlank()) title = getValue(context, parser, appNamespace, "title")
                    var titleEng = getValue(englishContext, parser, namespace, "title")
                    if (titleEng.isBlank()) titleEng = getValue(englishContext, parser, appNamespace, "title")
                    var summary = getValue(context, parser, namespace, "summary")
                    if (summary.isBlank()) summary = getValue(context, parser, appNamespace, "summary")
                    var summaryEng = getValue(englishContext, parser, namespace, "summary")
                    if (summaryEng.isBlank()) summaryEng = getValue(englishContext, parser, appNamespace, "summary")
                    val defaultValue = getValue(context, parser, namespace, "defaultValue")
                    if (summary.contains("%s") && element == "ListPreference") {
                        //get the current value for the string substitution
                        try {
                            val rawValue = Settings.getInstance(context).getString(key, defaultValue) ?: ""
                            val entriesId = parser.getAttributeResourceValue(namespace, "entries", -1)
                            val entryValuesId = parser.getAttributeResourceValue(namespace, "entryValues", -1)
                            val index = context.resources.getStringArray(entryValuesId).indexOf(rawValue)
                            summary = summary.replace("%s", context.resources.getStringArray(entriesId)[index])
                            summaryEng = summaryEng.replace("%s", englishContext.resources.getStringArray(entriesId)[index])
                        } catch (e: Exception) {
                        }
                    }
                    if (key.isNotBlank()) result.add(PreferenceItem(key, id, title, summary, titleEng, summaryEng, category, categoryEng, defaultValue))
                }
            }
            eventType = parser.next()
        }
        return result
    }

    /**
     * Get the value of an xml node
     * @param context the context to be used to retrieve the value. This context can be localized in English to retrieve the strings
     * @param parser the [XmlResourceParser] to use to parse the attributes
     * @param namespace the namespace to use to parse the attributes
     * @param node the node to be parsed
     *
     * @return the parsed value
     */
    private fun getValue(context: Context, parser: XmlResourceParser, namespace: String, node: String): String {
        try {
            val titleResId = parser.getAttributeResourceValue(namespace, node, -1)
            return if (titleResId == -1) {
                parser.getAttributeValue(namespace, node)
            } else {
                context.resources.getString(titleResId)
            }
        } catch (e: Exception) {
        }
        return ""
    }

    /**
     * Export the preferences to a file
     *
     * @param activity the activity to use to export the preferences
     * @param dst the destination file
     */
    suspend fun exportPreferences(activity: Activity, dst: File) = withContext(Dispatchers.IO) {
        val changedPrefs = getChangedPrefsJson(activity)
        var success = false
        val stream: FileOutputStream
        try {
            stream = FileOutputStream(dst)
            val output = OutputStreamWriter(stream)
            val bw = BufferedWriter(output)
            try {
                bw.write(changedPrefs)
                success = true
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                CloseableUtils.close(bw)
                CloseableUtils.close(output)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        withContext(Dispatchers.Main) {
            if (success)
                if (activity is FragmentActivity && !Settings.tvUI)
                    UiTools.snackerConfirm(activity, activity.getString(R.string.export_settings_success), confirmMessage = R.string.share, overAudioPlayer = false) {
                        activity.share(dst)
                    }
                else Toast.makeText(activity, R.string.export_settings_success, Toast.LENGTH_LONG).show()
            else
                Toast.makeText(activity, R.string.export_settings_failure, Toast.LENGTH_LONG).show()
        }
    }


    suspend fun checkRestoreFile(file: Uri)= withContext(Dispatchers.IO) {
        file.path?.let {
            val changedPrefs = FileUtils.getStringFromFile(it)
            val moshi = Moshi.Builder().build()
            val adapter: JsonAdapter<SettingsBackup> = moshi.adapter(SettingsBackup::class.java)
            val savedSettings = adapter.fromJson(changedPrefs)

            if (savedSettings?.settings == null || savedSettings.version == 0) throw IllegalStateException("Invalid file")
        }
    }

    /**
     * Restore the preferences from a file
     *
     * @param activity the activity to use to restore the preferences
     * @param file the file to restore the preferences from
     */
    suspend fun restoreSettings(activity: Activity, file: Uri) = withContext(Dispatchers.IO) {
        file.path?.let {
            val changedPrefs = FileUtils.getStringFromFile(it)
            val moshi = Moshi.Builder().build()
            val adapter: JsonAdapter<SettingsBackup> = moshi.adapter(SettingsBackup::class.java)
            val savedSettings =  adapter.fromJson(changedPrefs)

            if (savedSettings?.settings == null || savedSettings.version == 0) throw IllegalStateException("Invalid file")


            //First create a dedicated shared preference for restoring
            val restorePrefsName = activity.packageName + "_restore"
            val restoringPrefs = activity.getSharedPreferences(restorePrefsName, Context.MODE_PRIVATE)

            restoringPrefs.edit {
                savedSettings.settings.forEach { settingEntry ->
                    when (settingEntry.type) {
                        SettingType.BOOLEAN -> putBoolean(settingEntry.key, settingEntry.value as Boolean)
                        SettingType.INT -> putInt(settingEntry.key, (settingEntry.value as Double).toInt())
                        SettingType.LONG -> putLong(settingEntry.key, (settingEntry.value as Double).toLong())
                        SettingType.FLOAT -> putFloat(settingEntry.key, (settingEntry.value as Double).toFloat())
                        SettingType.SET ->  putStringSet(settingEntry.key, (settingEntry.value as ArrayList<String>).toHashSet())

                        else -> putString(settingEntry.key, settingEntry.value as String)
                    }
                }
            }

            //Migrate the new shared preferences using the version of the restored json file
            VersionMigration.migrateVersion(activity, restoringPrefs, savedSettings?.version)

            // Copy all the restored settings to the main shared preferences
            restoringPrefs.all.forEach { setting ->
                setting.value?.let { newValue ->
                    Settings.getInstance(activity).putSingle(setting.key, newValue)
                }
            }

            EqualizerUtil.importAll(activity, savedSettings.equalizers, null)

            //wait a bit for the file to be available to be deleted
            delay(100)

            //Delete the restore preferences
           if (!deleteSharedPreferences(activity, restorePrefsName))
               Log.e("PreferenceParser", "Cannot delete restore preferences")

        }
    }
}

/**
 * Object describing a [androidx.preference.Preference] with useful values to search / display them
 */
@Parcelize
data class PreferenceItem(val key: String, val parentScreen: Int, val title: String, val summary: String, val titleEng:String, val summaryEng: String, val category: String, val categoryEng: String, val defaultValue:String?) : Parcelable



class SettingsBackup(val settings: List<SettingEntry>, val equalizers: EqualizerExport, val version: Int)

class SettingEntry (val key: String, val value: Any, val type: SettingType)

enum class SettingType {
    STRING, BOOLEAN, INT, LONG, FLOAT, SET;

    companion object {
        fun getFromAny(any: Any): SettingType {
            return when (any) {
                is Boolean -> BOOLEAN
                is Int -> INT
                is Long -> LONG
                is Float -> FLOAT
                is HashSet<*> -> SET
                else -> STRING
            }
        }
    }
}
