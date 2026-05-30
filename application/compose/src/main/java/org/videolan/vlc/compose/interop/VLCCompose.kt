package org.videolan.vlc.compose.interop

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import org.videolan.vlc.compose.theme.VLCTheme

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
) : ComposeView(context, attrs, defStyleAttr) {

    init {
        // Ensure we always wrap content in our theme
        // The actual setContent call from host code will provide the content
    }
}

/**
 * Example of an AbstractComposeView widget that can be placed directly in legacy layouts
 * (including databound ones). Subclass and override Content().
 */
abstract class VLCAbstractComposeWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        VLCTheme {
            WidgetContent()
        }
    }

    @Composable
    protected abstract fun WidgetContent()
}
