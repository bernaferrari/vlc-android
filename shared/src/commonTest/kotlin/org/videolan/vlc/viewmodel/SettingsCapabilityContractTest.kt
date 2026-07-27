@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.MutableStateFlow
import org.videolan.tools.SettingsWriteBridge
import org.videolan.tools.KEY_BROWSE_NETWORK
import org.videolan.tools.BROWSER_SHOW_ONLY_MULTIMEDIA
import org.videolan.tools.VIDEO_HUD_TIMEOUT
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.platform.RemoteAccessServerController
import org.videolan.vlc.platform.RemoteAccessServerState
import org.videolan.vlc.platform.AppLockController
import org.videolan.vlc.platform.AppLockState
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
        SettingsWriteBridge.onInt = null
    }

    @AfterTest
    fun tearDown() {
        SettingsWriteBridge.onBoolean = null
        SettingsWriteBridge.onInt = null
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

    @Test
    fun multimediaOnlyBrowserFilterIsASharedSetting() {
        val writes = mutableListOf<Pair<String, Boolean>>()
        SettingsWriteBridge.onBoolean = { key, value -> writes += key to value }
        val viewModel = SettingsViewModel(prefs = null)

        viewModel.setShowOnlyMultimedia(true)

        assertTrue(viewModel.state.value.showOnlyMultimedia)
        assertEquals(listOf(BROWSER_SHOW_ONLY_MULTIMEDIA to true), writes)
        viewModel.onCleared()
    }

    @Test
    fun videoHudTimeoutIsSharedAndBoundedBeforeWritingToNativeSettings() {
        val writes = mutableListOf<Pair<String, Int>>()
        SettingsWriteBridge.onInt = { key, value -> writes += key to value }
        val viewModel = SettingsViewModel(prefs = null)

        viewModel.setVideoHudTimeout(99)

        assertEquals(10, viewModel.state.value.videoHudTimeoutSeconds)
        assertEquals(listOf(VIDEO_HUD_TIMEOUT to 10), writes)
        viewModel.onCleared()
    }

    @Test
    fun appLockPolicyIsSharedWhileCredentialPromptsStayNative() {
        val appLock = FakeAppLock()
        val viewModel = SettingsViewModel(prefs = null, appLock = appLock)

        viewModel.enableAppLock()
        assertEquals(AppLockState(supported = true, enabled = true), viewModel.state.value.appLock)

        appLock.setBiometricsAvailable(true)
        viewModel.setAppLockBiometrics(true)
        assertEquals(
            AppLockState(supported = true, enabled = true, biometricsAvailable = true, biometricsEnabled = true),
            viewModel.state.value.appLock,
        )

        appLock.lock()
        assertEquals(
            AppLockState(supported = true, enabled = true, locked = true, biometricsAvailable = true, biometricsEnabled = true),
            viewModel.state.value.appLock,
        )

        viewModel.unlockAppLock()
        assertEquals(
            AppLockState(supported = true, enabled = true, biometricsAvailable = true, biometricsEnabled = true),
            viewModel.state.value.appLock,
        )

        viewModel.disableAppLock()
        assertEquals(AppLockState(supported = true, biometricsAvailable = true), viewModel.state.value.appLock)
        viewModel.onCleared()
    }

    @Test
    fun remoteServerStatusAndLifecycleAreSharedWhileTheSocketRemainsNative() {
        val server = FakeRemoteServer()
        val viewModel = SettingsViewModel(
            prefs = null,
            capabilities = VlcPlatformCapabilities(nativePlayback = true, remoteAccessServer = true),
            remoteAccessServer = server,
        )

        viewModel.setRemoteAccess(true)
        server.mutableState.value = RemoteAccessServerState(
            isRunning = true,
            address = "http://192.168.1.7:1234/upload?token=private",
        )

        assertTrue(viewModel.state.value.supportsRemoteAccess)
        assertEquals(listOf(true), server.enabled)
        assertEquals("http://192.168.1.7:1234/upload?token=private", viewModel.state.value.remoteAccessAddress)
        assertFalse(viewModel.state.value.remoteAccessStarting)
        viewModel.onCleared()
    }

    private class FakeRemoteServer : RemoteAccessServerController {
        val mutableState = MutableStateFlow(RemoteAccessServerState())
        override val state = mutableState
        val enabled = mutableListOf<Boolean>()

        override fun setEnabled(enabled: Boolean) {
            this.enabled += enabled
        }
    }

    private class FakeAppLock : AppLockController {
        private val mutableState = MutableStateFlow(AppLockState(supported = true))
        override val state = mutableState

        fun setBiometricsAvailable(available: Boolean) {
            mutableState.value = mutableState.value.copy(
                biometricsAvailable = available,
                biometricsEnabled = available && mutableState.value.biometricsEnabled,
            )
        }

        override suspend fun enable(): Boolean {
            mutableState.value = mutableState.value.copy(enabled = true, locked = false)
            return true
        }

        override suspend fun disable(): Boolean {
            mutableState.value = mutableState.value.copy(enabled = false, locked = false, biometricsEnabled = false)
            return true
        }

        override suspend fun unlock(): Boolean {
            if (!mutableState.value.enabled) return false
            mutableState.value = mutableState.value.copy(locked = false)
            return true
        }

        override suspend fun setBiometricsEnabled(enabled: Boolean): Boolean {
            if (!mutableState.value.enabled || !mutableState.value.biometricsAvailable) return false
            mutableState.value = mutableState.value.copy(biometricsEnabled = enabled)
            return true
        }

        override fun lock() {
            if (mutableState.value.enabled) {
                mutableState.value = mutableState.value.copy(locked = true)
            }
        }
    }
}
