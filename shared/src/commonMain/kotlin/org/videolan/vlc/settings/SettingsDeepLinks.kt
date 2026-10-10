package org.videolan.vlc.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A request to show Settings, optionally scrolled to (and highlighting) one preference key. */
data class SettingsDeepLinkRequest(
    val key: String? = null,
    val sectionId: String? = null,
)

/**
 * Process-wide mailbox for "open settings at …" requests coming from outside the shared shell
 * (notifications, dialogs, other activities). The shell consumes the latest request once it is
 * composed and unlocked, resolves the section that owns [SettingsDeepLinkRequest.key], and pushes the
 * matching Nav3 routes.
 */
object SettingsDeepLinks {
    private val _pending = MutableStateFlow<SettingsDeepLinkRequest?>(null)
    val pending: StateFlow<SettingsDeepLinkRequest?> = _pending.asStateFlow()

    fun open(key: String? = null, sectionId: String? = null) {
        _pending.value = SettingsDeepLinkRequest(key = key, sectionId = sectionId)
    }

    /** Returns and clears the pending request. */
    fun consume(): SettingsDeepLinkRequest? {
        val request = _pending.value
        _pending.value = null
        return request
    }
}
