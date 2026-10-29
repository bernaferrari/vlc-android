package org.videolan.vlc.remoteaccessserver

internal fun sessionExpiresAt(nowMillis: Long, lifetimeSeconds: Long): Long =
    nowMillis + lifetimeSeconds * 1_000L

internal fun refreshValidSession(
    presented: UserSession,
    stored: List<UserSession>,
    nowMillis: Long,
    lifetimeSeconds: Long,
): UserSession? {
    // The cookie may predate a refresh; only the persisted expiry authorizes renewal.
    val authorized = stored.firstOrNull { it.id == presented.id && it.maxAge > nowMillis }
        ?: return null
    return authorized.copy(maxAge = sessionExpiresAt(nowMillis, lifetimeSeconds))
}
