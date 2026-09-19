package com.fastiptv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    @SerialName("user_info") val userInfo: UserInfoDto? = null,
    @SerialName("server_info") val serverInfo: ServerInfoDto? = null
)

@Serializable
data class UserInfoDto(
    @SerialName("username") val username: String? = null,
    @SerialName("password") val password: String? = null,
    @SerialName("auth") val auth: Int? = 0,
    @SerialName("status") val status: String? = null,
    @SerialName("exp_date") val expDate: String? = null,
    @SerialName("is_trial") val isTrial: String? = null,
    @SerialName("active_cons") val activeCons: Int? = 0,
    @SerialName("max_connections") val maxConnections: String? = null,
    @SerialName("allowed_output_formats") val allowedOutputFormats: List<String>? = emptyList()
)

@Serializable
data class ServerInfoDto(
    @SerialName("url") val url: String? = null,
    @SerialName("port") val port: String? = null,
    @SerialName("https_port") val httpsPort: String? = null,
    @SerialName("server_protocol") val serverProtocol: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("timestamp_now") val timestampNow: Long? = null
)

@Serializable
data class CategoryDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("parent_id") val parentId: Int? = 0
)

@Serializable
data class LiveStreamDto(
    @SerialName("num") val num: Int? = null,
    @SerialName("name") val name: String,
    @SerialName("stream_type") val streamType: String? = "live",
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("tv_archive") val tvArchive: Int? = 0,
    @SerialName("tv_archive_duration") val tvArchiveDuration: Int? = 0
)

@Serializable
data class VodStreamDto(
    @SerialName("num") val num: Int? = null,
    @SerialName("name") val name: String,
    @SerialName("stream_type") val streamType: String? = "movie",
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = null,
    @SerialName("added") val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = "mp4"
)

@Serializable
data class SeriesDto(
    @SerialName("num") val num: Int? = null,
    @SerialName("name") val name: String,
    @SerialName("series_id") val seriesId: Int,
    @SerialName("cover") val cover: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = null,
    @SerialName("category_id") val categoryId: String? = null
)

@Serializable
data class ShortEpgDto(
    @SerialName("epg_listings") val epgListings: List<EpgListingDto>? = emptyList()
)

@Serializable
data class EpgListingDto(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("description") val description: String? = null,
    @SerialName("now_playing") val nowPlaying: Int? = 0,
    @SerialName("has_archive") val hasArchive: Int? = 0
)

@Serializable
data class SeriesInfoDto(
    @SerialName("seasons") val seasons: List<SeasonDto>? = emptyList(),
    @SerialName("episodes") val episodes: Map<String, List<EpisodeDto>>? = emptyMap()
)

@Serializable
data class SeasonDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("episode_count") val episodeCount: Int? = 0,
    @SerialName("cover") val cover: String? = null
)

@Serializable
data class EpisodeDto(
    @SerialName("id") val id: String? = null,
    @SerialName("episode_num") val episodeNum: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = "mp4"
)

