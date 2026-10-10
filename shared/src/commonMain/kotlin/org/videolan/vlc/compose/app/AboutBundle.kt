package org.videolan.vlc.compose.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.videolan.vlc.compose.components.VLCLibraryLicense
import vlc_android.shared.generated.resources.Res

/**
 * The About content every VLC build ships: GPL text, contributors and third-party licenses.
 * It lives in shared resources so Android, iOS, desktop and web present the same, complete page.
 */
object AboutBundle {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun licenseText(): String = runCatching {
        Res.readBytes("files/about/vlc_license.txt").decodeToString()
    }.getOrDefault(VLC_DEFAULT_LICENSE_TEXT)

    suspend fun authors(): List<String> = runCatching {
        json.decodeFromString<List<String>>(Res.readBytes("files/about/authors.json").decodeToString())
    }.getOrDefault(emptyList())

    suspend fun libraries(): List<VLCLibraryLicense> = runCatching {
        val document = json.decodeFromString<LibrariesDocument>(Res.readBytes("files/about/libraries.json").decodeToString())
        val licenses = document.licenses.associateBy(LicenseEntry::id)
        document.libraries.mapNotNull { library ->
            licenses[library.license]?.let { license ->
                VLCLibraryLicense(
                    title = library.title,
                    copyright = library.copyright,
                    licenseTitle = license.name,
                    licenseDescription = license.description,
                    licenseLink = license.link,
                )
            }
        }
    }.getOrDefault(emptyList())

    @Serializable
    private data class LibrariesDocument(
        val libraries: List<LibraryEntry> = emptyList(),
        val licenses: List<LicenseEntry> = emptyList(),
    )

    @Serializable
    private data class LibraryEntry(val title: String, val copyright: String = "", val license: String)

    @Serializable
    private data class LicenseEntry(
        val id: String,
        val name: String = "",
        val description: String = "",
        val link: String = "",
    )
}
