package org.videolan.vlc.compose.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VLCThemePreferenceTest {

    @Test
    fun orangeAndSystemAreTheStableProductDefaults() {
        val preference = resolveVLCThemePreference(null, null)

        assertEquals(VLCThemeAppearance.System, preference.appearance)
        assertEquals(VLCThemeAccent.Orange, preference.accent)
    }

    @Test
    fun persistedThemeChoiceParsesToSharedModels() {
        val preference = resolveVLCThemePreference("dark", "teal")

        assertEquals(VLCThemeAppearance.Dark, preference.appearance)
        assertEquals(VLCThemeAccent.Teal, preference.accent)
        assertTrue(preference.appearance.resolveDarkTheme(systemIsDark = false))
    }

    @Test
    fun invalidStoredValuesNeverReplaceVlcDefaults() {
        val preference = resolveVLCThemePreference("sunset", "neon")

        assertEquals(VLCThemeAppearance.System, preference.appearance)
        assertEquals(VLCThemeAccent.Orange, preference.accent)
        assertFalse(VLCThemeAppearance.Light.resolveDarkTheme(systemIsDark = true))
    }
}
