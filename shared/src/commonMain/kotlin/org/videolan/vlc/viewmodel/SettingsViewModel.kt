package org.videolan.vlc.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.videolan.tools.AUDIO_RESUME_PLAYBACK
import org.videolan.tools.KEY_ENABLE_REMOTE_ACCESS
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.KEY_BROWSE_NETWORK
import org.videolan.tools.KEY_SHOW_HEADERS
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.SHOW_VIDEO_THUMBNAILS
import org.videolan.tools.SettingsWriteBridge
import org.videolan.tools.VIDEO_RESUME_PLAYBACK
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.platform.platformCapabilities

data class SettingsUiState(
    val showVideoThumbs: Boolean = true,
    val playbackHistory: Boolean = true,
    val audioResume: Boolean = true,
    val videoResume: Boolean = true,
    val incognito: Boolean = false,
    val remoteAccess: Boolean = false,
    val supportsRemoteAccess: Boolean = false,
    val showHeaders: Boolean = true,
    val browseNetwork: Boolean = true,
    val supportsNetworkBrowsing: Boolean = false,
    val platformLabel: String = "",
)

/**
 * Settings surface backed by [VlcSettings] cache + [VlcPreferences] writes.
 */
class SettingsViewModel(
    private val prefs: VlcPreferences? = runCatching {
        VlcKoin.get().get<VlcPreferences>()
    }.getOrNull(),
    private val capabilities: VlcPlatformCapabilities = platformCapabilities,
) : VlcViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            showVideoThumbs = VlcSettings.showVideoThumbs.value,
            playbackHistory = true,
            audioResume = true,
            videoResume = true,
            incognito = VlcSettings.incognitoMode.value,
            remoteAccess = if (capabilities.remoteAccessServer) VlcSettings.remoteAccessEnabled.value else false,
            supportsRemoteAccess = capabilities.remoteAccessServer,
            showHeaders = VlcSettings.showHeaders.value,
            browseNetwork = VlcSettings.browseNetwork.value,
            supportsNetworkBrowsing = capabilities.networkBrowsing,
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
            VlcSettings.showHeaders.collect { v -> _state.update { it.copy(showHeaders = v) } }
        }
        if (capabilities.networkBrowsing) launch {
            VlcSettings.browseNetwork.collect { v -> _state.update { it.copy(browseNetwork = v) } }
        }
        if (capabilities.remoteAccessServer) {
            launch {
                VlcSettings.remoteAccessEnabled.collect { v -> _state.update { it.copy(remoteAccess = v) } }
            }
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
                    remoteAccess = if (capabilities.remoteAccessServer) {
                        p.getBoolean(KEY_ENABLE_REMOTE_ACCESS, false)
                    } else {
                        false
                    },
                    showHeaders = p.getBoolean(KEY_SHOW_HEADERS, true),
                    browseNetwork = if (capabilities.networkBrowsing) {
                        p.getBoolean(KEY_BROWSE_NETWORK, true)
                    } else {
                        false
                    },
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

    fun setShowHeaders(value: Boolean) = setBool(KEY_SHOW_HEADERS, value) {
        _state.update { it.copy(showHeaders = value) }
    }

    fun setBrowseNetwork(value: Boolean) {
        if (!capabilities.networkBrowsing) return
        setBool(KEY_BROWSE_NETWORK, value) {
            _state.update { it.copy(browseNetwork = value) }
        }
    }

    fun setRemoteAccess(value: Boolean) {
        if (!capabilities.remoteAccessServer) return
        setBool(KEY_ENABLE_REMOTE_ACCESS, value) {
            _state.update { it.copy(remoteAccess = value) }
        }
    }

    private fun setBool(key: String, value: Boolean, local: () -> Unit) {
        local()
        SettingsWriteBridge.onBoolean?.invoke(key, value)
        val p = prefs ?: return
        launchIo {
            try {
                VlcSettings.updateBoolean(p, key, value)
            } catch (_: Exception) {
            }
        }
    }
}
