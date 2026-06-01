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

    private val pathOperationDelegate = browser.getPathOperationDelegate()

    init {
        //we save Base64 encoded strings to be used in substitutions to avoid false positives if a user directory is named as the media title
        // ie "SDCARD", "Internal Memory" and so on
        if (media.hasStateFlags(MediaLibraryItem.FLAG_STORAGE)) PathOperationDelegate.storages.put(Uri.decode(media.uri.path), pathOperationDelegate.makePathSafe(media.title))
        PathOperationDelegate.storages.put(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY, pathOperationDelegate.makePathSafe(browser.currentContext().getString(R.string.internal_memory)))
    }

    private val browserTitle = browser.currentContext().getString(R.string.browser)
    private val otgDevice = browser.currentContext().getString(R.string.otg_device_title)
    private val segments = prepareSegments(media.uri)

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
        val segmentUri = segments[position].toUri()
        val segmentPath = segmentUri.path
        val text: String? = when {
            //substitute a storage path to its name. See [replaceStoragePath]
            segmentPath != null && PathOperationDelegate.storages.containsKey(segmentPath) -> {
                val safePath = PathOperationDelegate.storages.valueAt(PathOperationDelegate.storages.indexOfKey(segmentPath))
                safePath?.let { pathOperationDelegate.retrieveSafePath(it) }
            }
            else -> segmentUri.lastPathSegment
        }
        val contentDescription = text?.let {
            val isFile = try {
                segments[position].toUri().toFile().isFile
            } catch (e: Exception) {
                false
            }
            holder.root.context.getString(if (isFile) R.string.talkback_file else R.string.talkback_folder, it)
        }
        holder.bind(
            text = text.orEmpty(),
            contentDescription = contentDescription,
            isLastSegment = position == segments.size - 1,
            onClick = { browser.backTo(if (position == 0) "root" else segments[position]) }
        )
    }

    inner class ViewHolder(val root: ComposeView) : RecyclerView.ViewHolder(root) {
        fun bind(text: String, contentDescription: String?, isLastSegment: Boolean, onClick: () -> Unit) {
            root.setContent {
                PathSegmentRow(
                    text = text,
                    contentDescription = contentDescription,
                    enabled = !isLastSegment,
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
    /**
     * Splits an [Uri] in a list of string used as the adapter items
     * Each item is a string representing a valid path
     *
     * @param uri the [Uri] that has to be split
     * @return a list of strings representing the items
     */
    private fun prepareSegments(uri: Uri): MutableList<String> {
        val path = Uri.decode(uri.path)
        val isOtg = path.startsWith("/tree/")
        val string = when {
            isOtg -> if (path.endsWith(':')) "" else path.substringAfterLast(':')
            else -> pathOperationDelegate.replaceStoragePath(path)
        }
        val list: MutableList<String> = mutableListOf()
        if (isOtg) list.add(otgDevice)

        //list of all the path chunks
        val pathParts = string.split('/').filter { it.isNotEmpty() }
        for (index in pathParts.indices) {
            //start creating the Uri
            val currentPathUri = Uri.Builder().scheme(uri.scheme).encodedAuthority(uri.authority)
            //append all the previous paths and the current one
            for (i in 0..index) pathOperationDelegate.appendPathToUri(pathParts[i], currentPathUri)
            list.add(currentPathUri.toString())
        }
        if (browser.showRoot()) list.add(0, browserTitle)
        return list
    }



}

interface PathAdapterListener {
    fun backTo(tag: String)
    fun currentContext(): Context
    fun showRoot(): Boolean
    fun getPathOperationDelegate(): IPathOperationDelegate
}
