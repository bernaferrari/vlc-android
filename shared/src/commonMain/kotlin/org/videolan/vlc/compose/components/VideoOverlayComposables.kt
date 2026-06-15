package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Tiny label shown in the video HUD during seek jumps (e.g. "+10s", "-30s").
 *
 * Extracted from VideoHudSeekJumpLabelHost so the rendering lives in shared code.
 * The Android host only manages state and passes [text] + [color].
 */
@Composable
fun VLCSeekJumpLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = VLCThemeDefaults.colors.playerIconColor
) {
    Text(
        text = text,
        color = color,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/**
 * Transient overlay showing codec/scale/info text in the video player.
 *
 * Extracted from VideoInfoOverlayHost so the rendering lives in shared code.
 * The Android host passes [bgColor] and [textColor] from platform resources.
 */
@Composable
fun VLCVideoInfoOverlay(
    text: String,
    modifier: Modifier = Modifier,
    subText: String = "",
    bgColor: Color = Color(0xFF000000),
    textColor: Color = Color.White
) {
    Column(
        modifier = modifier
            .background(
                color = bgColor,
                shape = RoundedCornerShape(8.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 36.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (subText.isNotBlank()) {
            Text(
                text = subText,
                color = textColor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}
