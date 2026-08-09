package org.videolan.vlc.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Official VLC cone silhouette, kept as a Compose vector so every KMP target renders it. */
val VlcCone: ImageVector
    get() {
        cachedVlcCone?.let { return it }
        return ImageVector.Builder(
            name = "VLC cone",
            defaultWidth = 108.dp,
            defaultHeight = 108.dp,
            viewportWidth = 108f,
            viewportHeight = 108f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(OrangeConePath).toNodes(),
                pathFillType = PathFillType.NonZero,
                fill = SolidColor(Color(0xFFFF8800)),
            )
            addPath(
                pathData = PathParser().parsePathString(WhiteConePath).toNodes(),
                pathFillType = PathFillType.NonZero,
                fill = SolidColor(Color(0xFFFAFAFA)),
            )
        }.build().also { cachedVlcCone = it }
    }

private var cachedVlcCone: ImageVector? = null

private const val OrangeConePath =
    "M63.223 18.858c-6.938 3.66-11.714 3.347-18.408 0l-5.857 17.571c5.02 3.765 9.699 5.648 15.061 5.648s10.041-1.883 15.061-5.648zM28.918 66.551l-1.674 5.02c-.837 2.511-1.262 3.786 0 5.021 6.74 6.595 17.337 9.201 26.775 9.203 9.431.003 20.081-2.51 26.763-9.182 1.209-1.392.821-2.613.012-5.042l-1.673-5.02-3.347-10.041c-6.694 6.694-13.985 8.368-21.755 8.368s-15.061-1.674-21.754-8.368z"

private const val WhiteConePath =
    "M54.002 5.47c-2.092 0-3.465.931-4.603 2.094-1.573 1.606-2.928 6.276-2.928 6.276l-1.674 5.018c6.694 3.347 11.471 3.66 18.409 0l-1.674-5.018s-1.355-4.67-2.928-6.276c-1.138-1.163-2.51-2.094-4.602-2.094zM38.94 36.428l-6.691 20.083c6.693 6.692 13.984 8.366 21.753 8.366s15.059-1.672 21.753-8.366l-6.692-20.083c-5.02 3.765-9.7 5.649-15.061 5.649-5.362 0-10.041-1.884-15.062-5.649zM21.786 66.55c-2.225 0-4.331 1.947-5.018 4.183L9.445 95.834c-.978 3.354-.627 6.696 3.975 6.696h81.163c4.602 0 4.949-3.342 3.971-6.696l-7.319-25.101c-.687-2.236-2.797-4.183-5.021-4.183h-7.112l1.674 5.022c.808 2.429 1.197 3.65-.012 5.042-6.682 6.672-17.331 9.183-26.763 9.18-9.437-.002-20.035-2.609-26.775-9.204-1.262-1.235-.837-2.507 0-5.018l1.674-5.022z"
