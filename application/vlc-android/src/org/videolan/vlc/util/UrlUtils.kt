/*
 * ************************************************************************
 *  UrlUtils.kt
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
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.net.toUri
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.videolan.vlc.R
import org.videolan.vlc.gui.dialogs.showSimpleBitmapComposeDialog


fun Context.openLinkIfPossible(url: String, size: Int = 512) {


    try {


            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        val match: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

        if (match.size == 1) {
            val resolveActivity = intent.resolveActivity(packageManager)
            if (resolveActivity == null || resolveActivity.packageName.startsWith("com.google.android.tv.frameworkpackagestubs")) throw IllegalStateException("No web browser found")
        }
        startActivity(intent)
    } catch (e: Exception) {
        showSimpleBitmapComposeDialog(
            title = getString(R.string.no_web_browser),
            message = getString(R.string.no_web_browser_message, url),
            bitmap = UrlUtils.generateQRCode(url, size),
            confirmText = getString(R.string.ok)
        )
    }
}

object UrlUtils {

    fun generateQRCode(url: String, size: Int = 512): Bitmap {
        val bits = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    it.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }
}
