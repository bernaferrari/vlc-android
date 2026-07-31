/*
 * ************************************************************************
 *  NextApiService.kt
 * *************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
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

package org.videolan.moviepedia

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.util.Date

private const val USER_AGENT = "VLC-Android"

private fun buildClient(): IMoviepediaApiService = KtorMoviepediaApiService()

private class KtorMoviepediaApiService : IMoviepediaApiService {
    private val baseUrl = BuildConfig.MOVIEPEDIA_API_URL.trimEnd('/')
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
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header("Client", "vlc-android")
            header("Client-Version", BuildConfig.VLC_VERSION_CODE.toString())
            header("Client-Type", BuildConfig.BUILD_TYPE)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
    }

    private suspend fun <T> decode(response: HttpResponse, adapter: com.squareup.moshi.JsonAdapter<T>): T {
        check(response.status.isSuccess()) {
            "Moviepedia request failed: HTTP ${response.status.value}"
        }
        return adapter.fromJson(response.bodyAsText())
            ?: error("Moviepedia returned an empty response")
    }

    override suspend fun searchMedia(body: org.videolan.moviepedia.models.body.ScrobbleBody): org.videolan.moviepedia.models.identify.IdentifyResult =
        decode(
            client.post("$baseUrl/search-media/identify") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(moshi.adapter(org.videolan.moviepedia.models.body.ScrobbleBody::class.java).toJson(body))
            },
            moshi.adapter(org.videolan.moviepedia.models.identify.IdentifyResult::class.java),
        )

    override suspend fun searchMediaBatch(body: List<org.videolan.moviepedia.models.body.ScrobbleBodyBatch>): List<org.videolan.moviepedia.models.identify.IdentifyBatchResult> =
        decode(
            client.post("$baseUrl/search-media/batchidentify") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    moshi.adapter<List<org.videolan.moviepedia.models.body.ScrobbleBodyBatch>>(
                        Types.newParameterizedType(List::class.java, org.videolan.moviepedia.models.body.ScrobbleBodyBatch::class.java),
                    ).toJson(body),
                )
            },
            moshi.adapter<List<org.videolan.moviepedia.models.identify.IdentifyBatchResult>>(
                Types.newParameterizedType(List::class.java, org.videolan.moviepedia.models.identify.IdentifyBatchResult::class.java),
            ),
        )

    override suspend fun getMedia(mediaId: String): org.videolan.moviepedia.models.identify.MoviepediaMedia =
        decode(
            client.get("$baseUrl/media/${mediaId.encodePathSegment()}"),
            moshi.adapter(org.videolan.moviepedia.models.identify.MoviepediaMedia::class.java),
        )

    override suspend fun getMediaCast(mediaId: String): org.videolan.moviepedia.models.media.cast.CastResult =
        decode(
            client.get("$baseUrl/media/${mediaId.encodePathSegment()}/cast"),
            moshi.adapter(org.videolan.moviepedia.models.media.cast.CastResult::class.java),
        )
}

private fun String.encodePathSegment(): String = buildString {
    for (character in this@encodePathSegment) {
        if (character.isLetterOrDigit() || character in "-._~") append(character)
        else append('%').append(character.code.toString(16).uppercase().padStart(2, '0'))
    }
}

interface MoviepediaApiClient {

    companion object {
        val instance = buildClient()
    }
}
