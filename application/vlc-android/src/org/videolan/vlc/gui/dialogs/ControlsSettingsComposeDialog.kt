package org.videolan.vlc.gui.dialogs

import android.media.AudioManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.resources.AndroidDevices
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
import org.videolan.tools.KEY_SAVE_INDIVIDUAL_AUDIO_DELAY
import org.videolan.tools.KEY_VIDEO_CONFIRM_RESUME
import org.videolan.tools.KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY
import org.videolan.tools.KEY_VIDEO_JUMP_DELAY
import org.videolan.tools.KEY_VIDEO_LONG_JUMP_DELAY
import org.videolan.tools.LOCK_USE_SENSOR
import org.videolan.tools.POPUP_KEEPSCREEN
import org.videolan.tools.SAVE_BRIGHTNESS
import org.videolan.tools.SCREENSHOT_MODE
import org.videolan.tools.Settings
import org.videolan.tools.VIDEO_HUD_TIMEOUT
import org.videolan.tools.VIDEO_TRANSITION_SHOW
import org.videolan.tools.coerceInOrDefault
import org.videolan.tools.putSingle
import org.videolan.tools.readableString
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded
import org.videolan.vlc.gui.video.VideoPlayerActivity

fun ComponentActivity.showAudioControlsSettingsComposeDialog() {
    if (showPinIfNeeded()) return
    ControlsSettingsComposeDialog(this, ControlsSettingsMode.Audio).show()
}

fun ComponentActivity.showVideoControlsSettingsComposeDialog() {
    if (showPinIfNeeded()) return
    ControlsSettingsComposeDialog(this, ControlsSettingsMode.Video).show()
}

private enum class ControlsSettingsMode {
    Audio,
    Video
}

private data class ListOption(val value: String, val label: String)

private class ControlsSettingsComposeDialog(
    private val activity: ComponentActivity,
    private val mode: ControlsSettingsMode
) {
    private val settings = Settings.getInstance(activity)
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private var rootView: ComposeView? = null

    fun show() {
        normalizeUnavailableVideoSettings()
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    ControlsSettingsContent(mode = mode)
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnShowListener { configureBottomSheet() }
        dialog.setOnDismissListener { rootView = null }
        dialog.show()
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
        }
        rootView?.let { view ->
            if (AndroidDevices.isTv) applyOverscanMargin(view)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
        }
    }

    private fun applyOverscanMargin(view: View) {
        val verticalMargin = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
        view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + verticalMargin)
    }

    private fun normalizeUnavailableVideoSettings() {
        if (mode != ControlsSettingsMode.Video || !isVolumeControlUnavailable()) return
        if (settings.getBoolean(KEY_AUDIO_BOOST, true)) settings.putSingle(KEY_AUDIO_BOOST, false)
        if (settings.getBoolean(ENABLE_VOLUME_GESTURE, true)) settings.putSingle(ENABLE_VOLUME_GESTURE, false)
    }

    private fun isVolumeControlUnavailable(): Boolean {
        val audioManager = activity.getSystemService<AudioManager>() ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || audioManager.isVolumeFixed
    }

    private fun putBoolean(key: String, value: Boolean) {
        settings.putSingle(key, value)
        onSettingChanged(key)
    }

    private fun putInt(key: String, value: Int) {
        settings.putSingle(key, value)
        onSettingChanged(key)
    }

    private fun putString(key: String, value: String) {
        settings.putSingle(key, value)
        onSettingChanged(key)
    }

    private fun onSettingChanged(key: String) {
        (activity as? VideoPlayerActivity)?.onChangedControlSetting(key)
        when (key) {
            KEY_AUDIO_JUMP_DELAY -> Settings.audioJumpDelay = settings.getInt(KEY_AUDIO_JUMP_DELAY, 10)
            KEY_AUDIO_LONG_JUMP_DELAY -> Settings.audioLongJumpDelay = settings.getInt(KEY_AUDIO_LONG_JUMP_DELAY, 20)
            KEY_AUDIO_SHOW_TRACK_NUMBERS -> Settings.audioShowTrackNumbers.postValue(settings.getBoolean(KEY_AUDIO_SHOW_TRACK_NUMBERS, false))
            KEY_VIDEO_JUMP_DELAY -> Settings.videoJumpDelay = settings.getInt(KEY_VIDEO_JUMP_DELAY, 10)
            KEY_VIDEO_LONG_JUMP_DELAY -> Settings.videoLongJumpDelay = settings.getInt(KEY_VIDEO_LONG_JUMP_DELAY, 20)
            KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY -> Settings.videoDoubleTapJumpDelay = settings.getInt(KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY, 10)
            VIDEO_HUD_TIMEOUT -> Settings.videoHudDelay = settings.getInt(VIDEO_HUD_TIMEOUT, 4).coerceInOrDefault(1, 15, -1)
            FASTPLAY_SPEED -> Settings.fastplaySpeed = settings.getInt(FASTPLAY_SPEED, 20) / 10f
        }
        if (mode == ControlsSettingsMode.Audio) Settings.onAudioControlsChanged()
    }

    @Composable
    private fun ControlsSettingsContent(mode: ControlsSettingsMode) {
        val colors = VLCThemeDefaults.colors
        Surface(color = colors.backgroundDefault, contentColor = colors.fontDefault) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = activity.getString(R.string.controls_setting),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                when (mode) {
                    ControlsSettingsMode.Audio -> AudioControlsSettings()
                    ControlsSettingsMode.Video -> VideoControlsSettings()
                }
            }
        }
    }

    @Composable
    private fun AudioControlsSettings() {
        CategoryHeader(activity.getString(R.string.controls_prefs_category))
        IntStepperPreferenceRow(
            title = activity.getString(R.string.jump_delay),
            value = settings.getInt(KEY_AUDIO_JUMP_DELAY, 10),
            valueLabel = { activity.getString(R.string.jump_delay_summary, it.toString()) },
            onValueChange = { putInt(KEY_AUDIO_JUMP_DELAY, it) }
        )
        IntStepperPreferenceRow(
            title = activity.getString(R.string.long_jump_delay),
            value = settings.getInt(KEY_AUDIO_LONG_JUMP_DELAY, 20),
            valueLabel = { activity.getString(R.string.jump_delay_summary, it.toString()) },
            onValueChange = { putInt(KEY_AUDIO_LONG_JUMP_DELAY, it) }
        )
        SwitchPreferenceRow(
            title = activity.getString(R.string.force_shuffle_title),
            summary = activity.getString(R.string.force_shuffle_summary),
            checked = settings.getBoolean(KEY_AUDIO_FORCE_SHUFFLE, false),
            onCheckedChange = { putBoolean(KEY_AUDIO_FORCE_SHUFFLE, it) }
        )

        CategoryHeader(activity.getString(R.string.interface_prefs_screen))
        SwitchPreferenceRow(
            title = activity.getString(R.string.blurred_cover_background_title),
            summary = activity.getString(R.string.blurred_cover_background_summary),
            checked = settings.getBoolean(KEY_BLURRED_COVER_BACKGROUND, true),
            onCheckedChange = { putBoolean(KEY_BLURRED_COVER_BACKGROUND, it) }
        )
        SwitchPreferenceRow(
            title = activity.getString(R.string.albums_show_track_numbers),
            checked = settings.getBoolean(KEY_AUDIO_SHOW_TRACK_NUMBERS, false),
            onCheckedChange = { putBoolean(KEY_AUDIO_SHOW_TRACK_NUMBERS, it) }
        )
        SwitchPreferenceRow(
            title = activity.getString(R.string.show_chapter_buttons),
            summary = activity.getString(R.string.show_chapter_buttons_summary),
            checked = settings.getBoolean(KEY_AUDIO_SHOW_CHAPTER_BUTTONS, true),
            onCheckedChange = { putBoolean(KEY_AUDIO_SHOW_CHAPTER_BUTTONS, it) }
        )
        var showBookmarkButtons by remember { mutableStateOf(settings.getBoolean(KEY_AUDIO_SHOW_BOOkMARK_BUTTONS, true)) }
        SwitchPreferenceRow(
            title = activity.getString(R.string.show_bookmark_buttons),
            summary = activity.getString(R.string.show_bookmark_buttons_summary),
            checked = showBookmarkButtons,
            onCheckedChange = { checked ->
                showBookmarkButtons = checked
                putBoolean(KEY_AUDIO_SHOW_BOOkMARK_BUTTONS, checked)
                if (!checked) putBoolean(KEY_AUDIO_SHOW_BOOKMARK_MARKERS, false)
            }
        )
        SwitchPreferenceRow(
            title = activity.getString(R.string.show_bookmark_markers),
            summary = activity.getString(R.string.show_bookmark_markers_summary),
            checked = settings.getBoolean(KEY_AUDIO_SHOW_BOOKMARK_MARKERS, true) && showBookmarkButtons,
            enabled = showBookmarkButtons,
            onCheckedChange = { putBoolean(KEY_AUDIO_SHOW_BOOKMARK_MARKERS, it) }
        )
    }

    @Composable
    private fun VideoControlsSettings() {
        val volumeControlUnavailable = remember { isVolumeControlUnavailable() }
        val systemVolumeDisabledText = activity.getString(R.string.system_volume_disabled, activity.getString(R.string.audio_boost_summary))
        if (!AndroidDevices.isAndroidTv) {
            SwitchPreferenceRow(
                title = activity.getString(R.string.audio_boost_title),
                summary = if (volumeControlUnavailable) systemVolumeDisabledText else activity.getString(R.string.audio_boost_summary),
                checked = settings.getBoolean(KEY_AUDIO_BOOST, true) && !volumeControlUnavailable,
                enabled = !volumeControlUnavailable,
                onCheckedChange = { putBoolean(KEY_AUDIO_BOOST, it) }
            )
        }
        SwitchPreferenceRow(
            title = activity.getString(R.string.save_audiodelay_title),
            summary = activity.getString(R.string.save_audiodelay_summary),
            checked = settings.getBoolean(KEY_SAVE_INDIVIDUAL_AUDIO_DELAY, true),
            onCheckedChange = { putBoolean(KEY_SAVE_INDIVIDUAL_AUDIO_DELAY, it) }
        )
        ListPreferenceRow(
            title = activity.getString(R.string.confirm_resume_title),
            selectedValue = settings.getString(KEY_VIDEO_CONFIRM_RESUME, "0") ?: "0",
            options = listOptions(R.array.ask_confirmation_values, R.array.ask_confirmation_entries),
            onValueChange = { putString(KEY_VIDEO_CONFIRM_RESUME, it) }
        )

        CategoryHeader(activity.getString(R.string.gestures))
        if (AndroidDevices.hasTsp) {
            SwitchPreferenceRow(
                title = activity.getString(R.string.enable_volume_gesture_title),
                summary = if (volumeControlUnavailable) activity.getString(R.string.system_volume_disabled, activity.getString(R.string.enable_volume_gesture_summary)) else activity.getString(R.string.enable_volume_gesture_summary),
                checked = settings.getBoolean(ENABLE_VOLUME_GESTURE, true) && !volumeControlUnavailable,
                enabled = !volumeControlUnavailable,
                onCheckedChange = { putBoolean(ENABLE_VOLUME_GESTURE, it) }
            )
            SwitchPreferenceRow(
                title = activity.getString(R.string.enable_brightness_gesture_title),
                summary = activity.getString(R.string.enable_brightness_gesture_summary),
                checked = settings.getBoolean(ENABLE_BRIGHTNESS_GESTURE, true),
                onCheckedChange = { putBoolean(ENABLE_BRIGHTNESS_GESTURE, it) }
            )
        }
        SwitchPreferenceRow(
            title = activity.getString(R.string.save_brightness_title),
            summary = activity.getString(R.string.save_brightness_summary),
            checked = settings.getBoolean(SAVE_BRIGHTNESS, false),
            onCheckedChange = { putBoolean(SAVE_BRIGHTNESS, it) }
        )
        if (!AndroidDevices.isAndroidTv) {
            SwitchPreferenceRow(
                title = activity.getString(R.string.enable_swipe_seek_title),
                summary = activity.getString(R.string.enable_swipe_seek_summary),
                checked = settings.getBoolean(ENABLE_SWIPE_SEEK, true),
                onCheckedChange = { putBoolean(ENABLE_SWIPE_SEEK, it) }
            )
            SwitchPreferenceRow(
                title = activity.getString(R.string.enable_scale_gesture_title),
                summary = activity.getString(R.string.enable_scale_gesture_summary),
                checked = settings.getBoolean(ENABLE_SCALE_GESTURE, true),
                onCheckedChange = { putBoolean(ENABLE_SCALE_GESTURE, it) }
            )
            var doubleTapSeek by remember { mutableStateOf(settings.getBoolean(ENABLE_DOUBLE_TAP_SEEK, true)) }
            SwitchPreferenceRow(
                title = activity.getString(R.string.enable_double_tap_seek_title),
                summary = activity.getString(R.string.enable_double_tap_seek_summary),
                checked = doubleTapSeek,
                onCheckedChange = {
                    doubleTapSeek = it
                    putBoolean(ENABLE_DOUBLE_TAP_SEEK, it)
                }
            )
            IntStepperPreferenceRow(
                title = activity.getString(R.string.video_double_tap_jump_delay),
                value = settings.getInt(KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY, 10),
                valueLabel = { activity.getString(R.string.jump_delay_summary, it.toString()) },
                enabled = doubleTapSeek,
                onValueChange = { putInt(KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY, it) }
            )
            SwitchPreferenceRow(
                title = activity.getString(R.string.enable_double_tap_play_title),
                summary = activity.getString(R.string.enable_double_tap_play_summary),
                checked = settings.getBoolean(ENABLE_DOUBLE_TAP_PLAY, true),
                onCheckedChange = { putBoolean(ENABLE_DOUBLE_TAP_PLAY, it) }
            )
            ListPreferenceRow(
                title = activity.getString(R.string.enable_video_screenshot),
                selectedValue = settings.getString(SCREENSHOT_MODE, "0") ?: "0",
                options = listOptions(R.array.video_screenshot_values, R.array.video_screenshot),
                onValueChange = { putString(SCREENSHOT_MODE, it) }
            )
            var fastplayEnabled by remember { mutableStateOf(settings.getBoolean(ENABLE_FASTPLAY, false)) }
            SwitchPreferenceRow(
                title = activity.getString(R.string.enable_tap_and_hold_fastplay_title),
                summary = activity.getString(R.string.enable_tap_and_hold_fastplay_summary),
                checked = fastplayEnabled,
                onCheckedChange = {
                    fastplayEnabled = it
                    putBoolean(ENABLE_FASTPLAY, it)
                }
            )
            SliderPreferenceRow(
                title = activity.getString(R.string.fastplay_speed_title),
                value = settings.getInt(FASTPLAY_SPEED, 20).coerceIn(11, 80),
                min = 11,
                max = 80,
                valueLabel = { String.format("%sx", (it / 10f).readableString()) },
                enabled = fastplayEnabled,
                onValueChange = { putInt(FASTPLAY_SPEED, it) }
            )
        } else {
            IntStepperPreferenceRow(
                title = activity.getString(R.string.video_key_jump_delay),
                value = settings.getInt(KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY, 10),
                valueLabel = { activity.getString(R.string.jump_delay_summary, it.toString()) },
                onValueChange = { putInt(KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY, it) }
            )
        }

        CategoryHeader(activity.getString(R.string.player_controls))
        var seekButtonsEnabled by remember { mutableStateOf(settings.getBoolean(ENABLE_SEEK_BUTTONS, false)) }
        SwitchPreferenceRow(
            title = activity.getString(R.string.enable_seek_buttons),
            summary = activity.getString(R.string.enable_seek_buttons_summary),
            checked = seekButtonsEnabled,
            onCheckedChange = {
                seekButtonsEnabled = it
                putBoolean(ENABLE_SEEK_BUTTONS, it)
            }
        )
        IntStepperPreferenceRow(
            title = activity.getString(R.string.jump_delay),
            value = settings.getInt(KEY_VIDEO_JUMP_DELAY, 10),
            valueLabel = { activity.getString(R.string.jump_delay_summary, it.toString()) },
            enabled = seekButtonsEnabled,
            onValueChange = { putInt(KEY_VIDEO_JUMP_DELAY, it) }
        )
        IntStepperPreferenceRow(
            title = activity.getString(R.string.long_jump_delay),
            value = settings.getInt(KEY_VIDEO_LONG_JUMP_DELAY, 20),
            valueLabel = { activity.getString(R.string.jump_delay_summary, it.toString()) },
            enabled = seekButtonsEnabled,
            onValueChange = { putInt(KEY_VIDEO_LONG_JUMP_DELAY, it) }
        )
        if (!AndroidDevices.isAndroidTv && !AndroidUtil.isOOrLater) {
            SwitchPreferenceRow(
                title = activity.getString(R.string.popup_keepscreen_title),
                summary = activity.getString(R.string.popup_keepscreen_summary),
                checked = settings.getBoolean(POPUP_KEEPSCREEN, false),
                onCheckedChange = { putBoolean(POPUP_KEEPSCREEN, it) }
            )
        }
        SliderPreferenceRow(
            title = activity.getString(R.string.video_hud_timeout),
            value = settings.getInt(VIDEO_HUD_TIMEOUT, 4).coerceIn(1, 16),
            min = 1,
            max = 16,
            valueLabel = { videoHudTimeoutLabel(it) },
            onValueChange = { putInt(VIDEO_HUD_TIMEOUT, it) }
        )
        SwitchPreferenceRow(
            title = activity.getString(R.string.video_transition_title),
            summary = activity.getString(R.string.video_transition_summary),
            checked = settings.getBoolean(VIDEO_TRANSITION_SHOW, true),
            onCheckedChange = { putBoolean(VIDEO_TRANSITION_SHOW, it) }
        )
        if (!AndroidDevices.isAndroidTv) {
            SwitchPreferenceRow(
                title = activity.getString(R.string.lock_use_sensor_title),
                summary = activity.getString(R.string.lock_use_sensor_summary),
                checked = settings.getBoolean(LOCK_USE_SENSOR, true),
                onCheckedChange = { putBoolean(LOCK_USE_SENSOR, it) }
            )
        }
    }

    private fun listOptions(valuesRes: Int, entriesRes: Int): List<ListOption> {
        val values = activity.resources.getStringArray(valuesRes)
        val labels = activity.resources.getStringArray(entriesRes)
        return values.mapIndexed { index, value -> ListOption(value, labels[index]) }
    }

    private fun videoHudTimeoutLabel(value: Int): String {
        return if (value.coerceInOrDefault(1, 15, -1) == -1) {
            activity.getString(R.string.timeout_infinite)
        } else {
            activity.getString(R.string.video_hud_timeout_summary, value.toString())
        }
    }
}

@Composable
private fun CategoryHeader(text: String) {
    val colors = VLCThemeDefaults.colors
    Text(
        text = text,
        color = colors.primary,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun PreferenceRowFrame(
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    role: Role? = null,
    trailingContent: @Composable () -> Unit,
    textContent: @Composable () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = role) { onClick() } else Modifier)
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                textContent()
            }
            trailingContent()
        }
        HorizontalDivider(
            color = colors.defaultDivider,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PreferenceText(title: String, summary: String?, enabled: Boolean) {
    val colors = VLCThemeDefaults.colors
    Column {
        Text(
            text = title,
            color = if (enabled) colors.fontDefault else colors.fontDisabled,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                color = if (enabled) colors.fontLight else colors.fontDisabled,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SwitchPreferenceRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true
) {
    var current by remember(title, checked) { mutableStateOf(checked) }
    PreferenceRowFrame(
        enabled = enabled,
        role = Role.Switch,
        onClick = {
            current = !current
            onCheckedChange(current)
        },
        trailingContent = {
            Switch(
                checked = current,
                enabled = enabled,
                onCheckedChange = {
                    current = it
                    onCheckedChange(it)
                }
            )
        },
        textContent = { PreferenceText(title = title, summary = summary, enabled = enabled) }
    )
}

@Composable
private fun IntStepperPreferenceRow(
    title: String,
    value: Int,
    valueLabel: (Int) -> String,
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true,
    min: Int = 1,
    max: Int = 100
) {
    var current by remember(title, value) { mutableStateOf(value.coerceIn(min, max)) }
    PreferenceRowFrame(
        enabled = enabled,
        trailingContent = {
            Stepper(
                value = current,
                enabled = enabled,
                min = min,
                max = max,
                onValueChange = {
                    current = it
                    onValueChange(it)
                }
            )
        },
        textContent = {
            PreferenceText(
                title = title,
                summary = valueLabel(current),
                enabled = enabled
            )
        }
    )
}

@Composable
private fun Stepper(
    value: Int,
    enabled: Boolean,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            enabled = enabled && value > min,
            onClick = { onValueChange((value - 1).coerceAtLeast(min)) },
            modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 36.dp)
        ) {
            Text("-")
        }
        Text(
            text = value.toString(),
            color = if (enabled) colors.fontDefault else colors.fontDisabled,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(36.dp)
        )
        OutlinedButton(
            enabled = enabled && value < max,
            onClick = { onValueChange((value + 1).coerceAtMost(max)) },
            modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 36.dp)
        ) {
            Text("+")
        }
    }
}

@Composable
private fun SliderPreferenceRow(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    valueLabel: (Int) -> String,
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    var current by remember(title, value) { mutableStateOf(value.coerceIn(min, max)) }
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        PreferenceText(
            title = title,
            summary = valueLabel(current),
            enabled = enabled
        )
        Slider(
            value = current.toFloat(),
            onValueChange = { current = it.toInt().coerceIn(min, max) },
            onValueChangeFinished = { onValueChange(current) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(color = colors.defaultDivider)
    }
}

@Composable
private fun ListPreferenceRow(
    title: String,
    selectedValue: String,
    options: List<ListOption>,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember(title) { mutableStateOf(false) }
    var currentValue by remember(title, selectedValue) { mutableStateOf(selectedValue) }
    val selectedLabel = options.firstOrNull { it.value == currentValue }?.label ?: currentValue
    PreferenceRowFrame(
        enabled = enabled,
        onClick = { expanded = true },
        trailingContent = {
            Box {
                TextButton(enabled = enabled, onClick = { expanded = true }) {
                    Text(selectedLabel)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                expanded = false
                                currentValue = option.value
                                onValueChange(option.value)
                            }
                        )
                    }
                }
            }
        },
        textContent = {
            PreferenceText(
                title = title,
                summary = selectedLabel,
                enabled = enabled
            )
        }
    )
}
