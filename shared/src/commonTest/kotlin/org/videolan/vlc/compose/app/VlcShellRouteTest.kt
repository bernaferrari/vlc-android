package org.videolan.vlc.compose.app

import kotlin.test.Test
import kotlin.test.assertEquals
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.viewmodel.MainTab

class VlcShellRouteTest {

    @Test
    fun rootRoutesRoundTripToTheirTabs() {
        MainTab.entries.forEach { tab ->
            assertEquals(tab, tab.toVlcShellRoute().toMainTabOrNull())
        }
    }

    @Test
    fun overlaysRetainTheRootTab() {
        assertEquals(
            MainTab.AUDIO,
            listOf(
                AudioRoute,
                AudioEntityRoute(7L, "Artist", AudioEntityRouteKind.ARTIST),
                PlayerRoute,
                SettingsRoute,
            ).activeTab(),
        )
    }

    @Test
    fun browserRouteRetainsTheCompleteFolderStack() {
        val folders = listOf(
            MediaFolder(1L, "Storage", "/storage", isRoot = true),
            MediaFolder(2L, "Videos", "/storage/videos", kind = FolderKind.MEDIA_FOLDER),
        )

        val route = BrowserFolderRoute.from(folders)

        assertEquals(folders, route.toMediaFolders())
        assertEquals(MainTab.BROWSER, route.toMainTabOrNull())
    }

    @Test
    fun detailRoutesResolveToTheirRootTabs() {
        assertEquals(
            MainTab.VIDEO,
            VideoContainerRoute(3L, "Movies", VideoContainerRouteKind.FOLDER).toMainTabOrNull(),
        )
        assertEquals(
            MainTab.PLAYLISTS,
            PlaylistDetailRoute(4L, "Road trip").toMainTabOrNull(),
        )
    }

    @Test
    fun emptyStackFallsBackToVideo() {
        assertEquals(MainTab.VIDEO, emptyList<VlcShellRoute>().activeTab())
    }
}
