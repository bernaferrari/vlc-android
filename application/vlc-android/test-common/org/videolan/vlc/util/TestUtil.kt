package org.videolan.vlc.util

import android.net.Uri
import androidx.core.net.toUri
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.resources.TYPE_LOCAL_FAV
import org.videolan.resources.TYPE_NETWORK_FAV
import org.videolan.vlc.gui.dialogs.State
import org.videolan.vlc.gui.dialogs.SubtitleItem
import org.videolan.vlc.mediadb.models.BrowserFav
import org.videolan.vlc.mediadb.models.CustomDirectory
import org.videolan.vlc.mediadb.models.ExternalSub
import org.videolan.vlc.mediadb.models.Slave
import org.videolan.resources.opensubtitles.Attributes
import org.videolan.resources.opensubtitles.Data
import org.videolan.resources.opensubtitles.FeatureDetails
import org.videolan.resources.opensubtitles.File as OpenSubtitleFile
import org.videolan.resources.opensubtitles.OpenSubV1

object TestUtil {
    private const val fakeUri = "https://www.videolan.org/fake_"
    private const val fakeSubUri = "/storage/emulated/0/Android/data/org.videolan.vlc.debug/files/subs/"
    private const val fakeMediaUri = "/storage/emulated/0/Android/data/org.videolan.vlc.debug/files/media/"

    fun createLocalFav(uri: Uri, title: String, iconUrl: String?) =
        BrowserFav(uri, TYPE_LOCAL_FAV, title, iconUrl)

    fun createLocalUris(count: Int) = (0 until count).map { "$fakeMediaUri/local_$it.mp4" }

    fun createLocalFavs(count: Int) = (0 until count).map {
        createLocalFav("$fakeMediaUri/$it.mp4".toUri(), "local$it", null)
    }

    fun createNetworkFav(uri: Uri, title: String, iconUrl: String?) =
        BrowserFav(uri, TYPE_NETWORK_FAV, title, iconUrl)

    fun createNetworkUris(count: Int) = (0 until count).map { "$fakeUri/network$it.mp4" }

    fun createNetworkFavs(count: Int) = (0 until count).map {
        createNetworkFav("$fakeUri/network$it".toUri(), "network$it", null)
    }

    fun createExternalSub(
        idSubtitle: String,
        subtitlePath: String,
        mediaPath: String,
        subLanguageID: String,
        movieReleaseName: String,
    ) = ExternalSub(idSubtitle, subtitlePath, mediaPath, subLanguageID, movieReleaseName, false)

    fun createExternalSubsForMedia(mediaPath: String, mediaName: String, count: Int) =
        (0 until count).map {
            createExternalSub(it.toString(), "$fakeSubUri$mediaName$it", mediaPath, "en", mediaName)
        }

    fun createSubtitleSlave(mediaPath: String, uri: String) =
        Slave(mediaPath, IMedia.Slave.Type.Subtitle, 2, uri)

    fun createSubtitleSlavesForMedia(mediaName: String, count: Int) = (0 until count).map {
        createSubtitleSlave("$fakeMediaUri$mediaName", "$fakeSubUri$mediaName$it.srt")
    }

    fun createCustomDirectory(path: String) = CustomDirectory(path)

    fun createCustomDirectories(count: Int) = (0 until count).map {
        createCustomDirectory("/sdcard/foo$it")
    }

    fun createDownloadingSubtitleItem(
        idSubtitle: String,
        mediaUri: Uri,
        subLanguageID: String,
        movieReleaseName: String,
        zipDownloadLink: String,
    ) = SubtitleItem(
        idSubtitle = idSubtitle,
        fileId = -1L,
        mediaUri = mediaUri,
        subLanguageID = subLanguageID,
        movieReleaseName = movieReleaseName,
        state = State.Downloading,
        zipDownloadLink = zipDownloadLink,
        hearingImpaired = false,
        rating = 0F,
        downloadNumber = 0L,
    )

    fun createOpenSubtitle(
        id: String,
        language: String,
        releaseName: String,
        downloadUrl: String,
    ) = Data(
        attributes = Attributes(
            aiTranslated = null,
            comments = null,
            featureDetails = FeatureDetails(
                episodeNumber = null,
                featureId = null,
                featureType = null,
                imdbId = null,
                movieName = releaseName,
                parentFeatureId = null,
                parentImdbId = null,
                parentTitle = null,
                parentTmdbId = null,
                seasonNumber = null,
                title = releaseName,
                tmdbId = null,
                year = null,
            ),
            fileHashes = null,
            files = listOf(OpenSubtitleFile(cdNumber = null, fileId = id.toLong(), fileName = "$releaseName.srt")),
            foreignPartsOnly = null,
            fps = null,
            fromTrusted = null,
            hd = null,
            language = language,
            legacySubtitleId = null,
            legacyUploaderId = null,
            machineTranslated = null,
            nbCd = null,
            newDownloadCount = null,
            relatedLinks = null,
            release = releaseName,
            slug = null,
            subtitleId = id,
            uploadDate = null,
            uploader = null,
            url = downloadUrl,
            votes = null,
        ),
        id = id,
        type = "subtitle",
    )

    fun createOpenSubtitleResponse(subtitles: List<Data>) = OpenSubV1(
        `data` = subtitles,
        page = null,
        perPage = null,
        totalCount = subtitles.size,
        totalPages = null,
    )
}
