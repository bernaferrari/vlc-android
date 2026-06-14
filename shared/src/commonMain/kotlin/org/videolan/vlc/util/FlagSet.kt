package org.videolan.vlc.util

/**
 * Interface for types that can be represented as a bitmask value.
 * Used by [FlagSet] for efficient set operations on enums.
 */
interface Flag {
    fun toLong(): Long
}

/**
 * A set of enum flags backed by a Long bitmask.
 *
 * Platform-agnostic replacement for the original `java.util.EnumSet`-based
 * implementation. Works identically on Android, JVM, and iOS.
 *
 * @param T the enum type implementing [Flag]
 * @param values all enum entries (pass `YourEnum.entries`)
 */
class FlagSet<T>(
    private val values: List<T>
) where T : Enum<T>, T : Flag {

    private var bits: Long = 0L

    override fun toString(): String {
        return values.filter { contains(it) }.toString()
    }

    fun add(action: T) {
        bits = bits or action.toLong()
    }

    fun remove(action: T) {
        bits = bits and action.toLong().inv()
    }

    fun addAll(vararg actions: T) = actions.forEach { add(it) }

    fun removeAll(vararg actions: T) = actions.forEach { remove(it) }

    fun contains(action: T): Boolean = bits and action.toLong() != 0L

    fun isNotEmpty(): Boolean = bits != 0L

    fun isEmpty(): Boolean = bits == 0L

    fun getCapabilities(): Long = bits

    fun setCapabilities(capabilities: Long) {
        bits = capabilities
    }

    /**
     * Returns all flags currently enabled in this set.
     */
    fun enabledFlags(): List<T> = values.filter { contains(it) }
}
