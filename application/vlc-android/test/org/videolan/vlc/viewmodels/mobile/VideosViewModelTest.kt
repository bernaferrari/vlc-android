package org.videolan.vlc.viewmodels.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import org.videolan.medialibrary.stubs.StubDataSource
import org.videolan.vlc.BaseTest

/** Android video-provider contract; shared visual/navigation behavior is covered in commonTest. */
class VideosViewModelTest : BaseTest() {
    override fun beforeTest() {
        super.beforeTest()
        StubDataSource.getInstance().resetData()
    }

    @Test
    fun videoProviderReadsAndFiltersTheNativeVideoCatalog() {
        StubDataSource.getInstance().setVideoByCount(2, null)
        val model = VideosViewModel(context, VideoGroupingType.NONE, null, null)

        assertEquals(2, model.provider.getTotalCount())
        model.filter("Invincible")
        assertEquals(2, model.provider.getTotalCount())
        model.filter("missing")
        assertEquals(0, model.provider.getTotalCount())
    }
}
