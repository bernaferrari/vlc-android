/*
 * ************************************************************************
 *  TextUtils.kt
 * *************************************************************************
 * Copyright © 2022 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.util

import android.content.Context
import org.videolan.vlc.R
import org.videolan.vlc.util.VlcTextUtils

object TextUtils {

    const val SEPARATOR = VlcTextUtils.SEPARATOR
    const val EN_DASH = VlcTextUtils.EN_DASH

    @JvmName("separatedStringArgs")
    fun separatedString(vararg pieces: String?) = VlcTextUtils.separatedString(arrayOf(*pieces))

    fun separatedString(pieces: Array<String?>) = VlcTextUtils.separatedString(pieces)

    @JvmName("separatedStringArgsSeparator")
    fun separatedString(separator: Char, vararg pieces: String?) = VlcTextUtils.separatedString(separator, arrayOf(*pieces))

    fun separatedString(separator: Char, pieces: Array<String?>) = VlcTextUtils.separatedString(separator, pieces)

    /**
     * Format the chapter title.
     * If title is null return "Chapter: <num>"
     * If title contains letters only prepend "Chapter: <title>"
     * If title contains any non alpha characters return as-is
     *
     * @param context the context to use to retrieve the string
     * @param chapterNum the current chapter number
     * @param title the title to format
     * @return a formatted string
     */
    fun formatChapterTitle(context: Context, chapterNum: Int, title: String?): String {
        return when {
            title.isNullOrBlank() -> context.getString(R.string.current_chapter, chapterNum.toString())
            title.all { it.isLetter() } -> context.getString(R.string.current_chapter, title)
            else -> title
        }
    }
}