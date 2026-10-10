package org.videolan.vlc.kmp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.videolan.tools.Settings
import org.videolan.vlc.gui.MainActivity

/**
 * The single way phone entry points show video: MainActivity's shared Compose PlayerRoute.
 *
 * Notification taps, external VIEW intents, picture-in-picture restore and renderer hand-back all
 * come through here, so only the route-owned [org.videolan.libvlc.util.VLCVideoLayout] ever
 * becomes a vout. TV keeps the legacy VideoPlayerActivity; check [usesSharedPlayer] first.
 */
object SharedPlayerLauncher {
    const val EXTRA_OPEN_PLAYER = "org.videolan.vlc.extra.OPEN_SHARED_PLAYER"

    /** Also re-enable video for media that was moved to background audio. */
    const val EXTRA_SWITCH_TO_VIDEO = "org.videolan.vlc.extra.SWITCH_TO_VIDEO"

    private val requests = Channel<Unit>(Channel.CONFLATED)

    /** Consumed by the shared shell; a request made before it is composed waits here. */
    val openRequests: Flow<Unit> = requests.receiveAsFlow()

    /** True while a MainActivity is started and can push PlayerRoute without a new task. */
    @Volatile
    internal var shellStarted: Boolean = false

    /** Phones use the shared player; Android TV keeps its D-pad-first VideoPlayerActivity. */
    fun usesSharedPlayer(): Boolean = !Settings.showTvUi

    fun intent(context: Context, switchToVideo: Boolean = false): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .putExtra(EXTRA_OPEN_PLAYER, true)
            .putExtra(EXTRA_SWITCH_TO_VIDEO, switchToVideo)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun pendingIntent(context: Context, switchToVideo: Boolean = false): PendingIntent =
        PendingIntent.getActivity(
            context,
            if (switchToVideo) 2 else 1,
            intent(context, switchToVideo),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Asks a running shell to show the player, starting MainActivity when it is not visible. */
    fun open(context: Context) {
        requests.trySend(Unit)
        if (!shellStarted) {
            runCatching { context.startActivity(intent(context)) }
        }
    }

    /** Called by MainActivity for its launch and new intents. Returns true when consumed. */
    fun handleIntent(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) != true) return false
        if (intent.getBooleanExtra(EXTRA_SWITCH_TO_VIDEO, false)) {
            org.videolan.vlc.PlaybackService.instance?.let { service ->
                service.currentMediaWrapper?.removeFlags(
                    org.videolan.medialibrary.interfaces.media.MediaWrapper.MEDIA_FORCE_AUDIO,
                )
                service.playlistManager.switchToVideo()
            }
        }
        intent.removeExtra(EXTRA_OPEN_PLAYER)
        intent.removeExtra(EXTRA_SWITCH_TO_VIDEO)
        requests.trySend(Unit)
        return true
    }
}
