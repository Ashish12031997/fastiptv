package com.fastiptv.data.repository

import androidx.room.withTransaction
import com.fastiptv.data.api.XtreamApi
import com.fastiptv.data.db.AppDatabase
import com.fastiptv.data.db.dao.CategoryDao
import com.fastiptv.data.db.dao.ChannelDao
import com.fastiptv.data.db.dao.FavoriteDao
import com.fastiptv.data.db.dao.RecentDao
import com.fastiptv.data.db.dao.SeriesDao
import com.fastiptv.data.db.dao.VodDao
import com.fastiptv.data.db.entity.CategoryEntity
import com.fastiptv.data.db.entity.ChannelEntity
import com.fastiptv.data.db.entity.FavoriteEntity
import com.fastiptv.data.db.entity.RecentEntity
import com.fastiptv.data.db.entity.SeriesEntity
import com.fastiptv.data.db.entity.VodEntity
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.CategoryType
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.model.Movie
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.domain.model.Series
import com.fastiptv.domain.model.SeriesDetail
import com.fastiptv.domain.model.SeriesEpisode
import com.fastiptv.domain.model.SeriesSeason
import com.fastiptv.domain.repository.IptvRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvRepositoryImpl @Inject constructor(
    private val api: XtreamApi,
    private val database: AppDatabase,
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao
) : IptvRepository {

    // --- Live Channels ---
    override fun observeLiveCategories(): Flow<List<Category>> {
        return categoryDao.observeCategories(CategoryType.LIVE.name)
            .map { list ->
                list.map { entity ->
                    Category(
                        id = entity.categoryId,
                        name = entity.name,
                        type = CategoryType.valueOf(entity.type)
                    )
                }
            }
            .onStart {
                syncLiveCategories()
            }
    }

    override fun observeChannelsByCategory(categoryId: String): Flow<List<Channel>> {
        return channelDao.observeChannelsByCategory(categoryId)
            .map { list -> list.map { it.toDomain() } }
            .onStart {
                syncChannels(categoryId)
            }
    }

    override fun observeFavorites(): Flow<List<Channel>> {
        return channelDao.observeFavorites()
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeRecents(limit: Int): Flow<List<RecentItem>> {
        return recentDao.observeRecent(limit)
            .map { list ->
                list.map { entity ->
                    RecentItem(
                        streamId = entity.streamId,
                        type = entity.type,
                        title = entity.title,
                        iconUrl = entity.iconUrl,
                        lastWatched = entity.lastWatched,
                        watchPositionMs = entity.watchPositionMs,
                        durationMs = entity.durationMs
                    )
                }
            }
    }

    override fun searchChannels(query: String): Flow<List<Channel>> {
        val fallback = getSearchFallback(query)
        return channelDao.searchChannels(query, fallback)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordRecent(recent: RecentItem): Result<Unit> = runCatching {
        recentDao.insertRecent(
            RecentEntity(
                streamId = recent.streamId,
                type = recent.type,
                title = recent.title,
                iconUrl = recent.iconUrl,
                lastWatched = recent.lastWatched,
                watchPositionMs = recent.watchPositionMs,
                durationMs = recent.durationMs
            )
        )
    }

    override suspend fun getRecentByStreamId(streamId: Int): RecentItem? {
        val entity = recentDao.getRecentByStreamId(streamId) ?: return null
        return RecentItem(
            streamId = entity.streamId,
            type = entity.type,
            title = entity.title,
            iconUrl = entity.iconUrl,
            lastWatched = entity.lastWatched,
            watchPositionMs = entity.watchPositionMs,
            durationMs = entity.durationMs
        )
    }

    override suspend fun updatePlaybackPosition(
        streamId: Int,
        positionMs: Long,
        durationMs: Long
    ): Result<Unit> = runCatching {
        recentDao.updatePlaybackPosition(
            streamId = streamId,
            positionMs = positionMs,
            durationMs = durationMs
        )
    }

    override suspend fun syncLiveCategories(): Result<Unit> = runCatching {
        val dtos = api.getLiveCategories()
        val entities = dtos.map { dto ->
            CategoryEntity(
                categoryId = dto.categoryId,
                name = dto.categoryName,
                type = CategoryType.LIVE.name
            )
        }
        database.withTransaction {
            categoryDao.deleteByType(CategoryType.LIVE.name)
            categoryDao.upsertAll(entities)
        }
    }

    override suspend fun syncChannels(categoryId: String): Result<Unit> = runCatching {
        val dtos = api.getLiveStreams(categoryId = categoryId)
        val entities = dtos.map { dto ->
            ChannelEntity(
                streamId = dto.streamId,
                name = dto.name,
                iconUrl = dto.streamIcon,
                categoryId = dto.categoryId ?: categoryId,
                epgChannelId = dto.epgChannelId,
                streamType = dto.streamType ?: "live"
            )
        }
        database.withTransaction {
            channelDao.deleteByCategory(categoryId)
            channelDao.upsertAll(entities)
        }
    }

    override suspend fun getChannelById(streamId: Int): Channel? {
        val channel = channelDao.getChannelById(streamId)?.toDomain()
        if (channel != null) return channel
        val isFav = favoriteDao.isFavorite(streamId)
        return if (isFav) {
            Channel(
                id = streamId,
                name = "Channel $streamId",
                logoUrl = null,
                categoryId = null,
                epgChannelId = null,
                isFavorite = true
            )
        } else null
    }

    override suspend fun getShortEpg(streamId: Int): Result<List<EpgProgram>> = runCatching {
        val dto = api.getShortEpg(streamId = streamId)
        val listings = dto.epgListings ?: emptyList()
        listings.map { item ->
            EpgProgram(
                id = item.id,
                title = decodeEpgText(item.title),
                description = item.description?.let { decodeEpgText(it) },
                startTimestamp = parseEpgTime(item.start),
                endTimestamp = parseEpgTime(item.end),
                isNowPlaying = item.nowPlaying == 1
            )
        }
    }

    override suspend fun toggleFavorite(channel: Channel): Result<Boolean> = runCatching {
        val isCurrentlyFavorite = favoriteDao.isFavorite(channel.id)
        if (isCurrentlyFavorite) {
            favoriteDao.delete(channel.id)
            false
        } else {
            channelDao.upsertAll(listOf(
                ChannelEntity(
                    streamId = channel.id,
                    name = channel.name,
                    iconUrl = channel.logoUrl,
                    categoryId = channel.categoryId ?: "0",
                    epgChannelId = channel.epgChannelId
                )
            ))
            favoriteDao.insert(FavoriteEntity(streamId = channel.id, type = "live"))
            true
        }
    }

    // --- VOD Movies ---
    override fun observeVodCategories(): Flow<List<Category>> {
        return categoryDao.observeCategories(CategoryType.VOD.name)
            .map { list ->
                list.map { entity ->
                    Category(
                        id = entity.categoryId,
                        name = entity.name,
                        type = CategoryType.VOD
                    )
                }
            }
            .onStart {
                syncVodCategories()
            }
    }

    override fun observeMoviesByCategory(categoryId: String): Flow<List<Movie>> {
        return vodDao.observeMoviesByCategory(categoryId)
            .map { list ->
                list.map { entity ->
                    Movie(
                        id = entity.streamId,
                        name = entity.name,
                        posterUrl = entity.posterUrl,
                        categoryId = entity.categoryId,
                        containerExt = entity.containerExt,
                        rating = entity.rating
                    )
                }
            }
            .onStart {
                syncMovies(categoryId)
            }
    }

    override fun searchMovies(query: String): Flow<List<Movie>> {
        val fallback = getSearchFallback(query)
        return vodDao.searchMovies(query, fallback)
            .map { list ->
                list.map { entity ->
                    Movie(
                        id = entity.streamId,
                        name = entity.name,
                        posterUrl = entity.posterUrl,
                        categoryId = entity.categoryId,
                        containerExt = entity.containerExt,
                        rating = entity.rating
                    )
                }
            }
    }

    override fun observeFavoriteMovies(): Flow<List<Movie>> {
        return vodDao.observeFavoriteMovies().map { list ->
            list.map { entity ->
                Movie(
                    id = entity.streamId,
                    name = entity.name,
                    posterUrl = entity.posterUrl,
                    categoryId = entity.categoryId,
                    containerExt = entity.containerExt,
                    rating = entity.rating,
                    isFavorite = true
                )
            }
        }
    }

    override suspend fun toggleFavoriteMovie(movie: Movie): Result<Boolean> = runCatching {
        val isCurrentlyFavorite = favoriteDao.isFavorite(movie.id)
        if (isCurrentlyFavorite) {
            favoriteDao.delete(movie.id)
            false
        } else {
            vodDao.upsertAll(listOf(
                VodEntity(
                    streamId = movie.id,
                    name = movie.name,
                    posterUrl = movie.posterUrl,
                    categoryId = movie.categoryId ?: "0",
                    containerExt = movie.containerExt,
                    rating = movie.rating
                )
            ))
            favoriteDao.insert(FavoriteEntity(streamId = movie.id, type = "vod"))
            true
        }
    }

    override suspend fun clearCache(): Result<Unit> = runCatching {
        database.clearAllTables()
    }

    override suspend fun syncVodCategories(): Result<Unit> = runCatching {
        android.util.Log.d("FastIPTV", "syncVodCategories starting...")
        val dtos = api.getVodCategories()
        android.util.Log.d("FastIPTV", "syncVodCategories fetched ${dtos.size} categories")
        val entities = dtos.map { dto ->
            CategoryEntity(
                categoryId = dto.categoryId,
                name = dto.categoryName,
                type = CategoryType.VOD.name
            )
        }
        database.withTransaction {
            categoryDao.deleteByType(CategoryType.VOD.name)
            categoryDao.upsertAll(entities)
        }
        android.util.Log.d("FastIPTV", "syncVodCategories saved ${entities.size} categories to DB")
        Unit
    }.onFailure {
        android.util.Log.e("FastIPTV", "syncVodCategories error: ${it.message}", it)
    }

    override suspend fun syncMovies(categoryId: String): Result<Unit> = runCatching {
        android.util.Log.d("FastIPTV", "syncMovies starting for category $categoryId")
        val dtos = api.getVodStreams(categoryId = categoryId)
        android.util.Log.d("FastIPTV", "syncMovies fetched ${dtos.size} movies for category $categoryId")
        val entities = dtos.map { dto ->
            VodEntity(
                streamId = dto.streamId,
                name = dto.name,
                posterUrl = dto.streamIcon,
                categoryId = dto.categoryId ?: categoryId,
                containerExt = dto.containerExtension ?: "mp4",
                rating = dto.rating
            )
        }
        database.withTransaction {
            vodDao.deleteByCategory(categoryId)
            vodDao.upsertAll(entities)
        }
        android.util.Log.d("FastIPTV", "syncMovies saved ${entities.size} movies for category $categoryId to DB")
        Unit
    }.onFailure {
        android.util.Log.e("FastIPTV", "syncMovies error: ${it.message}", it)
    }

    // --- TV Series ---
    override fun observeSeriesCategories(): Flow<List<Category>> {
        return categoryDao.observeCategories(CategoryType.SERIES.name)
            .map { list ->
                list.map { entity ->
                    Category(
                        id = entity.categoryId,
                        name = entity.name,
                        type = CategoryType.SERIES
                    )
                }
            }
            .onStart {
                syncSeriesCategories()
            }
    }

    override fun observeSeriesByCategory(categoryId: String): Flow<List<Series>> {
        return seriesDao.observeSeriesByCategory(categoryId)
            .map { list ->
                list.map { entity ->
                    Series(
                        id = entity.seriesId,
                        name = entity.name,
                        coverUrl = entity.coverUrl,
                        categoryId = entity.categoryId,
                        rating = entity.rating,
                        genre = entity.genre
                    )
                }
            }
            .onStart {
                syncSeries(categoryId)
            }
    }

    override fun searchSeries(query: String): Flow<List<Series>> {
        val fallback = getSearchFallback(query)
        return seriesDao.searchSeries(query, fallback)
            .map { list ->
                list.map { entity ->
                    Series(
                        id = entity.seriesId,
                        name = entity.name,
                        coverUrl = entity.coverUrl,
                        categoryId = entity.categoryId,
                        rating = entity.rating,
                        genre = entity.genre
                    )
                }
            }
    }

    override suspend fun syncSeriesCategories(): Result<Unit> = runCatching {
        android.util.Log.d("FastIPTV", "syncSeriesCategories starting...")
        val dtos = api.getSeriesCategories()
        android.util.Log.d("FastIPTV", "syncSeriesCategories fetched ${dtos.size} categories")
        val entities = dtos.map { dto ->
            CategoryEntity(
                categoryId = dto.categoryId,
                name = dto.categoryName,
                type = CategoryType.SERIES.name
            )
        }
        database.withTransaction {
            categoryDao.deleteByType(CategoryType.SERIES.name)
            categoryDao.upsertAll(entities)
        }
        android.util.Log.d("FastIPTV", "syncSeriesCategories saved ${entities.size} categories to DB")
        Unit
    }.onFailure {
        android.util.Log.e("FastIPTV", "syncSeriesCategories error: ${it.message}", it)
    }

    override suspend fun syncSeries(categoryId: String): Result<Unit> = runCatching {
        android.util.Log.d("FastIPTV", "syncSeries starting for category $categoryId")
        val dtos = api.getSeries(categoryId = categoryId)
        android.util.Log.d("FastIPTV", "syncSeries fetched ${dtos.size} series for category $categoryId")
        val entities = dtos.map { dto ->
            SeriesEntity(
                seriesId = dto.seriesId,
                name = dto.name,
                coverUrl = dto.cover,
                categoryId = dto.categoryId ?: categoryId,
                rating = dto.rating,
                genre = dto.genre
            )
        }
        database.withTransaction {
            seriesDao.deleteByCategory(categoryId)
            seriesDao.upsertAll(entities)
        }
        android.util.Log.d("FastIPTV", "syncSeries saved ${entities.size} series for category $categoryId to DB")
        Unit
    }.onFailure {
        android.util.Log.e("FastIPTV", "syncSeries error: ${it.message}", it)
    }

    override suspend fun getSeriesInfo(seriesId: Int): Result<SeriesDetail> = runCatching {
        android.util.Log.d("FastIPTV", "getSeriesInfo START seriesId=$seriesId")
        val dto = api.getSeriesInfo(seriesId = seriesId)
        android.util.Log.d("FastIPTV", "getSeriesInfo fetched dto: seasons=${dto.seasons?.size}, episodes=${dto.episodes?.size}")
        val seasons = dto.seasons?.map { s ->
            SeriesSeason(
                id = s.id,
                name = s.name ?: "Season ${s.seasonNumber ?: 1}",
                seasonNumber = s.seasonNumber ?: 1,
                episodeCount = s.episodeCount ?: 0,
                coverUrl = s.cover
            )
        } ?: emptyList()

        val episodesMap = dto.episodes?.mapValues { (_, epList) ->
            epList.map { ep ->
                SeriesEpisode(
                    id = ep.id ?: "",
                    episodeNum = ep.episodeNum ?: 1,
                    title = ep.title ?: "Episode ${ep.episodeNum ?: 1}",
                    containerExt = ep.containerExtension ?: "mp4"
                )
            }
        } ?: emptyMap()

        SeriesDetail(seasons = seasons, episodes = episodesMap)
    }.onFailure {
        android.util.Log.e("FastIPTV", "getSeriesInfo FAILED for seriesId=$seriesId", it)
    }

    override fun searchCategories(query: String): Flow<List<Category>> {
        val fallback = getSearchFallback(query)
        return categoryDao.searchCategories(query, fallback)
            .map { list ->
                list.map { entity ->
                    Category(
                        id = entity.categoryId,
                        name = entity.name,
                        type = try { CategoryType.valueOf(entity.type) } catch (_: Exception) { CategoryType.LIVE }
                    )
                }
            }
    }

    override suspend fun syncAllLiveChannels(): Result<Unit> = runCatching {
        android.util.Log.d("FastIPTV", "syncAllLiveChannels starting...")
        val dtos = api.getLiveStreams(categoryId = null)
        android.util.Log.d("FastIPTV", "syncAllLiveChannels fetched ${dtos.size} channels")
        val entities = dtos.map { dto ->
            ChannelEntity(
                streamId = dto.streamId,
                name = dto.name,
                iconUrl = dto.streamIcon,
                categoryId = dto.categoryId ?: "0",
                epgChannelId = dto.epgChannelId,
                streamType = dto.streamType ?: "live"
            )
        }
        entities.chunked(1000).forEachIndexed { index, chunk ->
            database.withTransaction {
                channelDao.upsertAll(chunk)
            }
            android.util.Log.d("FastIPTV", "syncAllLiveChannels inserted chunk $index (${chunk.size} items)")
        }
        android.util.Log.d("FastIPTV", "syncAllLiveChannels saved ${entities.size} channels to DB")
        Unit
    }.onFailure {
        android.util.Log.e("FastIPTV", "syncAllLiveChannels error: ${it.message}", it)
    }

    override suspend fun syncAllMovies(): Result<Unit> = runCatching {
        android.util.Log.d("FastIPTV", "syncAllMovies starting...")
        val dtos = api.getVodStreams(categoryId = null)
        android.util.Log.d("FastIPTV", "syncAllMovies fetched ${dtos.size} movies")
        val entities = dtos.map { dto ->
            VodEntity(
                streamId = dto.streamId,
                name = dto.name,
                posterUrl = dto.streamIcon,
                categoryId = dto.categoryId ?: "0",
                containerExt = dto.containerExtension ?: "mp4",
                rating = dto.rating
            )
        }
        entities.chunked(1000).forEachIndexed { index, chunk ->
            database.withTransaction {
                vodDao.upsertAll(chunk)
            }
        }
        android.util.Log.d("FastIPTV", "syncAllMovies saved ${entities.size} movies to DB")
        Unit
    }.onFailure {
        android.util.Log.e("FastIPTV", "syncAllMovies error: ${it.message}", it)
    }

    override suspend fun syncAllSeries(): Result<Unit> = runCatching {
        android.util.Log.d("FastIPTV", "syncAllSeries starting...")
        val dtos = api.getSeries(categoryId = null)
        android.util.Log.d("FastIPTV", "syncAllSeries fetched ${dtos.size} series")
        val entities = dtos.map { dto ->
            SeriesEntity(
                seriesId = dto.seriesId,
                name = dto.name,
                coverUrl = dto.cover,
                categoryId = dto.categoryId ?: "0",
                rating = dto.rating,
                genre = dto.genre
            )
        }
        entities.chunked(1000).forEachIndexed { index, chunk ->
            database.withTransaction {
                seriesDao.upsertAll(chunk)
            }
        }
        android.util.Log.d("FastIPTV", "syncAllSeries saved ${entities.size} series to DB")
        Unit
    }.onFailure {
        android.util.Log.e("FastIPTV", "syncAllSeries error: ${it.message}", it)
    }

    override suspend fun runFullBackgroundSync(): Result<Unit> = runCatching {
        android.util.Log.d("FastIPTV", "runFullBackgroundSync started")
        // Phase 1: Categories
        syncLiveCategories()
        syncVodCategories()
        syncSeriesCategories()

        // Phase 2: Live Channels (Immediate priority for sports/TV search)
        syncAllLiveChannels()

        // Phase 3: TV Series
        syncAllSeries()

        // Phase 4: Movies
        syncAllMovies()
        android.util.Log.d("FastIPTV", "runFullBackgroundSync completed successfully")
        Unit
    }.onFailure {
        android.util.Log.e("FastIPTV", "runFullBackgroundSync error: ${it.message}", it)
    }

    private fun getSearchFallback(query: String): String {
        val q = query.lowercase().trim()
        return when {
            q.contains("cricket") -> "cric"
            q == "cric" -> "cricket"
            q.contains("football") -> "foot"
            q == "foot" -> "football"
            q.contains("soccer") -> "socc"
            q == "socc" -> "soccer"
            q == "f1" -> "formula"
            q == "formula" -> "f1"
            q == "nba" -> "basketball"
            q.contains("basketball") -> "nba"
            else -> ""
        }
    }

    // --- Helper Functions ---
    private fun parseEpgTime(timeStr: String): Long {
        timeStr.toLongOrNull()?.let { return it }
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            (format.parse(timeStr)?.time ?: 0L) / 1000
        } catch (_: Exception) {
            0L
        }
    }

    private fun decodeEpgText(text: String): String {
        if (text.length >= 8 && text.length % 4 == 0 && text.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }) {
            try {
                val decoded = java.util.Base64.getDecoder().decode(text)
                val str = String(decoded, Charsets.UTF_8)
                if (str.isNotBlank() && str.all { it.code in 32..126 || it.code > 127 || it == '\n' || it == '\r' }) {
                    return str
                }
            } catch (_: Exception) {}
        }
        return text
    }
}
