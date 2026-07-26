package org.videolan.vlc.repository

import org.videolan.vlc.model.MediaItem
import kotlin.test.Test
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
}
