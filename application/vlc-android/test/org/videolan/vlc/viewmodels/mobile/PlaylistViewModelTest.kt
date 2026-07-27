package org.videolan.vlc.viewmodels.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.stubs.StubDataSource
import org.videolan.vlc.BaseTest

/** Android playlist-provider contract. Queue manipulation itself is shared and tested in commonTest. */
class PlaylistViewModelTest : BaseTest() {
    override fun beforeTest() {
        super.beforeTest()
        StubDataSource.getInstance().resetData()
    }

    @Test
    fun playlistProviderUsesOnlyThePlaylistMembershipAndItsFilter() {
        val source = StubDataSource.getInstance()
        val memberIds = (0 until 2).map { source.addMediaWrapper("member $it", MediaWrapper.TYPE_AUDIO).id }
        source.addMediaWrapper("outside", MediaWrapper.TYPE_AUDIO)
        val playlist = medialibrary.createPlaylist("Shared queue", false, false).apply { append(memberIds) }
        val model = PlaylistViewModel(context, playlist)

        assertEquals(2, model.tracksProvider.getTotalCount())
        model.filter("outside")
        assertEquals(0, model.tracksProvider.getTotalCount())
        model.restore()
        assertEquals(2, model.tracksProvider.getTotalCount())
    }
}
