/*
 * ************************************************************************
 *  RemoteAccessSession.kt
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
import android.content.SharedPreferences
import android.util.Log
import com.squareup.moshi.JsonAdapter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.sessions
import org.videolan.resources.AppContextProvider
import org.videolan.vlc.remoteaccessserver.ssl.RemoteAccessSecureStore
import org.videolan.vlc.remoteaccessserver.ssl.SecretGenerator

private const val TAG = "VLC/RemoteAccessSession"

object RemoteAccessSession {
    val maxAgeInSeconds = if (BuildConfig.DEBUG) 4 * 3600L else 3600L * 24L * 365L

    private val adapter: JsonAdapter<List<UserSession>> by lazy { userSessionListAdapter() }

    /**
     * Verify if the user is logged in
     *
     * @param settings the SharedPreferences to look into
     */
    suspend fun RoutingContext.verifyLogin(settings: SharedPreferences) {
        if (RemoteAccessServer.byPassAuth) return
        val userSession = call.sessions.get("user_session") as? UserSession
        val validated = userSession?.let { validateSession(appContext(), settings, it) }
        if (validated == null) {
            call.respond(HttpStatusCode.Unauthorized)
            throw IllegalStateException("Not logged in")
        }
        call.sessions.set("user_session", validated)
    }

    /** The sealed allowlist is authoritative for every authenticated route. */
    @Synchronized
    fun validateSession(context: Context, settings: SharedPreferences, session: UserSession): UserSession? {
        val sessions = getSessions(context, settings)
        val refreshed = refreshValidSession(session, sessions, System.currentTimeMillis(), maxAgeInSeconds)
            ?: return null
        val updated = sessions.map { if (it.id == refreshed.id) refreshed else it }
        return if (saveSessions(context, settings, updated)) refreshed else null
    }

    /**
     * Get all the valid sessions and trim the expired ones if needed
     *
     * @param context app context for keystore-backed decrypt
     * @param settings the settings to retrieve the sessions
     * @return a list of valid sessions
     */
    private fun getSessions(context: Context, settings: SharedPreferences): List<UserSession> {
        val sessionsString = try {
            RemoteAccessSecureStore.getValidSessionsPayload(context, settings) ?: "[]"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read sealed sessions", e)
            "[]"
        }
        return try {
            adapter.fromJson(sessionsString)?.let { list ->
                val valid = list.filter { it.maxAge > System.currentTimeMillis() }
                // Persist trimmed list when anything expired.
                if (valid.size != list.size) {
                    saveSessions(context, settings, valid)
                }
                valid
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse sessions payload", e)
            emptyList()
        }
    }

    private fun saveSessions(context: Context, settings: SharedPreferences, newList: List<UserSession>): Boolean {
        return try {
            RemoteAccessSecureStore.saveValidSessionsPayload(context, settings, adapter.toJson(newList))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seal sessions payload", e)
            false
        }
    }

    /**
     * injects the cookie in the [call] headers
     *
     * @param call the call to inject the cookie into
     * @param settings the settings used to store the valid sessions
     */
    fun injectCookie(call: ApplicationCall, settings: SharedPreferences) {
        injectCookie(call, settings, appContext())
    }

    @Synchronized
    fun injectCookie(call: ApplicationCall, settings: SharedPreferences, context: Context) {
        val id = SecretGenerator.generateRandomString()
        val value = UserSession(id = id, sessionExpiresAt(System.currentTimeMillis(), maxAgeInSeconds))
        val newList = getSessions(context, settings).toMutableList()
        newList.add(value)
        check(saveSessions(context, settings, newList.toList())) { "Unable to persist session" }
        call.sessions.set("user_session", value)
    }

    private fun appContext(): Context = AppContextProvider.appContext
}
