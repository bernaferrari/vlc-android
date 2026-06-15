package org.videolan.vlc.util

/**
 * Pure string manipulation utilities shared across all platforms.
 *
 * The Android-specific parts of TextUtils (formatChapterTitle, etc.) remain
 * in the vlc-android module. These functions have zero platform dependencies.
 */
object VlcTextUtils {

    /**
     * Common string separator used in the whole app.
     */
    const val SEPARATOR = '·'

    /**
     * En-dash separator used for ranges.
     */
    const val EN_DASH = '–'

    /**
     * Create a string separated by the common [SEPARATOR].
     * Accepts an array of optional strings; blank values are filtered out.
     */
    fun separatedString(pieces: Array<String?>) = separatedString(SEPARATOR, pieces)

    /**
     * Create a string separated by a custom [separator].
     * Accepts an array of optional strings; blank values are filtered out.
     */
    fun separatedString(separator: Char, pieces: Array<String?>) =
        pieces.filter { it?.isNotBlank() == true }.joinToString(separator = " $separator ")
}
