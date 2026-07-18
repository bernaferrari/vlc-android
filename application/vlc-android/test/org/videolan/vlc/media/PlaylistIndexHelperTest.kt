package org.videolan.vlc.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PlaylistIndexHelperTest {

    @Test
    fun adjustCurrentOnAdd_beforeCurrent_shiftsUp() {
        assertEquals(3, PlaylistIndexHelper.adjustCurrentOnAdd(currentIndex = 2, addedIndex = 0, expanding = false))
        // Insert at the current slot also shifts current right (currentIndex >= addedIndex).
        assertEquals(3, PlaylistIndexHelper.adjustCurrentOnAdd(currentIndex = 2, addedIndex = 2, expanding = false))
    }

    @Test
    fun adjustCurrentOnAdd_afterCurrent_unchanged() {
        assertEquals(1, PlaylistIndexHelper.adjustCurrentOnAdd(currentIndex = 1, addedIndex = 3, expanding = false))
    }

    @Test
    fun adjustCurrentOnAdd_whileExpanding_unchanged() {
        assertEquals(2, PlaylistIndexHelper.adjustCurrentOnAdd(currentIndex = 2, addedIndex = 0, expanding = true))
    }

    @Test
    fun adjustCurrentOnRemove_beforeCurrent_shiftsDown() {
        val (index, removed) = PlaylistIndexHelper.adjustCurrentOnRemove(currentIndex = 3, removedIndex = 1, expanding = false)
        assertEquals(2, index)
        assertFalse(removed)
    }

    @Test
    fun adjustCurrentOnRemove_currentItem() {
        val (index, removed) = PlaylistIndexHelper.adjustCurrentOnRemove(currentIndex = 2, removedIndex = 2, expanding = false)
        assertEquals(1, index)
        assertTrue(removed)
    }

    @Test
    fun adjustCurrentOnRemove_afterCurrent_unchanged() {
        val (index, removed) = PlaylistIndexHelper.adjustCurrentOnRemove(currentIndex = 1, removedIndex = 4, expanding = false)
        assertEquals(1, index)
        assertFalse(removed)
    }

    @Test
    fun adjustCurrentOnRemove_whileExpanding_unchanged() {
        val (index, removed) = PlaylistIndexHelper.adjustCurrentOnRemove(currentIndex = 2, removedIndex = 0, expanding = true)
        assertEquals(2, index)
        assertFalse(removed)
    }

    @Test
    fun adjustCurrentOnMove_movingCurrentForward() {
        // MediaWrapperList move convention: endPosition is insertion index before removal adjust.
        // Moving index 1 -> end 4 yields final position 3 for the moved item.
        assertEquals(3, PlaylistIndexHelper.adjustCurrentOnMove(currentIndex = 1, indexBefore = 1, indexAfter = 4))
    }

    @Test
    fun adjustCurrentOnMove_movingCurrentBackward() {
        assertEquals(0, PlaylistIndexHelper.adjustCurrentOnMove(currentIndex = 2, indexBefore = 2, indexAfter = 0))
    }

    @Test
    fun adjustCurrentOnMove_itemSlidesAcrossCurrent() {
        // Item after current moves before it -> current shifts right
        assertEquals(2, PlaylistIndexHelper.adjustCurrentOnMove(currentIndex = 1, indexBefore = 3, indexAfter = 0))
        // Item before current moves after it -> current shifts left
        assertEquals(0, PlaylistIndexHelper.adjustCurrentOnMove(currentIndex = 1, indexBefore = 0, indexAfter = 3))
    }

    @Test
    fun adjustCurrentOnMove_unrelatedIndices_unchanged() {
        assertEquals(1, PlaylistIndexHelper.adjustCurrentOnMove(currentIndex = 1, indexBefore = 3, indexAfter = 4))
    }

    @Test
    fun determineSequentialPrevNext_middle() {
        assertEquals(1 to 3, PlaylistIndexHelper.determineSequentialPrevNext(currentIndex = 2, size = 5, repeatAll = false))
    }

    @Test
    fun determineSequentialPrevNext_startAndEnd() {
        assertEquals(-1 to 1, PlaylistIndexHelper.determineSequentialPrevNext(currentIndex = 0, size = 3, repeatAll = false))
        assertEquals(1 to -1, PlaylistIndexHelper.determineSequentialPrevNext(currentIndex = 2, size = 3, repeatAll = false))
        assertEquals(1 to 0, PlaylistIndexHelper.determineSequentialPrevNext(currentIndex = 2, size = 3, repeatAll = true))
    }

    @Test
    fun determineSequentialPrevNext_emptyOrInvalid() {
        assertEquals(-1 to -1, PlaylistIndexHelper.determineSequentialPrevNext(currentIndex = 0, size = 0, repeatAll = true))
        assertEquals(-1 to -1, PlaylistIndexHelper.determineSequentialPrevNext(currentIndex = -1, size = 4, repeatAll = true))
    }
}
