package org.videolan.vlc.repository

import androidx.paging.PagingSource
import org.videolan.vlc.model.MediaItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PagingDefaultsTest {

    @Test
    fun invalidationCallbackInvalidatesSourceAndReleasesSubscription() {
        var invalidate: (() -> Unit)? = null
        var released = false
        val source = ListPagingSource<MediaItem>(
            registerInvalidation = { callback ->
                invalidate = callback
                { released = true }
            },
        ) { emptyList() }

        assertFalse(source.invalid)
        invalidate?.invoke()

        assertTrue(source.invalid)
        assertTrue(released)
    }

    @Test
    fun usesOffsetsWhenConsecutiveLoadsHaveDifferentSizes() = kotlinx.coroutines.test.runTest {
        val media = (0 until 6).map { id ->
            MediaItem(id.toLong(), "Item $id", "file:///item-$id.mp3")
        }
        val source = ListPagingSource { media }

        val first = source.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 3, placeholdersEnabled = false))
            as PagingSource.LoadResult.Page
        val second = source.load(PagingSource.LoadParams.Append(key = first.nextKey!!, loadSize = 2, placeholdersEnabled = false))
            as PagingSource.LoadResult.Page

        assertEquals(listOf(0L, 1L, 2L), first.data.map { it.id })
        assertEquals(listOf(3L, 4L), second.data.map { it.id })
    }
}
