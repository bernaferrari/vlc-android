package org.videolan.vlc.gui.helpers

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun getBitmapFromDrawable(context: Context, @DrawableRes drawableId: Int): Bitmap {
    val drawable = AppCompatResources.getDrawable(context, drawableId)

    return if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else if (drawable is VectorDrawableCompat || drawable is VectorDrawable) {
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } else {
        throw IllegalArgumentException("unsupported drawable type")
    }
}
