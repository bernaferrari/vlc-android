package org.videolan.vlc.compose.app

import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.util.ContextOption

/**
 * Platform-neutral content for the native information presentation. Keeping this
 * here makes the sheet/dialog on every host describe the exact same media item.
 */
internal data class MediaInfoPresentation(
    val title: String,
    val details: String,
)

internal fun MediaItem.infoPresentation(): MediaInfoPresentation {
    val title = displayTitle.ifBlank { fileName.orEmpty().ifBlank { uri } }
    return MediaInfoPresentation(
        title = title,
        details = listOfNotNull(
            artist?.takeIf(String::isNotBlank),
            album?.takeIf(String::isNotBlank),
            uri.takeIf(String::isNotBlank),
        ).joinToString("\n"),
    )
}

internal fun MediaInfoPresentation.dialogMessage(): String =
    listOf(title, details).filter(String::isNotBlank).joinToString("\n")

/** Shared wording for host-owned About affordances; the product chrome is not platform-branded. */
internal const val SHARED_ABOUT_MESSAGE =
    "VLC's media library and player are shared across platforms."

internal const val VLC_DONATION_URL = "https://www.videolan.org/contribute.html"

/**
 * Platform-owned actions the shared shell cannot perform in commonMain
 * (intents, SAF, JNI side-effects outside MediaRepository).
 *
 * Android wires these from MainActivity / Navigator; iOS may no-op or map
 * to UIKit share sheets.
 */
fun interface ShellHostCallbacks {
    fun onContextAction(item: MediaItem, option: ContextOption)

    /** True only when this host can perform a platform-owned menu action. */
    fun supportsContextAction(option: ContextOption): Boolean = false
    fun onOpenInfo(item: MediaItem) = Unit
    fun onShare(item: MediaItem) = Unit
    fun onDownloadSubtitles(item: MediaItem) = Unit
    fun onCreateShortcut(item: MediaItem) = Unit
    fun onSetRingtone(item: MediaItem) = Unit
    fun onBanFolder(folder: MediaFolder) = Unit
    fun onOpenAbout() = Unit
    fun onOpenDonate() = Unit
    fun onAddToPlaylist(items: List<MediaItem>) = Unit
    fun onOpenPlaylistEditor(playlist: PlaylistInfo) = Unit
    /** Request SAF/OTG document tree grant; platform updates OtgAccess.otgRoot. */
    fun onRequestOtgRoot() = Unit
    /** True when the host can present its native media-import flow. */
    fun supportsMediaImport(): Boolean = false
    /** Opens the host's media-import chooser. The permanent trigger stays in the shared shell. */
    fun onImportMedia() = Unit

    companion object {
        val NoOp = ShellHostCallbacks { _, _ -> }
    }
}

/**
 * Default dispatcher: routes known ContextOptions to typed callbacks.
 */
fun ShellHostCallbacks.dispatch(item: MediaItem, option: ContextOption) {
    when (option) {
        ContextOption.CTX_INFORMATION -> onOpenInfo(item)
        ContextOption.CTX_SHARE -> onShare(item)
        ContextOption.CTX_DOWNLOAD_SUBTITLES -> onDownloadSubtitles(item)
        ContextOption.CTX_ADD_SHORTCUT -> onCreateShortcut(item)
        ContextOption.CTX_SET_RINGTONE -> onSetRingtone(item)
        ContextOption.CTX_BAN_FOLDER -> {
            onBanFolder(
                MediaFolder(
                    id = item.id,
                    title = item.displayTitle,
                    path = item.uri,
                    uri = item.uri,
                )
            )
        }
        ContextOption.CTX_ADD_TO_PLAYLIST -> onAddToPlaylist(listOf(item))
        else -> onContextAction(item, option)
    }
}
