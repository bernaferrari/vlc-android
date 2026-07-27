@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.videolan.tools.SettingsWriteBridge
import org.videolan.tools.KEY_BROWSE_NETWORK
import org.videolan.vlc.platform.VlcPlatformCapabilities
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsCapabilityContractTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        SettingsWriteBridge.onBoolean = null
    }

    @AfterTest
    fun tearDown() {
        SettingsWriteBridge.onBoolean = null
        Dispatchers.resetMain()
    }

    @Test
    fun unsupportedRemoteServerIsHiddenAndCannotPersistASetting() {
        var writes = 0
        SettingsWriteBridge.onBoolean = { _, _ -> writes += 1 }
        val viewModel = SettingsViewModel(
            prefs = null,
            capabilities = VlcPlatformCapabilities(nativePlayback = true),
        )

        viewModel.setRemoteAccess(true)

        assertFalse(viewModel.state.value.supportsRemoteAccess)
        assertFalse(viewModel.state.value.remoteAccess)
        assertEquals(0, writes)
        viewModel.onCleared()
    }

    @Test
    fun networkBrowsingIsCapabilityGatedAndWritesTheSharedPreference() {
        val writes = mutableListOf<Pair<String, Boolean>>()
        SettingsWriteBridge.onBoolean = { key, value -> writes += key to value }
        val viewModel = SettingsViewModel(
            prefs = null,
            capabilities = VlcPlatformCapabilities(nativePlayback = true, networkBrowsing = true),
        )

        viewModel.setBrowseNetwork(false)

        assertTrue(viewModel.state.value.supportsNetworkBrowsing)
        assertFalse(viewModel.state.value.browseNetwork)
        assertEquals(listOf(KEY_BROWSE_NETWORK to false), writes)
        viewModel.onCleared()
    }
}
