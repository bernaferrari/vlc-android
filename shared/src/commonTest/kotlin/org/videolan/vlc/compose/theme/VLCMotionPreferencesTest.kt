package org.videolan.vlc.compose.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VLCMotionPreferencesTest {

    @Test
    fun reducedMotionMakesSharedMotionImmediate() {
        val motion = VLCMotionPreferences(reducedMotion = true)

        assertEquals(0, motion.durationShort)
        assertEquals(0, motion.durationMedium)
        assertEquals(0, motion.durationLong)
    }

    @Test
    fun defaultMotionRetainsExpressiveDurations() {
        val motion = VLCMotionPreferences()

        assertTrue(motion.durationShort > 0)
        assertTrue(motion.durationMedium > motion.durationShort)
        assertTrue(motion.durationLong > motion.durationMedium)
    }
}
