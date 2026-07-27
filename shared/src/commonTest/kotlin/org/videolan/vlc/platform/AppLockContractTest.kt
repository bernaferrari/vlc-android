package org.videolan.vlc.platform

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppLockContractTest {
    @Test
    fun unsupported_hosts_never_advertise_or_unlock_an_app_lock() = runTest {
        assertFalse(NoOpAppLockController.state.value.enabled)
        assertFalse(NoOpAppLockController.state.value.locked)
        assertFalse(NoOpAppLockController.enable())
        assertFalse(NoOpAppLockController.unlock())
        assertFalse(NoOpAppLockController.disable())
        NoOpAppLockController.lock()
        assertEquals(AppLockState(), NoOpAppLockController.state.value)
    }
}
