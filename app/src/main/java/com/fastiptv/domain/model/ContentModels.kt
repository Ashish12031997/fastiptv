package com.fastiptv.domain.model

data class Channel(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val categoryId: String?,
    val epgChannelId: String?,
    val isFavorite: Boolean = false,
    val nowPlaying: String? = null
)

data class Category(
    val id: String,
    val name: String,
    val type: CategoryType
)

enum class CategoryType {
    LIVE, VOD, SERIES
}

data class Movie(
    val id: Int,
    val name: String,
    val posterUrl: String?,
    val categoryId: String?,
    val containerExt: String,
    val rating: String?,
    val isFavorite: Boolean = false
)

data class Series(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val categoryId: String?,
    val rating: String?,
    val genre: String?,
    val isFavorite: Boolean = false
)

data class SeriesDetail(
    val seasons: List<SeriesSeason>,
    val episodes: Map<String, List<SeriesEpisode>>
)

data class SeriesSeason(
    val id: Int?,
    val name: String,
    val seasonNumber: Int,
    val episodeCount: Int,
    val coverUrl: String?
)

data class SeriesEpisode(
    val id: String,
    val episodeNum: Int,
    val title: String,
    val containerExt: String
)


data class RecentItem(
    val streamId: Int,
    val type: String,
    val title: String,
    val iconUrl: String?,
    val lastWatched: Long,
    val watchPositionMs: Long = 0,
    val durationMs: Long = 0
)

data class EpgProgram(
    val id: String?,
    val title: String,
    val description: String?,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isNowPlaying: Boolean = false
) {
    val progressPercent: Float
        get() {
            val now = System.currentTimeMillis() / 1000
            val total = endTimestamp - startTimestamp
            if (total <= 0) return 0f
            val elapsed = now - startTimestamp
            return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
}
