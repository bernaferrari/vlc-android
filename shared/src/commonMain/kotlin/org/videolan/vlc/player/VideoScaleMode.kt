package org.videolan.vlc.player

/**
 * Shared union of the video resize choices exposed by VLC Android and iOS.
 *
 * The platform decoder owns the exact rendering primitive, while this value keeps selection,
 * accessibility labels and the Compose player surface identical on every target.
 */
enum class VideoScaleMode(
    val label: String,
    internal val nativeAspectRatio: String?,
    internal val nativeScale: Float,
    internal val cssObjectFit: String,
) {
    BEST_FIT("Best fit", null, 0f, "contain"),
    FIT_SCREEN("Fit screen", null, 0f, "cover"),
    FILL("Fill", null, 0f, "fill"),
    RATIO_16_9("16:9", "16:9", 0f, "contain"),
    RATIO_4_3("4:3", "4:3", 0f, "contain"),
    RATIO_16_10("16:10", "16:10", 0f, "contain"),
    RATIO_2_1("2:1", "2:1", 0f, "contain"),
    RATIO_221_1("2.21:1", "221:100", 0f, "contain"),
    RATIO_235_1("2.35:1", "235:100", 0f, "contain"),
    RATIO_239_1("2.39:1", "239:100", 0f, "contain"),
    RATIO_5_4("5:4", "5:4", 0f, "contain"),
    ORIGINAL("Center", null, 1f, "none"),
}
