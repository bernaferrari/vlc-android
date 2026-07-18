/*
 * ************************************************************************
 *  CypherUtils.kt
 * *************************************************************************
 * Copyright © 2025 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 *
 *
 */

package org.videolan.vlc.remoteaccessserver.utils

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CypherUtils {

    private const val HMAC_ALG = "HmacSHA256"
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * Legacy unsalted SHA-256 hex digest. Kept for compatibility with older web clients
     * that still hash `code + challenge` client-side. Prefer [hmacSha256Hex] for new flows.
     */
    fun hash(message: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(message.toByteArray(Charsets.UTF_8))
        return toHex(digest)
    }

    /**
     * HMAC-SHA256 of [message] keyed by [secret], returned as lowercase hex.
     */
    fun hmacSha256Hex(secret: String, message: String): String {
        val mac = Mac.getInstance(HMAC_ALG)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_ALG))
        return toHex(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    /**
     * Constant-time equality for equal-length strings (hex digests). Returns false if lengths differ.
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * Secure alphanumeric code of [length] characters from a 32-symbol alphabet (~5 bits/char).
     * Length 8 ≈ 40 bits of entropy.
     */
    fun generateAlphanumericCode(length: Int = 8): String {
        require(length > 0) { "length must be positive" }
        // Avoid ambiguous glyphs (0/O, 1/I/L) for human entry.
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        val out = CharArray(length)
        // Rejection sampling to avoid modulo bias.
        val bound = 256 - (256 % alphabet.length)
        var i = 0
        while (i < length) {
            val v = random.nextInt(256)
            if (v >= bound) continue
            out[i++] = alphabet[v % alphabet.length]
        }
        return String(out)
    }

    private fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            out[i * 2] = HEX_CHARS[v ushr 4]
            out[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return String(out)
    }
}
