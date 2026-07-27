package org.videolan.vlc.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.videolan.tools.VlcSettings

/** Swift-owned local-network upload server seam for the shared Settings screen. */
interface IosRemoteAccessHandler {
    fun setEnabled(enabled: Boolean)
}

/**
 * Receives native server callbacks and is exported to Swift as
 * `IosRemoteAccessServer.shared`. Keeping the state here makes the UI and its
 * tests target-agnostic while Network.framework remains entirely in the iOS host.
 */
object IosRemoteAccessServer : RemoteAccessServerController {
    private val mutableState = MutableStateFlow(RemoteAccessServerState())
    override val state: StateFlow<RemoteAccessServerState> = mutableState

    private var handler: IosRemoteAccessHandler? = null

    fun setHandler(handler: IosRemoteAccessHandler?) {
        this.handler = handler
    }

    /** Call after DataStore hydration so a persisted enabled server is restored. */
    fun restorePersistedState() {
        setEnabled(VlcSettings.remoteAccessEnabled.value)
    }

    override fun setEnabled(enabled: Boolean) {
        if (!enabled) {
            mutableState.value = RemoteAccessServerState()
            handler?.setEnabled(false)
            return
        }
        mutableState.value = RemoteAccessServerState(isStarting = true)
        handler?.setEnabled(true)
    }

    fun publishRunning(address: String) {
        mutableState.value = RemoteAccessServerState(isRunning = true, address = address)
    }

    fun publishFailure(message: String) {
        mutableState.value = RemoteAccessServerState(error = message)
    }
}
