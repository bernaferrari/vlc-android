package org.videolan.vlc.gui.dialogs

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.libvlc.RendererItem
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.RendererDelegate
import org.videolan.vlc.compose.components.VLCRendererPickerDialogContent
import org.videolan.vlc.compose.components.VLCRendererUiItem
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded

private var isRenderersComposeDialogShowing = false

fun ComponentActivity.showRenderersComposeDialog() {
    if (isRenderersComposeDialogShowing) return
    isRenderersComposeDialogShowing = true
    lifecycleScope.launch {
        if (showPinIfNeeded()) {
            isRenderersComposeDialogShowing = false
            return@launch
        }
        RenderersComposeDialog(
            activity = this@showRenderersComposeDialog,
            onDismissed = { isRenderersComposeDialogShowing = false }
        ).show()
    }
}

private class RenderersComposeDialog(
    private val activity: ComponentActivity,
    private val onDismissed: () -> Unit
) {
    private val dialog = Dialog(activity)
    private val renderers = mutableStateOf(RendererDelegate.renderers.value.toList())
    private val selectedRenderer = mutableStateOf(PlaybackService.renderer.value)
    private var rootView: ComposeView? = null
    private val renderersObserver = Observer<MutableList<RendererItem>?> { items ->
        renderers.value = items?.toList().orEmpty()
    }
    private val selectedRendererObserver = Observer<RendererItem?> { item ->
        selectedRenderer.value = item
    }

    fun show() {
        setupContent()
        RendererDelegate.renderers.observe(activity, renderersObserver)
        PlaybackService.renderer.observe(activity, selectedRendererObserver)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupContent() {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val currentRenderers = renderers.value
                val selected = selectedRenderer.value
                val uiItems = currentRenderers.mapIndexed { index, renderer ->
                    VLCRendererUiItem(
                        id = index.toString(),
                        displayName = renderer.displayName,
                        isSelected = renderer == selected,
                        isChromecast = renderer.type == "chromecast"
                    )
                }
                VLCRendererPickerDialogContent(
                    title = activity.getString(R.string.renderer_list_title),
                    renderers = uiItems,
                    disconnectText = activity.getString(R.string.renderers_disconnect),
                    showDisconnect = selected != null,
                    onRendererSelected = { item ->
                        currentRenderers.getOrNull(item.id.toInt())?.let(::connect)
                    },
                    onDisconnect = { connect(null) },
                    rendererIcon = { item, tint ->
                        Icon(
                            painter = painterResource(
                                if (item.isChromecast) R.drawable.ic_dialog_renderer
                                else R.drawable.ic_dialog_unknown
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = tint ?: Color.Unspecified
                        )
                    }
                )
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            RendererDelegate.renderers.removeObserver(renderersObserver)
            PlaybackService.renderer.removeObserver(selectedRendererObserver)
            rootView = null
            onDismissed()
        }
    }

    private fun connect(item: RendererItem?) {
        PlaybackService.renderer.value = item
        dialog.dismiss()
        item?.run {
            activity.window.findViewById<View>(R.id.audio_player_container)?.let {
                UiTools.snacker(activity, activity.getString(R.string.casting_connected_renderer, displayName))
            }
        }
    }
}
