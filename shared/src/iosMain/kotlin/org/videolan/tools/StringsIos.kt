package org.videolan.tools

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun Float.formatRateString(): String {
    val rounded = (this * 100).toInt() / 100.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()}.00x"
    else {
        val tenths = ((this * 10).toInt())
        "${tenths / 10}.${tenths % 10}x"
    }
}

actual fun Float.readableString(): String {
    return if (this % 1.0 == 0.0) toLong().toString()
    else toString()
}

actual fun String.firstLetterUppercase(): String {
    if (isEmpty()) return ""
    return if (length == 1) uppercase()
    else this[0].uppercaseChar() + substring(1).lowercase()
}

actual fun String.markBidi(markLtr: Boolean): String {
    val lri = "\u2066"
    val rli = "\u2067"
    val pdi = "\u2069"
    return when {
        markLtr -> lri + this + pdi
        this.hasRtl() -> rli + this + pdi
        else -> this
    }
}

actual fun String.hasRtl(): Boolean {
    // Arabic, Hebrew, and other RTL Unicode blocks
    val rtlRanges = listOf(
        0x0590 to 0x05FF,  // Hebrew
        0x0600 to 0x06FF,  // Arabic
        0x0700 to 0x074F,  // Syriac
        0x0750 to 0x077F,  // Arabic Supplement
        0x0780 to 0x07BF,  // Thaana
        0x07C0 to 0x07FF,  // NKo
        0x0800 to 0x083F,  // Samaritan
        0x0840 to 0x085F,  // Mandaic
        0x08A0 to 0x08FF,  // Arabic Extended-A
        0xFB1D to 0xFB4F,  // Hebrew Presentation Forms
        0xFB50 to 0xFDFF,  // Arabic Presentation Forms-A
        0xFE70 to 0xFEFF   // Arabic Presentation Forms-B
    )
    for (ch in this) {
        val code = ch.code
        for ((start, end) in rtlRanges) {
            if (code in start..end) return true
        }
    }
    return false
}
