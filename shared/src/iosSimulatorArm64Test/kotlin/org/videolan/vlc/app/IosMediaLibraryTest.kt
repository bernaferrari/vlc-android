package org.videolan.vlc.app

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType

class IosMediaLibraryTest {

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
