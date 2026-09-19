package com.fastiptv.ui.home

import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.CategoryType
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.domain.repository.IptvRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: IptvRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        every { repository.observeLiveCategories() } returns flowOf(
            listOf(Category("1", "News", CategoryType.LIVE))
        )
        every { repository.observeFavorites() } returns flowOf(
            listOf(Channel(101, "BBC News", null, "1", null, isFavorite = true))
        )
        every { repository.observeRecents(any()) } returns flowOf(
            listOf(RecentItem(101, "live", "BBC News", null, 1000L))
        )
        every { repository.observeChannelsByCategory(any()) } returns flowOf(emptyList())
        coEvery { repository.getShortEpg(any()) } returns Result.success(emptyList())

        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testObservesCategoriesAndFavoritesAndRecents() = runTest(testDispatcher) {
        val categories = viewModel.categories.first { it.isNotEmpty() }
        assertEquals(1, categories.size)
        assertEquals("News", categories[0].name)

        val favorites = viewModel.favorites.first { it.isNotEmpty() }
        assertEquals(1, favorites.size)
        assertEquals("BBC News", favorites[0].name)

        val recents = viewModel.recents.first { it.isNotEmpty() }
        assertEquals(1, recents.size)
        assertEquals("BBC News", recents[0].title)
    }

    @Test
    fun testGetChannelsForCategoryDelegatesToRepository() = runTest(testDispatcher) {
        val expectedChannels = listOf(Channel(201, "CNN", null, "1", null))
        every { repository.observeChannelsByCategory("1") } returns flowOf(expectedChannels)

        val channels = viewModel.getChannelsForCategory("1").first()
        assertEquals(1, channels.size)
        assertEquals("CNN", channels[0].name)
    }
}
