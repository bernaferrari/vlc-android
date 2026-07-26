package org.videolan.tools

actual fun Float.formatRateString(): String {
    val scaled = (this * 100).toInt()
    return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}x"
}

actual fun Float.readableString(): String =
    if (this % 1f == 0f) toInt().toString() else toString()

actual fun String.firstLetterUppercase(): String =
    replaceFirstChar { it.uppercaseChar() }

actual fun String.markBidi(markLtr: Boolean): String = when {
    markLtr -> "\u2066$this\u2069"
    hasRtl() -> "\u2067$this\u2069"
    else -> this
}

actual fun String.hasRtl(): Boolean = any { char ->
    char in '\u0590'..'\u08ff' ||
        char in '\ufb1d'..'\ufdff' ||
        char in '\ufe70'..'\ufeff'
}
