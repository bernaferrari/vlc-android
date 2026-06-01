package org.videolan.vlc.gui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import org.videolan.tools.SLEEP_TIMER_DEFAULT_INTERVAL
import org.videolan.tools.SLEEP_TIMER_DEFAULT_RESET_INTERACTION
import org.videolan.tools.SLEEP_TIMER_DEFAULT_WAIT
import org.videolan.tools.SLEEP_TIMER_RESET_INTERACTION
import org.videolan.tools.SLEEP_TIMER_WAIT
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.TalkbackUtil
import java.util.Calendar

fun ComponentActivity.showJumpToTimeComposeDialog(onDismiss: (() -> Unit)? = null) {
    PlaybackTimePickerComposeDialog(
        activity = this,
        mode = TimePickerMode.JumpToTime,
        onDismiss = onDismiss
    ).show()
}

fun ComponentActivity.showSleepTimerComposeDialog(
    forDefault: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    PlaybackTimePickerComposeDialog(
        activity = this,
        mode = TimePickerMode.SleepTimer,
        forDefault = forDefault,
        onDismiss = onDismiss
    ).show()
}

private enum class TimePickerMode {
    JumpToTime,
    SleepTimer
}

private class PlaybackTimePickerComposeDialog(
    activity: ComponentActivity,
    private val mode: TimePickerMode,
    private val forDefault: Boolean = false,
    onDismiss: (() -> Unit)? = null
) : PlaybackComposeBottomSheetDialog(
    activity = activity,
    onDismiss = onDismiss,
    dismissOnServiceEnded = mode != TimePickerMode.SleepTimer || !forDefault,
    dismissOnPlaybackEnded = mode != TimePickerMode.SleepTimer || !forDefault
) {
    private val settings = Settings.getInstance(activity)

    private fun executeAction(rawTime: String, wait: Boolean, reset: Boolean) {
        val time = parseTime(rawTime, maxTimeSize())
        when (mode) {
            TimePickerMode.JumpToTime -> {
                service?.let {
                    it.setTime(time.millis)
                    it.playlistManager.player.updateProgress(time.millis)
                    dismiss()
                }
            }
            TimePickerMode.SleepTimer -> executeSleepTimer(time, wait, reset)
        }
    }

    private fun executeSleepTimer(time: ParsedTime, wait: Boolean, reset: Boolean) {
        val interval = time.sleepIntervalMillis()
        if (forDefault) {
            settings.edit {
                putLong(SLEEP_TIMER_DEFAULT_INTERVAL, interval)
                putBoolean(SLEEP_TIMER_DEFAULT_WAIT, wait)
                putBoolean(SLEEP_TIMER_DEFAULT_RESET_INTERACTION, reset)
            }
            dismiss()
            return
        }

        service?.waitForMediaEnd = wait
        service?.resetOnInteraction = reset
        settings.putSingle(SLEEP_TIMER_RESET_INTERACTION, reset)
        settings.putSingle(SLEEP_TIMER_WAIT, wait)
        service?.sleepTimerInterval = interval

        val sleepTime = Calendar.getInstance()
        sleepTime.timeInMillis = sleepTime.timeInMillis + interval
        sleepTime.set(Calendar.SECOND, 0)
        service?.setSleepTimer(sleepTime)
        dismiss()
    }

    private fun removeCurrentSleepTimer() {
        if (forDefault) {
            settings.putSingle(SLEEP_TIMER_DEFAULT_INTERVAL, -1L)
        } else {
            service?.waitForMediaEnd = false
            service?.setSleepTimer(null)
            settings.putSingle(SLEEP_TIMER_WAIT, false)
        }
        dismiss()
    }

    private fun maxTimeSize() = if (mode == TimePickerMode.SleepTimer) 4 else 6

    private fun initialRawTime(): String {
        if (mode != TimePickerMode.SleepTimer || !forDefault) return ""
        val interval = settings.getLong(SLEEP_TIMER_DEFAULT_INTERVAL, -1L)
        if (interval <= 0L) return ""
        val hours = interval / HOURS_IN_MILLIS
        val minutes = interval % HOURS_IN_MILLIS / MINUTES_IN_MILLIS
        return if (hours > 0L) "$hours${minutes.toString().padStart(2, '0')}" else minutes.toString()
    }

    private fun initialWaitChecked(): Boolean {
        return if (forDefault) {
            settings.getBoolean(SLEEP_TIMER_DEFAULT_WAIT, false)
        } else {
            PlaybackService.instance?.waitForMediaEnd == true
        }
    }

    private fun initialResetChecked(): Boolean {
        return if (forDefault) {
            settings.getBoolean(SLEEP_TIMER_DEFAULT_RESET_INTERACTION, false)
        } else {
            PlaybackService.instance?.resetOnInteraction == true
        }
    }

    @Composable
    override fun Content() {
        var rawTime by rememberSaveable { mutableStateOf(initialRawTime()) }
        var wait by rememberSaveable { mutableStateOf(initialWaitChecked()) }
        var reset by rememberSaveable { mutableStateOf(initialResetChecked()) }
        val maxTimeSize = remember(mode) { maxTimeSize() }
        val parsedTime = parseTime(rawTime, maxTimeSize)
        val firstButtonFocusRequester = remember { FocusRequester() }
        val context = LocalContext.current
        val view = LocalView.current
        val colors = VLCThemeDefaults.colors

        fun announceTime(updatedRawTime: String) {
            view.announceForAccessibility(TalkbackUtil.millisToString(context, parseTime(updatedRawTime, maxTimeSize).millis))
        }

        fun append(value: String) {
            val updated = appendRawTime(rawTime, value, maxTimeSize)
            if (updated == rawTime) return
            rawTime = updated
            announceTime(updated)
        }

        fun deleteLastNumber() {
            if (rawTime.isEmpty()) return
            rawTime = rawTime.dropLast(1)
            announceTime(rawTime)
        }

        LaunchedEffect(Unit) {
            firstButtonFocusRequester.requestFocus()
        }

        Surface(color = colors.backgroundDefault, contentColor = colors.fontDefault) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = activity.getString(if (mode == TimePickerMode.JumpToTime) R.string.jump_to_time else R.string.sleep_in),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = parsedTime.formatted,
                        color = colors.fontDefault,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                    )
                    IconButton(onClick = ::deleteLastNumber) {
                        Icon(
                            painter = painterResource(R.drawable.ic_backspace),
                            contentDescription = activity.getString(R.string.clear),
                            tint = colors.fontDefault
                        )
                    }
                }
                TimeKeypad(
                    firstButtonFocusRequester = firstButtonFocusRequester,
                    onValue = ::append
                )
                if (mode == TimePickerMode.SleepTimer) {
                    CheckboxRow(
                        text = activity.getString(R.string.wait_before_sleep),
                        checked = wait,
                        onCheckedChange = { wait = it }
                    )
                    CheckboxRow(
                        text = activity.getString(R.string.reset_on_interaction),
                        checked = reset,
                        onCheckedChange = { reset = it }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    if (mode == TimePickerMode.SleepTimer) {
                        OutlinedButton(
                            onClick = ::removeCurrentSleepTimer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(activity.getString(R.string.remove_current))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        onClick = { executeAction(rawTime, wait, reset) },
                        modifier = if (mode == TimePickerMode.SleepTimer) Modifier.weight(1f) else Modifier
                    ) {
                        Text(activity.getString(R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeKeypad(
    firstButtonFocusRequester: FocusRequester,
    onValue: (String) -> Unit
) {
    val rows = listOf(
        listOf("1" to "1", "2" to "2", "3" to "3"),
        listOf("4" to "4", "5" to "5", "6" to "6"),
        listOf("7" to "7", "8" to "8", "9" to "9"),
        listOf(":00" to "00", "0" to "0", ":30" to "30")
    )
    rows.forEachIndexed { rowIndex, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (rowIndex == 0) 8.dp else 0.dp)
        ) {
            row.forEachIndexed { columnIndex, key ->
                TimeKeyButton(
                    label = key.first,
                    onClick = { onValue(key.second) },
                    modifier = if (rowIndex == 0 && columnIndex == 0) {
                        Modifier.focusRequester(firstButtonFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.TimeKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .weight(1f)
            .height(44.dp)
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(top = 8.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text = text)
    }
}

private data class ParsedTime(
    val hours: String,
    val minutes: String,
    val seconds: String,
    val formatted: String,
    val millis: Long
) {
    fun sleepIntervalMillis(): Long {
        val hoursInMicros = hours.toLongOrNull()?.times(HOURS_IN_MICROS) ?: 0L
        val minutesInMicros = minutes.toLongOrNull()?.times(MINUTES_IN_MICROS) ?: 0L
        return (hoursInMicros + minutesInMicros) / MILLIS_IN_MICROS
    }
}

private fun parseTime(rawTime: String, maxTimeSize: Int): ParsedTime {
    var tempRawTime = rawTime
    var formatted = ""
    val seconds: String
    if (maxTimeSize > 4) {
        seconds = getLastNumbers(tempRawTime)
        if (seconds.isNotEmpty()) formatted = seconds + "s"
        tempRawTime = removeLastNumbers(tempRawTime)
    } else {
        seconds = ""
    }

    val minutes = getLastNumbers(tempRawTime)
    if (minutes.isNotEmpty()) formatted = minutes + "m " + formatted
    tempRawTime = removeLastNumbers(tempRawTime)

    val hours = getLastNumbers(tempRawTime)
    if (hours.isNotEmpty()) formatted = hours + "h " + formatted

    val hoursInMicros = hours.toLongOrNull()?.times(HOURS_IN_MICROS) ?: 0L
    val minutesInMicros = minutes.toLongOrNull()?.times(MINUTES_IN_MICROS) ?: 0L
    val secondsInMicros = seconds.toLongOrNull()?.times(SECONDS_IN_MICROS) ?: 0L
    return ParsedTime(
        hours = hours,
        minutes = minutes,
        seconds = seconds,
        formatted = formatted,
        millis = (hoursInMicros + minutesInMicros + secondsInMicros) / MILLIS_IN_MICROS
    )
}

private fun appendRawTime(rawTime: String, value: String, maxTimeSize: Int): String {
    if (rawTime.length >= maxTimeSize) return rawTime
    return rawTime + value
}

private fun getLastNumbers(rawTime: String): String {
    if (rawTime.isEmpty()) return ""
    return if (rawTime.length == 1) rawTime else rawTime.substring(rawTime.length - 2)
}

private fun removeLastNumbers(rawTime: String): String {
    return if (rawTime.length <= 1) "" else rawTime.substring(0, rawTime.length - 2)
}

private const val MILLIS_IN_MICROS = 1000L
private const val SECONDS_IN_MICROS = 1000L * MILLIS_IN_MICROS
private const val MINUTES_IN_MICROS = 60L * SECONDS_IN_MICROS
private const val HOURS_IN_MICROS = 60L * MINUTES_IN_MICROS
private const val MINUTES_IN_MILLIS = MINUTES_IN_MICROS / MILLIS_IN_MICROS
private const val HOURS_IN_MILLIS = HOURS_IN_MICROS / MILLIS_IN_MICROS
