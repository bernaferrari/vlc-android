package org.videolan.vlc.net

import android.net.Uri as AndroidUri

actual class VlcUri private constructor(val platformValue: AndroidUri) {
    actual fun asString(): String = platformValue.toString()

    actual companion object {
        actual fun parse(string: String): VlcUri = VlcUri(AndroidUri.parse(string))
    }

    override fun toString(): String = asString()
    override fun equals(other: Any?): Boolean =
        other is VlcUri && platformValue == other.platformValue
    override fun hashCode(): Int = platformValue.hashCode()
}
