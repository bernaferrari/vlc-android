@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.videolan.vlc.app

import kotlinx.coroutines.test.runTest
import org.videolan.vlc.model.MediaType
import org.w3c.files.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BrowserMediaRepositoryTest {
    @Test
    fun production_repository_starts_with_only_user_media() = runTest {
        val repository = BrowserMediaRepository(
            catalogStorage = RecordingCatalogStorage(),
            fileStore = RecordingFileStore(),
        )

        assertEquals(0, repository.count(MediaType.ALL))
    }

    @Test
    fun browser_playable_uris_are_limited_to_local_objects_and_http_streams() {
        assertTrue("blob:vlc-import".isBrowserPlayableUri())
        assertTrue("https://media.example.org/live.m3u8".isBrowserPlayableUri())
        assertTrue("http://media.example.org/live.ogg".isBrowserPlayableUri())
        assertFalse("file:///Documents/local.mkv".isBrowserPlayableUri())
        assertFalse("rtsp://camera.example.org/live".isBrowserPlayableUri())
    }

    @Test
    fun catalog_snapshot_roundTrips_escaped_metadata() {
        val original = listOf(
            BrowserStoredMedia(
                id = 42L,
                title = "Live set 100%\t\u00e9",
                fileKey = "media-42",
                type = MediaType.AUDIO,
                mime = "audio/ogg; codecs=opus",
                size = 4_294_967_296L,
                lastModified = 1_710_000_000_000L,
                fileName = "set & encore.ogg",
                playedCount = 4,
                lastPlayed = 1_710_000_000_123L,
                isFavorite = true,
                seen = 1L,
            ),
        )

        assertEquals(original, decodeBrowserMediaCatalog(encodeBrowserMediaCatalog(original)))
    }

    @Test
    fun legacy_catalog_snapshot_migrates_to_default_mutable_metadata() {
        val legacy = "vlc-web-media-v1\n8\tLegacy\tmedia-8\tAUDIO\taudio%2Fogg\t12\t34\tlegacy.ogg"

        assertEquals(
            listOf(
                BrowserStoredMedia(
                    id = 8L,
                    title = "Legacy",
                    fileKey = "media-8",
                    type = MediaType.AUDIO,
                    mime = "audio/ogg",
                    size = 12L,
                    lastModified = 34L,
                    fileName = "legacy.ogg",
                ),
            ),
            decodeBrowserMediaCatalog(legacy),
        )
    }

    @Test
    fun invalid_or_duplicate_catalog_entries_are_ignored() {
        val valid = BrowserStoredMedia(
            id = 7L,
            title = "Session",
            fileKey = "media-7",
            type = MediaType.VIDEO,
            mime = "video/webm",
            size = 8L,
            lastModified = 9L,
            fileName = "session.webm",
        )
        val encoded = encodeBrowserMediaCatalog(listOf(valid))
        val duplicate = encoded.substringAfter('\n')
        val invalid = "-1\tbad\tbad\tALL\t\t-1\t-1\tbad"

        assertEquals(listOf(valid), decodeBrowserMediaCatalog("$encoded\n$duplicate\n$invalid"))
        assertEquals(emptyList(), decodeBrowserMediaCatalog("unknown-version\n$duplicate"))
    }

    @Test
    fun imported_media_reopens_from_durable_browser_storage() = runTest {
        val catalog = RecordingCatalogStorage()
        val fileStore = RecordingFileStore()
        val first = BrowserMediaRepository(catalog, fileStore, includeDemoCatalog = false)

        first.importFiles(listOf(browserFile("mix.ogg", "audio/ogg")))

        val imported = assertNotNull(first.getMedia(10_001L))
        assertEquals(MediaType.AUDIO, imported.type)
        assertTrue(imported.uri.startsWith("blob:import-media-10001"))
        assertNotNull(catalog.value)
        first.markAsPlayed(imported.id)
        first.setFavorite(imported.id, favorite = true)

        val reopened = BrowserMediaRepository(catalog, fileStore, includeDemoCatalog = false)
        val restored = assertNotNull(reopened.getMedia(10_001L))
        assertEquals("mix", restored.title)
        assertEquals("blob:restored-media-10001", restored.uri)
        assertEquals(1, restored.playedCount)
        assertTrue(restored.isFavorite)
        assertEquals(1L, restored.seen)
    }

    private class RecordingCatalogStorage : BrowserMediaCatalogStorage {
        var value: String? = null

        override fun read(): String? = value

        override fun write(value: String) {
            this.value = value
        }
    }

    private class RecordingFileStore : BrowserMediaFileStore {
        private val fileKeys = mutableSetOf<String>()

        override fun persist(file: File, fileKey: String, onComplete: (String?, File?, Boolean) -> Unit) {
            fileKeys += fileKey
            onComplete("blob:import-$fileKey", file, true)
        }

        override fun restore(fileKey: String, onComplete: (String?, File?) -> Unit) {
            val present = fileKey.takeIf(fileKeys::contains)
            onComplete(
                present?.let { "blob:restored-$it" },
                present?.let { browserFile("$it.ogg", "audio/ogg") },
            )
        }
    }
}

private fun browserFile(name: String, type: String): File = js(
    "new File(['browser repository test'], name, { type, lastModified: 1710000000000 })",
)
