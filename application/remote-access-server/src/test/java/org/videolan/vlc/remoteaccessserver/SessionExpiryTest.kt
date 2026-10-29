package org.videolan.vlc.remoteaccessserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionExpiryTest {
    private val now = 1_000_000L
    private val fourHours = 4 * 3600L

    @Test
    fun storedSessionSchemaRoundTripsWithoutKotlinReflection() {
        val adapter = userSessionListAdapter()
        val sessions = listOf(UserSession("test-session", 123_456L))
        assertEquals(sessions, adapter.fromJson(adapter.toJson(sessions)))
        assertEquals(sessions, adapter.fromJson("""[{"id":"test-session","maxAge":123456,"future":true}]"""))
    }

    @Test
    fun cookieLifetimeSecondsBecomeTimestampMilliseconds() {
        assertEquals(now + 14_400_000L, sessionExpiresAt(now, fourHours))
        assertEquals(now + 31_536_000_000L, sessionExpiresAt(now, 365 * 24 * 3600L))
    }

    @Test
    fun absentOrExpiredStoredSessionCannotBeRenewed() {
        val presented = UserSession("session", now + 100_000L)
        assertNull(refreshValidSession(presented, emptyList(), now, fourHours))
        assertNull(refreshValidSession(presented, listOf(presented.copy(maxAge = now)), now, fourHours))
        assertNull(refreshValidSession(presented, listOf(presented.copy(maxAge = now - 1)), now, fourHours))
        assertNull(refreshValidSession(presented, listOf(UserSession("different", now + 1)), now, fourHours))
    }

    @Test
    fun validStoredSessionRefreshesEvenWhenCookieContainsOlderExpiry() {
        val presented = UserSession("session", now - 1)
        val stored = presented.copy(maxAge = now + 1)
        assertEquals(
            UserSession("session", now + 14_400_000L),
            refreshValidSession(presented, listOf(stored), now, fourHours),
        )
        assertEquals(now + 1, stored.maxAge)
    }
}
