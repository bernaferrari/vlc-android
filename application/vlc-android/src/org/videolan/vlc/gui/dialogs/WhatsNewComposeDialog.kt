package org.videolan.vlc.gui.dialogs

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.resources.AndroidDevices
import org.videolan.tools.KEY_SHOW_WHATS_NEW
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCWhatsNewDialogContent
import org.videolan.vlc.compose.components.VLCWhatsNewItem
import org.videolan.vlc.gui.EqualizerSettingsActivity
import org.videolan.vlc.gui.preferences.PreferencesActivity
import org.videolan.vlc.util.isTalkbackIsEnabled

private const val WHATS_NEW_EQUALIZER = "equalizer"
private const val WHATS_NEW_IMPORT_EXPORT = "import_export"
private const val WHATS_NEW_ANDROID_AUTO = "android_auto"
private const val WHATS_NEW_VERSION = "3.7"

/** Compose-hosted What's New dialog. */
fun AppCompatActivity.showWhatsNewComposeDialog() {
    WhatsNewComposeDialog(this).show()
}

private class WhatsNewComposeDialog(private val activity: AppCompatActivity) {
    private val settings = Settings.getInstance(activity)
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private var rootView: ComposeView? = null
    private var neverShowAgain by mutableStateOf(!settings.getBoolean(KEY_SHOW_WHATS_NEW, true))

    fun show() {
        setupContent()
        dialog.show()
        configureBottomSheet()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCWhatsNewDialogContent(
                    title = activity.getString(R.string.whats_new_title, WHATS_NEW_VERSION),
                    items = buildItems(),
                    neverShowAgainText = activity.getString(R.string.never_show_again),
                    neverShowAgain = neverShowAgain,
                    onNeverShowAgainChange = ::updateNeverShowAgain,
                    onItemAction = ::handleAction,
                    titleIconContent = { WhatsNewIcon(R.drawable.ic_whats_new) },
                    itemIconContent = { item ->
                        WhatsNewIcon(
                            when (item.id) {
                                WHATS_NEW_EQUALIZER -> R.drawable.ic_equalizer
                                WHATS_NEW_IMPORT_EXPORT -> R.drawable.ic_whats_new_import_export
                                WHATS_NEW_ANDROID_AUTO -> R.drawable.ic_pref_android_auto
                                else -> R.drawable.ic_whats_new
                            }
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

    private fun buildItems() = listOf(
        VLCWhatsNewItem(
            id = WHATS_NEW_EQUALIZER,
            title = activity.getString(R.string.equalizer),
            body = activity.getString(R.string.whats_new_37_equalizer_text),
            actionText = activity.getString(R.string.show_in_settings)
        ),
        VLCWhatsNewItem(
            id = WHATS_NEW_IMPORT_EXPORT,
            title = activity.getString(R.string.export_settings),
            body = activity.getString(R.string.whats_new_37_import_export),
            actionText = activity.getString(R.string.show_in_settings)
        ),
        VLCWhatsNewItem(
            id = WHATS_NEW_ANDROID_AUTO,
            title = activity.getString(R.string.android_auto),
            body = activity.getString(R.string.whats_new_37_android_auto),
            actionText = activity.getString(R.string.show_in_settings)
        )
    )

    private fun updateNeverShowAgain(checked: Boolean) {
        neverShowAgain = checked
        settings.putSingle(KEY_SHOW_WHATS_NEW, !checked)
    }

    private fun handleAction(id: String) {
        when (id) {
            WHATS_NEW_EQUALIZER -> activity.startActivity(Intent(activity, EqualizerSettingsActivity::class.java))
            WHATS_NEW_IMPORT_EXPORT -> activity.lifecycleScope.launch {
                PreferencesActivity.launchWithPref(activity, "export_settings")
            }
            WHATS_NEW_ANDROID_AUTO -> activity.lifecycleScope.launch {
                PreferencesActivity.launchWithPref(activity, "playback_speed_audio_global")
            }
        }
        dialog.dismiss()
    }
}

@Composable
private fun WhatsNewIcon(drawable: Int) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}
