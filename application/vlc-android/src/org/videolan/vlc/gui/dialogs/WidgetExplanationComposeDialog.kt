package org.videolan.vlc.gui.dialogs

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import org.videolan.resources.AndroidDevices
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCWidgetExplanationDialogContent
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.util.isTalkbackIsEnabled

/**
 * Compose-hosted widget explanation bottom sheet.
 */
fun ComponentActivity.showWidgetExplanationComposeDialog() {
    WidgetExplanationComposeDialog(this).show()
}

private class WidgetExplanationComposeDialog(private val activity: ComponentActivity) {
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private val sizeDrawables = listOf(
        R.drawable.vlc_widget_mini,
        R.drawable.vlc_widget_micro,
        R.drawable.vlc_widget_pill,
        R.drawable.vlc_widget_macro
    )
    private var rootView: ComposeView? = null

    fun show() {
        setupContent()
        dialog.show()
        configureBottomSheet()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCWidgetExplanationDialogContent(
                    title = activity.getString(R.string.widget_explanation_title),
                    sizeText = activity.getString(R.string.widget_explanation_size),
                    resizeText = activity.getString(R.string.widget_explanation_resize),
                    endText = activity.getString(R.string.widget_explanation_end),
                    nextText = activity.getString(R.string.next),
                    closeText = activity.getString(R.string.close),
                    sizePreviewCount = sizeDrawables.size,
                    onClose = { dialog.dismiss() },
                    sizePreviewContent = { index, modifier ->
                        Image(
                            painter = painterResource(sizeDrawables[index.coerceIn(sizeDrawables.indices)]),
                            contentDescription = null,
                            modifier = modifier.fillMaxSize(),
                            contentScale = ContentScale.Inside
                        )
                    },
                    resizePreviewContent = { modifier ->
                        Image(
                            painter = painterResource(R.drawable.vlc_widget_mini),
                            contentDescription = null,
                            modifier = modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    },
                    tapIconContent = { modifier ->
                        Icon(
                            painter = painterResource(activity.resolveDrawableAttribute(R.attr.ic_tips_tap)),
                            contentDescription = null,
                            modifier = modifier,
                            tint = Color.Unspecified
                        )
                    },
                    themeIconContent = { modifier ->
                        Icon(
                            painter = painterResource(R.drawable.ic_theme),
                            contentDescription = null,
                            modifier = modifier,
                            tint = VLCThemeDefaults.colors.primary
                        )
                    }
                )
            }
        }
        dialog.setContentView(rootView!!)
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusable = false
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusableInTouchMode = false
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            if (activity.isTalkbackIsEnabled()) view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }
}

private fun Context.resolveDrawableAttribute(@AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.resourceId
}
