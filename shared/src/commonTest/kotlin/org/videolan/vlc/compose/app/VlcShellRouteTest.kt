package org.videolan.vlc.compose.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
                AboutRoute,
                AboutLibrariesRoute,
                AboutAuthorsRoute,
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

    @Test
    fun aboutDetailsAreDedicatedNav3Levels() {
        val stack = mutableListOf<VlcShellRoute>(MoreRoute, AboutRoute, AboutLibrariesRoute)

        stack.removeAt(stack.lastIndex)

        assertEquals(listOf<VlcShellRoute>(MoreRoute, AboutRoute), stack)
        assertEquals(MainTab.MORE, stack.activeTab())
    }

    @Test
    fun wideLayoutReplacesSiblingDetailsInsteadOfGrowingTheBackStack() {
        val current = VideoContainerRoute(3L, "Movies", VideoContainerRouteKind.FOLDER)
        val sibling = VideoContainerRoute(4L, "Series", VideoContainerRouteKind.FOLDER)

        assertTrue(shouldReplaceDetailRoute(singlePaneLayout = false, current = current, next = sibling))
        assertFalse(shouldReplaceDetailRoute(singlePaneLayout = true, current = current, next = sibling))
        assertFalse(shouldReplaceDetailRoute(singlePaneLayout = false, current = current, next = AudioRoute))
    }

    @Test
    fun wideBrowserBackKeepsTheDetailPaneUntilTheRoot() {
        assertTrue(shouldReplaceBrowserDetailAfterBack(singlePaneLayout = false, stackSize = 2))
        assertFalse(shouldReplaceBrowserDetailAfterBack(singlePaneLayout = false, stackSize = 1))
        assertFalse(shouldReplaceBrowserDetailAfterBack(singlePaneLayout = true, stackSize = 2))
    }

    @Test
    fun wideLibraryDetailIsReservedForPopulatedRoots() {
        assertTrue(shouldUseWideLibraryDetailLayout(singlePaneLayout = false, hasLibraryContent = true))
        assertFalse(shouldUseWideLibraryDetailLayout(singlePaneLayout = false, hasLibraryContent = false))
        assertFalse(shouldUseWideLibraryDetailLayout(singlePaneLayout = true, hasLibraryContent = true))
    }

    @Test
    fun restoredStackUsesTheLatestRootAndDropsAnObsoleteHierarchy() {
        assertEquals(
            listOf(
                BrowserRoute,
                BrowserFolderRoute.from(
                    listOf(MediaFolder(8L, "Downloads", "/storage/downloads")),
                ),
            ),
            canonicalVlcShellRouteStack(
                restored = listOf(
                    MoreRoute,
                    AboutRoute,
                    BrowserRoute,
                    BrowserFolderRoute.from(
                        listOf(MediaFolder(8L, "Downloads", "/storage/downloads")),
                    ),
                ),
                fallbackRoot = VideoRoute,
            ),
        )
    }

    @Test
    fun restoredDetailOnlyStackGetsItsRequiredRoot() {
        assertEquals(
            listOf(
                PlaylistsRoute,
                PlaylistDetailRoute(4L, "Road trip"),
                PlayerRoute,
            ),
            canonicalVlcShellRouteStack(
                restored = listOf(
                    PlaylistDetailRoute(4L, "Road trip"),
                    PlayerRoute,
                    SettingsRoute,
                ),
                fallbackRoot = VideoRoute,
            ),
        )
    }

    @Test
    fun restoredAboutDetailCannotBypassItsParent() {
        assertEquals(
            listOf(MoreRoute, AboutRoute, AboutLibrariesRoute),
            canonicalVlcShellRouteStack(
                restored = listOf(AboutLibrariesRoute),
                fallbackRoot = MoreRoute,
            ),
        )
        assertEquals(
            listOf(MoreRoute, AboutRoute, AboutAuthorsRoute),
            canonicalVlcShellRouteStack(
                restored = listOf(AboutRoute, AboutAuthorsRoute),
                fallbackRoot = VideoRoute,
            ),
        )
    }
}
