package org.videolan.vlc.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared app-lock policy boundary.
 *
 * The shell owns whether access is currently locked; platform implementations
 * own credential storage, biometric prompts and their native presentation.
 * This deliberately does not reuse Android's legacy safe-mode preference:
 * that feature protects individual mutations, whereas an app lock protects
 * foreground access to the whole product.
 */
data class AppLockState(
    /** False for targets that have no secure native credential vault. */
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val locked: Boolean = false,
    val biometricsAvailable: Boolean = false,
)

interface AppLockController {
    val state: StateFlow<AppLockState>

    /** Starts native credential configuration. State changes only after success. */
    suspend fun enable(): Boolean

    /** Removes the native credential after native confirmation, if required. */
    suspend fun disable(): Boolean

    /** Presents native authentication and returns whether the app can be shown. */
    suspend fun unlock(): Boolean

    /** Called by a host lifecycle boundary when foreground access must be protected again. */
    fun lock()
}

/** A target without a credential vault must never advertise an app lock. */
object NoOpAppLockController : AppLockController {
    private val mutableState = MutableStateFlow(AppLockState())
    override val state: StateFlow<AppLockState> = mutableState.asStateFlow()
    override suspend fun enable(): Boolean = false
    override suspend fun disable(): Boolean = false
    override suspend fun unlock(): Boolean = false
    override fun lock() = Unit
}
