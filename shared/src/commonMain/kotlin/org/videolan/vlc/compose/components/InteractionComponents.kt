package org.videolan.vlc.compose.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import org.videolan.vlc.compose.theme.LocalVLCMotion

/**
 * Gives a whole row or card a quiet, consistent acknowledgement of touch without introducing
 * decorative navigation motion. The interaction source is intentionally shared by the clickable
 * and transform so it remains interruptible while a user changes their mind mid-press.
 */
@Composable
fun VLCPressableContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = LocalVLCMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.975f else 1f,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 0.8f, stiffness = 900f),
        label = "vlcPressScale",
    )

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = onClick,
            ),
        content = content,
    )
}

/**
 * A simple in-place disclosure for utility forms. Forms are often opened and closed repeatedly;
 * changing layout height there made the surrounding grouped rows jump and compete with the
 * deliberate Nav3 detail transition. Keeping the state change immediate is calmer and avoids
 * layout-property animation on a scrolling screen.
 */
@Composable
fun VLCExpandableContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (visible) {
        Box(modifier = modifier) { content() }
    }
}
