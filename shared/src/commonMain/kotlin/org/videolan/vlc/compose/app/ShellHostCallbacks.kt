package org.videolan.vlc.compose.app

import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.util.ContextOption

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
