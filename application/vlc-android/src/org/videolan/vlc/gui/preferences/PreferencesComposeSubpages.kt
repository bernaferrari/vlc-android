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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.videolan.tools.KEY_ANDROID_AUTO_QUEUE_INFO_POS_VAL
import org.videolan.tools.KEY_CASTING_AUDIO_ONLY
import org.videolan.tools.KEY_CASTING_PASSTHROUGH
import org.videolan.tools.KEY_CASTING_QUALITY
import org.videolan.tools.KEY_ENABLE_CASTING
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL
import org.videolan.tools.KEY_RESTRICT_SETTINGS
import org.videolan.tools.KEY_SAFE_MODE
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import kotlin.math.roundToInt

private const val KEY_ANDROID_AUTO_QUEUE_INFO_POS = "android_auto_queue_info_pos"
private const val KEY_ANDROID_AUTO_QUEUE_FORMAT = "android_auto_queue_format"
private const val KEY_ANDROID_AUTO_TITLE_SCALE = "android_auto_title_scale_val"
private const val KEY_ANDROID_AUTO_SUBTITLE_SCALE = "android_auto_subtitle_scale_val"
private const val KEY_ANDROID_AUTO_SPEED_BUTTONS = "enable_android_auto_speed_buttons"
private const val KEY_ANDROID_AUTO_SEEK_BUTTONS = "enable_android_auto_seek_buttons"

/**
 * Compose replacement for the small phone preference XML screens:
 * preferences_casting.xml, preferences_parental_control.xml, and preferences_android_auto.xml.
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
        onPlaybackSpeedGlobalChanged: () -> Unit
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
