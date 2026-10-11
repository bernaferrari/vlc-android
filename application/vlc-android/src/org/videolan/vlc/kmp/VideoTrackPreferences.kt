package org.videolan.vlc.kmp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.tools.AppScope
import org.videolan.tools.KEY_AUDIO_PREFERRED_LANGUAGE
import org.videolan.tools.KEY_SUBTITLE_PREFERRED_LANGUAGE
import org.videolan.tools.Settings
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.getAllTracks
import org.videolan.vlc.util.LocaleUtil
import org.videolan.vlc.util.LocaleUtil.localeEquivalent

/**
 * Picks the audio, subtitle and video tracks a video starts with, as the old video player did:
 * a track the person chose for this media last time wins, otherwise the preferred audio and
 * subtitle languages from the settings. Each kind is decided once per media, when libVLC first
 * reports a track of that kind, so a later manual choice is never overridden.
 *
 * Attached by [SharedVideoSurfaceRegistry] while a shared player host owns the video output.
 */
internal object VideoTrackPreferences : PlaybackService.Callback {
    private const val TAG = "VLC/TrackPreferences"
    private var service: PlaybackService? = null
    private var mediaKey: String? = null
    private val decided: MutableSet<Int> = java.util.Collections.synchronizedSet(mutableSetOf())

    fun attach(target: PlaybackService) {
        if (service === target) return
        detach()
        service = target
        target.addCallback(this)
    }

    fun detach() {
        service?.removeCallback(this)
        service = null
        mediaKey = null
        decided.clear()
    }

    override fun update() = Unit

    override fun onMediaEvent(event: IMedia.Event) = Unit

    override fun onMediaPlayerEvent(event: MediaPlayer.Event) {
        if (event.type != MediaPlayer.Event.ESAdded) return
        val service = service ?: return
        val current = service.currentMediaWrapper ?: return
        val key = current.uri?.toString()
        if (key != mediaKey) {
            mediaKey = key
            decided.clear()
        }
        val type = event.esChangedType
        if (type != IMedia.Track.Type.Audio && type != IMedia.Track.Type.Text && type != IMedia.Track.Type.Video) return
        if (type in decided) return
        AppScope.launch(Dispatchers.IO) {
            // Undecided kinds are retried on the next track: the preferred language is often
            // not the first track libVLC reports.
            runCatching { if (apply(service, current, type) && mediaKey == key) decided.add(type) }
                .onFailure { Log.w(TAG, "Unable to restore track preferences", it) }
        }
    }

    /** Returns true once the kind is settled (a saved or preferred track was selected). */
    private fun apply(service: PlaybackService, wrapper: MediaWrapper, type: Int): Boolean {
        val media = Medialibrary.getInstance().findMedia(wrapper) ?: wrapper
        val settings = Settings.getInstance(service)
        when (type) {
            IMedia.Track.Type.Audio -> {
                val saved = media.getMetaString(MediaWrapper.META_AUDIOTRACK)?.takeIf { it != "0" }
                val language = settings.getString(KEY_AUDIO_PREFERRED_LANGUAGE, "").orEmpty()
                val track = saved ?: preferredTrack(service, service.audioTracks?.map { it.getId() }, language) { lang ->
                    LocaleUtil.getLocaleFromVLC(lang) == language
                }
                track?.let(service::setAudioTrack)
                return track != null
            }
            IMedia.Track.Type.Text -> {
                val saved = media.getMetaString(MediaWrapper.META_SUBTITLE_TRACK)?.takeIf { it != "0" }
                val language = settings.getString(KEY_SUBTITLE_PREFERRED_LANGUAGE, "").orEmpty()
                val track = saved ?: preferredTrack(service, service.spuTracks?.map { it.getId() }, language) { lang ->
                    LocaleUtil.getLocaleFromVLC(lang) in language.localeEquivalent()
                }
                track?.let(service::setSpuTrack)
                return track != null
            }
            IMedia.Track.Type.Video -> {
                val saved = media.getMetaString(MediaWrapper.META_VIDEOTRACK)?.takeIf { it != "0" }
                if (saved != null && media.id != 0L) service.setVideoTrack(saved)
                return true
            }
        }
        return true
    }

    /** First track whose libVLC language matches the preference (see [LocaleUtil] limitations). */
    private fun preferredTrack(
        service: PlaybackService,
        ids: List<String>?,
        language: String,
        matches: (String) -> Boolean,
    ): String? {
        if (language.isEmpty() || ids.isNullOrEmpty()) return null
        val all = service.mediaplayer.media?.getAllTracks().orEmpty()
        return ids.firstOrNull { id ->
            val track = all.firstOrNull { it.id.toString() == id }
            matches(track?.language.orEmpty())
        }
    }
}
