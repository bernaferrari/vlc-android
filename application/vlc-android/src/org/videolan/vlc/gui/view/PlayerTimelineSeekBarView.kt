package org.videolan.vlc.gui.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAudioTimelineSlider
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget
import org.videolan.vlc.gui.helpers.TalkbackUtil
import org.videolan.vlc.util.isTalkbackIsEnabled

/**
 * Shared view-friendly Compose replacement for player timeline seekbars. It
 * exposes the small max/progress and drag callback surface that legacy player
 * controllers already use while removing native SeekBar widgets from layouts.
 */
open class PlayerTimelineSeekBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    interface Listener {
        fun onStartTrackingTouch()
        fun onStopTrackingTouch(progress: Int)
        fun onProgressChanged(progress: Int, fromUser: Boolean)
    }

    private var maxState by mutableIntStateOf(100)
    private var progressState by mutableIntStateOf(0)

    var max: Int
        get() = maxState
        set(value) {
            maxState = value.coerceAtLeast(0)
            progress = progress.coerceIn(0, maxState.coerceAtLeast(1))
        }

    var progress: Int
        get() = progressState
        set(value) {
            progressState = value.coerceIn(0, max.coerceAtLeast(1))
        }

    private var listener: Listener? = null
    private var accessibilityEventsDisabled = true

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setOnTimelineSeekChangeListener(listener: Listener?) {
        this.listener = listener
    }

    fun forceAccessibilityUpdate() {
        if ((context as? Activity)?.isTalkbackIsEnabled() == true) {
            announceForAccessibility(accessibilityText())
        }
    }

    fun disableAccessibilityEvents() {
        accessibilityEventsDisabled = true
    }

    fun enableAccessibilityEvents() {
        accessibilityEventsDisabled = false
    }

    @Composable
    override fun WidgetContent() {
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
        return context.getString(
            R.string.talkback_out_of,
            TalkbackUtil.millisToString(context, progress.toLong()),
            TalkbackUtil.millisToString(context, max.toLong())
        )
    }
}

/**
 * View-friendly Compose replacement for the video HUD's former native timeline.
 */
class VideoTimelineSeekBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerTimelineSeekBarView(context, attrs, defStyleAttr)
