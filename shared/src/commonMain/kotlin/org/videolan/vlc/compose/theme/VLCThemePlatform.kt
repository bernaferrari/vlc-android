package org.videolan.vlc.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Whether this target can supply the system wallpaper-derived Material colour scheme. */
expect fun supportsDynamicTheme(): Boolean

/** Returns Android's real dynamic scheme, or null on platforms where it does not exist. */
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?
