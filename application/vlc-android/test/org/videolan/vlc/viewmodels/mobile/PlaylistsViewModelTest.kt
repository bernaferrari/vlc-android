package org.videolan.vlc.viewmodels.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.stubs.StubDataSource
import org.videolan.vlc.BaseTest

/** Native playlist catalog query contract. */
class PlaylistsViewModelTest : BaseTest() {
    override fun beforeTest() {
        super.beforeTest()
        StubDataSource.getInstance().resetData()
    }

    @Test
    fun playlistProviderReportsCurrentNativePlaylistsAndSearchesByTitle() {
        medialibrary.createPlaylist("Morning mix", false, false)
        medialibrary.createPlaylist("Evening mix", false, false)
        val model = PlaylistsViewModel(context, Playlist.Type.All)

        assertEquals(2, model.provider.getTotalCount())
        model.filter("Morning")
        assertEquals(1, model.provider.getTotalCount())
        model.restore()
        assertEquals(2, model.provider.getTotalCount())
    }
}
