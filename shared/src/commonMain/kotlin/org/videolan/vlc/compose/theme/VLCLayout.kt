package org.videolan.vlc.compose.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spatial language for the Compose app.
 *
 * Media is deliberately allowed to use a wider adaptive grid, but every list, form and sheet
 * starts from these values.  Keeping the values here prevents the subtle per-screen drift that
 * makes an app feel assembled rather than designed.
 */
object VLCLayout {
    val ScreenGutter = 16.dp
    val SectionGap = 24.dp
    val GroupGap = 2.dp
    val GroupOuterCorner = 24.dp
    val GroupInnerCorner = 6.dp
    val MediaCardCorner = 20.dp
    val ArtworkCorner = 14.dp
    val RowGap = 12.dp
    val RowHeight = 64.dp
    val MediaRowHeight = 72.dp
    val IconChip = 40.dp
    /** Minimum touch target for standalone icon actions. */
    val IconTouchTarget = 48.dp
    val DestinationIconChip = 48.dp
    val ListMaxWidth = 720.dp
    val FastScrollerClearance = 32.dp
    val SheetHorizontalPadding = 20.dp
    val SheetBottomPadding = 32.dp
}
