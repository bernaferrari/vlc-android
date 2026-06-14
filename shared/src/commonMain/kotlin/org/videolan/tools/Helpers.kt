package org.videolan.tools

/**
 * Force an [Int] to be in a range else set it to a default value.
 *
 * @param min the minimum value to accept
 * @param max the maximum value to accept
 * @param defautValue the default value to return if it's not in the range
 * @return an [Int] in the range
 */
fun Int.coerceInOrDefault(min: Int, max: Int, defautValue: Int) = if (this < min || this > max) defautValue else this
