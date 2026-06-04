/*
 * ************************************************************************
 *  SwipeToUnlockHost.kt
 * *************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.view

import android.animation.ValueAnimator
import android.util.LayoutDirection
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.resources.AndroidDevices
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import java.util.Locale
import kotlin.math.roundToInt

internal fun VLCComposeView.installSwipeToUnlockHost() {
    val host = SwipeToUnlockHost(this)
    setTag(R.id.swipe_to_unlock, host)
    isFocusable = true
    isFocusableInTouchMode = true
    setOnTouchListener { _, event -> host.onTouchEvent(event) }
    setOnKeyListener { _, keyCode, event -> host.onKeyEvent(keyCode, event) }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) = host.cancelAnimations()
    })
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

internal fun VLCComposeView.swipeToUnlockHost(): SwipeToUnlockHost =
    getTag(R.id.swipe_to_unlock) as? SwipeToUnlockHost ?: error("Missing swipe-to-unlock host")

internal fun VLCComposeView.showSwipeToUnlock() {
    swipeToUnlockHost().onShown()
    setVisible()
}

internal class SwipeToUnlockHost(private val view: VLCComposeView) {

    private val extremum: Int = (28 * view.resources.displayMetrics.density).toInt()

    private var unlocking: Boolean = false
    private var onStartTouching: () -> Unit = {}
    private var onStopTouching: () -> Unit = {}
    private var onUnlock: () -> Unit = {}
    private lateinit var keyAnimation: ValueAnimator
    private var currentText by mutableStateOf("")
    private var swipeCenterPx by mutableFloatStateOf(extremum.toFloat())

    var isDPADAllowed = true
    set(value) {
        field = value
        updateText()
    }

    private val tvAcceptedKeys = arrayOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)

    init {
        updateText()
    }

    fun setOnStartTouchingListener(listener: () -> Unit) {
        onStartTouching = listener
    }

    fun setOnStopTouchingListener(listener: () -> Unit) {
        onStopTouching = listener
    }

    fun setOnUnlockListener(listener: () -> Unit) {
        onUnlock = listener
    }

    fun onShown() {
        unlocking = false
        playStep(extremum)
        view.requestFocus()
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
        if (unlocking) return false
        event?.let {
            val maxX = (view.width - extremum).coerceAtLeast(extremum)
            val currentX = event.x.toInt().coerceAtLeast(extremum).coerceAtMost(maxX).run {
                if (view.layoutDirection == LayoutDirection.RTL) view.width - this
                else this
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onStartTouching.invoke()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {

                    if (currentX >= maxX) unlock()

                    playStep(currentX)

                    return true
                }
                MotionEvent.ACTION_UP -> {
                    animateBack(currentX)
                    onStopTouching.invoke()
                    return true
                }
                else -> return true
            }
        }
        return false
    }

    private fun animateBack(currentX: Int) {
        val animation = ValueAnimator.ofInt(currentX, extremum)
        animation.duration = 250 // milliseconds

        animation.addUpdateListener { animator -> playStep(animator.animatedValue as Int) }
        animation.start()
    }

    private fun playStep(currentX: Int) {
        swipeCenterPx = currentX.toFloat()
    }

    fun onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
        return when (event?.action) {
            KeyEvent.ACTION_DOWN -> onKeyDown(keyCode, event)
            KeyEvent.ACTION_UP -> onKeyUp(keyCode, event)
            else -> false
        }
    }

    private fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isDPADAllowed && keyCode in tvAcceptedKeys && event?.keyCode in tvAcceptedKeys && !unlocking) {
            onStartTouching.invoke()
            if (!::keyAnimation.isInitialized || !keyAnimation.isRunning) {
                keyAnimation = ValueAnimator.ofInt(extremum, view.width - extremum)
                keyAnimation.interpolator = AccelerateInterpolator()
                keyAnimation.duration = 2000
                keyAnimation.addUpdateListener { animator ->
                    run {
                        playStep(animator.animatedValue as Int)
                        if (animator.animatedValue == view.width - extremum) unlock()
                    }
                }
                keyAnimation.start()
            }
            return true
        }
        return false
    }

    private fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in tvAcceptedKeys && event?.keyCode in tvAcceptedKeys) {
            onStopTouching.invoke()

            if (::keyAnimation.isInitialized && keyAnimation.isRunning) {
                animateBack(keyAnimation.animatedValue as Int)
                keyAnimation.removeAllUpdateListeners()
                keyAnimation.cancel()
            }
            return true
        }
        return false
    }

    private fun unlock() {
        unlocking = true
        onUnlock.invoke()
        view.setGone()
        playStep(extremum)
    }

    private fun updateText() {
        currentText = if (!isDPADAllowed || !AndroidDevices.isTv) view.context.getString(R.string.swipe_unlock) else view.context.getString(R.string.swipe_unlock_no_touch)
    }

    fun cancelAnimations() {
        if (::keyAnimation.isInitialized) keyAnimation.cancel()
    }

    @Composable
    fun Content() {
        SwipeUnlockContent(
            text = currentText,
            swipeCenterPx = swipeCenterPx,
            extremumPx = extremum.toFloat()
        )
    }
}

@Composable
private fun SwipeUnlockContent(
    text: String,
    swipeCenterPx: Float,
    extremumPx: Float
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .width(252.dp)
                .height(56.dp)
                .background(colorResource(R.color.playerbackground), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val travelPx = (widthPx - extremumPx * 2F).coerceAtLeast(1F)
            val progress = ((swipeCenterPx - extremumPx) / travelPx).coerceIn(0F, 1F)
            val iconSizePx = with(density) { 48.dp.toPx() }

            Text(
                text = text.uppercase(Locale.getDefault()),
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 76.dp, end = 24.dp)
                    .alpha(1F - progress)
                    .align(Alignment.Center)
            )

            Icon(
                painter = painterResource(R.drawable.ic_swipe_unlock),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (swipeCenterPx - iconSizePx / 2F).roundToInt(),
                            y = 0
                        )
                    }
                    .size(48.dp)
            )
        }
    }
}
