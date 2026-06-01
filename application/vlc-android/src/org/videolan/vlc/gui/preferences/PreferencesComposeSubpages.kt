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

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.videolan.tools.KEY_ANDROID_AUTO_QUEUE_INFO_POS_VAL
import org.videolan.tools.KEY_ALWAYS_FAST_SEEK
import org.videolan.tools.KEY_CASTING_AUDIO_ONLY
import org.videolan.tools.KEY_CASTING_PASSTHROUGH
import org.videolan.tools.KEY_CASTING_QUALITY
import org.videolan.tools.KEY_ENABLE_CASTING
import org.videolan.tools.KEY_ENABLE_CLONE_MODE
import org.videolan.tools.KEY_ENABLE_REMOTE_ACCESS
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL
import org.videolan.tools.KEY_PREFERRED_RESOLUTION
import org.videolan.tools.KEY_REMOTE_ACCESS_ML_CONTENT
import org.videolan.tools.KEY_RESTRICT_SETTINGS
import org.videolan.tools.KEY_SAFE_MODE
import org.videolan.tools.KEY_VIDEO_MATCH_FRAME_RATE
import org.videolan.tools.POPUP_FORCE_LEGACY
import org.videolan.tools.REMOTE_ACCESS_FILE_BROWSER_CONTENT
import org.videolan.tools.REMOTE_ACCESS_HISTORY_CONTENT
import org.videolan.tools.REMOTE_ACCESS_LOGS
import org.videolan.tools.REMOTE_ACCESS_NETWORK_BROWSER_CONTENT
import org.videolan.tools.REMOTE_ACCESS_PLAYBACK_CONTROL
import org.videolan.tools.RESTORE_BACKGROUND_VIDEO
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.util.TextUtils
import kotlin.math.roundToInt

private const val KEY_REMOTE_ACCESS_STATUS = "remote_access_status"
private const val KEY_ANDROID_AUTO_QUEUE_INFO_POS = "android_auto_queue_info_pos"
private const val KEY_ANDROID_AUTO_QUEUE_FORMAT = "android_auto_queue_format"
private const val KEY_ANDROID_AUTO_TITLE_SCALE = "android_auto_title_scale_val"
private const val KEY_ANDROID_AUTO_SUBTITLE_SCALE = "android_auto_subtitle_scale_val"
private const val KEY_ANDROID_AUTO_SPEED_BUTTONS = "enable_android_auto_speed_buttons"
private const val KEY_ANDROID_AUTO_SEEK_BUTTONS = "enable_android_auto_seek_buttons"

/**
 * Compose replacement for the small phone preference XML screens:
 * preferences_video.xml, preferences_casting.xml, preferences_parental_control.xml,
 * preferences_remote_access.xml, and preferences_android_auto.xml.
 *
 * Those XML files stay parseable by PreferenceParser for search metadata while this screen owns
 * the active phone rendering path.
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
        onPopupForceLegacyChanged: (Boolean) -> Unit
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                when (destination) {
                    PreferencesRootDestination.Video -> item {
                        VideoPreferencesContent(
                                settings = settings,
                                highlightedKey = highlightedKey,
                                onPreferredResolutionChanged = onPreferredResolutionChanged,
                                onPopupForceLegacyChanged = onPopupForceLegacyChanged
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
                    else -> item {
                        Text(
                                text = stringResource(R.string.preferences),
                                color = VLCThemeDefaults.colors.fontDefault,
                                style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
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

@Composable
private fun StaticPreferenceRow(
        summary: String,
        highlighted: Boolean
) {
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .background(if (highlighted) VLCThemeDefaults.colors.subtleSelection else VLCThemeDefaults.colors.backgroundDefault)
                    .padding(top = 10.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            PreferenceText(
                    title = summary,
                    summary = null,
                    enabled = true
            )
        }
        HorizontalDivider(
                color = VLCThemeDefaults.colors.defaultDivider,
                modifier = Modifier.padding(top = 10.dp)
        )
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
                    .background(if (highlighted) VLCThemeDefaults.colors.subtleSelection else VLCThemeDefaults.colors.backgroundDefault)
                    .padding(top = 10.dp)
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
        HorizontalDivider(
                color = VLCThemeDefaults.colors.defaultDivider,
                modifier = Modifier.padding(top = 4.dp)
        )
    }
}
