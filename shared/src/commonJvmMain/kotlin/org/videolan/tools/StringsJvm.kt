@file:JvmName("StringsActual")

package org.videolan.tools

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

actual fun Float.formatRateString(): String = String.format(Locale.US, "%.2fx", this)

actual fun Float.readableString(): String {
    return if (this % 1.0 == 0.0) String.format("%d", toLong())
    else String.format("%s", this)
}

actual fun String.firstLetterUppercase(): String {
    if (isEmpty()) {
        return ""
    }
    return if (length == 1) {
        uppercase(Locale.getDefault())
    } else Character.toUpperCase(this[0]) + substring(1).lowercase(Locale.getDefault())
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
    return this.toCharArray().any { ch ->
        when (Character.getDirectionality(ch)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE -> return true
            else -> false
        }
    }
}
