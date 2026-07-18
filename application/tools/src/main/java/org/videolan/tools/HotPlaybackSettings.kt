package org.videolan.tools

import android.content.SharedPreferences

/**
 * Cached playback-critical preferences read on hot paths (PlaylistManager /
 * PlaybackService). Values are loaded once from SharedPreferences and kept in
 * sync via [SharedPreferences.OnSharedPreferenceChangeListener] plus [putSingle]
 * dual-write.
 *
 * Prefer these fields over repeated `settings.getBoolean(...)` in playback code.
 */
object HotPlaybackSettings {

    @Volatile var audioResumePlayback: Boolean = true
        private set
    @Volatile var videoResumePlayback: Boolean = true
        private set
    @Volatile var playbackHistory: Boolean = true
        private set
    @Volatile var audioForceShuffle: Boolean = false
        private set
    @Volatile var saveIndividualAudioDelay: Boolean = true
        private set
    @Volatile var enableHeadsetDetection: Boolean = true
        private set
    @Volatile var enablePlayOnHeadsetInsertion: Boolean = false
        private set
    @Volatile var alwaysFastSeek: Boolean = false
        private set
    @Volatile var lockscreenCover: Boolean = true
        private set
    @Volatile var restoreBackgroundVideo: Boolean = false
        private set
    @Volatile var showRemainingTime: Boolean = false
        private set
    @Volatile var audioStopAfter: Int = -1
        private set
    @Volatile var sleepTimerDefaultInterval: Long = -1L
        private set
    @Volatile var sleepTimerDefaultWait: Boolean = false
        private set
    @Volatile var sleepTimerDefaultResetInteraction: Boolean = false
        private set
    @Volatile var audioDelayGlobal: Long = 0L
        private set
    @Volatile var videoAppSwitch: String = "0"
        private set
    @Volatile var audioConfirmResume: String = "0"
        private set
    @Volatile var videoConfirmResume: String = "0"
        private set
    @Volatile var playbackSpeedAudioGlobal: Boolean = false
        private set
    @Volatile var playbackSpeedVideoGlobal: Boolean = false
        private set
    @Volatile var playbackSpeedAudioGlobalValue: Float = 1.0f
        private set
    @Volatile var playbackSpeedVideoGlobalValue: Float = 1.0f
        private set
    @Volatile var audioTaskRemoved: Boolean = false
        private set
    @Volatile var showSeekInCompactNotification: Boolean = false
        private set

    @Volatile
    private var registered = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key != null) applyKey(prefs, key)
        else reload(prefs)
    }

    fun attach(prefs: SharedPreferences) {
        reload(prefs)
        if (!registered) {
            prefs.registerOnSharedPreferenceChangeListener(listener)
            registered = true
        }
    }

    fun reload(prefs: SharedPreferences) {
        audioResumePlayback = prefs.getBoolean(AUDIO_RESUME_PLAYBACK, true)
        videoResumePlayback = prefs.getBoolean(VIDEO_RESUME_PLAYBACK, true)
        playbackHistory = prefs.getBoolean(PLAYBACK_HISTORY, true)
        audioForceShuffle = prefs.getBoolean(KEY_AUDIO_FORCE_SHUFFLE, false)
        saveIndividualAudioDelay = prefs.getBoolean(KEY_SAVE_INDIVIDUAL_AUDIO_DELAY, true)
        enableHeadsetDetection = prefs.getBoolean(KEY_ENABLE_HEADSET_DETECTION, true)
        enablePlayOnHeadsetInsertion = prefs.getBoolean(KEY_ENABLE_PLAY_ON_HEADSET_INSERTION, false)
        alwaysFastSeek = prefs.getBoolean(KEY_ALWAYS_FAST_SEEK, false)
        lockscreenCover = prefs.getBoolean(LOCKSCREEN_COVER, true)
        restoreBackgroundVideo = prefs.getBoolean(RESTORE_BACKGROUND_VIDEO, false)
        showRemainingTime = prefs.getBoolean(SHOW_REMAINING_TIME, false)
        audioStopAfter = prefs.getInt(AUDIO_STOP_AFTER, -1)
        sleepTimerDefaultInterval = prefs.getLong(SLEEP_TIMER_DEFAULT_INTERVAL, -1L)
        sleepTimerDefaultWait = prefs.getBoolean(SLEEP_TIMER_DEFAULT_WAIT, false)
        sleepTimerDefaultResetInteraction = prefs.getBoolean(SLEEP_TIMER_DEFAULT_RESET_INTERACTION, false)
        audioDelayGlobal = prefs.getLong(AUDIO_DELAY_GLOBAL, 0L)
        videoAppSwitch = prefs.getString(KEY_VIDEO_APP_SWITCH, "0") ?: "0"
        audioConfirmResume = prefs.getString(KEY_AUDIO_CONFIRM_RESUME, "0") ?: "0"
        videoConfirmResume = prefs.getString(KEY_VIDEO_CONFIRM_RESUME, "0") ?: "0"
        playbackSpeedAudioGlobal = prefs.getBoolean(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL, false)
        playbackSpeedVideoGlobal = prefs.getBoolean(KEY_PLAYBACK_SPEED_VIDEO_GLOBAL, false)
        playbackSpeedAudioGlobalValue = prefs.getFloat(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE, 1.0f)
        playbackSpeedVideoGlobalValue = prefs.getFloat(KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE, 1.0f)
        audioTaskRemoved = prefs.getBoolean(KEY_AUDIO_TASK_REMOVED, false)
        showSeekInCompactNotification = prefs.getBoolean(SHOW_SEEK_IN_COMPACT_NOTIFICATION, false)
    }

    private fun applyKey(prefs: SharedPreferences, key: String) {
        when (key) {
            AUDIO_RESUME_PLAYBACK -> audioResumePlayback = prefs.getBoolean(key, true)
            VIDEO_RESUME_PLAYBACK -> videoResumePlayback = prefs.getBoolean(key, true)
            PLAYBACK_HISTORY -> playbackHistory = prefs.getBoolean(key, true)
            KEY_AUDIO_FORCE_SHUFFLE -> audioForceShuffle = prefs.getBoolean(key, false)
            KEY_SAVE_INDIVIDUAL_AUDIO_DELAY -> saveIndividualAudioDelay = prefs.getBoolean(key, true)
            KEY_ENABLE_HEADSET_DETECTION -> enableHeadsetDetection = prefs.getBoolean(key, true)
            KEY_ENABLE_PLAY_ON_HEADSET_INSERTION -> enablePlayOnHeadsetInsertion = prefs.getBoolean(key, false)
            KEY_ALWAYS_FAST_SEEK -> alwaysFastSeek = prefs.getBoolean(key, false)
            LOCKSCREEN_COVER -> lockscreenCover = prefs.getBoolean(key, true)
            RESTORE_BACKGROUND_VIDEO -> restoreBackgroundVideo = prefs.getBoolean(key, false)
            SHOW_REMAINING_TIME -> showRemainingTime = prefs.getBoolean(key, false)
            AUDIO_STOP_AFTER -> audioStopAfter = prefs.getInt(key, -1)
            SLEEP_TIMER_DEFAULT_INTERVAL -> sleepTimerDefaultInterval = prefs.getLong(key, -1L)
            SLEEP_TIMER_DEFAULT_WAIT -> sleepTimerDefaultWait = prefs.getBoolean(key, false)
            SLEEP_TIMER_DEFAULT_RESET_INTERACTION -> sleepTimerDefaultResetInteraction = prefs.getBoolean(key, false)
            AUDIO_DELAY_GLOBAL -> audioDelayGlobal = prefs.getLong(key, 0L)
            KEY_VIDEO_APP_SWITCH -> videoAppSwitch = prefs.getString(key, "0") ?: "0"
            KEY_AUDIO_CONFIRM_RESUME -> audioConfirmResume = prefs.getString(key, "0") ?: "0"
            KEY_VIDEO_CONFIRM_RESUME -> videoConfirmResume = prefs.getString(key, "0") ?: "0"
            KEY_PLAYBACK_SPEED_AUDIO_GLOBAL -> playbackSpeedAudioGlobal = prefs.getBoolean(key, false)
            KEY_PLAYBACK_SPEED_VIDEO_GLOBAL -> playbackSpeedVideoGlobal = prefs.getBoolean(key, false)
            KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE -> playbackSpeedAudioGlobalValue = prefs.getFloat(key, 1.0f)
            KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE -> playbackSpeedVideoGlobalValue = prefs.getFloat(key, 1.0f)
            KEY_AUDIO_TASK_REMOVED -> audioTaskRemoved = prefs.getBoolean(key, false)
            SHOW_SEEK_IN_COMPACT_NOTIFICATION -> showSeekInCompactNotification = prefs.getBoolean(key, false)
        }
    }
}
