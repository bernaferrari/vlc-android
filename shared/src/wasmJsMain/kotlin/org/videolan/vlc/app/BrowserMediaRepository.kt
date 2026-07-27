@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.videolan.vlc.app

import kotlinx.browser.document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.repository.FakeCatalog
import org.videolan.vlc.repository.MediaRepository
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File

/**
 * Browser-local media catalog used by the shared VLC library screens.
 *
 * Imported bytes are stored in OPFS when the browser exposes it. Only compact
 * metadata lives in localStorage, and every session reconstructs fresh blob
 * URLs from OPFS; stale entries are never presented as playable files. Demo
 * media is opt-in for previews and tests only, never shown to a real user.
 */
internal class BrowserMediaRepository(
    private val catalogStorage: BrowserMediaCatalogStorage = LocalStorageBrowserMediaCatalogStorage,
    private val fileStore: BrowserMediaFileStore = OpfsBrowserMediaFileStore,
    includeDemoCatalog: Boolean = false,
) : MediaRepository {
    private val catalogEntries = decodeBrowserMediaCatalog(catalogStorage.read()).toMutableList()
    private val items = MutableStateFlow(if (includeDemoCatalog) FakeCatalog.items else emptyList())
    private val recent = MutableStateFlow(emptyList<MediaItem>())
    // Keep the browser File handle as long as the catalog row is live. It lets Web Share hand
    // receivers the actual media bytes, rather than a process-local blob: URL.
    private val shareableFiles = mutableMapOf<Long, File>()
    private var nextId = (catalogEntries.maxOfOrNull(BrowserStoredMedia::id) ?: 10_000L) + 1L

    init {
        // A persisted entry becomes visible only after its OPFS file is opened.
        // This avoids a convincing-looking row that would fail as soon as it is played.
        catalogEntries.toList().forEach { entry ->
            fileStore.restore(entry.fileKey) { objectUrl, file ->
                if (objectUrl == null) {
                    catalogEntries.removeAll { it.fileKey == entry.fileKey }
                    shareableFiles.remove(entry.id)
                    saveCatalog()
                } else {
                    file?.let { shareableFiles[entry.id] = it }
                    addOrReplace(entry.toMediaItem(objectUrl))
                }
            }
        }
    }

    /** Called by the browser picker after a deliberate user gesture. */
    fun importFiles(files: List<File>) {
        files.forEach { file ->
            val mediaType = file.toMediaTypeOrNull() ?: return@forEach
            val id = nextId++
            val entry = BrowserStoredMedia(
                id = id,
                title = file.name.substringBeforeLast('.', missingDelimiterValue = file.name),
                fileKey = "media-$id",
                type = mediaType,
                mime = file.type.takeIf(String::isNotBlank),
                size = browserFileSize(file),
                lastModified = browserFileLastModified(file),
                fileName = file.name,
            )
            fileStore.persist(file, entry.fileKey) { objectUrl, persistedFile, durable ->
                if (objectUrl == null) return@persist
                persistedFile?.let { shareableFiles[id] = it }
                addOrReplace(entry.toMediaItem(objectUrl))
                if (durable) {
                    catalogEntries.removeAll { it.fileKey == entry.fileKey }
                    catalogEntries += entry
                    saveCatalog()
                }
            }
        }
    }

    /** Shares a selected file when the browser supports file sharing, with a useful URL/text fallback. */
    fun share(item: MediaItem) {
        val shareText = item.uri.takeIf(String::isBrowserShareableLink) ?: item.displayTitle
        shareBrowserMedia(item.displayTitle, shareText, shareableFiles[item.id])
    }

    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> =
        items.map { current ->
            if (type == MediaType.ALL) current else current.filter { it.type == type }
        }

    override suspend fun getMedia(id: Long): MediaItem? = items.value.firstOrNull { it.id == id }

    override suspend fun getMediaByIds(ids: List<Long>): List<MediaItem> {
        val idsSet = ids.toSet()
        return items.value.filter { it.id in idsSet }
    }

    override fun search(query: String, type: MediaType): Flow<List<MediaItem>> =
        observeMedia(type).map { current ->
            current.filter { item ->
                item.displayTitle.contains(query, ignoreCase = true) ||
                    item.fileName.orEmpty().contains(query, ignoreCase = true)
            }
        }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<MediaItem>> =
        recent.map { it.take(limit) }

    override suspend fun count(type: MediaType): Int =
        if (type == MediaType.ALL) items.value.size else items.value.count { it.type == type }

    override suspend fun markAsPlayed(id: Long) {
        val existing = items.value.firstOrNull { it.id == id } ?: return
        val updated = existing.copy(
            lastPlayed = currentTimeMillis(),
            playedCount = existing.playedCount + 1,
            seen = 1L,
        )
        replaceItem(updated)
        updateStoredMetadata(updated)
        recent.value = listOf(updated) + recent.value.filterNot { it.id == id }
    }

    override suspend fun markAsUnplayed(id: Long) {
        items.value.firstOrNull { it.id == id }?.let {
            val updated = it.copy(playedCount = 0, seen = 0L, lastPlayed = 0L)
            replaceItem(updated)
            updateStoredMetadata(updated)
            recent.value = recent.value.filterNot { item -> item.id == id }
        }
    }

    override suspend fun incrementPlayCount(id: Long) = markAsPlayed(id)

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        items.value.firstOrNull { it.id == id }?.let {
            val updated = it.copy(isFavorite = favorite)
            replaceItem(updated)
            updateStoredMetadata(updated)
        }
        recent.value = recent.value.map { if (it.id == id) it.copy(isFavorite = favorite) else it }
    }

    private fun addOrReplace(item: MediaItem) {
        items.value = listOf(item) + items.value.filterNot { it.id == item.id }
    }

    private fun replaceItem(item: MediaItem) {
        items.value = items.value.map { if (it.id == item.id) item else it }
    }

    private fun saveCatalog() {
        runCatching { catalogStorage.write(encodeBrowserMediaCatalog(catalogEntries)) }
    }

    /** Persists mutable library state only for the browser-owned imported catalog. */
    private fun updateStoredMetadata(item: MediaItem) {
        val index = catalogEntries.indexOfFirst { it.id == item.id }
        if (index < 0) return
        catalogEntries[index] = catalogEntries[index].copy(
            playedCount = item.playedCount,
            lastPlayed = item.lastPlayed,
            isFavorite = item.isFavorite,
            seen = item.seen,
        )
        saveCatalog()
    }
}

/** Compact metadata record for a file that has been copied into OPFS. */
internal data class BrowserStoredMedia(
    val id: Long,
    val title: String,
    val fileKey: String,
    val type: MediaType,
    val mime: String?,
    val size: Long,
    val lastModified: Long,
    val fileName: String,
    val playedCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isFavorite: Boolean = false,
    val seen: Long = 0L,
) {
    fun toMediaItem(objectUrl: String) = MediaItem(
        id = id,
        title = title,
        uri = objectUrl,
        type = type,
        mime = mime,
        size = size,
        lastModified = lastModified,
        fileName = fileName,
        playedCount = playedCount,
        lastPlayed = lastPlayed,
        isFavorite = isFavorite,
        seen = seen,
    )
}

/** Isolates synchronous catalog persistence so codec behavior remains unit-testable. */
internal interface BrowserMediaCatalogStorage {
    fun read(): String?
    fun write(value: String)
}

internal object LocalStorageBrowserMediaCatalogStorage : BrowserMediaCatalogStorage {
    override fun read(): String? = readBrowserMediaCatalog(WEB_MEDIA_CATALOG_KEY)

    override fun write(value: String) {
        writeBrowserMediaCatalog(WEB_MEDIA_CATALOG_KEY, value)
    }
}

/** OPFS stores the actual file; an object URL is returned only while this page is alive. */
internal interface BrowserMediaFileStore {
    fun persist(
        file: File,
        fileKey: String,
        onComplete: (objectUrl: String?, file: File?, durable: Boolean) -> Unit,
    )

    fun restore(fileKey: String, onComplete: (objectUrl: String?, file: File?) -> Unit)
}

internal object OpfsBrowserMediaFileStore : BrowserMediaFileStore {
    override fun persist(file: File, fileKey: String, onComplete: (String?, File?, Boolean) -> Unit) {
        persistBrowserMediaFile(file, fileKey, onComplete)
    }

    override fun restore(fileKey: String, onComplete: (String?, File?) -> Unit) {
        restoreBrowserMediaFile(fileKey, onComplete)
    }
}

/** Opens the native file picker without adding a separate Web-only route or chrome. */
internal fun openBrowserMediaPicker(onFiles: (List<File>) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    var finished = false
    fun finish(files: List<File>) {
        if (finished) return
        finished = true
        input.parentNode?.removeChild(input)
        if (files.isNotEmpty()) onFiles(files)
    }

    input.type = "file"
    input.accept = "audio/*,video/*"
    input.multiple = true
    input.style.display = "none"
    input.onchange = {
        val selected = input.files
        val files = buildList {
            if (selected != null) {
                repeat(selected.length) { index -> selected.item(index)?.let(::add) }
            }
        }
        finish(files)
    }
    observeBrowserPickerCancellation(input) { finish(emptyList()) }
    document.body?.appendChild(input)
    input.click()
}

internal fun encodeBrowserMediaCatalog(entries: List<BrowserStoredMedia>): String = buildString {
    append(BROWSER_MEDIA_CATALOG_VERSION)
    entries.forEach { entry ->
        append('\n')
        append(
            listOf(
                entry.id.toString(),
                encodeCatalogPart(entry.title),
                encodeCatalogPart(entry.fileKey),
                entry.type.name,
                encodeCatalogPart(entry.mime.orEmpty()),
                entry.size.toString(),
                entry.lastModified.toString(),
                encodeCatalogPart(entry.fileName),
                entry.playedCount.toString(),
                entry.lastPlayed.toString(),
                entry.isFavorite.toString(),
                entry.seen.toString(),
            ).joinToString("\t"),
        )
    }
}

internal fun decodeBrowserMediaCatalog(snapshot: String?): List<BrowserStoredMedia> {
    if (snapshot.isNullOrBlank()) return emptyList()
    val lines = snapshot.lineSequence().iterator()
    if (!lines.hasNext()) return emptyList()
    val version = lines.next()
    val isLegacyV1 = version == BROWSER_MEDIA_CATALOG_VERSION_V1
    if (!isLegacyV1 && version != BROWSER_MEDIA_CATALOG_VERSION) return emptyList()
    val seenIds = mutableSetOf<Long>()
    val seenFileKeys = mutableSetOf<String>()
    return buildList {
        lines.forEach { line ->
            val fields = line.split('\t')
            if (fields.size != if (isLegacyV1) 8 else 12) return@forEach
            val id = fields[0].toLongOrNull() ?: return@forEach
            val type = runCatching { MediaType.valueOf(fields[3]) }.getOrNull() ?: return@forEach
            val title = decodeCatalogPart(fields[1]) ?: return@forEach
            val fileKey = decodeCatalogPart(fields[2]) ?: return@forEach
            val mime = decodeCatalogPart(fields[4]) ?: return@forEach
            val size = fields[5].toLongOrNull() ?: return@forEach
            val lastModified = fields[6].toLongOrNull() ?: return@forEach
            val fileName = decodeCatalogPart(fields[7]) ?: return@forEach
            val playedCount = if (isLegacyV1) 0 else fields[8].toIntOrNull() ?: return@forEach
            val lastPlayed = if (isLegacyV1) 0L else fields[9].toLongOrNull() ?: return@forEach
            val isFavorite = if (isLegacyV1) false else fields[10].toBooleanStrictOrNull() ?: return@forEach
            val seen = if (isLegacyV1) 0L else fields[11].toLongOrNull() ?: return@forEach
            if (
                id <= 0L ||
                type == MediaType.ALL ||
                fileKey.isBlank() ||
                fileName.isBlank() ||
                size < 0L ||
                lastModified < 0L ||
                playedCount < 0 ||
                lastPlayed < 0L ||
                seen < 0L ||
                !seenIds.add(id) ||
                !seenFileKeys.add(fileKey)
            ) return@forEach
            add(
                BrowserStoredMedia(
                    id = id,
                    title = title,
                    fileKey = fileKey,
                    type = type,
                    mime = mime.ifBlank { null },
                    size = size,
                    lastModified = lastModified,
                    fileName = fileName,
                    playedCount = playedCount,
                    lastPlayed = lastPlayed,
                    isFavorite = isFavorite,
                    seen = seen,
                ),
            )
        }
    }
}

private fun File.toMediaTypeOrNull(): MediaType? {
    val mime = type.lowercase()
    if (mime.startsWith("video/")) return MediaType.VIDEO
    if (mime.startsWith("audio/")) return MediaType.AUDIO
    return when (name.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "mp4", "m4v", "mkv", "mov", "webm", "avi" -> MediaType.VIDEO
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus" -> MediaType.AUDIO
        else -> null
    }
}

private fun currentTimeMillis(): Long = js("BigInt(Date.now())")

// Kotlin/Wasm represents Long as JavaScript BigInt. Browser File fields are Number, so the
// conversion belongs at this small interop boundary instead of leaking Number precision into
// common MediaItem metadata.
private fun browserFileSize(file: File): Long = js("BigInt(file.size ?? 0)")

private fun browserFileLastModified(file: File): Long = js("BigInt(Math.trunc(file.lastModified ?? 0))")

private fun encodeCatalogPart(value: String): String = js("encodeURIComponent(value)")

private fun decodeCatalogPart(value: String): String? = js(
    "(() => { try { return decodeURIComponent(value); } catch (_) { return null; } })()",
)

private fun readBrowserMediaCatalog(key: String): String? = js(
    "(() => { try { return globalThis.localStorage?.getItem(key) ?? null; } catch (_) { return null; } })()",
)

private fun writeBrowserMediaCatalog(key: String, value: String): Unit = js(
    "{ try { globalThis.localStorage?.setItem(key, value); } catch (_) {} }",
)

private fun observeBrowserPickerCancellation(input: HTMLInputElement, onCancel: () -> Unit): Unit = js(
    """{
        input.addEventListener?.('cancel', () => onCancel(), { once: true });
        globalThis.addEventListener?.('focus', () => {
            globalThis.setTimeout(() => {
                if (!input.files?.length) onCancel();
            }, 0);
        }, { once: true });
    }""",
)

private fun persistBrowserMediaFile(
    file: File,
    fileKey: String,
    onComplete: (String?, File?, Boolean) -> Unit,
): Unit = js(
    """{
        (async () => {
            try {
                const root = await globalThis.navigator?.storage?.getDirectory?.();
                if (!root) throw new Error('OPFS unavailable');
                const handle = await root.getFileHandle(fileKey, { create: true });
                const writable = await handle.createWritable();
                await writable.write(file);
                await writable.close();
                onComplete(globalThis.URL?.createObjectURL?.(file) ?? null, file, true);
            } catch (_) {
                try {
                    onComplete(globalThis.URL?.createObjectURL?.(file) ?? null, file, false);
                } catch (_) {
                    onComplete(null, null, false);
                }
            }
        })();
    }""",
)

private fun restoreBrowserMediaFile(fileKey: String, onComplete: (String?, File?) -> Unit): Unit = js(
    """{
        (async () => {
            try {
                const root = await globalThis.navigator?.storage?.getDirectory?.();
                if (!root) throw new Error('OPFS unavailable');
                const handle = await root.getFileHandle(fileKey);
                const file = await handle.getFile();
                onComplete(globalThis.URL?.createObjectURL?.(file) ?? null, file);
            } catch (_) {
                onComplete(null, null);
            }
        })();
    }""",
)

/** Prefer the actual File where supported; never expose a process-local blob URL as share text. */
private fun shareBrowserMedia(title: String, text: String, file: File?): Unit = js(
    """{
        const navigator = globalThis.navigator;
        const fallback = () => {
            const value = text || title;
            const writeText = navigator?.clipboard?.writeText;
            if (typeof writeText === 'function') {
                writeText.call(navigator.clipboard, value)
                    .catch(() => globalThis.prompt?.('Copy this media to share it:', value));
            } else {
                globalThis.prompt?.('Copy this media to share it:', value);
            }
        };
        const share = navigator?.share;
        if (typeof share !== 'function') {
            fallback();
            return;
        }
        try {
            const canShareFiles = file != null &&
                typeof navigator?.canShare === 'function' && navigator.canShare({ files: [file] });
            const payload = canShareFiles ? { title, files: [file] } : { title, text };
            const result = share.call(navigator, payload);
            result?.catch?.(error => {
                // Cancelling the native sheet is intentional; only capability failures need a fallback.
                if (error?.name !== 'AbortError') fallback();
            });
        } catch (_) {
            fallback();
        }
    }""",
)

private fun String.isBrowserShareableLink(): Boolean =
    startsWith("https://") || startsWith("http://")

private const val WEB_MEDIA_CATALOG_KEY = "org.videolan.vlc.web.media-catalog"
private const val BROWSER_MEDIA_CATALOG_VERSION = "vlc-web-media-v2"
private const val BROWSER_MEDIA_CATALOG_VERSION_V1 = "vlc-web-media-v1"
