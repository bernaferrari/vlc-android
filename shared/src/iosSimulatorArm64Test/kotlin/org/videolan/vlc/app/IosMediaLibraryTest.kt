package org.videolan.vlc.app

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okio.FileSystem
import okio.Path
import okio.buffer
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.Playlist
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.platform.IosPipController
import org.videolan.vlc.platform.IosPipHandler
import org.videolan.vlc.platform.platformCapabilities

class IosMediaLibraryTest {

    @Test
    fun iosCapabilityContractMatchesTheInstalledNativeBridges() {
        assertTrue(platformCapabilities.nativePlayback)
        assertTrue(platformCapabilities.rendererSelection)
        assertTrue(platformCapabilities.networkBrowsing)
        assertTrue(platformCapabilities.remoteAccessServer)
        assertFalse(platformCapabilities.pictureInPicture)
    }

    @Test
    fun pipCapabilityOnlyAppearsAfterTheNativeVlckitBridgeBinds() {
        val handler = RecordingPipHandler()
        IosPipController.setHandler(handler)
        try {
            assertTrue(platformCapabilities.pictureInPicture)
            assertTrue(IosPipController.enterPip())
            assertTrue(handler.entered)
            IosPipController.exitPip()
            assertTrue(handler.exited)
        } finally {
            IosPipController.setHandler(null)
        }
    }

    @Test
    fun durableCatalogRestoresMediaPlaylistsFavoritesAndHistory() = runTest {
        val store = InMemoryIosCatalogStore()
        val original = IosMediaLibrary.forTesting(store)
        val media = video(id = 40_001, uri = "file:///Documents/road-trip.mp4")

        original.upsert(media)
        val playlist = original.createPlaylist("Weekend")
        original.addToPlaylist(playlist.id, listOf(media))
        original.setFavorite(media.id, true)
        original.setFavorite(playlist.id, true)
        original.markAsPlayed(media.id)

        val restored = IosMediaLibrary.forTesting(store)
        val restoredMedia = assertNotNull(restored.getMedia(media.id))
        assertTrue(restoredMedia.isFavorite)
        assertEquals(1, restoredMedia.playedCount)
        assertEquals(listOf(media.uri), assertNotNull(restored.getPlaylist(playlist.id)).items.map { it.uri })
        assertTrue(restored.observePlaylists().firstValue().single().isFavorite)
        assertEquals(media.uri, restored.observeHistory(1).firstValue().single().item.uri)
    }

    private class RecordingPipHandler : IosPipHandler {
        var entered = false
        var exited = false
        override val isSupported: Boolean = true
        override fun enter(): Boolean {
            entered = true
            return true
        }
        override fun exit() {
            exited = true
        }
        override fun isActive(): Boolean = entered && !exited
    }

    @Test
    fun repeatedUriKeepsTheOriginalMediaIdentity() = runTest {
        val library = IosMediaLibrary.forTesting(InMemoryIosCatalogStore())
        val uri = "file:///Documents/clip.mp4"
        library.upsert(video(id = 50_000, uri = uri))
        library.upsert(video(id = 99_999, uri = uri, title = "Renamed clip"))

        assertEquals(1, library.snapshot().size)
        assertEquals(50_000, assertNotNull(library.getMedia(50_000)).id)
        assertEquals("Renamed clip", library.snapshot().single().title)
    }

    @Test
    fun renameMigratesCatalogPlaylistHistoryAndSavedSessionReferences() = runTest {
        val library = IosMediaLibrary.forTesting(InMemoryIosCatalogStore())
        val original = video(id = 51_000, uri = "file:///Documents/original.mp4")
        library.upsert(original)
        val playlist = library.createPlaylist("Saved")
        library.addToPlaylist(playlist.id, listOf(original))
        library.markAsPlayed(original.id)
        library.savePlaybackSession(Playlist(id = 0L, name = "Current", items = listOf(original)), 1_000, 100, 1f)

        library.updateMediaAfterFileRename(original.id, "file:///Documents/renamed.mp4", "renamed.mp4")

        assertEquals("file:///Documents/renamed.mp4", assertNotNull(library.getMedia(original.id)).uri)
        assertEquals("file:///Documents/renamed.mp4", assertNotNull(library.getPlaylist(playlist.id)).items.single().uri)
        assertEquals("file:///Documents/renamed.mp4", library.observeHistory(1).firstValue().single().item.uri)
        assertEquals("file:///Documents/renamed.mp4", assertNotNull(library.playbackSession()).playlist.current?.uri)
    }

    @Test
    fun bookmarksPersistAndFollowAnImportedFileRename() {
        val store = InMemoryIosCatalogStore()
        val library = IosMediaLibrary.forTesting(store)
        val original = video(id = 52_000, uri = "file:///Documents/bookmarked.mp4")
        library.upsert(original)
        val created = assertNotNull(library.addBookmark(original.uri, 12_000L))
        library.renameBookmark(original.uri, created.id, "Opening")
        library.updateMediaAfterFileRename(original.id, "file:///Documents/renamed.mp4", "renamed.mp4")

        val restored = IosMediaLibrary.forTesting(store)
        assertEquals(
            listOf("Opening" to 12_000L),
            restored.bookmarksFor("file:///Documents/renamed.mp4").map { it.title to it.timeMs },
        )
    }

    @Test
    fun documentReconciliationRemovesMissingLocalFilesButKeepsStreamsAndPlaylistReferences() = runTest {
        val library = IosMediaLibrary.forTesting(InMemoryIosCatalogStore())
        val local = video(id = 60_001, uri = "file:///Documents/missing.mp4")
        val stream = video(id = 60_002, uri = "https://example.com/live.m3u8", type = MediaType.STREAM)
        library.upsert(local)
        library.upsert(stream)
        val playlist = library.createPlaylist("Saved")
        library.addToPlaylist(playlist.id, listOf(local, stream))

        library.reconcileLocalFiles(emptyList(), roots = listOf("/Documents"))

        assertNull(library.getMedia(local.id))
        assertNotNull(library.getMedia(stream.id))
        val saved = assertNotNull(library.getPlaylist(playlist.id))
        assertEquals(listOf(local.uri, stream.uri), saved.items.map { it.uri })
        assertFalse(library.snapshot().any { it.uri == local.uri })
    }

    @Test
    fun documentsRescanUsesTheSameEscapedFileIdentityAsTheUIKitPicker() {
        assertEquals(
            // NSURL follows the filesystem's decomposed Unicode identity on Apple platforms.
            "file:///Documents/road%20trip%20e%CC%81pisode.mp4",
            canonicalIosFileUri("/Documents/road trip épisode.mp4"),
        )
    }

    @Test
    fun pausedPlaybackSessionSurvivesCatalogRecreation() {
        val store = InMemoryIosCatalogStore()
        val original = IosMediaLibrary.forTesting(store)
        val first = video(id = 70_001, uri = "file:///Documents/first.mp4")
        val second = video(id = 70_002, uri = "file:///Documents/second.mp4")
        original.upsert(first)
        original.upsert(second)
        original.savePlaybackSession(
            playlist = Playlist(
                id = 0L,
                name = "Current",
                items = listOf(first, second),
                currentIndex = 1,
                shuffle = true,
                repeatMode = RepeatMode.ALL,
            ),
            positionMs = 42_000L,
            volume = 140,
            rate = 1.25f,
        )

        val restored = assertNotNull(IosMediaLibrary.forTesting(store).playbackSession())
        assertEquals(second.uri, restored.playlist.current?.uri)
        assertEquals(42_000L, restored.positionMs)
        assertEquals(140, restored.volume)
        assertEquals(1.25f, restored.rate)
        assertTrue(restored.playlist.shuffle)
        assertEquals(RepeatMode.ALL, restored.playlist.repeatMode)
    }

    @Test
    fun scannedMetadataRefreshKeepsUserOwnedFields() = runTest {
        val library = IosMediaLibrary.forTesting(InMemoryIosCatalogStore())
        val original = video(
            id = 80_001,
            uri = "file:///Documents/metadata.mp4",
            title = "Original",
        )
        library.upsert(original)
        library.setFavorite(original.id, true)
        library.markAsPlayed(original.id)

        library.mergeScannedMetadata(
            original.copy(
                id = 99_999,
                title = "Embedded title",
                duration = 12_000L,
                artist = "Artist",
                album = "Album",
            ),
        )

        val merged = assertNotNull(library.getMedia(original.id))
        assertEquals("Embedded title", merged.title)
        assertEquals(12_000L, merged.duration)
        assertEquals("Artist", merged.artist)
        assertEquals("Album", merged.album)
        assertTrue(merged.isFavorite)
        assertEquals(1, merged.playedCount)
    }

    @Test
    fun fileCatalogRecoversTheLastKnownGoodSnapshotAfterPrimaryCorruption() {
        withTemporaryCatalog { path, fileSystem ->
            val store = FileIosCatalogStore(path, fileSystem)
            val first = IosCatalogSnapshot(nextId = 11_000L)
            val second = IosCatalogSnapshot(nextId = 12_000L)
            store.write(first)
            store.write(second)
            fileSystem.writeUtf8(path, "{not-json")

            val recovered = assertNotNull(FileIosCatalogStore(path, fileSystem).read())

            assertEquals(first, recovered)
        }
    }

    @Test
    fun fileCatalogDoesNotTreatUnrecoverableCorruptionAsAnEmptyLibrary() {
        withTemporaryCatalog { path, fileSystem ->
            fileSystem.writeUtf8(path, "{not-json")

            assertFailsWith<IosCatalogStorageException> {
                FileIosCatalogStore(path, fileSystem).read()
            }
        }
    }

    @Test
    fun fileCatalogSurfacesWriteFailures() {
        withTemporaryCatalog { path, fileSystem ->
            val parentFile = assertNotNull(path.parent) / "not-a-directory"
            fileSystem.writeUtf8(parentFile, "blocking file")

            assertFailsWith<IosCatalogStorageException> {
                FileIosCatalogStore(parentFile / "catalog.json", fileSystem).write(IosCatalogSnapshot())
            }
        }
    }

    @Test
    fun fileCatalogMigratesTheLegacyDocumentsSnapshotOnce() {
        withTemporaryCatalog { path, fileSystem ->
            val legacy = assertNotNull(path.parent) / "Documents" / "vlc.catalog.v1.json"
            val destination = assertNotNull(path.parent) / "Application Support" / "VLC" / "vlc.catalog.v1.json"
            val snapshot = IosCatalogSnapshot(nextId = 14_000L)
            FileIosCatalogStore(legacy, fileSystem).write(snapshot)

            val restored = FileIosCatalogStore(destination, fileSystem, legacy).read()

            assertEquals(snapshot, restored)
            assertTrue(fileSystem.exists(destination))
            assertFalse(fileSystem.exists(legacy))
        }
    }

    private fun video(
        id: Long,
        uri: String,
        title: String = "Video",
        type: MediaType = MediaType.VIDEO,
    ) = MediaItem(id = id, title = title, uri = uri, type = type)
}

private class InMemoryIosCatalogStore : IosCatalogStore {
    private var snapshot: IosCatalogSnapshot? = null

    override fun read(): IosCatalogSnapshot? = snapshot

    override fun write(snapshot: IosCatalogSnapshot) {
        this.snapshot = snapshot
    }
}

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T = first()

private inline fun withTemporaryCatalog(block: (Path, FileSystem) -> Unit) {
    val fileSystem = FileSystem.SYSTEM
    val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "vlc-catalog-${Random.nextLong()}"
    val path = directory / "catalog.json"
    fileSystem.createDirectories(directory)
    try {
        block(path, fileSystem)
    } finally {
        fileSystem.deleteRecursively(directory, mustExist = false)
    }
}

private fun FileSystem.writeUtf8(path: Path, value: String) {
    val sink = sink(path).buffer()
    try {
        sink.writeUtf8(value)
    } finally {
        sink.close()
    }
}
