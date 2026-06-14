package org.videolan.tools

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Platform-agnostic VLC preference abstraction backed by Jetpack DataStore.
 *
 * Replaces the SharedPreferences-based [Settings] singleton for all NEW code.
 * Existing code can continue to use [Settings] during the incremental migration.
 *
 * Usage from common code:
 * ```
 * // Read
 * val enabled: Flow<Boolean> = prefs.getFlow(KEY_ENABLE_FASTPLAY, true)
 *
 * // Write (suspend)
 * prefs.put(KEY_ENABLE_FASTPLAY, true)
 * ```
 *
 * The actual [DataStore] instance is injected via constructor so that each
 * platform can create it with the appropriate storage path (Android Context
 * vs JVM file path). See [VlcDataStoreFactory] for platform creation.
 */
class VlcPreferences(private val dataStore: DataStore<Preferences>) {

    // --- Boolean ---

    fun getBooleanFlow(key: String, default: Boolean): Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(key)] ?: default }

    suspend fun getBoolean(key: String, default: Boolean): Boolean =
        getBooleanFlow(key, default).first()

    suspend fun putBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    // --- Int ---

    fun getIntFlow(key: String, default: Int): Flow<Int> =
        dataStore.data.map { it[intPreferencesKey(key)] ?: default }

    suspend fun getInt(key: String, default: Int): Int =
        getIntFlow(key, default).first()

    suspend fun putInt(key: String, value: Int) {
        dataStore.edit { it[intPreferencesKey(key)] = value }
    }

    // --- Float ---

    fun getFloatFlow(key: String, default: Float): Flow<Float> =
        dataStore.data.map { it[floatPreferencesKey(key)] ?: default }

    suspend fun getFloat(key: String, default: Float): Float =
        getFloatFlow(key, default).first()

    suspend fun putFloat(key: String, value: Float) {
        dataStore.edit { it[floatPreferencesKey(key)] = value }
    }

    // --- Long ---

    fun getLongFlow(key: String, default: Long): Flow<Long> =
        dataStore.data.map { it[longPreferencesKey(key)] ?: default }

    suspend fun getLong(key: String, default: Long): Long =
        getLongFlow(key, default).first()

    suspend fun putLong(key: String, value: Long) {
        dataStore.edit { it[longPreferencesKey(key)] = value }
    }

    // --- String ---

    fun getStringFlow(key: String, default: String = ""): Flow<String> =
        dataStore.data.map { it[stringPreferencesKey(key)] ?: default }

    suspend fun getString(key: String, default: String = ""): String =
        getStringFlow(key, default).first()

    suspend fun putString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    // --- String Set ---

    fun getStringSetFlow(key: String, default: Set<String> = emptySet()): Flow<Set<String>> =
        dataStore.data.map { it[stringSetPreferencesKey(key)] ?: default }

    suspend fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> =
        getStringSetFlow(key, default).first()

    suspend fun putStringSet(key: String, value: Set<String>) {
        dataStore.edit { it[stringSetPreferencesKey(key)] = value }
    }

    // --- Generic typed put (mirrors Settings.putSingle) ---

    suspend fun put(key: String, value: Any) {
        dataStore.edit { prefs ->
            when (value) {
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
                is Int     -> prefs[intPreferencesKey(key)] = value
                is Float   -> prefs[floatPreferencesKey(key)] = value
                is Long    -> prefs[longPreferencesKey(key)] = value
                is String  -> prefs[stringPreferencesKey(key)] = value
                is Set<*>  -> @Suppress("UNCHECKED_CAST")
                    prefs[stringSetPreferencesKey(key)] = value as Set<String>
                else -> throw IllegalArgumentException("value $value class is invalid!")
            }
        }
    }

    // --- Remove ---

    suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

/**
 * Factory contract for creating a platform-specific [DataStore] instance.
 * Each platform provides its own implementation (expect/actual or DI).
 */
interface VlcDataStoreFactory {
    fun create(): DataStore<Preferences>
}
