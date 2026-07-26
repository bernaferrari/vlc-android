package org.videolan.vlc.compose.app

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.about
import vlc_android.shared.generated.resources.add_to_playlist
import vlc_android.shared.generated.resources.append
import vlc_android.shared.generated.resources.back
import vlc_android.shared.generated.resources.cancel
import vlc_android.shared.generated.resources.clear
import vlc_android.shared.generated.resources.delete
import vlc_android.shared.generated.resources.display_settings
import vlc_android.shared.generated.resources.download_subtitles
import vlc_android.shared.generated.resources.favorites
import vlc_android.shared.generated.resources.history
import vlc_android.shared.generated.resources.info
import vlc_android.shared.generated.resources.insert_next
import vlc_android.shared.generated.resources.move_down
import vlc_android.shared.generated.resources.move_up
import vlc_android.shared.generated.resources.ok
import vlc_android.shared.generated.resources.play
import vlc_android.shared.generated.resources.play_all
import vlc_android.shared.generated.resources.remove
import vlc_android.shared.generated.resources.retry
import vlc_android.shared.generated.resources.search
import vlc_android.shared.generated.resources.select
import vlc_android.shared.generated.resources.set_song
import vlc_android.shared.generated.resources.settings
import vlc_android.shared.generated.resources.share
import vlc_android.shared.generated.resources.sortby

/**
 * Shared chrome labels backed by composeResources.
 * Prefer these over hard-coded English in main shell panes.
 */
object ShellStrings {
    @Composable fun play(): String = stringResource(Res.string.play)
    @Composable fun playAll(): String = stringResource(Res.string.play_all)
    @Composable fun append(): String = stringResource(Res.string.append)
    @Composable fun addToPlaylist(): String = stringResource(Res.string.add_to_playlist)
    @Composable fun insertNext(): String = stringResource(Res.string.insert_next)
    @Composable fun delete(): String = stringResource(Res.string.delete)
    @Composable fun remove(): String = stringResource(Res.string.remove)
    @Composable fun retry(): String = stringResource(Res.string.retry)
    @Composable fun search(): String = stringResource(Res.string.search)
    @Composable fun history(): String = stringResource(Res.string.history)
    @Composable fun favorites(): String = stringResource(Res.string.favorites)
    @Composable fun settings(): String = stringResource(Res.string.settings)
    @Composable fun about(): String = stringResource(Res.string.about)
    @Composable fun info(): String = stringResource(Res.string.info)
    @Composable fun share(): String = stringResource(Res.string.share)
    @Composable fun downloadSubtitles(): String = stringResource(Res.string.download_subtitles)
    @Composable fun setRingtone(): String = stringResource(Res.string.set_song)
    @Composable fun sortBy(): String = stringResource(Res.string.sortby)
    @Composable fun cancel(): String = stringResource(Res.string.cancel)
    @Composable fun ok(): String = stringResource(Res.string.ok)
    @Composable fun displaySettings(): String = stringResource(Res.string.display_settings)
    @Composable fun clear(): String = stringResource(Res.string.clear)
    @Composable fun select(): String = stringResource(Res.string.select)
    @Composable fun back(): String = stringResource(Res.string.back)
    @Composable fun moveUp(): String = stringResource(Res.string.move_up)
    @Composable fun moveDown(): String = stringResource(Res.string.move_down)
}
