package org.videolan.tools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.vlc.platform.PlatformInfo

/**
 * Cross-platform settings facade backed by [VlcPreferences] (DataStore).
 *
 * Mirrors the API of the Android `Settings` singleton: synchronous cached
 * property reads that are hydrated once at startup. Writes go through the
 * suspend [VlcPreferences] API.
 *
 * Platform code is responsible for calling [load] at startup (e.g. from
 * `Application.onCreate` on Android) to populate the cache before any reads.
 *
 * Existing Android code continues to use the original `Settings` singleton.
 * NEW shared code should use `VlcSettings` instead. The two can coexist
 * during the incremental migration.
 */
object VlcSettings {

    // --- Mutable cached state (hydrated by [load]) ---

    private val _firstRun = MutableStateFlow(false)
    private val _showVideoThumbs = MutableStateFlow(true)
    private val _tvUI = MutableStateFlow(false)
    private val _listTitleEllipsize = MutableStateFlow(0)
    private val _overrideTvUI = MutableStateFlow(false)
    private val _videoHudDelay = MutableStateFlow(2)
    private val _includeMissing = MutableStateFlow(true)
    private val _showHeaders = MutableStateFlow(true)
    private val _showAudioTrackInfo = MutableStateFlow(false)
    private val _videoJumpDelay = MutableStateFlow(10)
    private val _videoLongJumpDelay = MutableStateFlow(20)
    private val _videoDoubleTapJumpDelay = MutableStateFlow(10)
    private val _audioJumpDelay = MutableStateFlow(10)
    private val _audioLongJumpDelay = MutableStateFlow(20)
    private val _audioShowTrackNumbers = MutableStateFlow(false)
    private val _showHiddenFiles = MutableStateFlow(false)
    private val _showTrackNumber = MutableStateFlow(true)
    private val _tvFoldersFirst = MutableStateFlow(true)
    private val _incognitoMode = MutableStateFlow(false)
    private val _safeMode = MutableStateFlow(false)
    private val _remoteAccessEnabled = MutableStateFlow(false)
    private val _fastplaySpeed = MutableStateFlow(2f)

    // --- Public read-only StateFlows (reactive) ---

    val firstRun: StateFlow<Boolean> = _firstRun.asStateFlow()
    val showVideoThumbs: StateFlow<Boolean> = _showVideoThumbs.asStateFlow()
    val tvUI: StateFlow<Boolean> = _tvUI.asStateFlow()
    val listTitleEllipsize: StateFlow<Int> = _listTitleEllipsize.asStateFlow()
    val overrideTvUI: StateFlow<Boolean> = _overrideTvUI.asStateFlow()
    val videoHudDelay: StateFlow<Int> = _videoHudDelay.asStateFlow()
    val includeMissing: StateFlow<Boolean> = _includeMissing.asStateFlow()
    val showHeaders: StateFlow<Boolean> = _showHeaders.asStateFlow()
    val showAudioTrackInfo: StateFlow<Boolean> = _showAudioTrackInfo.asStateFlow()
    val videoJumpDelay: StateFlow<Int> = _videoJumpDelay.asStateFlow()
    val videoLongJumpDelay: StateFlow<Int> = _videoLongJumpDelay.asStateFlow()
    val videoDoubleTapJumpDelay: StateFlow<Int> = _videoDoubleTapJumpDelay.asStateFlow()
    val audioJumpDelay: StateFlow<Int> = _audioJumpDelay.asStateFlow()
    val audioLongJumpDelay: StateFlow<Int> = _audioLongJumpDelay.asStateFlow()
    val audioShowTrackNumbers: StateFlow<Boolean> = _audioShowTrackNumbers.asStateFlow()
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()
    val showTrackNumber: StateFlow<Boolean> = _showTrackNumber.asStateFlow()
    val tvFoldersFirst: StateFlow<Boolean> = _tvFoldersFirst.asStateFlow()
    val incognitoMode: StateFlow<Boolean> = _incognitoMode.asStateFlow()
    val safeMode: StateFlow<Boolean> = _safeMode.asStateFlow()
    val remoteAccessEnabled: StateFlow<Boolean> = _remoteAccessEnabled.asStateFlow()
    val fastplaySpeed: StateFlow<Float> = _fastplaySpeed.asStateFlow()

    /**
     * Platform-specific device info. Set by platform code at startup.
     * On Android this is populated from Context/PackageManager.
     */
    var platformInfo: PlatformInfo? = null

    /**
     * Whether to show the TV UI. Mirrors `Settings.showTvUi`.
     * Requires [platformInfo] to be set.
     */
    val showTvUi: Boolean
        get() = !overrideTvUI.value && (platformInfo?.isTv == true || tvUI.value)

    /**
     * Hydrate all cached settings from [VlcPreferences].
     * Must be called once at startup (e.g. from Application.onCreate).
     *
     * This is a suspend function because DataStore reads are async.
     * Call from a coroutine scope; the cached values will be available
     * synchronously afterwards.
     */
    suspend fun load(prefs: VlcPreferences) {
        _showVideoThumbs.value = prefs.getBoolean(SHOW_VIDEO_THUMBNAILS, true)
        _tvUI.value = prefs.getBoolean(PREF_TV_UI, false)
        _listTitleEllipsize.value = prefs.getString(LIST_TITLE_ELLIPSIZE, "0").toIntOrNull() ?: 0
        _videoHudDelay.value = prefs.getInt(VIDEO_HUD_TIMEOUT, 4)
        _includeMissing.value = prefs.getBoolean(KEY_INCLUDE_MISSING, true)
        _showHeaders.value = prefs.getBoolean(KEY_SHOW_HEADERS, true)
        _showAudioTrackInfo.value = prefs.getBoolean(KEY_SHOW_TRACK_INFO, false)
        _videoJumpDelay.value = prefs.getInt(KEY_VIDEO_JUMP_DELAY, 10)
        _videoLongJumpDelay.value = prefs.getInt(KEY_VIDEO_LONG_JUMP_DELAY, 20)
        _videoDoubleTapJumpDelay.value = prefs.getInt(KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY, 10)
        _audioJumpDelay.value = prefs.getInt(KEY_AUDIO_JUMP_DELAY, 10)
        _audioLongJumpDelay.value = prefs.getInt(KEY_AUDIO_LONG_JUMP_DELAY, 20)
        _audioShowTrackNumbers.value = prefs.getBoolean(KEY_AUDIO_SHOW_TRACK_NUMBERS, false)
        _showHiddenFiles.value = prefs.getBoolean(BROWSER_SHOW_HIDDEN_FILES, !tvUI.value)
        _showTrackNumber.value = prefs.getBoolean(ALBUMS_SHOW_TRACK_NUMBER, true)
        _tvFoldersFirst.value = prefs.getBoolean(TV_FOLDERS_FIRST, true)
        _incognitoMode.value = prefs.getBoolean(KEY_INCOGNITO, false)
        _safeMode.value = prefs.getBoolean(KEY_SAFE_MODE, false)
        _remoteAccessEnabled.value = prefs.getBoolean(KEY_ENABLE_REMOTE_ACCESS, false)
    }

    /**
     * Hydrate post-migration settings (mirrors Settings.initPostMigration).
     * Called after preference migrations complete.
     */
    suspend fun loadPostMigration(prefs: VlcPreferences) {
        _fastplaySpeed.value = prefs.getInt(FASTPLAY_SPEED, 20) / 10f
    }

    /**
     * Update a single boolean setting in both cache and DataStore.
     */
    suspend fun updateBoolean(prefs: VlcPreferences, key: String, value: Boolean) {
        prefs.putBoolean(key, value)
        when (key) {
            SHOW_VIDEO_THUMBNAILS -> _showVideoThumbs.value = value
            PREF_TV_UI -> _tvUI.value = value
            KEY_INCLUDE_MISSING -> _includeMissing.value = value
            KEY_SHOW_HEADERS -> _showHeaders.value = value
            KEY_SHOW_TRACK_INFO -> _showAudioTrackInfo.value = value
            BROWSER_SHOW_HIDDEN_FILES -> _showHiddenFiles.value = value
            ALBUMS_SHOW_TRACK_NUMBER -> _showTrackNumber.value = value
            TV_FOLDERS_FIRST -> _tvFoldersFirst.value = value
            KEY_INCOGNITO -> _incognitoMode.value = value
            KEY_SAFE_MODE -> _safeMode.value = value
            KEY_ENABLE_REMOTE_ACCESS -> _remoteAccessEnabled.value = value
            KEY_OVERRIDE_TV_UI -> _overrideTvUI.value = value
            KEY_AUDIO_SHOW_TRACK_NUMBERS -> _audioShowTrackNumbers.value = value
        }
    }

    /**
     * Update a single int setting in both cache and DataStore.
     */
    suspend fun updateInt(prefs: VlcPreferences, key: String, value: Int) {
        prefs.putInt(key, value)
        when (key) {
            VIDEO_HUD_TIMEOUT -> _videoHudDelay.value = value
            KEY_VIDEO_JUMP_DELAY -> _videoJumpDelay.value = value
            KEY_VIDEO_LONG_JUMP_DELAY -> _videoLongJumpDelay.value = value
            KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY -> _videoDoubleTapJumpDelay.value = value
            KEY_AUDIO_JUMP_DELAY -> _audioJumpDelay.value = value
            KEY_AUDIO_LONG_JUMP_DELAY -> _audioLongJumpDelay.value = value
            LIST_TITLE_ELLIPSIZE -> _listTitleEllipsize.value = value
        }
    }
}
