package org.videolan.vlc.compose.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * True when a native host (iOS UINavigationController/UITabBarController) draws the navigation
 * bar, back button and tab bar. Screens then omit their own back arrow and duplicate titles.
 */
val LocalUseNativeNavigation = staticCompositionLocalOf { false }

/**
 * Space a native host's floating chrome (Liquid Glass tab bar, bottom accessory) occupies over
 * the bottom of a screen. Lists add it to their content padding so items scroll beneath the glass
 * instead of stopping short of it.
 */
val LocalShellBottomInset = staticCompositionLocalOf { 0.dp }

/**
 * Height of the native navigation bar a page is drawn beneath (0 when the platform shows none).
 * Under native navigation a detail page draws edge-to-edge and its own actions share this band
 * with the platform's back button, instead of adding a second toolbar row.
 */
val LocalNativeBarHeight = staticCompositionLocalOf { 0.dp }
