package org.videolan.vlc.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

/**
 * Compose screen replacement for:
 * - application/vlc-android/res/layout/otp_code_activity.xml
 * - application/vlc-android/res/layout/otp_code.xml
 *
 * This is intentionally a full-screen Compose surface rather than an interop leaf:
 * it lets OTPCodeActivity use a direct Compose host as part of the full Compose
 * migration tracked in bd compose-bwf.
 */
@Composable
fun VLCOTPCodeScreen(
    title: String,
    subtitle: String,
    cancelText: String,
    code: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.backgroundDefault
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = colors.fontDefault,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = subtitle,
                        color = colors.fontLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OTPDigits(
                    code = code.orEmpty(),
                    digitColor = colors.primary,
                    tileColor = colors.backgroundDefaultDarker,
                    modifier = Modifier.align(Alignment.Center)
                )

                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.primary
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(text = cancelText)
                }
            }
        }
    }
}

@Composable
private fun OTPDigits(
    code: String,
    digitColor: Color,
    tileColor: Color,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = 4.dp
        val tileSize = otpTileSize(maxWidth, spacing)
        val digitTextSize = if (tileSize < 52.dp) 30.sp else 34.sp
        val digits = code.padEnd(6).take(6)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            digits.forEach { digit ->
                Box(
                    modifier = Modifier
                        .size(tileSize)
                        .background(tileColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit.takeUnless { it.isWhitespace() }?.toString().orEmpty(),
                        color = digitColor,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = digitTextSize,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

private fun otpTileSize(maxWidth: Dp, spacing: Dp): Dp =
    ((maxWidth - spacing * 5f) / 6f)
        .coerceAtMost(56.dp)
        .coerceAtLeast(44.dp)
