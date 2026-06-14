package org.videolan.vlc.compose.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import kotlin.math.min

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/dialog_widget_explanation.xml
 *
 * The app module supplies the real drawable resources. This content owns the
 * former step/group visibility, widget-size rotation, and resize hint animation.
 */
@Composable
fun VLCWidgetExplanationDialogContent(
    title: String,
    sizeText: String,
    resizeText: String,
    endText: String,
    nextText: String,
    closeText: String,
    sizePreviewCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    sizePreviewContent: @Composable (Int, Modifier) -> Unit,
    resizePreviewContent: @Composable (Modifier) -> Unit,
    tapIconContent: @Composable (Modifier) -> Unit,
    themeIconContent: @Composable (Modifier) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var sizePreviewIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(step, sizePreviewCount) {
        while (step == 1 && sizePreviewCount > 1) {
            delay(2000)
            sizePreviewIndex = (sizePreviewIndex + 1) % sizePreviewCount
        }
    }

    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundDefault)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = title,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                when (step) {
                    1 -> WidgetSizeStep(
                        text = sizeText,
                        previewIndex = sizePreviewIndex,
                        sizePreviewContent = sizePreviewContent
                    )
                    2 -> WidgetResizeStep(
                        text = resizeText,
                        resizePreviewContent = resizePreviewContent,
                        tapIconContent = tapIconContent
                    )
                    else -> WidgetFinalStep(
                        text = endText,
                        themeIconContent = themeIconContent
                    )
                }

                WidgetExplanationActions(
                    buttonText = if (step < 3) nextText else closeText,
                    onClick = {
                        if (step < 3) step++ else onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun WidgetSizeStep(
    text: String,
    previewIndex: Int,
    sizePreviewContent: @Composable (Int, Modifier) -> Unit
) {
    Text(
        text = text,
        color = VLCThemeDefaults.colors.fontDefault,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .padding(top = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        sizePreviewContent(
            previewIndex,
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Composable
private fun WidgetResizeStep(
    text: String,
    resizePreviewContent: @Composable (Modifier) -> Unit,
    tapIconContent: @Composable (Modifier) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "widgetResize")
    val widthFraction by transition.animateFloat(
        initialValue = 0.58f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 2500),
            repeatMode = RepeatMode.Restart
        ),
        label = "widgetWidth"
    )
    val tapAlpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, delayMillis = 2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tapAlpha"
    )
    val handleAlpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 200, delayMillis = 3300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handleAlpha"
    )

    Text(
        text = text,
        color = VLCThemeDefaults.colors.fontDefault,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            resizePreviewContent(Modifier.fillMaxWidth())
            WidgetResizeHandleOverlay(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(handleAlpha)
            )
            tapIconContent(
                Modifier
                    .size(52.dp)
                    .graphicsLayer(
                        alpha = tapAlpha,
                        scaleX = 0.9f + (tapAlpha * 0.1f),
                        scaleY = 0.9f + (tapAlpha * 0.1f)
                    )
            )
        }
    }
}

@Composable
private fun WidgetFinalStep(
    text: String,
    themeIconContent: @Composable (Modifier) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        themeIconContent(Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = VLCThemeDefaults.colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WidgetResizeHandleOverlay(modifier: Modifier = Modifier) {
    val color = VLCThemeDefaults.colors.primary
    Canvas(modifier = modifier) {
        val padding = 8.dp.toPx()
        val handleHeight = size.height - (padding * 2)
        val handleWidth = min(size.width - (padding * 2), (handleHeight * 3.56f) - 6.dp.toPx())
            .coerceAtLeast(0f)
        val left = (size.width - handleWidth) / 2
        val top = padding
        val stroke = 3.dp.toPx()
        val radius = 12.dp.toPx()
        val circleRadius = 6.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(handleWidth, handleHeight),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke)
        )
        drawCircle(color, circleRadius, Offset(left, top + (handleHeight / 2f)))
        drawCircle(color, circleRadius, Offset(left + (handleWidth / 2f), top))
        drawCircle(color, circleRadius, Offset(left + handleWidth, top + (handleHeight / 2f)))
        drawCircle(color, circleRadius, Offset(left + (handleWidth / 2f), top + handleHeight))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetExplanationActions(
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onClick) {
            Text(buttonText)
        }
    }
}

@Composable
private fun PreviewWidgetBlock(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.Gray)
    )
}

@Composable
private fun PreviewCircle(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.Gray)
    )
}
