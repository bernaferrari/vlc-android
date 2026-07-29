package org.videolan.vlc.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual fun supportsDynamicTheme(): Boolean = false

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
