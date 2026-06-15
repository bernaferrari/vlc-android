package org.videolan.vlc.net

actual class VlcUri private constructor(val platformValue: String) {
    actual fun asString(): String = platformValue

    actual companion object {
        actual fun parse(string: String): VlcUri = VlcUri(string)
    }

    override fun toString(): String = platformValue
    override fun equals(other: Any?): Boolean =
        other is VlcUri && platformValue == other.platformValue
    override fun hashCode(): Int = platformValue.hashCode()
}
