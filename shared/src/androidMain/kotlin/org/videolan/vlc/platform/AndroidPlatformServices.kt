package org.videolan.vlc.platform

import android.content.Context

/**
 * Android implementation of [PlatformServices].
 *
 * Bridges shared code to Android Context-based services.
 */
class AndroidPlatformServices(private val context: Context) : PlatformServices {

    override fun getString(key: String): String {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        return if (resId != 0) context.getString(resId) else key
    }

    override fun getString(key: String, vararg args: Any): String {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        return if (resId != 0) context.getString(resId, *args) else key
    }

    override val filesDir: String
        get() = context.filesDir.absolutePath

    override val cacheDir: String
        get() = context.cacheDir.absolutePath

    override val packageName: String
        get() = context.packageName
}
