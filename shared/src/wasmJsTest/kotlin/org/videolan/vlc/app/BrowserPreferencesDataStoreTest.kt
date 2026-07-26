package org.videolan.vlc.app

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserPreferencesDataStoreTest {
    @Test
    fun snapshot_roundTrips_every_vlcPreferenceType() {
        val original = emptyPreferences().toMutablePreferences().apply {
            this[booleanPreferencesKey("resume")] = true
            this[intPreferencesKey("count")] = 7
            this[floatPreferencesKey("speed")] = 1.25f
            this[longPreferencesKey("position")] = 4_294_967_296L
            this[stringPreferencesKey("title")] = "Rock & roll\n100%"
            this[stringSetPreferencesKey("folders")] = setOf("file:///A B", "smb://nas/épisode")
        }.toPreferences()

        assertEquals(original.asMap(), decodePreferences(encodePreferences(original)).asMap())
    }

    @Test
    fun reopened_store_uses_the_persisted_snapshot() = runTest {
        val storage = RecordingStorage()
        val firstStore = BrowserPreferencesDataStore(storage)
        firstStore.updateData { current ->
            current.toMutablePreferences().apply {
                this[booleanPreferencesKey("incognito")] = true
            }.toPreferences()
        }

        val reopenedStore = BrowserPreferencesDataStore(storage)
        assertEquals(true, reopenedStore.data.first()[booleanPreferencesKey("incognito")])
    }

    @Test
    fun invalid_snapshot_falls_back_to_empty_preferences() {
        assertEquals(emptyMap(), decodePreferences("not a VLC snapshot").asMap())
    }

    @Test
    fun unavailable_storage_keeps_a_working_inMemory_store() = runTest {
        val store = BrowserPreferencesDataStore(FailingStorage())
        store.updateData { current ->
            current.toMutablePreferences().apply {
                this[booleanPreferencesKey("resume")] = true
            }.toPreferences()
        }

        assertEquals(true, store.data.first()[booleanPreferencesKey("resume")])
    }

    private class RecordingStorage : BrowserPreferenceStorage {
        private var value: String? = null

        override fun read(): String? = value

        override fun write(value: String) {
            this.value = value
        }
    }

    private class FailingStorage : BrowserPreferenceStorage {
        override fun read(): String? = error("storage is unavailable")

        override fun write(value: String) = error("storage is unavailable")
    }
}
