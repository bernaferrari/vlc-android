package org.videolan.vlc.gui.browser

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.vlc.R
import org.videolan.vlc.viewmodels.browser.IPathOperationDelegate
import org.videolan.vlc.viewmodels.browser.PathOperationDelegate

data class PathSegment(
    val text: String,
    val contentDescription: String?,
    val enabled: Boolean,
    val tag: String
)

fun buildPathSegments(browser: PathAdapterListener, media: MediaWrapper): List<PathSegment> {
    val context = browser.currentContext()
    val pathOperationDelegate = browser.getPathOperationDelegate()
    // We save Base64 encoded strings to avoid false positives if a user directory is named like a storage title.
    if (media.hasStateFlags(MediaLibraryItem.FLAG_STORAGE)) {
        PathOperationDelegate.storages.put(Uri.decode(media.uri.path), pathOperationDelegate.makePathSafe(media.title))
    }
    PathOperationDelegate.storages.put(
        AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY,
        pathOperationDelegate.makePathSafe(context.getString(R.string.internal_memory))
    )

    val rawSegments = preparePathSegmentUris(
        browserTitle = context.getString(R.string.browser),
        otgDevice = context.getString(R.string.otg_device_title),
        mediaUri = media.uri,
        showRoot = browser.showRoot(),
        pathOperationDelegate = pathOperationDelegate
    )

    return rawSegments.mapIndexed { index, rawSegment ->
        val segmentUri = rawSegment.toUri()
        val segmentPath = segmentUri.path
        val text = when {
            segmentPath != null && PathOperationDelegate.storages.containsKey(segmentPath) -> {
                val safePath = PathOperationDelegate.storages.valueAt(PathOperationDelegate.storages.indexOfKey(segmentPath))
                safePath?.let { pathOperationDelegate.retrieveSafePath(it) }
            }
            else -> segmentUri.lastPathSegment
        }.orEmpty()
        val isFile = try {
            segmentUri.toFile().isFile
        } catch (e: Exception) {
            false
        }
        PathSegment(
            text = text,
            contentDescription = context.getString(if (isFile) R.string.talkback_file else R.string.talkback_folder, text),
            enabled = index != rawSegments.lastIndex,
            tag = if (index == 0) "root" else rawSegment
        )
    }
}

private fun preparePathSegmentUris(
    browserTitle: String,
    otgDevice: String,
    mediaUri: Uri,
    showRoot: Boolean,
    pathOperationDelegate: IPathOperationDelegate
): MutableList<String> {
    val path = Uri.decode(mediaUri.path)
    val isOtg = path.startsWith("/tree/")
    val string = when {
        isOtg -> if (path.endsWith(':')) "" else path.substringAfterLast(':')
        else -> pathOperationDelegate.replaceStoragePath(path)
    }
    val list: MutableList<String> = mutableListOf()
    if (isOtg) list.add(otgDevice)

    val pathParts = string.split('/').filter { it.isNotEmpty() }
    for (index in pathParts.indices) {
        val currentPathUri = Uri.Builder().scheme(mediaUri.scheme).encodedAuthority(mediaUri.authority)
        for (i in 0..index) pathOperationDelegate.appendPathToUri(pathParts[i], currentPathUri)
        list.add(currentPathUri.toString())
    }
    if (showRoot) list.add(0, browserTitle)
    return list
}

interface PathAdapterListener {
    fun backTo(tag: String)
    fun currentContext(): Context
    fun showRoot(): Boolean
    fun getPathOperationDelegate(): IPathOperationDelegate
}
