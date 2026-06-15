package org.videolan.vlc.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Platform-agnostic ViewModel base class for shared presentation logic.
 *
 * Mirrors the key guarantees of `androidx.lifecycle.ViewModel`:
 * - Owns a [CoroutineScope] that is cancelled when the VM is cleared.
 * - Exposes [StateFlow] for reactive UI state.
 *
 * On Android, an `actual` wrapper or adapter can delegate to `ViewModel` to get
 * lifecycle integration; on JVM/iOS this class is used directly.
 *
 * Usage from common code:
 * ```
 * class PlayerViewModel : VlcViewModel() {
 *     private val _state = mutableStateFlow(PlayerState())
 *     val state = _state.asStateFlow()
 *
 *     fun togglePlay() = launch {
 *         _state.update { it.copy(isPlaying = !it.isPlaying) }
 *     }
 * }
 * ```
 */
abstract class VlcViewModel {

    /**
     * Supervisor-job scope that lives for the VM's lifetime.
     * Use [launch] to start coroutines that should be cancelled on [onCleared].
     */
    private val viewModelScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    /** Launch a coroutine in the ViewModel scope. */
    protected fun launch(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(block = block)

    /** Launch on [Dispatchers.Default] for CPU-intensive work. */
    protected fun launchDefault(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(Dispatchers.Default, block = block)

    /** Launch on IO for blocking I/O work (Dispatchers.IO on JVM, Default on iOS). */
    protected fun launchIo(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(org.videolan.tools.IOCoroutineDispatcher, block = block)

    /**
     * Called when the ViewModel is no longer used and will be destroyed.
     * Override to clean up resources. The default implementation cancels the scope.
     */
    open fun onCleared() {
        viewModelScope.cancel()
    }
}

/**
 * Convenience factory for [MutableStateFlow] in ViewModel initial values.
 */
fun <T> mutableStateFlow(initial: T): MutableStateFlow<T> = MutableStateFlow(initial)
