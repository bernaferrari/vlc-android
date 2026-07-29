package org.videolan.vlc.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
 * A low-frequency, in-place disclosure transition. It only changes height: screen changes and
 * frequently-used list navigation stay immediate, and reduced-motion users get no movement.
 */
@Composable
fun VLCExpandableContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motion = LocalVLCMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (motion.reducedMotion) {
            androidx.compose.animation.EnterTransition.None
        } else {
            expandVertically(animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f))
        },
        exit = if (motion.reducedMotion) {
            androidx.compose.animation.ExitTransition.None
        } else {
            shrinkVertically(animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f))
        },
    ) {
        content()
    }
}
