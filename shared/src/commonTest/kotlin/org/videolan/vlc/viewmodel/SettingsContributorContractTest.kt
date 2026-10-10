@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.videolan.vlc.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.videolan.tools.KEY_ENABLE_REMOTE_ACCESS
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.SettingsWriteBridge
import org.videolan.vlc.compose.app.MoreRoute
import org.videolan.vlc.compose.app.SettingsRoute
import org.videolan.vlc.compose.app.SettingsSectionRoute
import org.videolan.vlc.compose.app.VideoRoute
import org.videolan.vlc.compose.app.canonicalVlcShellRouteStack
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.settings.SettingsAction
import org.videolan.vlc.settings.SettingsConfirmation
import org.videolan.vlc.settings.SettingsContributor
import org.videolan.vlc.settings.SettingsGroup
import org.videolan.vlc.settings.SettingsSection
import org.videolan.vlc.settings.SettingsToggle
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsContributorContractTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeContributor : SettingsContributor {
        val writes = mutableListOf<Pair<String, Any>>()
        val observed = mutableListOf<Pair<String, Any>>()
        val actions = mutableListOf<String>()
        val opened = mutableListOf<String>()
        val closed = mutableListOf<String>()
        var settingsClosed = 0

        override fun sections() = listOf(
            SettingsSection(
                id = "advanced", title = "Advanced", icon = null,
                groups = listOf(
                    SettingsGroup(
                        "g", null, listOf(
                            SettingsToggle("verbose", "Verbose", false),
                            SettingsToggle(KEY_ENABLE_REMOTE_ACCESS, "Remote", false),
                            SettingsAction("clear", "Clear", confirmation = SettingsConfirmation("Clear", "Sure?", "Clear")),
                        )
                    )
                ),
            ),
            SettingsSection("flags", "Flags", null, listOf(SettingsGroup("f", null, listOf(SettingsToggle("flag", "Flag", false)))), parentId = "advanced"),
        )

        override fun onValueChange(key: String, value: Any) { writes += key to value }
        override fun afterValueChange(key: String, value: Any) { observed += key to value }
        override fun onAction(key: String) { actions += key }
        override fun confirmationFor(key: String, value: Any) =
            if (key == PLAYBACK_HISTORY && value == false) SettingsConfirmation("History", "Sure?", "OK", destructive = false) else null
        override fun onSectionOpened(id: String) { opened += id }
        override fun onSectionClosed(id: String) { closed += id }
        override fun onSettingsClosed() { settingsClosed++ }
    }

    private val contributor = FakeContributor()

    private fun viewModel() = SettingsViewModel(
        prefs = null,
        capabilities = VlcPlatformCapabilities(nativePlayback = true, remoteAccessServer = true),
        contributors = listOf(contributor),
    )

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
    fun contributedValuesPersistThroughTheirContributor() {
        val vm = viewModel()
        val item = vm.contributed.value.section("advanced")!!.groups.first().items.first()

        vm.change(item, true)

        assertEquals(listOf<Pair<String, Any>>("verbose" to true), contributor.writes)
    }

    @Test
    fun sharedKeysStayOnTheSharedPathEvenWhenAPlatformRowShowsThem() {
        val sharedWrites = mutableListOf<Pair<String, Boolean>>()
        SettingsWriteBridge.onBoolean = { key, value -> sharedWrites += key to value }
        val vm = viewModel()
        val remote = vm.contributed.value.section("advanced")!!.groups.first().items[1]

        vm.change(remote, true)

        assertTrue(contributor.writes.isEmpty())
        assertEquals(listOf(KEY_ENABLE_REMOTE_ACCESS to true), sharedWrites)
        assertEquals(listOf<Pair<String, Any>>(KEY_ENABLE_REMOTE_ACCESS to true), contributor.observed)
        assertTrue(vm.state.value.remoteAccess)
    }

    @Test
    fun confirmationsGateChangesAndActions() {
        val vm = viewModel()
        vm.change(SettingsToggle(PLAYBACK_HISTORY, "History", true), false)
        assertNotNull(vm.pendingConfirmation.value)
        assertTrue(vm.state.value.playbackHistory)
        vm.confirmPending()
        assertNull(vm.pendingConfirmation.value)
        assertEquals(false, vm.state.value.playbackHistory)

        val clear = vm.contributed.value.section("advanced")!!.groups.first().items[2] as SettingsAction
        vm.action(clear)
        assertTrue(contributor.actions.isEmpty())
        vm.dismissPending()
        assertTrue(contributor.actions.isEmpty())
        vm.action(clear)
        vm.confirmPending()
        assertEquals(listOf("clear"), contributor.actions)
    }

    @Test
    fun deepLinksResolveTheSectionChain() {
        val vm = viewModel()
        assertEquals(listOf("advanced"), vm.sectionPathFor("verbose"))
        assertEquals(listOf("advanced", "flags"), vm.sectionPathFor("flag"))
        assertEquals(emptyList(), vm.sectionPathFor(PLAYBACK_HISTORY))
        assertEquals(listOf("advanced"), vm.sectionPathFor("advanced"))
    }

    @Test
    fun sectionHooksFollowTheBackStack() {
        val vm = viewModel()
        vm.onOpenSectionsChanged(setOf("advanced"))
        vm.onOpenSectionsChanged(setOf("advanced", "flags"))
        vm.onOpenSectionsChanged(setOf("advanced"))
        vm.onSettingsClosed()

        assertEquals(listOf("advanced", "flags"), contributor.opened)
        assertEquals(listOf("flags", "advanced"), contributor.closed)
        assertEquals(1, contributor.settingsClosed)
    }

    @Test
    fun sectionRoutesAreRestoredAboveSettings() {
        assertEquals(
            listOf(MoreRoute, SettingsRoute, SettingsSectionRoute("advanced"), SettingsSectionRoute("flags")),
            canonicalVlcShellRouteStack(listOf(MoreRoute, SettingsSectionRoute("advanced"), SettingsSectionRoute("flags")), VideoRoute),
        )
        assertEquals(listOf(VideoRoute), canonicalVlcShellRouteStack(listOf(VideoRoute, SettingsSectionRoute("x")), VideoRoute))
    }
}
