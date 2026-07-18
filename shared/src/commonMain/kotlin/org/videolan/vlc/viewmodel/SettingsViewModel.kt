package org.videolan.vlc.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.videolan.tools.AUDIO_RESUME_PLAYBACK
import org.videolan.tools.KEY_ENABLE_REMOTE_ACCESS
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.SHOW_VIDEO_THUMBNAILS
import org.videolan.tools.SettingsWriteBridge
import org.videolan.tools.VIDEO_RESUME_PLAYBACK
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.app.VlcKoin

data class SettingsUiState(
    val showVideoThumbs: Boolean = true,
    val playbackHistory: Boolean = true,
    val audioResume: Boolean = true,
    val videoResume: Boolean = true,
    val incognito: Boolean = false,
    val remoteAccess: Boolean = false,
    val platformLabel: String = "",
)

/**
 * Settings surface backed by [VlcSettings] cache + [VlcPreferences] writes.
 */
class SettingsViewModel(
    private val prefs: VlcPreferences? = runCatching {
        VlcKoin.get().get<VlcPreferences>()
    }.getOrNull(),
) : VlcViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            showVideoThumbs = VlcSettings.showVideoThumbs.value,
            playbackHistory = true,
            audioResume = true,
            videoResume = true,
            incognito = VlcSettings.incognitoMode.value,
            remoteAccess = VlcSettings.remoteAccessEnabled.value,
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        launch {
            VlcSettings.showVideoThumbs.collect { v -> _state.update { it.copy(showVideoThumbs = v) } }
        }
        launch {
            VlcSettings.incognitoMode.collect { v -> _state.update { it.copy(incognito = v) } }
        }
        launch {
            VlcSettings.remoteAccessEnabled.collect { v -> _state.update { it.copy(remoteAccess = v) } }
        }
        launchIo {
            val p = prefs ?: return@launchIo
            _state.update {
                it.copy(
                    playbackHistory = p.getBoolean(PLAYBACK_HISTORY, true),
                    audioResume = p.getBoolean(AUDIO_RESUME_PLAYBACK, true),
                    videoResume = p.getBoolean(VIDEO_RESUME_PLAYBACK, true),
                    showVideoThumbs = p.getBoolean(SHOW_VIDEO_THUMBNAILS, true),
                    incognito = p.getBoolean(KEY_INCOGNITO, false),
                    remoteAccess = p.getBoolean(KEY_ENABLE_REMOTE_ACCESS, false),
                )
            }
        }
    }

    fun setShowVideoThumbs(value: Boolean) = setBool(SHOW_VIDEO_THUMBNAILS, value) {
        _state.update { it.copy(showVideoThumbs = value) }
    }

    fun setPlaybackHistory(value: Boolean) = setBool(PLAYBACK_HISTORY, value) {
        _state.update { it.copy(playbackHistory = value) }
    }

    fun setAudioResume(value: Boolean) = setBool(AUDIO_RESUME_PLAYBACK, value) {
        _state.update { it.copy(audioResume = value) }
    }

    fun setVideoResume(value: Boolean) = setBool(VIDEO_RESUME_PLAYBACK, value) {
        _state.update { it.copy(videoResume = value) }
    }

    fun setIncognito(value: Boolean) = setBool(KEY_INCOGNITO, value) {
        _state.update { it.copy(incognito = value) }
    }

    fun setRemoteAccess(value: Boolean) = setBool(KEY_ENABLE_REMOTE_ACCESS, value) {
        _state.update { it.copy(remoteAccess = value) }
    }

    private fun setBool(key: String, value: Boolean, local: () -> Unit) {
        local()
        SettingsWriteBridge.onBoolean?.invoke(key, value)
        val p = prefs ?: return
        launchIo {
            try {
                p.putBoolean(key, value)
            } catch (_: Exception) {
            }
        }
    }
}
