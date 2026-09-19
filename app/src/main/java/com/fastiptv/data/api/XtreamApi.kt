package com.fastiptv.data.api

import com.fastiptv.data.model.AuthResponseDto
import com.fastiptv.data.model.CategoryDto
import com.fastiptv.data.model.LiveStreamDto
import com.fastiptv.data.model.SeriesDto
import com.fastiptv.data.model.SeriesInfoDto
import com.fastiptv.data.model.ShortEpgDto
import com.fastiptv.data.model.VodStreamDto
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {

    @GET("player_api.php")
    suspend fun authenticate(): AuthResponseDto

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("action") action: String = "get_live_categories"
    ): List<CategoryDto>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<LiveStreamDto>

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("action") action: String = "get_vod_categories"
    ): List<CategoryDto>

    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null
    ): List<VodStreamDto>

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("action") action: String = "get_series_categories"
    ): List<CategoryDto>

    @GET("player_api.php")
    suspend fun getSeries(
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null
    ): List<SeriesDto>

    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Int
    ): SeriesInfoDto

    @GET("player_api.php")
    suspend fun getShortEpg(
        @Query("action") action: String = "get_short_epg",
        @Query("stream_id") streamId: Int,
        @Query("limit") limit: Int = 10
    ): ShortEpgDto
}
