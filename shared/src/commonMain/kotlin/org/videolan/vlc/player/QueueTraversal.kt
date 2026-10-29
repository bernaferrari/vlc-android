package org.videolan.vlc.player

import org.videolan.vlc.model.RepeatMode
import kotlin.random.Random

/** Traversal follows queue entries, including duplicate media, through edits to their positions. */
internal class QueueTraversal(private val random: Random = Random.Default) {
    private val entries = mutableListOf<Long>()
    private val history = ArrayDeque<Long>()
    private val visited = mutableSetOf<Long>()
    private var nextId = 0L

    fun reset(size: Int, current: Int) {
        entries.clear()
        repeat(size) { entries.add(nextId++) }
        resetTraversal(current)
    }

    fun resetTraversal(current: Int) {
        history.clear()
        visited.clear()
        visit(current)
    }

    fun visit(index: Int) {
        entries.getOrNull(index)?.let { visited.add(it) }
    }

    fun recordTransition(from: Int, to: Int) {
        if (from != to) entries.getOrNull(from)?.let(history::addLast)
        visit(to)
    }

    fun previous(): Int? {
        while (history.isNotEmpty()) {
            val index = entries.indexOf(history.removeLast())
            if (index >= 0) return index
        }
        return null
    }

    fun next(current: Int, shuffle: Boolean, repeat: RepeatMode): Int? {
        if (entries.isEmpty()) return null
        if (repeat == RepeatMode.ONE) return current
        if (!shuffle) return when {
            current < entries.lastIndex -> current + 1
            repeat == RepeatMode.ALL -> 0
            else -> null
        }
        visit(current)
        var candidates = entries.indices.filter { entries[it] !in visited }
        if (candidates.isEmpty() && repeat == RepeatMode.ALL) {
            visited.clear()
            visit(current)
            candidates = entries.indices.filter { it != current }
            if (candidates.isEmpty()) return current
        }
        return candidates.takeIf { it.isNotEmpty() }?.let { it[random.nextInt(it.size)] }
    }

    fun insert(index: Int, count: Int) {
        entries.addAll(index, List(count) { nextId++ })
    }

    fun move(from: Int, to: Int) {
        entries.add(to, entries.removeAt(from))
    }

    fun remove(index: Int) {
        val entry = entries.removeAt(index)
        visited.remove(entry)
        history.removeAll { it == entry }
    }
}
