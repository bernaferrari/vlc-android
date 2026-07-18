package org.videolan.vlc.remoteaccessclient

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import org.videolan.vlc.remoteaccessserver.websockets.IncomingMessageType
import java.io.Closeable

/**
 * Kotlin HTTP client for a VLC remote-access server.
 *
 * This module also packages the web UI assets (`assets/dist`) that the server
 * copies and serves. The classes here are the **programmatic** client surface
 * for Android apps that want to drive a remote VLC instance without the browser.
 *
 * Protocol notes (see remote-access-server routing):
 * - OTP: `POST /code` → challenge string; user enters code shown on the host;
 *   `POST /verify-code` with form field `code` sets the session cookie.
 * - Authenticated GETs live under `authenticate("user_session")`.
 * - Playback commands: `GET /playback-event?message=…` using [IncomingMessageType].
 */
class RemoteAccessClient(
    private val config: RemoteAccessClientConfig,
    private val http: HttpClient = defaultHttpClient(config),
) : Closeable {

    /**
     * Request a new OTP challenge. The host device shows the matching code.
     * @return opaque challenge string (send back when invalidating via [requestOtpChallenge]).
     */
    suspend fun requestOtpChallenge(previousChallenge: String? = null): String {
        val response = http.submitForm(
            url = absolute("/code"),
            formParameters = io.ktor.http.parameters {
                if (!previousChallenge.isNullOrBlank()) {
                    append("challenge", previousChallenge)
                }
            }
        )
        if (!response.status.isSuccess()) {
            throw RemoteAccessClientException("OTP challenge failed: HTTP ${response.status.value}")
        }
        return response.bodyAsText()
    }

    /**
     * Verify the user-entered OTP code. On success the session cookie is stored
     * in this client's cookie jar for subsequent authenticated calls.
     */
    suspend fun verifyOtp(code: String): Boolean {
        val response = http.submitForm(
            url = absolute("/verify-code"),
            formParameters = io.ktor.http.parameters {
                append("code", code)
            }
        )
        // Server responds with redirect on success or login error page on failure.
        return response.status == HttpStatusCode.Found ||
            response.status == HttpStatusCode.OK ||
            response.status == HttpStatusCode.SeeOther
    }

    /** Raw JSON for the authenticated video library listing. */
    suspend fun getVideoListJson(
        grouping: Int = 0,
        groupId: Long = 0L,
        folderId: Long = 0L,
    ): String {
        val response = http.get(absolute("/video-list")) {
            parameter("grouping", grouping)
            if (groupId != 0L) parameter("group", groupId)
            if (folderId != 0L) parameter("folder", folderId)
        }
        ensureSuccess(response.status, "video-list")
        return response.bodyAsText()
    }

    /** Raw JSON browser listing for [path] (network/storage browser). */
    suspend fun getBrowseListJson(path: String? = null): String {
        val response = http.get(absolute("/browse-list")) {
            if (!path.isNullOrBlank()) parameter("path", path)
        }
        ensureSuccess(response.status, "browse-list")
        return response.bodyAsText()
    }

    /** Raw JSON search results. */
    suspend fun searchJson(query: String): String {
        val response = http.get(absolute("/search")) {
            parameter("query", query)
        }
        ensureSuccess(response.status, "search")
        return response.bodyAsText()
    }

    /**
     * Send a playback control event to the host.
     * @param type one of [IncomingMessageType] string values (e.g. play, pause, next)
     */
    suspend fun sendPlaybackEvent(
        type: IncomingMessageType,
        id: Int? = null,
        longValue: Long? = null,
        floatValue: Float? = null,
        stringValue: String? = null,
    ) {
        val response = http.get(absolute("/playback-event")) {
            parameter("message", type.toString())
            if (id != null) parameter("id", id)
            if (longValue != null) parameter("longValue", longValue)
            if (floatValue != null) parameter("floatValue", floatValue)
            if (stringValue != null) parameter("stringValue", stringValue)
        }
        if (response.status == HttpStatusCode.Forbidden) {
            throw RemoteAccessClientException("Playback event forbidden (auth or control disabled)")
        }
        ensureSuccess(response.status, "playback-event")
    }

    /** Convenience wrappers */
    suspend fun play() = sendPlaybackEvent(IncomingMessageType.PLAY)
    suspend fun pause() = sendPlaybackEvent(IncomingMessageType.PAUSE)
    suspend fun next() = sendPlaybackEvent(IncomingMessageType.NEXT)
    suspend fun previous() = sendPlaybackEvent(IncomingMessageType.PREVIOUS)

    /**
     * Long-poll playback status / queue messages (JSON array).
     * Blocks up to ~3s on the server when idle.
     */
    suspend fun longPollJson(): String {
        val response = http.get(absolute("/longpolling"))
        ensureSuccess(response.status, "longpolling")
        return response.bodyAsText()
    }

    /** Best-effort connectivity check against the unauthenticated index. */
    suspend fun ping(): Boolean {
        return try {
            val response = http.get(absolute("/index.html"))
            response.status.isSuccess() || response.status == HttpStatusCode.Found
        } catch (_: Exception) {
            false
        }
    }

    private fun absolute(path: String): String {
        val base = config.baseUrl.trimEnd('/')
        val suffix = if (path.startsWith("/")) path else "/$path"
        return base + suffix
    }

    private fun ensureSuccess(status: HttpStatusCode, op: String) {
        if (!status.isSuccess()) {
            throw RemoteAccessClientException("$op failed: HTTP ${status.value}")
        }
    }

    override fun close() {
        http.close()
    }

    companion object {
        fun defaultHttpClient(config: RemoteAccessClientConfig): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            if (config.userAgent != null) {
                defaultRequest {
                    headers.append("User-Agent", config.userAgent)
                }
            }
            engine {
                config {
                    followRedirects(true)
                    // Self-signed certs are common on LAN VLC remote access.
                    if (config.trustAllCertificates) {
                        val trustAll = object : javax.net.ssl.X509TrustManager {
                            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                        }
                        sslSocketFactory(
                            javax.net.ssl.SSLContext.getInstance("TLS").apply {
                                init(null, arrayOf<javax.net.ssl.TrustManager>(trustAll), java.security.SecureRandom())
                            }.socketFactory,
                            trustAll
                        )
                        hostnameVerifier { _, _ -> true }
                    }
                }
            }
        }
    }
}

data class RemoteAccessClientConfig(
    /** Base URL including scheme and port, e.g. `https://192.168.1.20:8443` */
    val baseUrl: String,
    val userAgent: String? = "VLC-Android-RemoteAccessClient",
    /**
     * When true, trusts self-signed TLS certs (typical for LAN remote access).
     * Only enable for user-confirmed local servers.
     */
    val trustAllCertificates: Boolean = true,
)

class RemoteAccessClientException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

