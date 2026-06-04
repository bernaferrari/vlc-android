package org.videolan.vlc.gui.view

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioTimelineSlider
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.util.isTalkbackIsEnabled

interface PlayerTimelineSeekBarHost {
    interface Listener {
        fun onStartTrackingTouch()
        fun onStopTrackingTouch(progress: Int)
        fun onProgressChanged(progress: Int, fromUser: Boolean)
    }

    var max: Int
    var progress: Int
    fun setOnTimelineSeekChangeListener(listener: Listener?)
    fun forceAccessibilityUpdate()
    fun disableAccessibilityEvents()
    fun enableAccessibilityEvents()
}

/**
 * Direct Compose-backed video timeline. The HUD keeps a plain VLCComposeView
 * in the layout while this host exposes the former max/progress and seek
 * callback bridge used by the player controller.
 */
fun VLCComposeView.installVideoTimelineSeekBarHost() {
    val host = PlayerTimelineSeekBarController(this)
    setTag(R.id.player_overlay_seekbar, host)
    isFocusable = true
    isFocusableInTouchMode = true
    setContent {
        VLCTheme {
            host.Content()
        }
    }
}

fun VLCComposeView.playerTimelineSeekBarHost(): PlayerTimelineSeekBarHost =
    getTag(R.id.player_overlay_seekbar) as? PlayerTimelineSeekBarHost ?: error("Missing player timeline seekbar host")

private class PlayerTimelineSeekBarController(private val view: VLCComposeView) : PlayerTimelineSeekBarHost {

    private var maxState by mutableIntStateOf(100)
    private var progressState by mutableIntStateOf(0)
    private var listener: PlayerTimelineSeekBarHost.Listener? = null
    private var accessibilityEventsDisabled = true

    override var max: Int
        get() = maxState
        set(value) {
            maxState = value.coerceAtLeast(0)
            progress = progress.coerceIn(0, maxState.coerceAtLeast(1))
        }

    override var progress: Int
        get() = progressState
        set(value) {
            progressState = value.coerceIn(0, max.coerceAtLeast(1))
        }

    override fun setOnTimelineSeekChangeListener(listener: PlayerTimelineSeekBarHost.Listener?) {
        this.listener = listener
    }

    @Suppress("DEPRECATION")
    override fun forceAccessibilityUpdate() {
        if ((view.context as? Activity)?.isTalkbackIsEnabled() == true) {
            view.announceForAccessibility(accessibilityText())
        }
    }

    override fun disableAccessibilityEvents() {
        accessibilityEventsDisabled = true
    }

    override fun enableAccessibilityEvents() {
        accessibilityEventsDisabled = false
    }

    @Composable
    fun Content() {
        VLCAudioTimelineSlider(
            progress = progress,
            max = max,
            contentDescription = accessibilityText(),
            onUserDragStarted = { listener?.onStartTrackingTouch() },
            onUserProgressChange = { value ->
                progress = value
                listener?.onProgressChanged(value, true)
            },
            onUserDragStopped = { listener?.onStopTrackingTouch(progress) }
        )
    }

    private fun accessibilityText(): String {
        return view.context.getString(
            R.string.talkback_out_of,
            TalkbackUtil.millisToString(view.context, progress.toLong()),
            TalkbackUtil.millisToString(view.context, max.toLong())
        )
    }
}
