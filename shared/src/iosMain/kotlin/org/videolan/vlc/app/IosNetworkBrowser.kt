package org.videolan.vlc.app

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.videolan.vlc.model.FolderKind
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.repository.BrowserListing
import org.videolan.vlc.repository.MediaRepository

/**
 * Swift-owned MobileVLCKit network item.  It deliberately contains only values
 * that can cross the Kotlin/Swift boundary; native VLC media objects never leak
 * into shared navigation or playback state.
 */
data class IosNetworkEntry(
    val title: String,
    val uri: String,
    val isDirectory: Boolean,
    val artworkUri: String? = null,
    val durationMs: Long = 0L,
    val size: Long = 0L,
)

/** Native discovery and folder parsing seam implemented by [VlcKitNetworkBrowser] in Swift. */
interface IosNetworkBrowserBackend {
    fun startDiscovery(listener: DiscoveryListener?)
    fun browse(uri: String, listener: BrowseListener?)
    fun cancelBrowse()

    interface DiscoveryListener {
        fun onRootsChanged(entries: List<IosNetworkEntry>)
        fun onError(message: String)
    }

    interface BrowseListener {
        fun onListing(entries: List<IosNetworkEntry>)
        fun onError(message: String)
    }
}

/**
 * Keeps the optional Swift bridge out of Koin construction order.  AppDelegate
 * attaches MobileVLCKit before Compose asks for the repository, while this
 * still correctly handles a later attach in tests and previews.
 */
object IosNetworkBrowserController {
    private var backend: IosNetworkBrowserBackend? = null
    private var discoveryListener: IosNetworkBrowserBackend.DiscoveryListener? = null

    fun setBackend(backend: IosNetworkBrowserBackend?) {
        this.backend?.startDiscovery(null)
        this.backend?.cancelBrowse()
        this.backend = backend
        backend?.startDiscovery(discoveryListener)
    }

    fun startDiscovery(listener: IosNetworkBrowserBackend.DiscoveryListener?) {
        discoveryListener = listener
        backend?.startDiscovery(listener)
    }

    fun browse(uri: String, listener: IosNetworkBrowserBackend.BrowseListener?) {
        backend?.browse(uri, listener) ?: listener?.onListing(emptyList())
    }

    fun cancelBrowse() = backend?.cancelBrowse()
}

/**
 * Adds real MobileVLCKit LAN discovery and asynchronous server listing to the
 * durable iOS media catalog.  All product navigation continues to use the
 * common [MediaRepository] APIs shared with Android.
 */
class IosNetworkMediaRepository(
    private val local: IosMediaLibrary,
) : MediaRepository by local, IosMediaRepositoryMarker {
    private val roots = MutableStateFlow<List<MediaFolder>>(emptyList())

    init {
        IosNetworkBrowserController.startDiscovery(object : IosNetworkBrowserBackend.DiscoveryListener {
            override fun onRootsChanged(entries: List<IosNetworkEntry>) {
                roots.value = entries
                    .asSequence()
                    .filter { it.uri.isNotBlank() }
                    .map { entry -> entry.toFolder(isRoot = true) }
                    .distinctBy { it.uri }
                    .sortedBy { it.title.lowercase() }
                    .toList()
            }

            override fun onError(message: String) {
                // Discovery is opportunistic: an unavailable LAN service must not turn the
                // common browser root into an error state or hide local storage.
                roots.value = emptyList()
            }
        })
    }

    override fun observeNetworkRoots(): Flow<List<MediaFolder>> = roots

    // Preserve the public iOS catalog-import capability when this decorator is
    // what Koin exposes as MediaRepository.
    override fun replaceAllPublic(items: List<MediaItem>) = local.replaceAllPublic(items)

    override fun browseUri(uri: String): Flow<BrowserListing> = callbackFlow {
        IosNetworkBrowserController.browse(uri, object : IosNetworkBrowserBackend.BrowseListener {
            override fun onListing(entries: List<IosNetworkEntry>) {
                val folders = entries.asSequence()
                    .filter { it.isDirectory && it.uri.isNotBlank() }
                    .map { it.toFolder(isRoot = false) }
                    .distinctBy { it.uri }
                    .sortedBy { it.title.lowercase() }
                    .toList()
                val media = entries.asSequence()
                    .filterNot { it.isDirectory }
                    .filter { it.uri.isNotBlank() }
                    .map { entry ->
                        MediaItem(
                            id = entry.uri.hashCode().toLong(),
                            title = entry.title.ifBlank { entry.uri.substringAfterLast('/') },
                            uri = entry.uri,
                            type = MediaType.STREAM,
                            duration = entry.durationMs.coerceAtLeast(0L),
                            size = entry.size.coerceAtLeast(0L),
                            artworkUri = entry.artworkUri,
                            fileName = entry.uri.substringAfterLast('/').ifBlank { null },
                        )
                    }
                    .distinctBy { it.uri }
                    .sortedBy { it.displayTitle.lowercase() }
                    .toList()
                trySend(BrowserListing(folders = folders, media = media))
                close()
            }

            override fun onError(message: String) {
                close(IllegalStateException(message))
            }
        })
        awaitClose { IosNetworkBrowserController.cancelBrowse() }
    }
}

private fun IosNetworkEntry.toFolder(isRoot: Boolean): MediaFolder = MediaFolder(
    id = uri.hashCode().toLong(),
    title = title.ifBlank { uri.substringAfterLast('/') },
    path = uri,
    uri = uri,
    isRoot = isRoot,
    kind = FolderKind.NETWORK,
)
