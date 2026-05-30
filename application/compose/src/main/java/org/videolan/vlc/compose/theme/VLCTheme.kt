package org.videolan.vlc.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Placeholder tokens — will be expanded with real mapping from res/values/colors.xml + styles.xml in Phase 0
private val VLCPrimary = Color(0xFF1A73E8)
private val VLCPrimaryDark = Color(0xFF0D47A1)
private val VLCSurface = Color(0xFFFFFFFF)
private val VLCSurfaceDark = Color(0xFF121212)
private val VLCTextPrimary = Color(0xDE000000)
private val VLCTextPrimaryDark = Color(0xDEFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = VLCPrimary,
    onPrimary = Color.White,
    surface = VLCSurface,
    onSurface = VLCTextPrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = VLCPrimaryDark,
    onPrimary = Color.White,
    surface = VLCSurfaceDark,
    onSurface = VLCTextPrimaryDark,
)

val LocalVLCTheme = staticCompositionLocalOf { VLCThemeDefaults }

object VLCThemeDefaults {
    val primary = VLCPrimary
    // Add more semantic tokens here (error, background, etc.) as we audit styles.xml
}

@Composable
fun VLCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalVLCTheme provides VLCThemeDefaults) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
