package org.videolan.vlc.player

import org.videolan.vlc.model.RepeatMode
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueTraversalTest {
    @Test
    fun shuffleUsesRandomOrderAndExhaustsDistinctEntries() {
        val traversal = QueueTraversal(Random(42))
        traversal.reset(10, 0)
        var current = 0
        val order = mutableListOf(current)
        repeat(9) {
            val next = requireNotNull(traversal.next(current, true, RepeatMode.NONE))
            traversal.recordTransition(current, next)
            current = next
            order.add(next)
        }
        assertEquals(10, order.toSet().size)
        assertTrue(order != (0..9).toList())
        assertNull(traversal.next(current, true, RepeatMode.NONE))
    }

    @Test
    fun removedHistoryEntryCannotResolveToAnotherEntryAtItsOldPosition() {
        val traversal = QueueTraversal()
        traversal.reset(3, 0)
        traversal.recordTransition(0, 1)
        traversal.recordTransition(1, 2)
        traversal.remove(1)
        assertEquals(0, traversal.previous())
        assertNull(traversal.previous())
    }

    @Test
    fun appendedEntryIsEligibleAfterShufflePass() {
        val traversal = QueueTraversal()
        traversal.reset(1, 0)
        assertNull(traversal.next(0, true, RepeatMode.NONE))
        traversal.insert(1, 1)
        assertEquals(1, traversal.next(0, true, RepeatMode.NONE))
    }
}
