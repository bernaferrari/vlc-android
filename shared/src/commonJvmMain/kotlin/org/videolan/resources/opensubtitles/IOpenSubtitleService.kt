package org.videolan.resources.opensubtitles

interface IOpenSubtitleService {
    suspend fun query(
        episode: Int? = null,
        hearingImpaired: String,
        imdbId: String? = null,
        languageId: String = "",
        movieHash: String? = null,
        name: String? = null,
        season: Int? = null,
        orderBy: String = "download_count",
    ): OpenSubV1

    suspend fun queryDownloadUrl(downloadLinkBody: DownloadLinkBody): DownloadLink

    suspend fun login(loginBody: LoginBody): OpenSubtitleResponse<OpenSubtitleAccount>

    suspend fun userInfo(): OpenSubtitleResponse<UserInfo>

}

data class OpenSubtitleResponse<T>(
    private val statusCode: Int,
    private val value: T?,
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
    fun body(): T? = value
    fun code(): Int = statusCode
}
