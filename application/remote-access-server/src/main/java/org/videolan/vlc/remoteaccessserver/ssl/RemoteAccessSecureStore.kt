/*
 * ************************************************************************
 *  RemoteAccessSecureStore.kt
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

package org.videolan.vlc.remoteaccessserver.ssl

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.videolan.tools.KEY_COOKIE_ENCRYPT_KEY
import org.videolan.tools.KEY_COOKIE_SIGN_KEY
import org.videolan.tools.putSingle

/**
 * Keystore-backed storage for remote-access secrets that must never sit in plaintext
 * SharedPreferences (cookie transform keys, session allow-list, OTP HMAC secret).
 *
 * Values are sealed with [SecretGenerator.encryptSealed] (AES-GCM + embedded IV on API 23+).
 * Legacy plaintext values under the same keys are migrated once, then wiped.
 */
object RemoteAccessSecureStore {

    private const val TAG = "VLC/RASecureStore"
    private const val KEY_OTP_HMAC_SECRET = "ra_otp_hmac_secret_sealed"
    private const val KEY_VALID_SESSIONS_SEALED = "valid_sessions_sealed"
    /** Legacy plaintext session list key (migrated then removed). */
    const val LEGACY_VALID_SESSIONS = "valid_sessions"

    /**
     * Read a sealed string, or null if missing / undecryptable.
     * On decryption failure the entry is wiped so the next write regenerates cleanly.
     */
    fun getSealedString(context: Context, settings: SharedPreferences, key: String): String? {
        val sealed = settings.getString(key, null) ?: return null
        if (sealed.isBlank()) return null
        return try {
            SecretGenerator.decryptSealed(context, sealed)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decrypt sealed preference $key; wiping", e)
            settings.edit().remove(key).apply()
            null
        }
    }

    fun putSealedString(context: Context, settings: SharedPreferences, key: String, value: String) {
        val sealed = SecretGenerator.encryptSealed(context, value)
        settings.putSingle(key, sealed)
    }

    /**
     * Returns the 32-char hex cookie encrypt key, generating + sealing it if absent.
     * Migrates any leftover plaintext value under [KEY_COOKIE_ENCRYPT_KEY].
     */
    fun getOrCreateCookieEncryptKey(context: Context, settings: SharedPreferences): String {
        return getOrCreateSecret(
            context,
            settings,
            sealedKey = KEY_COOKIE_ENCRYPT_KEY,
            legacyPlainKey = KEY_COOKIE_ENCRYPT_KEY,
            generator = { SecretGenerator.generateRandomAlphanumericString(32) }
        )
    }

    /**
     * Returns the 32-char hex cookie sign key, generating + sealing it if absent.
     */
    fun getOrCreateCookieSignKey(context: Context, settings: SharedPreferences): String {
        return getOrCreateSecret(
            context,
            settings,
            sealedKey = KEY_COOKIE_SIGN_KEY,
            legacyPlainKey = KEY_COOKIE_SIGN_KEY,
            generator = { SecretGenerator.generateRandomAlphanumericString(32) }
        )
    }

    /**
     * Server-side HMAC secret used to bind OTP codes to challenges.
     */
    fun getOrCreateOtpHmacSecret(context: Context, settings: SharedPreferences): String {
        return getOrCreateSecret(
            context,
            settings,
            sealedKey = KEY_OTP_HMAC_SECRET,
            legacyPlainKey = null,
            generator = { SecretGenerator.generateRandomAlphanumericString(64) }
        )
    }

    fun getValidSessionsPayload(context: Context, settings: SharedPreferences): String? {
        // Prefer sealed payload.
        getSealedString(context, settings, KEY_VALID_SESSIONS_SEALED)?.let { return it }
        // Migrate legacy plaintext once.
        val legacy = settings.getString(LEGACY_VALID_SESSIONS, null)
        if (!legacy.isNullOrBlank() && legacy != "[]") {
            return try {
                putSealedString(context, settings, KEY_VALID_SESSIONS_SEALED, legacy)
                settings.edit().remove(LEGACY_VALID_SESSIONS).apply()
                legacy
            } catch (e: Exception) {
                Log.e(TAG, "Failed to migrate VALID_SESSIONS; dropping plaintext list", e)
                settings.edit().remove(LEGACY_VALID_SESSIONS).apply()
                null
            }
        }
        // Wipe empty legacy leftovers.
        if (settings.contains(LEGACY_VALID_SESSIONS)) {
            settings.edit().remove(LEGACY_VALID_SESSIONS).apply()
        }
        return null
    }

    fun saveValidSessionsPayload(context: Context, settings: SharedPreferences, json: String) {
        putSealedString(context, settings, KEY_VALID_SESSIONS_SEALED, json)
        // Ensure plaintext legacy key is gone.
        if (settings.contains(LEGACY_VALID_SESSIONS)) {
            settings.edit().remove(LEGACY_VALID_SESSIONS).apply()
        }
    }

    private fun getOrCreateSecret(
        context: Context,
        settings: SharedPreferences,
        sealedKey: String,
        legacyPlainKey: String?,
        generator: () -> String
    ): String {
        // Try sealed read first. Sealed payloads contain ':'; plain hex keys never do.
        val existing = settings.getString(sealedKey, null)
        if (!existing.isNullOrBlank()) {
            if (existing.contains(':')) {
                getSealedString(context, settings, sealedKey)?.let { return it }
                // Fall through to regenerate after wipe.
            } else if (legacyPlainKey != null && sealedKey == legacyPlainKey) {
                // Legacy plaintext under the same key — migrate in place.
                val plain = existing
                return try {
                    putSealedString(context, settings, sealedKey, plain)
                    plain
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to seal legacy secret $sealedKey; regenerating", e)
                    settings.edit().remove(sealedKey).apply()
                    createAndSeal(context, settings, sealedKey, generator)
                }
            }
        }
        return createAndSeal(context, settings, sealedKey, generator)
    }

    private fun createAndSeal(
        context: Context,
        settings: SharedPreferences,
        sealedKey: String,
        generator: () -> String
    ): String {
        val value = generator()
        putSealedString(context, settings, sealedKey, value)
        return value
    }
}
