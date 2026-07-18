package org.videolan.vlc.media

/**
 * Thin Android façade over the KMP [org.videolan.vlc.player.PlaylistIndexHelper].
 * Keeps existing import sites working while the algorithm lives in commonMain.
 */
object PlaylistIndexHelper {

    fun adjustCurrentOnAdd(currentIndex: Int, addedIndex: Int, expanding: Boolean): Int =
        org.videolan.vlc.player.PlaylistIndexHelper.adjustCurrentOnAdd(currentIndex, addedIndex, expanding)

    fun adjustCurrentOnRemove(currentIndex: Int, removedIndex: Int, expanding: Boolean): Pair<Int, Boolean> =
        org.videolan.vlc.player.PlaylistIndexHelper.adjustCurrentOnRemove(currentIndex, removedIndex, expanding)

    fun adjustCurrentOnMove(currentIndex: Int, indexBefore: Int, indexAfter: Int): Int =
        org.videolan.vlc.player.PlaylistIndexHelper.adjustCurrentOnMove(currentIndex, indexBefore, indexAfter)

    fun determineSequentialPrevNext(currentIndex: Int, size: Int, repeatAll: Boolean): Pair<Int, Int> =
        org.videolan.vlc.player.PlaylistIndexHelper.determineSequentialPrevNext(currentIndex, size, repeatAll)
}
