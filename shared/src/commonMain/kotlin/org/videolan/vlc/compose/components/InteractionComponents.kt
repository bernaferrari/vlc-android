package org.videolan.vlc.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion

/**
 * A restrained in-place disclosure for utility forms. The height follows the content with a
 * short accordion motion, while reduced-motion users get the same state change with no delay.
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
        enter = expandVertically(
            animationSpec = tween(motion.durationShort, easing = VLCMotion.Emphasized),
            expandFrom = Alignment.Top,
        ),
        exit = shrinkVertically(
            animationSpec = tween(motion.durationShort, easing = VLCMotion.Emphasized),
            shrinkTowards = Alignment.Top,
        ),
    ) {
        Box { content() }
    }
}
