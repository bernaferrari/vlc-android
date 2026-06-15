package org.videolan.vlc.net

/**
 * Platform-agnostic URI abstraction.
 *
 * Wraps each platform's native URI type:
 * - Android: `android.net.Uri`
 * - JVM: `java.net.URI`
 * - iOS: raw `String`
 *
 * Used by shared models that previously depended on `android.net.Uri`.
 */
expect class VlcUri {
    /** Returns the string representation of this URI. */
    fun asString(): String

    companion object {
        /** Parses a string into a [VlcUri]. */
        fun parse(string: String): VlcUri
    }
}

/** Extension to convert a string to a [VlcUri], mirroring `androidx.core.net.toUri`. */
fun String.toVlcUri(): VlcUri = VlcUri.parse(this)
