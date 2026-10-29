package org.videolan.vlc.remoteaccessserver

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/** Explicit adapter keeps the stored schema stable without Kotlin reflection. */
internal class UserSessionJsonAdapter : JsonAdapter<UserSession>() {
    override fun fromJson(reader: JsonReader): UserSession {
        var id: String? = null
        var expiry: Long? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "maxAge" -> expiry = reader.nextLong()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return UserSession(
            id = id ?: throw JsonDataException("Missing session id"),
            maxAge = expiry ?: throw JsonDataException("Missing session expiry"),
        )
    }

    override fun toJson(writer: JsonWriter, value: UserSession?) {
        val session = requireNotNull(value)
        writer.beginObject()
        writer.name("id").value(session.id)
        writer.name("maxAge").value(session.maxAge)
        writer.endObject()
    }
}

internal fun userSessionListAdapter(): JsonAdapter<List<UserSession>> =
    Moshi.Builder().add(UserSession::class.java, UserSessionJsonAdapter()).build()
        .adapter(Types.newParameterizedType(List::class.java, UserSession::class.java))
