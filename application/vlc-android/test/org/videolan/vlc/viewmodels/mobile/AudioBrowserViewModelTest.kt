package org.videolan.vlc.viewmodels.mobile

import androidx.core.content.edit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.stubs.StubDataSource
import org.videolan.tools.KEY_ARTISTS_SHOW_ALL
import org.videolan.tools.Settings
import org.videolan.vlc.BaseTest

/** Android medialibrary-provider contracts; shared browser behavior is in commonTest. */
class AudioBrowserViewModelTest : BaseTest() {
    override fun beforeTest() {
        super.beforeTest()
        StubDataSource.getInstance().resetData()
        Settings.getInstance(context).edit(commit = true) { putBoolean(KEY_ARTISTS_SHOW_ALL, false) }
    }

    @Test
    fun nativeAudioCatalogDeduplicatesAlbumArtistAndGenreEntities() {
        StubDataSource.getInstance().setAudioByCount(3, null)

        val model = AudioBrowserViewModel(context)

        assertEquals(3, model.tracksProvider.getTotalCount())
        assertEquals(1, model.albumsProvider.getTotalCount())
        assertEquals(1, model.artistsProvider.getTotalCount())
        assertEquals(1, model.genresProvider.getTotalCount())
    }

    @Test
    fun nativeAudioSearchFiltersTheTracksProviderWithoutChangingCatalogCounts() {
        StubDataSource.getInstance().addMediaWrapper("Shared title", MediaWrapper.TYPE_AUDIO)
        StubDataSource.getInstance().addMediaWrapper("Shared encore", MediaWrapper.TYPE_AUDIO)
        val model = AudioBrowserViewModel(context)

        model.filter("Shared")
        assertEquals(2, model.tracksProvider.getTotalCount())

        model.filter("missing")
        assertEquals(0, model.tracksProvider.getTotalCount())
        model.restore()
        assertEquals(2, model.tracksProvider.getTotalCount())
    }
}
