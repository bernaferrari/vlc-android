package org.videolan.vlc.compose.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShellBackNavigationTest {

    @Test
    fun `selection consumes back before root app exit`() {
        assertEquals(
            true,
            shouldInterceptShellBack(
                appLocked = false,
                hasActiveSelection = true,
                canNavigateBack = false,
            ),
        )
    }

    @Test
    fun `app lock keeps back disabled even during selection`() {
        assertEquals(
            false,
            shouldInterceptShellBack(
                appLocked = true,
                hasActiveSelection = true,
                canNavigateBack = true,
            ),
        )
    }

    @Test
    fun `nav3 pushes without duplicate routes and pops one level at a time`() {
        val stack = mutableListOf("more")

        assertTrue(pushNav3Route(stack, "about"))
        assertFalse(pushNav3Route(stack, "about"))
        assertTrue(pushNav3Route(stack, "libraries"))
        assertEquals(listOf("more", "about", "libraries"), stack)

        assertTrue(popNav3Route(stack))
        assertEquals(listOf("more", "about"), stack)
        assertTrue(popNav3Route(stack))
        assertEquals(listOf("more"), stack)
        assertFalse(popNav3Route(stack))
    }
}
