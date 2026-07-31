package org.videolan.resources.opensubtitles

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import org.videolan.resources.AppContextProvider
import org.videolan.resources.BuildConfig
import org.videolan.resources.util.NoConnectivityException
import org.videolan.tools.forbiddenChars
import org.videolan.tools.isConnected
import org.videolan.tools.substrlng
import java.io.IOException
import java.util.Date

private const val BASE_URL = "https://api.opensubtitles.com/api/v1/"
const val USER_AGENT = "VLSub v0.9"

private class KtorOpenSubtitleService : IOpenSubtitleService {
    private val moshi = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .build()
    private val client = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
    }

    private fun endpoint(path: String): String {
        val root = OpenSubtitleClient.userDomain
            ?.trimEnd('/')
            ?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
            ?: BASE_URL.trimEnd('/')
        return "$root/${path.trimStart('/')}"
    }

    private fun ensureConnected() {
        if (!AppContextProvider.appContext.isConnected()) throw NoConnectivityException()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.commonHeaders() {
        header(HttpHeaders.UserAgent, USER_AGENT)
        header("Api-Key", getOSK().substrlng(55))
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        OpenSubtitleClient.authorizationToken
            .takeIf(String::isNotEmpty)
            ?.let { header(HttpHeaders.Authorization, it) }
    }

    private suspend fun <T> decode(response: HttpResponse, adapter: JsonAdapter<T>): T {
        if (!response.status.isSuccess()) {
            throw IOException("OpenSubtitles request failed: HTTP ${response.status.value}")
        }
        return adapter.fromJson(response.bodyAsText())
            ?: error("OpenSubtitles returned an empty response")
    }

    private suspend fun <T> response(response: HttpResponse, adapter: JsonAdapter<T>): OpenSubtitleResponse<T> {
        val body = if (response.status.isSuccess()) adapter.fromJson(response.bodyAsText()) else null
        return OpenSubtitleResponse(response.status.value, body)
    }

    override suspend fun query(
        episode: Int?,
        hearingImpaired: String,
        imdbId: String?,
        languageId: String,
        movieHash: String?,
        name: String?,
        season: Int?,
        orderBy: String,
    ): OpenSubV1 {
        ensureConnected()
        return decode(
            client.get(endpoint("subtitles")) {
                commonHeaders()
                episode?.let { parameter("episode_number", it) }
                parameter("hearing_impaired", hearingImpaired)
                imdbId?.let { parameter("imdb_id", it) }
                parameter("languages", languageId)
                movieHash?.let { parameter("moviehash", it) }
                name?.let { parameter("query", it) }
                season?.let { parameter("season_number", it) }
                parameter("order_by", orderBy)
            },
            moshi.adapter(OpenSubV1::class.java),
        )
    }

    override suspend fun queryDownloadUrl(downloadLinkBody: DownloadLinkBody): DownloadLink {
        ensureConnected()
        return decode(
            client.post(endpoint("download")) {
                commonHeaders()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(moshi.adapter(DownloadLinkBody::class.java).toJson(downloadLinkBody))
            },
            moshi.adapter(DownloadLink::class.java),
        )
    }

    override suspend fun login(loginBody: LoginBody): OpenSubtitleResponse<OpenSubtitleAccount> {
        ensureConnected()
        return response(
            client.post(endpoint("login")) {
                commonHeaders()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(moshi.adapter(LoginBody::class.java).toJson(loginBody))
            },
            moshi.adapter(OpenSubtitleAccount::class.java),
        )
    }

    override suspend fun userInfo(): OpenSubtitleResponse<UserInfo> {
        ensureConnected()
        return response(
            client.get(endpoint("infos/user")) { commonHeaders() },
            moshi.adapter(UserInfo::class.java),
        )
    }
}

fun getOSK() = "${BuildConfig.VLC_OPEN_SUBTITLES_API_KEY}${(-47).forbiddenChars()}Y"

interface OpenSubtitleClient {
    companion object {
        val instance: IOpenSubtitleService by lazy { KtorOpenSubtitleService() }
        var authorizationToken: String = ""
        var userDomain: String? = null

        init {
            OpenSubtitleRepository.instance = lazy { OpenSubtitleRepository(instance) }
        }
    }
}
