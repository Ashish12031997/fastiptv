package com.fastiptv.ui.favorites

import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.model.Movie
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.domain.model.Series
import com.fastiptv.domain.model.SeriesDetail
import com.fastiptv.domain.repository.IptvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeFavoritesIptvRepository
    private lateinit var viewModel: FavoritesViewModel

    class FakeFavoritesIptvRepository : IptvRepository {
        val favoriteChannelsFlow = MutableStateFlow<List<Channel>>(emptyList())
        val favoriteMoviesFlow = MutableStateFlow<List<Movie>>(emptyList())
        val toggledChannels = mutableListOf<Channel>()
        val toggledMovies = mutableListOf<Movie>()

        override fun observeFavorites(): Flow<List<Channel>> = favoriteChannelsFlow.asStateFlow()
        override fun observeFavoriteMovies(): Flow<List<Movie>> = favoriteMoviesFlow.asStateFlow()

        override suspend fun toggleFavorite(channel: Channel): Result<Boolean> {
            toggledChannels.add(channel)
            return Result.success(true)
        }

        override suspend fun toggleFavoriteMovie(movie: Movie): Result<Boolean> {
            toggledMovies.add(movie)
            return Result.success(true)
        }

        override fun observeLiveCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeChannelsByCategory(categoryId: String): Flow<List<Channel>> = flowOf(emptyList())
        override fun observeRecents(limit: Int): Flow<List<RecentItem>> = flowOf(emptyList())
        override fun searchChannels(query: String): Flow<List<Channel>> = flowOf(emptyList())
        override suspend fun getChannelById(streamId: Int): Channel? = null
        override suspend fun getRecentByStreamId(streamId: Int): RecentItem? = null
        override suspend fun updatePlaybackPosition(streamId: Int, positionMs: Long, durationMs: Long): Result<Unit> = Result.success(Unit)
        override suspend fun getShortEpg(streamId: Int): Result<List<EpgProgram>> = Result.success(emptyList())
        override suspend fun recordRecent(recent: RecentItem): Result<Unit> = Result.success(Unit)
        override suspend fun syncLiveCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncChannels(categoryId: String): Result<Unit> = Result.success(Unit)
        override fun observeVodCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeMoviesByCategory(categoryId: String): Flow<List<Movie>> = flowOf(emptyList())
        override fun searchMovies(query: String): Flow<List<Movie>> = flowOf(emptyList())
        override suspend fun syncVodCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncMovies(categoryId: String): Result<Unit> = Result.success(Unit)
        override fun observeSeriesCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeSeriesByCategory(categoryId: String): Flow<List<Series>> = flowOf(emptyList())
        override fun searchSeries(query: String): Flow<List<Series>> = flowOf(emptyList())
        override suspend fun syncSeriesCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncSeries(categoryId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getSeriesInfo(seriesId: Int): Result<SeriesDetail> = Result.success(SeriesDetail(emptyList(), emptyMap()))
        override suspend fun clearCache(): Result<Unit> = Result.success(Unit)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeFavoritesIptvRepository()
        viewModel = FavoritesViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsFavoriteChannelsAndMovies() = runTest {
        val testChannels = listOf(
            Channel(id = 101, name = "Sky News HD", logoUrl = null, categoryId = "1", epgChannelId = null, isFavorite = true),
            Channel(id = 102, name = "BBC One HD", logoUrl = null, categoryId = "1", epgChannelId = null, isFavorite = true)
        )
        val testMovies = listOf(
            Movie(id = 501, name = "Inception", posterUrl = null, categoryId = "2", containerExt = "mp4", rating = "8.8", isFavorite = true)
        )

        fakeRepository.favoriteChannelsFlow.value = testChannels
        fakeRepository.favoriteMoviesFlow.value = testMovies

        // Start collecting uiState
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FavoriteTab.CHANNELS, state.selectedTab)
        assertEquals(2, state.channels.size)
        assertEquals("Sky News HD", state.channels[0].name)
        assertEquals(1, state.movies.size)
        assertEquals("Inception", state.movies[0].name)

        job.cancel()
    }

    @Test
    fun selectTab_switchesBetweenChannelsAndMovies() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(FavoriteTab.CHANNELS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(FavoriteTab.MOVIES)
        advanceUntilIdle()
        assertEquals(FavoriteTab.MOVIES, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(FavoriteTab.CHANNELS)
        advanceUntilIdle()
        assertEquals(FavoriteTab.CHANNELS, viewModel.uiState.value.selectedTab)

        job.cancel()
    }

    @Test
    fun toggleChannelFavorite_delegatesToRepository() = runTest {
        val channel = Channel(id = 201, name = "CNN International", logoUrl = null, categoryId = "1", epgChannelId = null)

        viewModel.toggleChannelFavorite(channel)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.toggledChannels.size)
        assertEquals(201, fakeRepository.toggledChannels[0].id)
    }

    @Test
    fun toggleMovieFavorite_delegatesToRepository() = runTest {
        val movie = Movie(id = 301, name = "Interstellar", posterUrl = null, categoryId = "2", containerExt = "mkv", rating = "8.7")

        viewModel.toggleMovieFavorite(movie)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.toggledMovies.size)
        assertEquals(301, fakeRepository.toggledMovies[0].id)
    }
}
