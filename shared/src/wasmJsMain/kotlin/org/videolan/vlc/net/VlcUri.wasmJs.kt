package org.videolan.vlc.net

actual class VlcUri private constructor(private val value: String) {
    actual fun asString(): String = value

    actual companion object {
        actual fun parse(string: String): VlcUri = VlcUri(string)
    }
}
