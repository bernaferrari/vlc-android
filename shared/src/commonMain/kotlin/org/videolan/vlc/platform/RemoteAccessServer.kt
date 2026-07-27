package org.videolan.vlc.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * State exposed by a target-owned local-network transfer service.
 *
 * The Compose surface deliberately owns no socket code: Android keeps its existing
 * RemoteAccessService and iOS supplies a small, authenticated Network.framework
 * server.  This contract keeps the user-visible enablement and error reporting
 * shared without pretending that either native implementation is portable.
 */
data class RemoteAccessServerState(
    val isStarting: Boolean = false,
    val isRunning: Boolean = false,
    val address: String? = null,
    val error: String? = null,
)

interface RemoteAccessServerController {
    val state: StateFlow<RemoteAccessServerState>

    /** Starts or stops the native service. Callers must also persist the preference. */
    fun setEnabled(enabled: Boolean)
}

/** Used on targets whose native host owns remote-access lifecycle independently. */
object NoOpRemoteAccessServerController : RemoteAccessServerController {
    override val state: StateFlow<RemoteAccessServerState> = MutableStateFlow(RemoteAccessServerState())

    override fun setEnabled(enabled: Boolean) = Unit
}
