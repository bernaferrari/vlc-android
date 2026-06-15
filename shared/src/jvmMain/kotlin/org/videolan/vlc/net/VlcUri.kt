package org.videolan.vlc.net

import java.net.URI

actual class VlcUri private constructor(val platformValue: URI) {
    actual fun asString(): String = platformValue.toString()

    actual companion object {
        actual fun parse(string: String): VlcUri = VlcUri(URI(string))
    }

    override fun toString(): String = asString()
    override fun equals(other: Any?): Boolean =
        other is VlcUri && platformValue == other.platformValue
    override fun hashCode(): Int = platformValue.hashCode()
}
