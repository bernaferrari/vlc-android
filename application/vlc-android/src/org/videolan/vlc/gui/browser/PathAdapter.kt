package org.videolan.vlc.gui.browser

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AndroidDevices
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.viewmodels.browser.IPathOperationDelegate
import org.videolan.vlc.viewmodels.browser.PathOperationDelegate

class PathAdapter(val browser: PathAdapterListener, val media: MediaWrapper) : RecyclerView.Adapter<PathAdapter.ViewHolder>() {

    private val segments = buildPathSegments(browser, media)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ComposeView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            }
        )
    }

    override fun getItemCount() = segments.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val segment = segments[position]
        holder.bind(
            text = segment.text,
            contentDescription = segment.contentDescription,
            enabled = segment.enabled,
            onClick = { browser.backTo(segment.tag) }
        )
    }

    inner class ViewHolder(val root: ComposeView) : RecyclerView.ViewHolder(root) {
        fun bind(text: String, contentDescription: String?, enabled: Boolean, onClick: () -> Unit) {
            root.setContent {
                PathSegmentRow(
                    text = text,
                    contentDescription = contentDescription,
                    enabled = enabled,
                    onClick = onClick
                )
            }
        }
    }

    @Composable
    private fun PathSegmentRow(text: String, contentDescription: String?, enabled: Boolean, onClick: () -> Unit) {
        VLCTheme {
            val semanticModifier = if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            }
            Text(
                text = text,
                color = VLCThemeDefaults.colors.fontLight,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                modifier = semanticModifier
                    .clickable(enabled = enabled, onClick = onClick)
                    .focusable()
                    .padding(top = 3.dp, bottom = 4.dp)
            )
        }
    }
}

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
