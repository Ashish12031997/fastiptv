package com.fastiptv.domain.repository

import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.model.Movie
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.domain.model.Series
import com.fastiptv.domain.model.SeriesDetail
import kotlinx.coroutines.flow.Flow

interface IptvRepository {
    // Live Channels
    fun observeLiveCategories(): Flow<List<Category>>
    fun observeChannelsByCategory(categoryId: String): Flow<List<Channel>>
    fun observeFavorites(): Flow<List<Channel>>
    fun observeRecents(limit: Int = 20): Flow<List<RecentItem>>
    fun searchChannels(query: String): Flow<List<Channel>>

    suspend fun getChannelById(streamId: Int): Channel?
    suspend fun getRecentByStreamId(streamId: Int): RecentItem?
    suspend fun updatePlaybackPosition(streamId: Int, positionMs: Long, durationMs: Long): Result<Unit>
    suspend fun getShortEpg(streamId: Int): Result<List<EpgProgram>>
    suspend fun recordRecent(recent: RecentItem): Result<Unit>
    suspend fun syncLiveCategories(): Result<Unit>
    suspend fun syncChannels(categoryId: String): Result<Unit>
    suspend fun toggleFavorite(channel: Channel): Result<Boolean>

    // VOD Movies
    fun observeVodCategories(): Flow<List<Category>>
    fun observeMoviesByCategory(categoryId: String): Flow<List<Movie>>
    fun observeFavoriteMovies(): Flow<List<Movie>> = kotlinx.coroutines.flow.emptyFlow()
    fun searchMovies(query: String): Flow<List<Movie>>
    suspend fun syncVodCategories(): Result<Unit>
    suspend fun syncMovies(categoryId: String): Result<Unit>
    suspend fun toggleFavoriteMovie(movie: Movie): Result<Boolean> = Result.success(false)

    // TV Series
    fun observeSeriesCategories(): Flow<List<Category>>
    fun observeSeriesByCategory(categoryId: String): Flow<List<Series>>
    fun searchSeries(query: String): Flow<List<Series>>
    suspend fun syncSeriesCategories(): Result<Unit>
    suspend fun syncSeries(categoryId: String): Result<Unit>
    suspend fun getSeriesInfo(seriesId: Int): Result<SeriesDetail>

    // Cache / Maintenance
    fun searchCategories(query: String): Flow<List<Category>> = kotlinx.coroutines.flow.emptyFlow()
    suspend fun syncAllLiveChannels(): Result<Unit> = Result.success(Unit)
    suspend fun syncAllMovies(): Result<Unit> = Result.success(Unit)
    suspend fun syncAllSeries(): Result<Unit> = Result.success(Unit)
    suspend fun runFullBackgroundSync(): Result<Unit> = Result.success(Unit)
    suspend fun clearCache(): Result<Unit> = Result.success(Unit)
}
