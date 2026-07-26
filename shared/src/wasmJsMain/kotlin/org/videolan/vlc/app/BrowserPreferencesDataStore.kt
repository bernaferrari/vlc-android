@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.videolan.vlc.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Small, synchronous browser storage boundary. It deliberately treats browser storage as
 * optional: private browsing, quota limits, and host security policies must not prevent the
 * media demo from opening.
 */
internal interface BrowserPreferenceStorage {
    fun read(): String?
    fun write(value: String)
}

internal object LocalStoragePreferenceStorage : BrowserPreferenceStorage {
    override fun read(): String? = readLocalStorage(WEB_PREFERENCES_KEY)

    override fun write(value: String) {
        writeLocalStorage(WEB_PREFERENCES_KEY, value)
    }
}

/**
 * A browser-backed [DataStore] for the settings exposed by [org.videolan.tools.VlcPreferences].
 *
 * Writes update the in-memory state first and then persist a versioned snapshot to localStorage.
 * If localStorage is unavailable, the same instance remains a correct in-memory DataStore. Media
 * is intentionally not persisted here: browser file handles need explicit user authorization and
 * belong in the browser-media capability layer.
 */
internal class BrowserPreferencesDataStore(
    private val storage: BrowserPreferenceStorage = LocalStoragePreferenceStorage,
) : DataStore<Preferences> {
    private val values = MutableStateFlow(
        runCatching { decodePreferences(storage.read()) }.getOrDefault(emptyPreferences()),
    )
    private val updateMutex = Mutex()

    override val data: Flow<Preferences> = values

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        updateMutex.withLock {
            transform(values.value).also { updated ->
                values.value = updated
                // Browser storage is an enhancement, never a reason to reject a settings change.
                runCatching { storage.write(encodePreferences(updated)) }
            }
        }
}

/** A compact versioned text format; values are URI-escaped before they become delimiters. */
internal fun encodePreferences(preferences: Preferences): String = buildString {
    append(PREFERENCES_FORMAT_VERSION)
    preferences.asMap().entries.forEach { (key, value) ->
        val encodedName = encodeStoragePart(key.name)
        when (value) {
            is Boolean -> appendEntry(PREF_BOOLEAN, encodedName, value.toString())
            is Int -> appendEntry(PREF_INT, encodedName, value.toString())
            is Float -> appendEntry(PREF_FLOAT, encodedName, value.toString())
            is Long -> appendEntry(PREF_LONG, encodedName, value.toString())
            is String -> appendEntry(PREF_STRING, encodedName, encodeStoragePart(value))
            is Set<*> -> {
                val strings = value.filterIsInstance<String>()
                if (strings.size == value.size) {
                    appendEntry(
                        PREF_STRING_SET,
                        encodedName,
                        strings.joinToString(separator = ",", transform = ::encodeStoragePart),
                    )
                }
            }
        }
    }
}

internal fun decodePreferences(snapshot: String?): Preferences {
    if (snapshot.isNullOrBlank()) return emptyPreferences()
    val lines = snapshot.lineSequence().iterator()
    if (!lines.hasNext() || lines.next() != PREFERENCES_FORMAT_VERSION) return emptyPreferences()

    return emptyPreferences().toMutablePreferences().apply {
        lines.forEach { line ->
            val fields = line.split('\t', limit = 3)
            if (fields.size != 3) return@forEach
            val name = decodeStoragePart(fields[1]) ?: return@forEach
            when (fields[0]) {
                PREF_BOOLEAN -> fields[2].toBooleanStrictOrNull()?.let { this[booleanPreferencesKey(name)] = it }
                PREF_INT -> fields[2].toIntOrNull()?.let { this[intPreferencesKey(name)] = it }
                PREF_FLOAT -> fields[2].toFloatOrNull()?.let { this[floatPreferencesKey(name)] = it }
                PREF_LONG -> fields[2].toLongOrNull()?.let { this[longPreferencesKey(name)] = it }
                PREF_STRING -> decodeStoragePart(fields[2])?.let { this[stringPreferencesKey(name)] = it }
                PREF_STRING_SET -> {
                    val decoded = fields[2]
                        .takeIf(String::isNotEmpty)
                        ?.split(',')
                        ?.map(::decodeStoragePart)
                        ?.takeIf { it.none { value -> value == null } }
                        ?.filterNotNull()
                        ?.toSet()
                        ?: emptySet()
                    this[stringSetPreferencesKey(name)] = decoded
                }
            }
        }
    }.toPreferences()
}

private fun StringBuilder.appendEntry(type: String, name: String, value: String) {
    append('\n').append(type).append('\t').append(name).append('\t').append(value)
}

private fun encodeStoragePart(value: String): String = js("encodeURIComponent(value)")

private fun decodeStoragePart(value: String): String? = js(
    "(() => { try { return decodeURIComponent(value); } catch (_) { return null; } })()",
)

private fun readLocalStorage(key: String): String? = js(
    "(() => { try { return globalThis.localStorage?.getItem(key) ?? null; } catch (_) { return null; } })()",
)

private fun writeLocalStorage(key: String, value: String): Unit = js(
    "{ try { globalThis.localStorage?.setItem(key, value); } catch (_) {} }",
)

private const val WEB_PREFERENCES_KEY = "org.videolan.vlc.web.preferences"
private const val PREFERENCES_FORMAT_VERSION = "vlc-web-preferences-v1"
private const val PREF_BOOLEAN = "b"
private const val PREF_INT = "i"
private const val PREF_FLOAT = "f"
private const val PREF_LONG = "l"
private const val PREF_STRING = "s"
private const val PREF_STRING_SET = "ss"
