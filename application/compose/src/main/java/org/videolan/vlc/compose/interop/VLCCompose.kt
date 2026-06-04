package org.videolan.vlc.compose.interop

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Drop-in replacement helper for hosting Composables inside legacy XML layouts.
 * Usage in XML:
 *   <org.videolan.vlc.compose.interop.VLCComposeView
 *       android:id="@+id/my_compose_host"
 *       ... />
 *
 * In Kotlin (even inside DataBinding fragments/activities):
 *   binding.myComposeHost.setContent {
 *       VLCTheme { MyComposable(...) }
 *   }
 */
class VLCComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

    fun setContent(content: @Composable () -> Unit) {
        this.content.value = content
    }

    @Composable
    override fun Content() {
        content.value?.invoke()
    }
}
