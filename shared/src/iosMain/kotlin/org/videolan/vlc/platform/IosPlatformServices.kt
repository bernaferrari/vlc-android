package org.videolan.vlc.platform

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation of [PlatformServices].
 */
class IosPlatformServices(
    override val packageName: String = "org.videolan.vlc"
) : PlatformServices {

    override fun getString(key: String): String = key

    override fun getString(key: String, vararg args: Any): String =
        if (args.isEmpty()) key else key + args.joinToString(prefix = "(", postfix = ")")

    override val filesDir: String = pathFor(NSDocumentDirectory)

    override val cacheDir: String = pathFor(NSCachesDirectory)

    private fun pathFor(directory: ULong): String {
        val urls = NSFileManager.defaultManager.URLsForDirectory(directory, NSUserDomainMask)
        val url = urls.firstOrNull() as? NSURL
        return url?.path ?: ""
    }
}
