package org.videolan.tools

import kotlin.math.log10
import kotlin.math.pow

private const val TAG = "VLC/UiTools/Strings"

fun String.stripTrailingSlash() = if (endsWith("/") && length > 1) dropLast(1) else this
fun String.addTrailingSlashIfNeeded() = if (endsWith("/")) this else "$this/"

//TODO: Remove this after convert the dependent code to kotlin
fun startsWith(array: Array<String>, text: String) = array.any { text.startsWith(it)}

//TODO: Remove this after convert the dependent code to kotlin
fun containsName(list: List<String>, text: String) = list.indexOfLast { it.endsWith(text) }

fun String.removeFileScheme() = if (this.startsWith("file://")) this.drop(7) else this

fun String.getFileNameFromPath() = substringBeforeLast('/')

const val FORBIDDEN_CHARS = "ha]/m(?-*"

fun String.password() =  "*".repeat(length)

fun String.abbreviate(maxLen: Int): String {
    val ellipsis = "\u2026"
    val trimmed = this.trim()
    return if (trimmed.length > maxLen) trimmed.take(maxLen - 1).trim().plus(ellipsis)
    else trimmed
}

fun Long.readableNumber(): String {
    if (this <= 1000) return toString()
    if (this <= 1000000) return (this / 1000).toString() + "K"
    return (this / 1000000).toString() + "M"
}

fun Int.forbiddenChars() = FORBIDDEN_CHARS.substrlng(this)

/**
 * Obfuscation helper extracted from LocaleUtils for shared-module access.
 * Pure Kotlin — works on all platforms.
 */
fun String.substrlng(value: Int): String {
    return this.map {
        '$' + ((it + value % 45) - '$' + 45) % 90
    }.joinToString("")
}

/**
 * Format a Float as a human-readable file size string (B, KB, MB, GB, TB).
 * Uses pure Kotlin math — no platform-specific formatting.
 */
fun Long.readableSize(): String {
    val size: Long = this
    if (size <= 0) return "0"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1000.0)).toInt()
    val divisor = (1000.0).pow(digitGroups.toDouble())
    val formatted = (size / divisor).let {
        val rounded = (it * 10).toLong().toDouble() / 10
        if (rounded % 1.0 == 0.0) rounded.toLong().toString()
        else rounded.toString()
    }
    return "$formatted ${units[digitGroups.coerceAtMost(units.lastIndex)]}"
}

// ────────────────────────────────────────────────────────────────────────────
// Platform-specific formatting (expect/actual)
// ────────────────────────────────────────────────────────────────────────────

/**
 * Get the formatted current playback speed in the form of 1.00x
 */
expect fun Float.formatRateString(): String

/**
 * Format the Float value to a readable string without trailing zeros
 */
expect fun Float.readableString(): String

/**
 * Capitalize the first letter of the string using the platform's locale.
 */
expect fun String.firstLetterUppercase(): String

/**
 * Wrap RTL text with Unicode directional isolates.
 */
expect fun String.markBidi(markLtr: Boolean = false): String

/**
 * Check if the string contains RTL characters.
 */
expect fun String.hasRtl(): Boolean
