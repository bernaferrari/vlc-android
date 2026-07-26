package org.videolan.vlc.compose.icons

import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Local Material Symbol plus the metadata necessary to mirror directional icons in RTL. */
@Immutable
data class MaterialIcon(
    val imageVector: ImageVector,
    val autoMirror: Boolean = false,
)

@Composable
fun Icon(
    icon: MaterialIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val mirroredModifier =
        if (icon.autoMirror && LocalLayoutDirection.current == LayoutDirection.Rtl) {
            modifier.graphicsLayer { scaleX = -1f }
        } else {
            modifier
        }
    Material3Icon(
        imageVector = icon.imageVector,
        contentDescription = contentDescription,
        modifier = mirroredModifier,
        tint = tint,
    )
}
