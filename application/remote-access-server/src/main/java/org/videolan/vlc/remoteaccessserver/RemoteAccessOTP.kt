/*
 * ************************************************************************
 *  RemoteAccessOTP.kt
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

package org.videolan.vlc.remoteaccessserver

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import org.videolan.tools.Settings
import org.videolan.vlc.gui.helpers.NotificationHelper
import org.videolan.vlc.gui.helpers.REMOTE_ACCESS_CODE_ID
import org.videolan.vlc.remoteaccessserver.ssl.RemoteAccessSecureStore
import org.videolan.vlc.remoteaccessserver.ssl.SecretGenerator
import org.videolan.vlc.remoteaccessserver.utils.CypherUtils
import org.videolan.vlc.util.RemoteAccessUtils
import java.util.concurrent.CopyOnWriteArrayList

object RemoteAccessOTP {

    /** ~40 bits of entropy (8 chars × log2(32)). */
    const val CODE_LENGTH = 8
    private const val CODE_TTL_MS = 60_000L

    private val codes = CopyOnWriteArrayList<OTPCode>()

    /**
     * generate an [OTPCode] and store it in memory for later use
     *
     * @return the generate [OTPCode]
     */
    private fun generateOTPCode(context: Context): OTPCode {
        val code = generateCode()
        val challenge = SecretGenerator.generateRandomString()
        val settings = Settings.getInstance(context)
        val secret = RemoteAccessSecureStore.getOrCreateOtpHmacSecret(context, settings)
        // Store only the server-side expected proof (HMAC), never rely on re-hashing alone.
        val expectedProof = CypherUtils.hmacSha256Hex(secret, "$code:$challenge")
        val otpCode = OTPCode(
            code = code,
            challenge = challenge,
            expiration = System.currentTimeMillis() + CODE_TTL_MS,
            expectedProof = expectedProof
        )
        codes.add(otpCode)
        return otpCode
    }

    /**
     * Human-enterable high-entropy code (alphanumeric, no ambiguous glyphs).
     * Also used by the onboarding demo animation.
     */
    fun generateCode(): String = CypherUtils.generateAlphanumericCode(CODE_LENGTH)

    /**
     * Verify if the code is valid by using the challenge.
     *
     * Accepts either:
     * - the new HMAC-SHA256 proof of `code:challenge` keyed by the server secret, or
     * - the legacy client-side SHA-256 of `code + challenge` (transition window).
     *
     * Comparison is constant-time. On success the matching code is consumed.
     *
     * @param appContext the app context used to cancel the notification
     * @param proof the client-supplied digest to verify
     * @return true if the code is valid
     */
    fun verifyCode(appContext: Context, proof: String): Boolean {
        val now = System.currentTimeMillis()
        // Drop expired entries first.
        codes.removeAll { now >= it.expiration }

        for (otp in codes) {
            val legacy = CypherUtils.hash(otp.code + otp.challenge)
            val hmacOk = CypherUtils.constantTimeEquals(otp.expectedProof, proof)
            val legacyOk = CypherUtils.constantTimeEquals(legacy, proof)
            if (hmacOk || legacyOk) {
                with(NotificationManagerCompat.from(appContext)) {
                    cancel(REMOTE_ACCESS_CODE_ID)
                }
                codes.remove(otp)
                return true
            }
        }
        return false
    }

    /**
     * Get the first code that is still valid
     *
     * @param appContext the app context used to manage the notification
     * @return the first valid code or a new one if none is found
     */
    fun getFirstValidCode(appContext: Context): OTPCode {
        val now = System.currentTimeMillis()
        codes.removeAll { now >= it.expiration }
        codes.firstOrNull()?.let { return it }

        val code = generateOTPCode(appContext)
        val notification = NotificationHelper.createRemoteAccessOtpNotification(appContext, code.code)
        with(NotificationManagerCompat.from(appContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(REMOTE_ACCESS_CODE_ID, notification)
        }
        return code
    }

    /**
     * Remove the code corresponding to the challenge
     *
     * @param challenge
     */
    fun removeCodeWithChallenge(challenge: String) {
        codes.removeAll { challenge == it.challenge }
    }

    /**
     * Remove code when the "it's not me" button is pressed
     *
     * @param code the code to remove
     */
    fun removeCode(appContext: Context, code: String) {
        codes.removeAll { code == it.code }
        with(NotificationManagerCompat.from(appContext)) {
            cancel(REMOTE_ACCESS_CODE_ID)
        }
    }

    suspend fun removeAllCodes(appContext: Context) {
        codes.clear()
        with(NotificationManagerCompat.from(appContext)) {
            cancel(REMOTE_ACCESS_CODE_ID)
        }
        RemoteAccessUtils.otpFlow.emit(null)
    }

    /** Test / diagnostics hook — number of live codes. */
    internal fun activeCodeCount(): Int = codes.size
}

data class OTPCode(
    val code: String,
    val challenge: String,
    val expiration: Long,
    /** Server-side HMAC-SHA256 hex of `code:challenge` under the OTP secret. */
    val expectedProof: String = ""
)
