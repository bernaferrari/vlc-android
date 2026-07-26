package org.videolan.vlc.kmp

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.TAG_ITEM
import org.videolan.vlc.compose.app.ShellHostCallbacks
import org.videolan.vlc.gui.AboutActivity
import org.videolan.vlc.gui.InfoActivity
import org.videolan.vlc.gui.helpers.AudioUtil.setRingtone
import org.videolan.vlc.gui.helpers.MedialibraryUtils
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.hf.requestOtgRoot
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.helpers.UiTools.showDonations
import org.videolan.vlc.gui.helpers.UiTools.showMediaInfo
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.share

/**
 * Android host-side actions for [org.videolan.vlc.compose.app.VlcMainShell].
 *
 * Resolves shared [MediaItem]s to medialibrary wrappers when possible and
 * forwards to existing UI helpers (info, share, subs, shortcuts, ban, …).
 * Failures are logged and ignored so the shared shell stays resilient.
 */
class AndroidShellHostCallbacks(
    private val activity: ComponentActivity,
    private val medialibrary: Medialibrary = Medialibrary.getInstance(),
) : ShellHostCallbacks {

    override fun onContextAction(item: MediaItem, option: ContextOption) {
        // Typed callbacks below cover known options; remaining are no-ops.
        Log.d(TAG, "Unhandled context action $option for ${item.uri}")
    }

    override fun onOpenInfo(item: MediaItem) {
        runCatching {
            val wrapper = resolveWrapper(item) ?: return
            if (activity is androidx.appcompat.app.AppCompatActivity) {
                activity.showMediaInfo(wrapper)
            } else {
                activity.startActivity(
                    Intent(activity, InfoActivity::class.java).putExtra(TAG_ITEM, wrapper),
                )
            }
        }.onFailure { Log.w(TAG, "onOpenInfo failed", it) }
    }

    override fun onShare(item: MediaItem) {
        val act = activity as? androidx.appcompat.app.AppCompatActivity ?: return
        act.lifecycleScope.launch {
            runCatching {
                val wrapper = resolveWrapper(item) ?: return@launch
                act.share(wrapper)
            }.onFailure { Log.w(TAG, "onShare failed", it) }
        }
    }

    override fun onDownloadSubtitles(item: MediaItem) {
        runCatching {
            val wrapper = resolveWrapper(item) ?: return
            MediaUtils.getSubs(activity, wrapper)
        }.onFailure { Log.w(TAG, "onDownloadSubtitles failed", it) }
    }

    override fun onCreateShortcut(item: MediaItem) {
        activity.lifecycleScope.launch {
            runCatching {
                val wrapper = resolveWrapper(item) ?: return@launch
                activity.createShortcut(wrapper)
            }.onFailure { Log.w(TAG, "onCreateShortcut failed", it) }
        }
    }

    override fun onSetRingtone(item: MediaItem) {
        runCatching {
            val wrapper = resolveWrapper(item) ?: return
            activity.setRingtone(wrapper)
        }.onFailure { Log.w(TAG, "onSetRingtone failed", it) }
    }

    override fun onBanFolder(folder: MediaFolder) {
        runCatching {
            val path = folder.path.ifBlank { folder.uri }
                .removePrefix("file://")
                .ifBlank { return }
            MedialibraryUtils.banDir(path)
        }.onFailure { Log.w(TAG, "onBanFolder failed", it) }
    }

    override fun onOpenAbout() {
        runCatching {
            activity.startActivity(Intent(activity, AboutActivity::class.java))
        }.onFailure { Log.w(TAG, "onOpenAbout failed", it) }
    }

    override fun onOpenDonate() {
        runCatching {
            activity.showDonations()
        }.onFailure { Log.w(TAG, "onOpenDonate failed", it) }
    }

    override fun onAddToPlaylist(items: List<MediaItem>) {
        runCatching {
            if (items.isEmpty()) return
            val wrappers = items.mapNotNull { resolveWrapper(it) }
            if (wrappers.isEmpty()) return
            activity.addToPlaylist(wrappers)
        }.onFailure { Log.w(TAG, "onAddToPlaylist failed", it) }
    }

    override fun onOpenPlaylistEditor(playlist: PlaylistInfo) {
        // Playlist editor stays on the Android native screens for now.
        Log.d(TAG, "onOpenPlaylistEditor id=${playlist.id} name=${playlist.name}")
    }

    override fun onRequestOtgRoot() {
        runCatching {
            activity.requestOtgRoot()
        }.onFailure { Log.w(TAG, "onRequestOtgRoot failed", it) }
    }

    private fun resolveWrapper(item: MediaItem): MediaWrapper? {
        return try {
            if (item.id > 0L && medialibrary.isInitiated) {
                medialibrary.getMedia(item.id)?.let { return it }
            }
            val uriString = item.uri
            if (uriString.isBlank()) return null
            val uri = uriString.toUri()
            if (medialibrary.isInitiated) {
                medialibrary.getMedia(uri)?.let { return it }
            }
            MLServiceLocator.getAbstractMediaWrapper(uri).apply {
                title = item.title
                type = when (item.type) {
                    MediaType.VIDEO -> MediaWrapper.TYPE_VIDEO
                    MediaType.AUDIO -> MediaWrapper.TYPE_AUDIO
                    MediaType.STREAM -> MediaWrapper.TYPE_STREAM
                    MediaType.DIR -> MediaWrapper.TYPE_DIR
                    MediaType.SUBTITLE -> MediaWrapper.TYPE_SUBTITLE
                    MediaType.PLAYLIST -> MediaWrapper.TYPE_PLAYLIST
                    MediaType.GROUP -> MediaWrapper.TYPE_GROUP
                    MediaType.ALL -> type
                }
                if (!item.artworkUri.isNullOrBlank()) {
                    artworkURL = item.artworkUri
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveWrapper failed for ${item.uri}", e)
            null
        }
    }

    companion object {
        private const val TAG = "AndroidShellHostCallbacks"
    }
}
