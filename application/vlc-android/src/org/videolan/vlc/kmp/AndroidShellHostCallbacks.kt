package org.videolan.vlc.kmp

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.TAG_ITEM
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.compose.app.AboutAction
import org.videolan.vlc.compose.app.ShellHostCallbacks
import org.videolan.vlc.compose.components.VLCAboutVersionInfo
import org.videolan.vlc.compose.components.VLCLibraryLicense
import org.videolan.vlc.gui.AuthorsActivity
import org.videolan.vlc.gui.FeedbackActivity
import org.videolan.vlc.gui.InfoActivity
import org.videolan.vlc.gui.LibrariesActivity
import org.videolan.vlc.gui.dialogs.showConfirmDeleteComposeDialog
import org.videolan.vlc.gui.dialogs.showRenameComposeDialog
import org.videolan.vlc.gui.helpers.AudioUtil.setRingtone
import org.videolan.vlc.gui.helpers.MedialibraryUtils
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.hf.requestOtgRoot
import org.videolan.vlc.gui.helpers.UiTools.createShortcut
import org.videolan.vlc.gui.helpers.UiTools.showDonations
import org.videolan.vlc.gui.helpers.UiTools.showMediaInfo
import org.videolan.vlc.gui.helpers.UiTools.snacker
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.util.ContextOption
import org.videolan.vlc.util.openLinkIfPossible
import org.videolan.vlc.util.share
import org.videolan.vlc.app.VlcKoin
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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

    private val appLockController = runCatching {
        VlcKoin.get().get<org.videolan.vlc.platform.AppLockController>() as? AndroidAppLockController
    }.getOrNull()

    /**
     * SAF grants read access to a specific user-picked document, so this import path works without
     * asking for broad storage access. The medialibrary persists the imported URI while Android
     * persists the corresponding read grant across launches.
     *
     * The no-lifecycle-owner overload is intentional: the shared shell can be attached from the
     * legacy MainActivity after `onStart`. We unregister explicitly with the Activity lifecycle.
     */
    private val mediaImportLauncher: ActivityResultLauncher<Array<String>> =
        activity.activityResultRegistry.register(
            "vlc-shared-media-import-${System.identityHashCode(this)}",
            ActivityResultContracts.OpenMultipleDocuments(),
            ::importMedia,
        )
    private var subtitleResult: ((String) -> Unit)? = null
    private val subtitleImportLauncher: ActivityResultLauncher<Array<String>> =
        activity.activityResultRegistry.register(
            "vlc-shared-subtitle-import-${System.identityHashCode(this)}",
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            val callback = subtitleResult
            subtitleResult = null
            uri?.let { picked ->
                runCatching {
                    activity.contentResolver.takePersistableUriPermission(
                        picked,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                callback?.invoke(picked.toString())
            }
        }

    init {
        appLockController?.attach(activity)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                mediaImportLauncher.unregister()
                subtitleImportLauncher.unregister()
                appLockController?.detach()
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    override fun supportsContextAction(option: ContextOption): Boolean = option in setOf(
        ContextOption.CTX_DELETE,
        ContextOption.CTX_RENAME,
        ContextOption.CTX_INFORMATION,
        ContextOption.CTX_SHARE,
        ContextOption.CTX_DOWNLOAD_SUBTITLES,
        ContextOption.CTX_ADD_SHORTCUT,
        ContextOption.CTX_SET_RINGTONE,
        ContextOption.CTX_BAN_FOLDER,
        ContextOption.CTX_ADD_TO_PLAYLIST,
    )

    override fun supportsMediaImport(): Boolean = true

    override fun onImportMedia() {
        runCatching {
            mediaImportLauncher.launch(arrayOf("audio/*", "video/*"))
        }.onFailure { Log.w(TAG, "Unable to open media picker", it) }
    }

    override fun supportsSubtitleImport(): Boolean = true

    override fun onImportSubtitle(onPicked: (String) -> Unit) {
        subtitleResult = onPicked
        runCatching { subtitleImportLauncher.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream")) }
            .onFailure { error ->
                subtitleResult = null
                Log.w(TAG, "Unable to open subtitle picker", error)
            }
    }

    override fun onContextAction(item: MediaItem, option: ContextOption) {
        when (option) {
            ContextOption.CTX_DELETE -> confirmDelete(item)
            ContextOption.CTX_RENAME -> confirmRename(item)
            // Typed callbacks below cover the rest; never advertise a no-op action.
            else -> Log.d(TAG, "Unhandled context action $option for ${item.uri}")
        }
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

    override fun aboutVersionInfo() = VLCAboutVersionInfo(
        version = BuildConfig.VLC_VERSION_NAME,
        buildDate = activity.getString(R.string.build_time),
        changelog = activity.getString(R.string.changelog).replace("*", "\u2022"),
        detailRows = emptyList(),
    )

    override suspend fun loadAboutLicenseText(): String = withContext(Dispatchers.IO) {
        activity.resources.openRawResource(R.raw.vlc_license).bufferedReader().use { it.readText() }
    }

    override suspend fun loadAboutLibraries(): List<VLCLibraryLicense> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = activity.resources.openRawResource(R.raw.libraries).bufferedReader().use { it.readText() }
            val document = JSONObject(raw)
            val licensesById = document.optJSONArray("licenses")
                ?.jsonObjects()
                ?.mapNotNull { license -> license.requiredString("id")?.let { it to license } }
                ?.toMap()
                .orEmpty()
            document.optJSONArray("libraries")
                ?.jsonObjects()
                ?.mapNotNull { library ->
                    val title = library.requiredString("title") ?: return@mapNotNull null
                    val license = licensesById[library.requiredString("license")] ?: return@mapNotNull null
                    VLCLibraryLicense(
                        title = title,
                        copyright = library.optString("copyright"),
                        licenseTitle = license.optString("name"),
                        licenseDescription = license.optString("description"),
                        licenseLink = license.optString("link"),
                    )
                }
                .orEmpty()
        }.getOrElse { error ->
            Log.w(TAG, "Could not load bundled library licenses", error)
            emptyList()
        }
    }

    override suspend fun loadAboutAuthors(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = activity.resources.openRawResource(R.raw.authors).bufferedReader().use { it.readText() }
            JSONArray(raw).jsonStrings()
        }.getOrElse { error ->
            Log.w(TAG, "Could not load bundled authors", error)
            emptyList()
        }
    }

    override fun onOpenExternalUrl(url: String) {
        activity.openLinkIfPossible(url)
    }

    override fun onOpenAboutAction(action: AboutAction) {
        runCatching {
            when (action) {
                AboutAction.WEBSITE -> activity.openLinkIfPossible("https://www.videolan.org/vlc/")
                AboutAction.FEEDBACK -> activity.startActivity(Intent(activity, FeedbackActivity::class.java))
                AboutAction.SOURCES -> activity.openLinkIfPossible("https://code.videolan.org/videolan/vlc-android")
                AboutAction.LIBRARIES -> activity.startActivity(Intent(activity, LibrariesActivity::class.java))
                AboutAction.AUTHORS -> activity.startActivity(Intent(activity, AuthorsActivity::class.java))
                AboutAction.LICENSE -> activity.openLinkIfPossible("https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt")
            }
        }.onFailure { Log.w(TAG, "About action $action failed", it) }
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

    /**
     * Reuse the mature Android confirmation and write-permission path.  In
     * particular, [MediaUtils.deleteItem] requests scoped-storage consent
     * before touching a MediaStore/content URI instead of treating its path as
     * a normal file.
     */
    private fun confirmDelete(item: MediaItem) {
        val wrapper = resolveWrapper(item) ?: return
        activity.showConfirmDeleteComposeDialog(arrayListOf(wrapper)) {
            MediaUtils.deleteItem(activity, wrapper) { failed ->
                snacker(activity, activity.getString(org.videolan.vlc.R.string.msg_delete_failed, failed.title))
            }
        }
    }

    private fun confirmRename(item: MediaItem) {
        val wrapper = resolveWrapper(item) ?: return
        activity.showRenameComposeDialog(wrapper, isFile = true) { _, proposedName ->
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val source = wrapper.uri.path?.let(::File) ?: return@launch
                val newName = proposedName.trim()
                if (!source.isFile || !newName.isSafeFileName()) {
                    reportRenameFailure(item)
                    return@launch
                }
                val target = File(source.parentFile, newName)
                if (!source.renameTo(target)) {
                    reportRenameFailure(item)
                    return@launch
                }
                source.parentFile?.path?.let(medialibrary::reload)
            }
        }
    }

    private fun String.isSafeFileName(): Boolean =
        isNotBlank() && this !in setOf(".", "..") && '/' !in this && '\\' !in this

    private fun reportRenameFailure(item: MediaItem) {
        activity.runOnUiThread {
            Log.w(TAG, "Could not rename ${item.uri}")
            snacker(activity, activity.getString(org.videolan.vlc.R.string.unknown_error))
        }
    }

    private fun importMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        activity.lifecycleScope.launch(Dispatchers.IO) {
            uris.distinct().forEach { uri ->
                runCatching {
                    // ACTION_OPEN_DOCUMENT providers may omit persistable support. Keep playback
                    // usable for the current session in that case, but never fail the whole batch.
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }.onFailure { Log.d(TAG, "Provider did not persist $uri", it) }

                runCatching {
                    // This mirrors the original VLC playlist import path and makes the entry
                    // visible through the AndroidMediaRepository callback flow immediately.
                    medialibrary.addMedia(uri.toString(), -1L)
                }.onFailure { Log.w(TAG, "Unable to import $uri", it) }
            }
        }
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

private fun JSONArray.jsonObjects(): List<JSONObject> = buildList {
    for (index in 0 until length()) {
        optJSONObject(index)?.let(::add)
    }
}

private fun JSONArray.jsonStrings(): List<String> = buildList {
    for (index in 0 until length()) {
        optString(index).takeIf(String::isNotBlank)?.let(::add)
    }
}

private fun JSONObject.requiredString(name: String): String? =
    optString(name).takeIf(String::isNotBlank)
