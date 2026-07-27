package org.videolan.vlc.viewmodels.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.stubs.StubDataSource
import org.videolan.vlc.BaseTest

/** Native parent-child provider coverage; screen state is exercised from commonTest. */
class AlbumSongsViewModelTest : BaseTest() {
    override fun beforeTest() {
        super.beforeTest()
        StubDataSource.getInstance().resetData()
    }

    @Test
    fun genreParentScopesAlbumsAndTracksToItsNativeCatalogEntries() {
        StubDataSource.getInstance().setAudioByCount(3, null)
        val genre = medialibrary.getGenres(Medialibrary.SORT_DEFAULT, false, true, false).single()

        val model = AlbumSongsViewModel(context, genre)

        assertEquals(1, model.albumsProvider.getTotalCount())
        assertEquals(3, model.tracksProvider.getTotalCount())
        model.filter("missing")
        assertEquals(0, model.albumsProvider.getTotalCount())
        assertEquals(0, model.tracksProvider.getTotalCount())
    }
}
