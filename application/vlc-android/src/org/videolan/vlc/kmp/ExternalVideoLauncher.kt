package org.videolan.vlc.kmp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.PLAY_DISABLE_HARDWARE
import org.videolan.resources.PLAY_EXTRA_FROM_START
import org.videolan.resources.PLAY_EXTRA_ITEM_LOCATION
import org.videolan.resources.PLAY_EXTRA_ITEM_TITLE
import org.videolan.resources.PLAY_EXTRA_START_TIME
import org.videolan.resources.PLAY_EXTRA_SUBTITLES_LOCATION
import org.videolan.resources.util.getFromMl
import org.videolan.resources.util.parcelable
import org.videolan.tools.AppScope
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.util.FileUtils

/**
 * Plays a video handed to VLC by another app (ACTION_VIEW) in the shared player.
 *
 * It honours the extras VideoPlayerActivity accepted (title, start time, from-start, external
 * subtitles, hardware decoding opt-out), then opens MainActivity's PlayerRoute. MainActivity is
 * started before the media loads so the app is foreground when PlaylistManager picks the video
 * route; otherwise a backgrounded start would downgrade the video to audio-only playback.
 */
internal object ExternalVideoLauncher {
    private const val TAG = "VLC/ExternalVideo"

    fun play(activity: Activity, intent: Intent) {
        val context = activity.applicationContext
        val extras = intent.extras
        val source: Uri = extras?.parcelable<Uri>(PLAY_EXTRA_ITEM_LOCATION) ?: intent.data ?: return
        val fromStart = extras?.getBoolean(PLAY_EXTRA_FROM_START, false) == true
        val startTime = when {
            fromStart || extras == null -> 0L
            else -> extras.getLong(PLAY_EXTRA_START_TIME).takeIf { it > 0L }
                ?: extras.getInt(PLAY_EXTRA_START_TIME).toLong()
        }
        val subtitles = extras?.getString(PLAY_EXTRA_SUBTITLES_LOCATION)?.takeIf(String::isNotBlank)
        val title = extras?.getString(PLAY_EXTRA_ITEM_TITLE)
        val disableHardware = intent.hasExtra(PLAY_DISABLE_HARDWARE)

        activity.startActivity(SharedPlayerLauncher.intent(activity))

        AppScope.launch {
            val uri = withContext(Dispatchers.IO) {
                runCatching { FileUtils.getPathFromURI(source) }.getOrDefault("")
                    .takeIf(String::isNotEmpty)?.toUri() ?: source
            }
            val media = context.getFromMl { getMedia(uri) } ?: MLServiceLocator.getAbstractMediaWrapper(uri)
            title?.let { media.title = Uri.decode(it) }
            media.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
            media.addFlags(MediaWrapper.MEDIA_VIDEO)
            if (fromStart) media.addFlags(MediaWrapper.MEDIA_FROM_START)
            if (disableHardware) media.addFlags(MediaWrapper.MEDIA_NO_HWACCEL)

            PlaybackService.start(context)
            val service = withTimeoutOrNull(10_000) { PlaybackService.serviceFlow.filterNotNull().first() }
            if (service == null) {
                Log.w(TAG, "Playback service unavailable for $uri")
                return@launch
            }
            if (startTime > 0L) service.saveStartTime(startTime)
            service.load(media)
            subtitles?.let { path -> attachSubtitlesWhenPlaying(service, uri, path) }
        }
    }

    /** Slaves must be added to the media that is actually playing, not to the previous one. */
    private suspend fun attachSubtitlesWhenPlaying(service: PlaybackService, uri: Uri, path: String) {
        val ready = withTimeoutOrNull(15_000) {
            while (!(service.isPlaying && service.currentMediaWrapper?.uri == uri)) delay(150)
            true
        }
        if (ready == true) service.addSubtitleTrack(path, true)
    }
}
