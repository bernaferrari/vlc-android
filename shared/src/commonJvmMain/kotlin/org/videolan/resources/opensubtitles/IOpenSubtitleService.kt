package org.videolan.resources.opensubtitles

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface IOpenSubtitleService {
    @GET("subtitles")
    suspend fun query(
        @Query("episode_number") episode: Int? = null,
        @Query("hearing_impaired") hearingImpaired: String,
        @Query("imdb_id") imdbId: String? = null,
        @Query("languages") languageId: String = "",
        @Query("moviehash") movieHash: String? = null,
        @Query("query") name: String? = null,
        @Query("season_number") season: Int? = null,
        @Query("order_by") orderBy: String = "download_count",
    ): OpenSubV1

    @POST("download")
    suspend fun queryDownloadUrl( @Body downloadLinkBody: DownloadLinkBody): DownloadLink

    @POST("login")
    suspend fun login(@Body loginBody: LoginBody): Response<OpenSubtitleAccount>

    @GET("infos/user")
    suspend fun userInfo(): Response<UserInfo>

}
