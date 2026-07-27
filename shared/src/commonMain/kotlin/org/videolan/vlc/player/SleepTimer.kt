package org.videolan.vlc.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Observable, platform-neutral sleep timer state. */
data class SleepTimerState(
    val remainingMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val waitForCurrentItem: Boolean = false,
    val awaitingCurrentItemEnd: Boolean = false,
) {
    val isActive: Boolean get() = remainingMillis > 0L || awaitingCurrentItemEnd
}

/**
 * Shared policy used by all player adapters. Decoder/OS code only provides [isPlaying] and
 * [stopPlayback]; the lifecycle semantics match VLC Android's service timer.
 */
class SleepTimerController(
    private val isPlaying: () -> Boolean,
    private val stopPlayback: () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()
    private var job: Job? = null

    fun start(durationMillis: Long, waitForCurrentItem: Boolean) {
        val safeDuration = durationMillis.coerceAtLeast(0L)
        if (safeDuration == 0L) {
            clear()
            return
        }
        job?.cancel()
        _state.value = SleepTimerState(
            remainingMillis = safeDuration,
            durationMillis = safeDuration,
            waitForCurrentItem = waitForCurrentItem,
        )
        job = scope.launch {
            var remaining = safeDuration
            while (isActive && remaining > 0L) {
                val tick = minOf(1_000L, remaining)
                delay(tick)
                remaining -= tick
                _state.value = _state.value.copy(remainingMillis = remaining)
            }
            if (!isActive) return@launch
            if (_state.value.waitForCurrentItem) {
                _state.value = _state.value.copy(awaitingCurrentItemEnd = true)
            } else {
                finishAtExpiry()
            }
        }
    }

    fun onCurrentItemEnded() {
        if (_state.value.awaitingCurrentItemEnd) finishAtExpiry()
    }

    fun clear() {
        job?.cancel()
        job = null
        _state.value = SleepTimerState()
    }

    private fun finishAtExpiry() {
        if (isPlaying()) stopPlayback()
        clear()
    }
}
