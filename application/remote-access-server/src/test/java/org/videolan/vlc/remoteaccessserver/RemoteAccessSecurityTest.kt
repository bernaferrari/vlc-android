/*
 * ************************************************************************
 *  RemoteAccessSecurityTest.kt
 * *************************************************************************
 * Copyright © 2025 VLC authors and VideoLAN
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 */

package org.videolan.vlc.remoteaccessserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.videolan.vlc.remoteaccessserver.utils.CypherUtils

/**
 * Host-side unit tests for remote-access crypto / CORS helpers that do not need Android runtime.
 */
class RemoteAccessSecurityTest {

    @Test
    fun otpCode_isHighEntropyAlphanumeric() {
        val code = CypherUtils.generateAlphanumericCode(RemoteAccessOTP.CODE_LENGTH)
        assertEquals(RemoteAccessOTP.CODE_LENGTH, code.length)
        // Alphabet excludes ambiguous 0/O/1/I/L
        assertTrue(code.all { it in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" })
        // Not the old 6-digit space
        assertFalse(code.all { it.isDigit() } && code.length == 6)
    }

    @Test
    fun otpCodes_areNotTriviallyRepeated() {
        val samples = (0 until 20).map { CypherUtils.generateAlphanumericCode(8) }.toSet()
        assertTrue("expected distinct OTP samples, got ${samples.size}", samples.size >= 18)
    }

    @Test
    fun hmac_differsFromUnsaltedSha256() {
        val secret = "server-secret-value"
        val message = "ABC12345:challenge-token"
        val hmac = CypherUtils.hmacSha256Hex(secret, message)
        val plain = CypherUtils.hash(message)
        assertEquals(64, hmac.length)
        assertEquals(64, plain.length)
        assertNotEquals(hmac, plain)
        // Deterministic
        assertEquals(hmac, CypherUtils.hmacSha256Hex(secret, message))
    }

    @Test
    fun constantTimeEquals_matchesEquality() {
        val a = CypherUtils.hash("same")
        val b = CypherUtils.hash("same")
        val c = CypherUtils.hash("other")
        assertTrue(CypherUtils.constantTimeEquals(a, b))
        assertFalse(CypherUtils.constantTimeEquals(a, c))
        assertFalse(CypherUtils.constantTimeEquals(a, a.dropLast(1)))
    }

    @Test
    fun cors_allowsLanAndLoopbackOnly() {
        assertTrue(RemoteAccessServer.isAllowedRemoteAccessOrigin("http://127.0.0.1:8080"))
        assertTrue(RemoteAccessServer.isAllowedRemoteAccessOrigin("https://localhost:8443"))
        assertTrue(RemoteAccessServer.isAllowedRemoteAccessOrigin("http://192.168.1.42:8080"))
        assertTrue(RemoteAccessServer.isAllowedRemoteAccessOrigin("http://10.0.0.5"))
        assertTrue(RemoteAccessServer.isAllowedRemoteAccessOrigin("http://172.16.4.1:9000"))
        assertTrue(RemoteAccessServer.isAllowedRemoteAccessOrigin("http://phone.local"))

        assertFalse(RemoteAccessServer.isAllowedRemoteAccessOrigin("https://evil.example.com"))
        assertFalse(RemoteAccessServer.isAllowedRemoteAccessOrigin("http://8.8.8.8"))
        assertFalse(RemoteAccessServer.isAllowedRemoteAccessOrigin("http://172.32.0.1")) // outside 172.16/12
        assertFalse(RemoteAccessServer.isAllowedRemoteAccessOrigin(""))
        assertFalse(RemoteAccessServer.isAllowedRemoteAccessOrigin("ftp://192.168.0.1"))
    }

    @Test
    fun localOrPrivateHost_classification() {
        assertTrue(RemoteAccessServer.isLocalOrPrivateHost("127.0.0.1"))
        assertTrue(RemoteAccessServer.isLocalOrPrivateHost("10.1.2.3"))
        assertTrue(RemoteAccessServer.isLocalOrPrivateHost("192.168.0.1"))
        assertTrue(RemoteAccessServer.isLocalOrPrivateHost("172.31.255.255"))
        assertTrue(RemoteAccessServer.isLocalOrPrivateHost("169.254.1.1"))
        assertTrue(RemoteAccessServer.isLocalOrPrivateHost("localhost"))
        assertTrue(RemoteAccessServer.isLocalOrPrivateHost("::1"))
        assertTrue(RemoteAccessServer.isLocalOrPrivateHost("fd12:3456:789a::1"))

        assertFalse(RemoteAccessServer.isLocalOrPrivateHost("1.1.1.1"))
        assertFalse(RemoteAccessServer.isLocalOrPrivateHost("172.15.0.1"))
        assertFalse(RemoteAccessServer.isLocalOrPrivateHost("example.com"))
    }
}
