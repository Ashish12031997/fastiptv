package com.fastiptv.ui.search

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSearchIptvRepository
    private lateinit var viewModel: SearchViewModel

    class FakeSearchIptvRepository : IptvRepository {
        var seriesDetailToReturn: SeriesDetail? = null

        override fun searchChannels(query: String): Flow<List<Channel>> {
            return if (query == "Sky") {
                flowOf(listOf(Channel(1, "Sky Sport 1", null, "1", null)))
            } else flowOf(emptyList())
        }

        override fun searchMovies(query: String): Flow<List<Movie>> {
            return if (query == "Sky") {
                flowOf(listOf(Movie(10, "Skyfall", null, "2", "mp4", "8.5")))
            } else flowOf(emptyList())
        }

        override fun searchSeries(query: String): Flow<List<Series>> {
            return if (query == "Sky") {
                flowOf(listOf(Series(20, "Sky Rojo", null, "3", "7.0", "Action")))
            } else flowOf(emptyList())
        }

        override suspend fun getSeriesInfo(seriesId: Int): Result<SeriesDetail> {
            val detail = seriesDetailToReturn ?: SeriesDetail(emptyList(), emptyMap())
            return Result.success(detail)
        }

        override fun observeLiveCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeChannelsByCategory(categoryId: String): Flow<List<Channel>> = flowOf(emptyList())
        override fun observeFavorites(): Flow<List<Channel>> = flowOf(emptyList())
        override fun observeRecents(limit: Int): Flow<List<RecentItem>> = flowOf(emptyList())
        override suspend fun getChannelById(streamId: Int): Channel? = null
        override suspend fun getRecentByStreamId(streamId: Int): RecentItem? = null
        override suspend fun updatePlaybackPosition(streamId: Int, positionMs: Long, durationMs: Long): Result<Unit> = Result.success(Unit)
        override suspend fun getShortEpg(streamId: Int): Result<List<EpgProgram>> = Result.success(emptyList())
        override suspend fun recordRecent(recent: RecentItem): Result<Unit> = Result.success(Unit)
        override suspend fun syncLiveCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncChannels(categoryId: String): Result<Unit> = Result.success(Unit)
        override suspend fun toggleFavorite(channel: Channel): Result<Boolean> = Result.success(true)

        override fun observeVodCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeMoviesByCategory(categoryId: String): Flow<List<Movie>> = flowOf(emptyList())
        override suspend fun syncVodCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncMovies(categoryId: String): Result<Unit> = Result.success(Unit)

        override fun observeSeriesCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeSeriesByCategory(categoryId: String): Flow<List<Series>> = flowOf(emptyList())
        override suspend fun syncSeriesCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncSeries(categoryId: String): Result<Unit> = Result.success(Unit)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSearchIptvRepository()
        viewModel = SearchViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsEmpty() = runTest(testDispatcher) {
        assertEquals("", viewModel.query.value)
        assertEquals(SearchFilter.ALL, viewModel.selectedFilter.value)
        assertEquals(emptyList<Channel>(), viewModel.channels.value)
        assertEquals(emptyList<Movie>(), viewModel.movies.value)
        assertEquals(emptyList<Series>(), viewModel.series.value)
    }

    @Test
    fun testFilterSelection() = runTest(testDispatcher) {
        viewModel.onFilterSelected(SearchFilter.MOVIES)
        assertEquals(SearchFilter.MOVIES, viewModel.selectedFilter.value)

        viewModel.onFilterSelected(SearchFilter.CHANNELS)
        assertEquals(SearchFilter.CHANNELS, viewModel.selectedFilter.value)
    }

    @Test
    fun testQuerySearchDebounced() = runTest(testDispatcher) {
        viewModel.onQueryChanged("Sky")
        advanceTimeBy(300)

        val channels = viewModel.channels.first { it.isNotEmpty() }
        assertEquals(1, channels.size)
        assertEquals("Sky Sport 1", channels[0].name)

        val movies = viewModel.movies.first { it.isNotEmpty() }
        assertEquals(1, movies.size)
        assertEquals("Skyfall", movies[0].name)

        val series = viewModel.series.first { it.isNotEmpty() }
        assertEquals(1, series.size)
        assertEquals("Sky Rojo", series[0].name)
    }

    @Test
    fun testClearQueryResetsQuery() = runTest(testDispatcher) {
        viewModel.onQueryChanged("Sky")
        assertEquals("Sky", viewModel.query.value)
        viewModel.clearQuery()
        assertEquals("", viewModel.query.value)
    }

    @Test
    fun testSelectSeriesLoadsDetail() = runTest(testDispatcher) {
        val series = Series(20, "Sky Rojo", null, "3", "7.0", "Action")
        val expectedDetail = SeriesDetail(
            seasons = emptyList(),
            episodes = emptyMap()
        )
        fakeRepository.seriesDetailToReturn = expectedDetail

        viewModel.selectSeries(series)
        assertEquals(series, viewModel.selectedSeries.value)

        advanceUntilIdle()
        assertEquals(expectedDetail, viewModel.seriesDetail.value)

        viewModel.closeSeriesDetail()
        assertNull(viewModel.selectedSeries.value)
        assertNull(viewModel.seriesDetail.value)
    }
}
