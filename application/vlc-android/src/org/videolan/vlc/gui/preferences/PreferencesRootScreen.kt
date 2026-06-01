/*
 * *************************************************************************
 *  PreferencesRootScreen.kt
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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.videolan.tools.AUDIO_RESUME_PLAYBACK
import org.videolan.tools.KEY_HARDWARE_ACCELERATION
import org.videolan.tools.KEY_MEDIALIBRARY_AUTO_RESCAN
import org.videolan.tools.KEY_METERED_CONNECTION
import org.videolan.tools.KEY_VIDEO_APP_SWITCH
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.SCREEN_ORIENTATION
import org.videolan.tools.VIDEO_RESUME_PLAYBACK
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

@Composable
internal fun PreferencesRootScreen(
        settings: SharedPreferences,
        highlightedKey: String?,
        isVisible: (String) -> Boolean,
        onDirectoriesClick: () -> Unit,
        onPermissionClick: () -> Unit,
        onEqualizerClick: () -> Unit,
        onCategoryClick: (PreferencesRootDestination) -> Unit,
        onPlaybackHistoryDisabled: () -> Unit,
        onAudioResumeDisabled: () -> Unit,
        onVideoResumeChanged: (Boolean) -> Unit,
        onVideoActionSwitchChanged: (String) -> Unit
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        var pendingWarning by remember { mutableStateOf<PendingWarning?>(null) }
        var playbackHistoryChecked by remember { mutableStateOf(settings.getBoolean(PLAYBACK_HISTORY, true)) }
        var videoResumeChecked by remember { mutableStateOf(settings.getBoolean(VIDEO_RESUME_PLAYBACK, true)) }
        var audioResumeChecked by remember { mutableStateOf(settings.getBoolean(AUDIO_RESUME_PLAYBACK, true)) }

        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .background(colors.backgroundDefault)
        ) {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    PreferenceCategoryHeader(title = stringResource(R.string.medialibrary))
                }
                item {
                    NavigationPreferenceRow(
                            key = "directories",
                            title = stringResource(R.string.medialibrary_directories),
                            summary = stringResource(R.string.directories_summary),
                            highlighted = highlightedKey == "directories",
                            onClick = onDirectoriesClick
                    )
                }
                item {
                    BooleanPreferenceRow(
                            key = KEY_MEDIALIBRARY_AUTO_RESCAN,
                            settings = settings,
                            title = stringResource(R.string.auto_rescan),
                            summary = stringResource(R.string.auto_rescan_summary),
                            defaultValue = true,
                            highlighted = highlightedKey == KEY_MEDIALIBRARY_AUTO_RESCAN
                    )
                }

                item {
                    PreferenceCategoryHeader(title = stringResource(R.string.video_prefs_category))
                }
                if (isVisible(KEY_VIDEO_APP_SWITCH)) {
                    item {
                        ListPreferenceRow(
                                key = KEY_VIDEO_APP_SWITCH,
                                settings = settings,
                                title = stringResource(R.string.video_app_switch_title),
                                summary = stringResource(R.string.video_app_switch_summary),
                                defaultValue = "0",
                                entries = stringArrayResource(R.array.video_app_switch_action_titles).toList(),
                                values = stringArrayResource(R.array.video_app_switch_action_values).toList(),
                                highlighted = highlightedKey == KEY_VIDEO_APP_SWITCH,
                                onValueChanged = onVideoActionSwitchChanged
                        )
                    }
                }
                item {
                    ListPreferenceRow(
                            key = KEY_HARDWARE_ACCELERATION,
                            settings = settings,
                            title = stringResource(R.string.hardware_acceleration),
                            summary = stringResource(R.string.hardware_acceleration_summary),
                            defaultValue = "-1",
                            entries = stringArrayResource(R.array.hardware_acceleration_list).toList(),
                            values = stringArrayResource(R.array.hardware_acceleration_values).toList(),
                            highlighted = highlightedKey == KEY_HARDWARE_ACCELERATION
                    )
                }
                if (isVisible(SCREEN_ORIENTATION)) {
                    item {
                        ListPreferenceRow(
                                key = SCREEN_ORIENTATION,
                                settings = settings,
                                title = stringResource(R.string.screen_orientation),
                                defaultValue = "99",
                                entries = stringArrayResource(R.array.screen_orientation_list).toList(),
                                values = stringArrayResource(R.array.screen_orientation_values).toList(),
                                highlighted = highlightedKey == SCREEN_ORIENTATION
                        )
                    }
                }

                item {
                    PreferenceCategoryHeader(title = stringResource(R.string.network))
                }
                item {
                    ListPreferenceRow(
                            key = KEY_METERED_CONNECTION,
                            settings = settings,
                            title = stringResource(R.string.metered_connection),
                            defaultValue = "0",
                            entries = stringArrayResource(R.array.metered_connection_list).toList(),
                            values = stringArrayResource(R.array.metered_connection_values).toList(),
                            highlighted = highlightedKey == KEY_METERED_CONNECTION
                    )
                }

                if (isVisible("permissions_title")) {
                    item {
                        PreferenceCategoryHeader(title = stringResource(R.string.permissions))
                    }
                    item {
                        NavigationPreferenceRow(
                                key = "permissions",
                                title = stringResource(R.string.permissions),
                                summary = stringResource(R.string.permissions_summary),
                                highlighted = highlightedKey == "permissions",
                                onClick = onPermissionClick
                        )
                    }
                }

                item {
                    PreferenceCategoryHeader(title = stringResource(R.string.history))
                }
                item {
                    BooleanPreferenceRow(
                            key = PLAYBACK_HISTORY,
                            settings = settings,
                            title = stringResource(R.string.playback_history_title),
                            summary = stringResource(R.string.playback_history_summary),
                            defaultValue = true,
                            highlighted = highlightedKey == PLAYBACK_HISTORY,
                            checked = playbackHistoryChecked,
                            onCheckedStateChange = { playbackHistoryChecked = it },
                            onBeforeChange = { checked ->
                                if (!checked) {
                                    pendingWarning = PendingWarning(
                                            title = R.string.playback_history_title,
                                            message = R.string.playback_history_warning,
                                            onConfirm = {
                                                onPlaybackHistoryDisabled()
                                                playbackHistoryChecked = false
                                            }
                                    )
                                    false
                                } else {
                                    settings.edit {
                                        putBoolean(AUDIO_RESUME_PLAYBACK, true)
                                        putBoolean(VIDEO_RESUME_PLAYBACK, true)
                                    }
                                    audioResumeChecked = true
                                    videoResumeChecked = true
                                    true
                                }
                            }
                    )
                }
                item {
                    BooleanPreferenceRow(
                            key = VIDEO_RESUME_PLAYBACK,
                            settings = settings,
                            title = stringResource(R.string.video_resume_playback_title),
                            summary = stringResource(R.string.video_resume_playback_summary),
                            defaultValue = true,
                            highlighted = highlightedKey == VIDEO_RESUME_PLAYBACK,
                            checked = videoResumeChecked,
                            onCheckedStateChange = { videoResumeChecked = it },
                            onAfterChange = onVideoResumeChanged
                    )
                }
                item {
                    BooleanPreferenceRow(
                            key = AUDIO_RESUME_PLAYBACK,
                            settings = settings,
                            title = stringResource(R.string.audio_resume_playback_title),
                            summary = stringResource(R.string.audio_resume_playback_summary),
                            defaultValue = true,
                            highlighted = highlightedKey == AUDIO_RESUME_PLAYBACK,
                            checked = audioResumeChecked,
                            onCheckedStateChange = { audioResumeChecked = it },
                            onBeforeChange = { checked ->
                                if (!checked) {
                                    pendingWarning = PendingWarning(
                                            title = R.string.audio_resume_playback_title,
                                            message = R.string.audio_resume_playback_warning,
                                            onConfirm = {
                                                onAudioResumeDisabled()
                                                audioResumeChecked = false
                                            }
                                    )
                                    false
                                } else {
                                    true
                                }
                            }
                    )
                }

                item {
                    PreferenceCategoryHeader(title = stringResource(R.string.extra_prefs_category))
                }
                item {
                    NavigationPreferenceRow(
                            key = "ui_category",
                            title = stringResource(R.string.interface_prefs_screen),
                            icon = R.drawable.ic_ui,
                            highlighted = highlightedKey == "ui_category",
                            onClick = { onCategoryClick(PreferencesRootDestination.Ui) }
                    )
                }
                item {
                    NavigationPreferenceRow(
                            key = "video_category",
                            title = stringResource(R.string.video_prefs_category),
                            icon = R.drawable.ic_pref_video,
                            highlighted = highlightedKey == "video_category",
                            onClick = { onCategoryClick(PreferencesRootDestination.Video) }
                    )
                }
                item {
                    NavigationPreferenceRow(
                            key = "subtitles_category",
                            title = stringResource(R.string.subtitles_prefs_category),
                            icon = R.drawable.ic_pref_subtitles,
                            highlighted = highlightedKey == "subtitles_category",
                            onClick = { onCategoryClick(PreferencesRootDestination.Subtitles) }
                    )
                }
                item {
                    NavigationPreferenceRow(
                            key = "audio_category",
                            title = stringResource(R.string.audio_prefs_category),
                            icon = R.drawable.ic_pref_audio,
                            highlighted = highlightedKey == "audio_category",
                            onClick = { onCategoryClick(PreferencesRootDestination.Audio) }
                    )
                }
                item {
                    NavigationPreferenceRow(
                            key = "equalizer",
                            title = stringResource(R.string.equalizer),
                            icon = R.drawable.ic_pref_equalizer,
                            highlighted = highlightedKey == "equalizer",
                            onClick = onEqualizerClick
                    )
                }
                if (isVisible("casting_category")) {
                    item {
                        NavigationPreferenceRow(
                                key = "casting_category",
                                title = stringResource(R.string.casting_category),
                                icon = R.drawable.ic_renderer,
                                highlighted = highlightedKey == "casting_category",
                                onClick = { onCategoryClick(PreferencesRootDestination.Casting) }
                        )
                    }
                }
                item {
                    NavigationPreferenceRow(
                            key = "parental_control",
                            title = stringResource(R.string.parental_control),
                            icon = R.drawable.ic_pref_parental_control,
                            highlighted = highlightedKey == "parental_control",
                            onClick = { onCategoryClick(PreferencesRootDestination.ParentalControl) }
                    )
                }
                if (isVisible("remote_access_category")) {
                    item {
                        NavigationPreferenceRow(
                                key = "remote_access_category",
                                title = stringResource(R.string.remote_access),
                                icon = R.drawable.ic_pref_remote_access,
                                highlighted = highlightedKey == "remote_access_category",
                                onClick = { onCategoryClick(PreferencesRootDestination.RemoteAccess) }
                        )
                    }
                }
                if (isVisible("android_auto_category")) {
                    item {
                        NavigationPreferenceRow(
                                key = "android_auto_category",
                                title = stringResource(R.string.android_auto),
                                icon = R.drawable.ic_pref_android_auto,
                                highlighted = highlightedKey == "android_auto_category",
                                onClick = { onCategoryClick(PreferencesRootDestination.AndroidAuto) }
                        )
                    }
                }
                item {
                    NavigationPreferenceRow(
                            key = "adv_category",
                            title = stringResource(R.string.advanced_prefs_category),
                            icon = R.drawable.ic_pref_advanced_settings,
                            highlighted = highlightedKey == "adv_category",
                            onClick = { onCategoryClick(PreferencesRootDestination.Advanced) }
                    )
                }
            }
        }

        pendingWarning?.let { warning ->
            AlertDialog(
                    onDismissRequest = { pendingWarning = null },
                    title = { Text(stringResource(warning.title)) },
                    text = { Text(stringResource(warning.message)) },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    warning.onConfirm()
                                    pendingWarning = null
                                }
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingWarning = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
            )
        }
    }
}

internal enum class PreferencesRootDestination {
    Ui,
    Video,
    Subtitles,
    Audio,
    Casting,
    ParentalControl,
    RemoteAccess,
    AndroidAuto,
    Advanced,
    OptionalFeatures
}

private data class PendingWarning(
        val title: Int,
        val message: Int,
        val onConfirm: () -> Unit
)

private data class PreferenceOption(
        val label: String,
        val value: String
)

@Composable
internal fun PreferenceCategoryHeader(title: String) {
    Text(
            text = title,
            color = VLCThemeDefaults.colors.audioBrowserSeparator,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
internal fun NavigationPreferenceRow(
        key: String,
        title: String,
        summary: String? = null,
        @DrawableRes icon: Int? = null,
        highlighted: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit
) {
    PreferenceRowFrame(
            highlighted = highlighted,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
            leadingContent = icon?.let {
                {
                    Icon(
                            painter = painterResource(it),
                            contentDescription = null,
                            tint = VLCThemeDefaults.colors.fontDefault,
                            modifier = Modifier.size(24.dp)
                    )
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
                Text(
                        text = ">",
                        color = VLCThemeDefaults.colors.fontLight,
                        style = MaterialTheme.typography.bodyLarge
                )
            }
    )
}

@Composable
internal fun BooleanPreferenceRow(
        key: String,
        settings: SharedPreferences,
        title: String,
        summary: String? = null,
        defaultValue: Boolean,
        highlighted: Boolean,
        enabled: Boolean = true,
        checked: Boolean? = null,
        onCheckedStateChange: (Boolean) -> Unit = {},
        onBeforeChange: (Boolean) -> Boolean = { true },
        onAfterChange: (Boolean) -> Unit = {}
) {
    var localChecked by remember(key) { mutableStateOf(settings.getBoolean(key, defaultValue)) }
    val currentChecked = checked ?: localChecked
    fun commitChange(newValue: Boolean) {
        if (onBeforeChange(newValue)) {
            if (checked == null) localChecked = newValue else onCheckedStateChange(newValue)
            settings.edit { putBoolean(key, newValue) }
            onAfterChange(newValue)
        }
    }
    PreferenceRowFrame(
            highlighted = highlighted,
            enabled = enabled,
            role = Role.Switch,
            onClick = {
                if (enabled) commitChange(!currentChecked)
            },
            textContent = {
                PreferenceText(
                        title = title,
                        summary = summary,
                        enabled = enabled
                )
            },
            trailingContent = {
                Switch(
                        checked = currentChecked,
                        enabled = enabled,
                        onCheckedChange = ::commitChange
                )
            }
    )
}

@Composable
internal fun ListPreferenceRow(
        key: String,
        settings: SharedPreferences,
        title: String,
        defaultValue: String,
        entries: List<String>,
        values: List<String>,
        highlighted: Boolean,
        enabled: Boolean = true,
        summary: String? = null,
        @StringRes summaryFormatRes: Int? = null,
        stateVersion: Int = 0,
        onValueChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val options = entries.zip(values).map { PreferenceOption(label = it.first, value = it.second) }
    var expanded by remember(key) { mutableStateOf(false) }
    var selectedValue by remember(key, stateVersion) { mutableStateOf(settings.getString(key, defaultValue) ?: defaultValue) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label ?: selectedValue
    val resolvedSummary = summaryFormatRes?.let { context.getString(it, selectedLabel) } ?: summary ?: selectedLabel
    PreferenceRowFrame(
            highlighted = highlighted,
            enabled = enabled,
            role = Role.Button,
            onClick = { if (enabled) expanded = true },
            textContent = {
                PreferenceText(
                        title = title,
                        summary = resolvedSummary,
                        enabled = enabled
                )
            },
            trailingContent = {
                Box {
                    TextButton(
                            enabled = enabled,
                            onClick = { expanded = true }
                    ) {
                        Text(selectedLabel)
                    }
                    DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        expanded = false
                                        selectedValue = option.value
                                        settings.edit { putString(key, option.value) }
                                        onValueChanged(option.value)
                                    }
                            )
                        }
                    }
                }
            }
    )
}

@Composable
internal fun PreferenceRowFrame(
        highlighted: Boolean,
        enabled: Boolean = true,
        role: Role,
        onClick: () -> Unit,
        leadingContent: (@Composable () -> Unit)? = null,
        textContent: @Composable () -> Unit,
        trailingContent: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .background(if (highlighted) colors.subtleSelection else colors.backgroundDefault)
                    .clickable(enabled = enabled, role = role, onClick = onClick)
                    .padding(top = 10.dp)
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
        ) {
            leadingContent?.invoke()
            Box(modifier = Modifier.weight(1f)) {
                textContent()
            }
            trailingContent()
        }
        HorizontalDivider(
                color = colors.defaultDivider,
                modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
internal fun PreferenceText(
        title: String,
        summary: String?,
        enabled: Boolean
) {
    val colors = VLCThemeDefaults.colors
    Column {
        Text(
                text = title,
                color = if (enabled) colors.fontDefault else colors.fontDisabled,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
        )
        summary?.takeIf { it.isNotBlank() }?.let {
            Text(
                    text = it,
                    color = if (enabled) colors.fontLight else colors.fontDisabled,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
