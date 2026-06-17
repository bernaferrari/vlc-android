package org.videolan.vlc.platform

import java.io.File

/**
 * JVM implementation of [PlatformServices].
 *
 * Provides basic file system access and no-op string resolution.
 * String resources should be loaded from classpath resource bundles in production.
 */
class JvmPlatformServices(
    override val packageName: String = "org.videolan.vlc"
) : PlatformServices {

    override fun getString(key: String): String = key

    override fun getString(key: String, vararg args: Any): String = key

    override val filesDir: String =
        System.getProperty("user.dir") + File.separator + "data"

    override val cacheDir: String =
        System.getProperty("java.io.tmpdir")
}
