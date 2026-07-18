package org.videolan.vlc.player

/**
 * Pure index arithmetic for playlist mutations.
 * Ported from Android [org.videolan.vlc.media.PlaylistIndexHelper] — zero platform deps.
 */
object PlaylistIndexHelper {

    fun adjustCurrentOnAdd(currentIndex: Int, addedIndex: Int, expanding: Boolean): Int {
        return if (currentIndex >= addedIndex && !expanding) currentIndex + 1 else currentIndex
    }

    /** @return pair of (newCurrentIndex, wasCurrentRemoved) */
    fun adjustCurrentOnRemove(currentIndex: Int, removedIndex: Int, expanding: Boolean): Pair<Int, Boolean> {
        val currentRemoved = currentIndex == removedIndex
        val newIndex = if (currentIndex >= removedIndex && !expanding) currentIndex - 1 else currentIndex
        return newIndex to currentRemoved
    }

    fun adjustCurrentOnMove(currentIndex: Int, indexBefore: Int, indexAfter: Int): Int {
        return when (currentIndex) {
            indexBefore -> {
                var newIndex = indexAfter
                if (indexAfter > indexBefore) --newIndex
                newIndex
            }
            in indexAfter until indexBefore -> currentIndex + 1
            in (indexBefore + 1) until indexAfter -> currentIndex - 1
            else -> currentIndex
        }
    }

    /** @return pair of (prevIndex, nextIndex); either may be -1 when unavailable. */
    fun determineSequentialPrevNext(currentIndex: Int, size: Int, repeatAll: Boolean): Pair<Int, Int> {
        if (size <= 0 || currentIndex < 0) return -1 to -1
        val prev = if (currentIndex > 0) currentIndex - 1 else -1
        val next = when {
            currentIndex + 1 < size -> currentIndex + 1
            !repeatAll -> -1
            else -> 0
        }
        return prev to next
    }
}
