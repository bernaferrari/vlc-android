package org.videolan.tools

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * JVM implementation of [VlcDataStoreFactory].
 * Stores preferences as a protobuf file in the given directory.
 */
class JvmVlcDataStoreFactory(
    private val storageDirectory: File,
    private val fileName: String = "vlc.preferences_pb"
) : VlcDataStoreFactory {
    override fun create(): DataStore<Preferences> =
        DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = { File(storageDirectory, fileName).absolutePath.toPath() }
            ),
            corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
        )
}
