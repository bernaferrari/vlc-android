package org.videolan.tools

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.vlcDataStore: DataStore<Preferences> by preferencesDataStore(name = "vlc_preferences")

/**
 * Android implementation of [VlcDataStoreFactory].
 * Uses the Android-specific `preferencesDataStore` delegate which stores
 * preferences in the app's files directory.
 */
class AndroidVlcDataStoreFactory(private val context: Context) : VlcDataStoreFactory {
    override fun create(): DataStore<Preferences> = context.applicationContext.vlcDataStore
}
