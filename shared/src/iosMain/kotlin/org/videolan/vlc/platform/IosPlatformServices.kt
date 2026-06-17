package org.videolan.vlc.platform

/**
 * iOS implementation of [PlatformServices].
 *
 * Provides basic platform services. String resources should be loaded
 * from the Compose MP resource system or an iOS bundle.
 */
class IosPlatformServices(
    override val packageName: String = "org.videolan.vlc"
) : PlatformServices {

    override fun getString(key: String): String = key

    override fun getString(key: String, vararg args: Any): String = key

    override val filesDir: String =
        NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() ?: ""

    override val cacheDir: String =
        NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull() ?: ""
}

// iOS Foundation imports via Kotlin/Native interop
private val NSDocumentDirectory: ULong = 9u
private val NSCachesDirectory: ULong = 13u
private val NSUserDomainMask: ULong = 1u

private fun NSSearchPathForDirectoriesInDomains(
    directory: ULong,
    domainMask: ULong,
    expandTilde: Boolean
): List<String> = emptyList()
