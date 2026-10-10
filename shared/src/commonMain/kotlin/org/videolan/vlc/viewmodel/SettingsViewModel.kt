package org.videolan.vlc.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.videolan.tools.AUDIO_RESUME_PLAYBACK
import org.videolan.tools.ALBUMS_SHOW_TRACK_NUMBER
import org.videolan.tools.BROWSER_SHOW_HIDDEN_FILES
import org.videolan.tools.BROWSER_SHOW_ONLY_MULTIMEDIA
import org.videolan.tools.KEY_ENABLE_REMOTE_ACCESS
import org.videolan.tools.KEY_INCOGNITO
import org.videolan.tools.KEY_BROWSE_NETWORK
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL
import org.videolan.tools.KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE
import org.videolan.tools.KEY_PLAYBACK_SPEED_VIDEO_GLOBAL
import org.videolan.tools.KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE
import org.videolan.tools.KEY_SHOW_HEADERS
import org.videolan.tools.KEY_SHARED_THEME_ACCENT
import org.videolan.tools.KEY_SHARED_THEME_APPEARANCE
import org.videolan.tools.VIDEO_HUD_TIMEOUT
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.SHOW_VIDEO_THUMBNAILS
import org.videolan.tools.SettingsWriteBridge
import org.videolan.tools.VIDEO_RESUME_PLAYBACK
import org.videolan.tools.VlcPreferences
import org.videolan.tools.VlcSettings
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.platform.VlcPlatformCapabilities
import org.videolan.vlc.platform.NoOpRemoteAccessServerController
import org.videolan.vlc.platform.RemoteAccessServerController
import org.videolan.vlc.platform.platformCapabilities
import org.videolan.vlc.platform.AppLockController
import org.videolan.vlc.platform.AppLockState
import org.videolan.vlc.platform.NoOpAppLockController
import org.videolan.vlc.compose.theme.VLCThemeAccent
import org.videolan.vlc.compose.theme.VLCThemeAppearance
import org.videolan.vlc.compose.theme.resolveVLCThemePreference
import org.videolan.vlc.player.PlaybackRate
import org.videolan.vlc.settings.SettingsAction
import org.videolan.vlc.settings.SettingsConfirmation
import org.videolan.vlc.settings.SettingsContributor
import org.videolan.vlc.settings.SettingsGroup
import org.videolan.vlc.settings.SettingsItem
import org.videolan.vlc.settings.SettingsMessage
import org.videolan.vlc.settings.SettingsSection
import org.videolan.vlc.settings.SettingsToggle

data class SettingsUiState(
    val showVideoThumbs: Boolean = true,
    val playbackHistory: Boolean = true,
    val audioResume: Boolean = true,
    val videoResume: Boolean = true,
    val defaultAudioPlaybackSpeed: Float = 1f,
    val defaultVideoPlaybackSpeed: Float = 1f,
    val incognito: Boolean = false,
    val remoteAccess: Boolean = false,
    val supportsRemoteAccess: Boolean = false,
    val remoteAccessStarting: Boolean = false,
    val remoteAccessAddress: String? = null,
    val remoteAccessError: String? = null,
    val showHeaders: Boolean = true,
    val showTrackNumbers: Boolean = true,
    val showHiddenFiles: Boolean = false,
    val showOnlyMultimedia: Boolean = false,
    val browseNetwork: Boolean = true,
    val supportsNetworkBrowsing: Boolean = false,
    /** The common video HUD observes this live; Android and iOS therefore share its timeout. */
    val videoHudTimeoutSeconds: Int = 4,
    val themeAppearance: VLCThemeAppearance = VLCThemeAppearance.System,
    val themeAccent: VLCThemeAccent = VLCThemeAccent.Default,
    val platformLabel: String = "",
    val appLock: AppLockState = AppLockState(),
)

/** Everything platform contributors add to the shared settings surface. */
data class ContributedSettings(
    val rootGroups: List<SettingsGroup> = emptyList(),
    val sections: List<SettingsSection> = emptyList(),
) {
    fun section(id: String): SettingsSection? = sections.firstOrNull { it.id == id }
}

/** A value change or action waiting for the user to confirm it. */
data class PendingSettingsConfirmation(
    val confirmation: SettingsConfirmation,
    val apply: () -> Unit,
)

/** Keys of shared settings rows that are not plain preferences. */
object SharedSettingsKeys {
    const val APP_LOCK = "shared_app_lock"
    const val APP_LOCK_BIOMETRICS = "shared_app_lock_biometrics"
    const val REMOTE_ACCESS_STATUS = "shared_remote_access_status"

    /** Preference keys rendered by the shared root page. */
    val rootPreferenceKeys = setOf(
        AUDIO_RESUME_PLAYBACK, VIDEO_RESUME_PLAYBACK, KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE,
        KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE, PLAYBACK_HISTORY, KEY_INCOGNITO, VIDEO_HUD_TIMEOUT,
        SHOW_VIDEO_THUMBNAILS, KEY_SHOW_HEADERS, ALBUMS_SHOW_TRACK_NUMBER, BROWSER_SHOW_HIDDEN_FILES,
        BROWSER_SHOW_ONLY_MULTIMEDIA, KEY_BROWSE_NETWORK, KEY_ENABLE_REMOTE_ACCESS,
        KEY_SHARED_THEME_APPEARANCE, KEY_SHARED_THEME_ACCENT, APP_LOCK, APP_LOCK_BIOMETRICS,
    )
}

/**
 * Settings surface backed by [VlcSettings] cache + [VlcPreferences] writes.
 */
class SettingsViewModel(
    private val prefs: VlcPreferences? = runCatching {
        VlcKoin.get().get<VlcPreferences>()
    }.getOrNull(),
    private val capabilities: VlcPlatformCapabilities = platformCapabilities,
    private val remoteAccessServer: RemoteAccessServerController = NoOpRemoteAccessServerController,
    private val appLock: AppLockController = NoOpAppLockController,
    private val contributors: List<SettingsContributor> = emptyList(),
) : VlcViewModel() {

    private val _contributed = MutableStateFlow(ContributedSettings())
    /** Platform sections and root extensions, rebuilt whenever a contributor reports a change. */
    val contributed: StateFlow<ContributedSettings> = _contributed.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingSettingsConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingSettingsConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _messages = MutableSharedFlow<SettingsMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<SettingsMessage> = _messages.asSharedFlow()

    private val _highlightedKey = MutableStateFlow<String?>(null)
    /** Key a deep link asked to reveal; the pane scrolls to it and clears it once shown. */
    val highlightedKey: StateFlow<String?> = _highlightedKey.asStateFlow()

    private var keyOwners: Map<String, SettingsContributor> = emptyMap()
    private var openSections: Set<String> = emptySet()

    private val _state = MutableStateFlow(
        SettingsUiState(
            showVideoThumbs = VlcSettings.showVideoThumbs.value,
            playbackHistory = VlcSettings.playbackHistory.value,
            audioResume = VlcSettings.audioResumePlayback.value,
            videoResume = VlcSettings.videoResumePlayback.value,
            defaultAudioPlaybackSpeed = VlcSettings.defaultAudioPlaybackSpeed.value,
            defaultVideoPlaybackSpeed = VlcSettings.defaultVideoPlaybackSpeed.value,
            incognito = VlcSettings.incognitoMode.value,
            remoteAccess = if (capabilities.remoteAccessServer) VlcSettings.remoteAccessEnabled.value else false,
            supportsRemoteAccess = capabilities.remoteAccessServer,
            showHeaders = VlcSettings.showHeaders.value,
            showTrackNumbers = VlcSettings.showTrackNumber.value,
            showHiddenFiles = VlcSettings.showHiddenFiles.value,
            showOnlyMultimedia = VlcSettings.showOnlyMultimedia.value,
            browseNetwork = VlcSettings.browseNetwork.value,
            supportsNetworkBrowsing = capabilities.networkBrowsing,
            videoHudTimeoutSeconds = VlcSettings.videoHudDelay.value.coerceIn(1, 10),
            themeAppearance = resolveVLCThemePreference(
                VlcSettings.themeAppearance.value,
                VlcSettings.themeAccent.value,
            ).appearance,
            themeAccent = resolveVLCThemePreference(
                VlcSettings.themeAppearance.value,
                VlcSettings.themeAccent.value,
            ).accent,
            appLock = appLock.state.value,
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        refreshContributed()
        contributors.forEach { contributor ->
            launch { contributor.changes.collect { refreshContributed() } }
            launch { contributor.messages.collect { _messages.emit(it) } }
        }
        launch {
            VlcSettings.showVideoThumbs.collect { v -> _state.update { it.copy(showVideoThumbs = v) } }
        }
        launch {
            VlcSettings.incognitoMode.collect { v -> _state.update { it.copy(incognito = v) } }
        }
        launch {
            VlcSettings.playbackHistory.collect { v -> _state.update { it.copy(playbackHistory = v) } }
        }
        launch {
            VlcSettings.audioResumePlayback.collect { v -> _state.update { it.copy(audioResume = v) } }
        }
        launch {
            VlcSettings.videoResumePlayback.collect { v -> _state.update { it.copy(videoResume = v) } }
        }
        launch {
            VlcSettings.defaultAudioPlaybackSpeed.collect { speed ->
                _state.update { it.copy(defaultAudioPlaybackSpeed = speed) }
            }
        }
        launch {
            VlcSettings.defaultVideoPlaybackSpeed.collect { speed ->
                _state.update { it.copy(defaultVideoPlaybackSpeed = speed) }
            }
        }
        launch {
            VlcSettings.showHeaders.collect { v -> _state.update { it.copy(showHeaders = v) } }
        }
        launch {
            VlcSettings.showTrackNumber.collect { v -> _state.update { it.copy(showTrackNumbers = v) } }
        }
        launch {
            VlcSettings.showHiddenFiles.collect { v -> _state.update { it.copy(showHiddenFiles = v) } }
        }
        launch {
            VlcSettings.showOnlyMultimedia.collect { v -> _state.update { it.copy(showOnlyMultimedia = v) } }
        }
        if (capabilities.networkBrowsing) launch {
            VlcSettings.browseNetwork.collect { v -> _state.update { it.copy(browseNetwork = v) } }
        }
        launch {
            VlcSettings.videoHudDelay.collect { seconds ->
                _state.update { it.copy(videoHudTimeoutSeconds = seconds.coerceIn(1, 10)) }
            }
        }
        launch {
            VlcSettings.themeAppearance.collect { appearance ->
                _state.update {
                    it.copy(
                        themeAppearance = resolveVLCThemePreference(
                            appearance,
                            VlcSettings.themeAccent.value,
                        ).appearance,
                    )
                }
            }
        }
        launch {
            VlcSettings.themeAccent.collect { accent ->
                _state.update {
                    it.copy(
                        themeAccent = resolveVLCThemePreference(
                            VlcSettings.themeAppearance.value,
                            accent,
                        ).accent,
                    )
                }
            }
        }
        launch {
            appLock.state.collect { lock -> _state.update { it.copy(appLock = lock) } }
        }
        if (capabilities.remoteAccessServer) {
            launch {
                VlcSettings.remoteAccessEnabled.collect { v -> _state.update { it.copy(remoteAccess = v) } }
            }
            launch {
                remoteAccessServer.state.collect { server ->
                    _state.update {
                        it.copy(
                            remoteAccessStarting = server.isStarting,
                            remoteAccessAddress = server.address,
                            remoteAccessError = server.error,
                        )
                    }
                }
            }
        }
        launchIo {
            val p = prefs ?: return@launchIo
            _state.update {
                it.copy(
                    playbackHistory = p.getBoolean(PLAYBACK_HISTORY, true),
                    audioResume = p.getBoolean(AUDIO_RESUME_PLAYBACK, true),
                    videoResume = p.getBoolean(VIDEO_RESUME_PLAYBACK, true),
                    defaultAudioPlaybackSpeed = normalizePlaybackSpeed(
                        p.getFloat(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE, 1f)
                    ),
                    defaultVideoPlaybackSpeed = normalizePlaybackSpeed(
                        p.getFloat(KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE, 1f)
                    ),
                    showVideoThumbs = p.getBoolean(SHOW_VIDEO_THUMBNAILS, true),
                    incognito = p.getBoolean(KEY_INCOGNITO, false),
                    remoteAccess = if (capabilities.remoteAccessServer) {
                        p.getBoolean(KEY_ENABLE_REMOTE_ACCESS, false)
                    } else {
                        false
                    },
                    showHeaders = p.getBoolean(KEY_SHOW_HEADERS, true),
                    showTrackNumbers = p.getBoolean(ALBUMS_SHOW_TRACK_NUMBER, true),
                    showHiddenFiles = p.getBoolean(BROWSER_SHOW_HIDDEN_FILES, false),
                    showOnlyMultimedia = p.getBoolean(BROWSER_SHOW_ONLY_MULTIMEDIA, false),
                    browseNetwork = if (capabilities.networkBrowsing) {
                        p.getBoolean(KEY_BROWSE_NETWORK, true)
                    } else {
                        false
                    },
                    videoHudTimeoutSeconds = p.getInt(VIDEO_HUD_TIMEOUT, 4).coerceIn(1, 10),
                    themeAppearance = resolveVLCThemePreference(
                        p.getString(KEY_SHARED_THEME_APPEARANCE, "system"),
                        p.getString(KEY_SHARED_THEME_ACCENT, "orange"),
                    ).appearance,
                    themeAccent = resolveVLCThemePreference(
                        p.getString(KEY_SHARED_THEME_APPEARANCE, "system"),
                        p.getString(KEY_SHARED_THEME_ACCENT, "orange"),
                    ).accent,
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

    /** Choosing a shared default intentionally enables the matching Android global-rate mode. */
    fun setDefaultAudioPlaybackSpeed(speed: Float) {
        setFloat(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE, speed) {
            _state.update { it.copy(defaultAudioPlaybackSpeed = normalizePlaybackSpeed(speed)) }
        }
        setBool(KEY_PLAYBACK_SPEED_AUDIO_GLOBAL, true) {}
    }

    /** Choosing a shared default intentionally enables the matching Android global-rate mode. */
    fun setDefaultVideoPlaybackSpeed(speed: Float) {
        setFloat(KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE, speed) {
            _state.update { it.copy(defaultVideoPlaybackSpeed = normalizePlaybackSpeed(speed)) }
        }
        setBool(KEY_PLAYBACK_SPEED_VIDEO_GLOBAL, true) {}
    }

    fun setIncognito(value: Boolean) = setBool(KEY_INCOGNITO, value) {
        _state.update { it.copy(incognito = value) }
    }

    fun setShowHeaders(value: Boolean) = setBool(KEY_SHOW_HEADERS, value) {
        _state.update { it.copy(showHeaders = value) }
    }

    fun setShowTrackNumbers(value: Boolean) = setBool(ALBUMS_SHOW_TRACK_NUMBER, value) {
        _state.update { it.copy(showTrackNumbers = value) }
    }

    fun setShowHiddenFiles(value: Boolean) = setBool(BROWSER_SHOW_HIDDEN_FILES, value) {
        _state.update { it.copy(showHiddenFiles = value) }
    }

    fun setShowOnlyMultimedia(value: Boolean) = setBool(BROWSER_SHOW_ONLY_MULTIMEDIA, value) {
        _state.update { it.copy(showOnlyMultimedia = value) }
    }

    fun setBrowseNetwork(value: Boolean) {
        if (!capabilities.networkBrowsing) return
        setBool(KEY_BROWSE_NETWORK, value) {
            _state.update { it.copy(browseNetwork = value) }
        }
    }

    fun setRemoteAccess(value: Boolean) {
        if (!capabilities.remoteAccessServer) return
        remoteAccessServer.setEnabled(value)
        setBool(KEY_ENABLE_REMOTE_ACCESS, value) {
            _state.update { it.copy(remoteAccess = value) }
        }
    }

    fun enableAppLock() = launch { appLock.enable() }

    fun disableAppLock() = launch { appLock.disable() }

    fun unlockAppLock() = launch { appLock.unlock() }

    fun setAppLockBiometrics(enabled: Boolean) = launch { appLock.setBiometricsEnabled(enabled) }

    /** Keep the upstream 1–10 second range while avoiding an unusable instant-hide HUD. */
    fun setVideoHudTimeout(seconds: Int) {
        val normalized = seconds.coerceIn(1, 10)
        setInt(VIDEO_HUD_TIMEOUT, normalized) {
            _state.update { it.copy(videoHudTimeoutSeconds = normalized) }
        }
    }

    fun setThemeAppearance(appearance: VLCThemeAppearance) {
        setString(KEY_SHARED_THEME_APPEARANCE, appearance.storageValue) {
            _state.update { it.copy(themeAppearance = appearance) }
        }
        contributors.forEach { runCatching { it.afterValueChange(KEY_SHARED_THEME_APPEARANCE, appearance.storageValue) } }
    }

    fun setThemeAccent(accent: VLCThemeAccent) =
        setString(KEY_SHARED_THEME_ACCENT, accent.storageValue) {
            _state.update { it.copy(themeAccent = accent) }
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

    private fun setInt(key: String, value: Int, local: () -> Unit) {
        local()
        SettingsWriteBridge.onInt?.invoke(key, value)
        val p = prefs ?: return
        launchIo {
            try {
                VlcSettings.updateInt(p, key, value)
            } catch (_: Exception) {
            }
        }
    }

    private fun setFloat(key: String, value: Float, local: () -> Unit) {
        val normalized = normalizePlaybackSpeed(value)
        local()
        SettingsWriteBridge.onFloat?.invoke(key, normalized)
        val p = prefs ?: return
        launchIo {
            try {
                VlcSettings.updateFloat(p, key, normalized)
            } catch (_: Exception) {
            }
        }
    }

    private fun setString(key: String, value: String, local: () -> Unit) {
        local()
        VlcSettings.updateStringCache(key, value)
        SettingsWriteBridge.onString?.invoke(key, value)
        val p = prefs ?: return
        launchIo {
            try {
                VlcSettings.updateString(p, key, value)
            } catch (_: Exception) {
            }
        }
    }

    private fun normalizePlaybackSpeed(value: Float): Float = PlaybackRate.normalize(value)

    // --- Declarative settings model -------------------------------------------------------------

    /** Rebuilds contributed items from their platform stores. */
    fun refreshContributed() {
        // Shared keys always persist through the shared path (and its caches); a platform row that
        // shows the same key, such as Android's remote access switch, only observes the side effect.
        val owners = mutableMapOf<String, SettingsContributor>()
        fun own(key: String, contributor: SettingsContributor) {
            if (key !in SharedSettingsKeys.rootPreferenceKeys) owners[key] = contributor
        }
        val rootGroups = mutableListOf<SettingsGroup>()
        val sections = mutableListOf<SettingsSection>()
        for (contributor in contributors) {
            val groups = runCatching { contributor.rootGroups() }.getOrDefault(emptyList())
            val contributedSections = runCatching { contributor.sections() }.getOrDefault(emptyList())
            groups.forEach { group -> group.items.forEach { own(it.key, contributor) } }
            contributedSections.forEach { section ->
                own(sectionOwnerKey(section.id), contributor)
                section.infoActionKey?.let { own(it, contributor) }
                section.groups.forEach { group -> group.items.forEach { own(it.key, contributor) } }
            }
            rootGroups += groups
            sections += contributedSections
        }
        keyOwners = owners
        _contributed.value = ContributedSettings(rootGroups, sections)
    }

    /** A row asked to change its value; confirmations are resolved before anything is written. */
    fun change(item: SettingsItem, value: Any) {
        val itemConfirmation = (item as? SettingsToggle)?.let { toggle ->
            if (value == true) toggle.confirmOn else toggle.confirmOff
        }
        val confirmation = itemConfirmation
            ?: contributors.firstNotNullOfOrNull { runCatching { it.confirmationFor(item.key, value) }.getOrNull() }
        if (confirmation != null) {
            _pendingConfirmation.value = PendingSettingsConfirmation(confirmation) { applyChange(item.key, value) }
        } else {
            applyChange(item.key, value)
        }
    }

    fun action(item: SettingsAction) {
        val confirmation = item.confirmation
        if (confirmation != null) {
            _pendingConfirmation.value = PendingSettingsConfirmation(confirmation) { dispatchAction(item.key) }
        } else {
            dispatchAction(item.key)
        }
    }

    fun infoAction(section: SettingsSection) {
        section.infoActionKey?.let(::dispatchAction)
    }

    fun confirmPending() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        pending.apply()
    }

    fun dismissPending() {
        _pendingConfirmation.value = null
    }

    private fun dispatchAction(key: String) {
        keyOwners[key]?.onAction(key)
        refreshContributed()
    }

    private fun applyChange(key: String, value: Any) {
        val owner = keyOwners[key]
        if (owner != null) {
            owner.onValueChange(key, value)
        } else {
            applySharedChange(key, value)
        }
        contributors.forEach { contributor ->
            if (contributor !== owner) runCatching { contributor.afterValueChange(key, value) }
        }
        refreshContributed()
    }

    private fun applySharedChange(key: String, value: Any) {
        when (key) {
            AUDIO_RESUME_PLAYBACK -> setAudioResume(value as Boolean)
            VIDEO_RESUME_PLAYBACK -> setVideoResume(value as Boolean)
            PLAYBACK_HISTORY -> setPlaybackHistory(value as Boolean)
            KEY_INCOGNITO -> setIncognito(value as Boolean)
            SHOW_VIDEO_THUMBNAILS -> setShowVideoThumbs(value as Boolean)
            KEY_SHOW_HEADERS -> setShowHeaders(value as Boolean)
            ALBUMS_SHOW_TRACK_NUMBER -> setShowTrackNumbers(value as Boolean)
            BROWSER_SHOW_HIDDEN_FILES -> setShowHiddenFiles(value as Boolean)
            BROWSER_SHOW_ONLY_MULTIMEDIA -> setShowOnlyMultimedia(value as Boolean)
            KEY_BROWSE_NETWORK -> setBrowseNetwork(value as Boolean)
            KEY_ENABLE_REMOTE_ACCESS -> setRemoteAccess(value as Boolean)
            KEY_PLAYBACK_SPEED_AUDIO_GLOBAL_VALUE -> (value as? String)?.toFloatOrNull()?.let(::setDefaultAudioPlaybackSpeed)
            KEY_PLAYBACK_SPEED_VIDEO_GLOBAL_VALUE -> (value as? String)?.toFloatOrNull()?.let(::setDefaultVideoPlaybackSpeed)
            VIDEO_HUD_TIMEOUT -> (value as? String)?.toIntOrNull()?.let(::setVideoHudTimeout)
            SharedSettingsKeys.APP_LOCK -> if (value == true) enableAppLock() else disableAppLock()
            SharedSettingsKeys.APP_LOCK_BIOMETRICS -> setAppLockBiometrics(value as Boolean)
        }
    }

    /** Gate for entering Settings (for example a parental PIN on Android). */
    suspend fun canOpenSettings(): Boolean =
        contributors.all { runCatching { it.canOpenSettings() }.getOrDefault(true) }

    suspend fun canOpenSection(id: String): Boolean =
        keyOwners[sectionOwnerKey(id)]?.let { runCatching { it.canOpenSection(id) }.getOrDefault(true) } ?: true

    /** The shell reports which sections are on its back stack so opened/closed hooks fire once. */
    fun onOpenSectionsChanged(ids: Set<String>) {
        val opened = ids - openSections
        val closed = openSections - ids
        openSections = ids
        closed.forEach { id -> keyOwners[sectionOwnerKey(id)]?.onSectionClosed(id) }
        opened.forEach { id -> keyOwners[sectionOwnerKey(id)]?.onSectionOpened(id) }
    }

    fun onSettingsClosed() {
        onOpenSectionsChanged(emptySet())
        contributors.forEach { runCatching { it.onSettingsClosed() } }
    }

    fun highlight(key: String?) {
        _highlightedKey.value = key
    }

    fun clearHighlight(key: String) {
        if (_highlightedKey.value == key) _highlightedKey.value = null
    }

    /**
     * Resolves the section chain (outermost first) that shows [key]. An empty list means the shared
     * root page. A key may also name a section directly.
     */
    fun sectionPathFor(key: String?, sectionId: String? = null): List<String> {
        val content = contributed.value
        val sections = content.sections
        val target = sectionId?.let(content::section)
            ?: key?.let { k ->
                if (k in SharedSettingsKeys.rootPreferenceKeys) return emptyList()
                if (content.rootGroups.any { group -> group.items.any { it.key == k } }) return emptyList()
                content.section(k)
                    ?: sections.firstOrNull { section -> section.groups.any { group -> group.items.any { it.key == k } } }
            }
            ?: return emptyList()
        val path = mutableListOf(target.id)
        var parent = target.parentId
        while (parent != null && path.size < 8) {
            path.add(0, parent)
            parent = content.section(parent)?.parentId
        }
        return path
    }

    private fun sectionOwnerKey(id: String) = "section:$id"
}
