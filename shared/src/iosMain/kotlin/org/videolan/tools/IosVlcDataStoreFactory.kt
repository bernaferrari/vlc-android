package org.videolan.tools

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation of [VlcDataStoreFactory].
 * Stores preferences in the app's Documents directory.
 */
class IosVlcDataStoreFactory(
    private val fileName: String = "vlc.preferences_pb"
) : VlcDataStoreFactory {

    override fun create(): DataStore<Preferences> {
        val documentsDir = documentsDirectory()
        return DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = { "$documentsDir/$fileName".toPath() }
            ),
            corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
        )
    }

    private fun documentsDirectory(): String {
        val paths = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        )
        return (paths.firstOrNull() as? NSURL)?.path ?: NSTemporaryDirectory()
    }

    private fun NSTemporaryDirectory(): String =
        platform.Foundation.NSTemporaryDirectory()
}
