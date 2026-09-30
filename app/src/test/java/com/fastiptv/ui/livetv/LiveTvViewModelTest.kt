package com.fastiptv.ui.livetv

import com.fastiptv.data.session.SessionManager
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.CategoryType
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.repository.IptvRepository
import com.fastiptv.player.StreamPlayerController
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: IptvRepository = mockk(relaxed = true)
    private val streamPlayer: StreamPlayerController = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    private val mockCategories = listOf(
        Category("1", "INDIA | HINDI", CategoryType.LIVE),
        Category("2", "INDIA | GUJARATI", CategoryType.LIVE),
        Category("3", "INDIA | SPORTS", CategoryType.LIVE)
    )

    private val mockChannels = listOf(
        Channel(101, "Star Plus HD", "http://logo/star.png", "1", "star.in"),
        Channel(102, "Sony TV HD", "http://logo/sony.png", "1", "sony.in"),
        Channel(103, "Colors HD", "http://logo/colors.png", "1", "colors.in")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeLiveCategories() } returns flowOf(mockCategories)
        every { repository.observeChannelsByCategory("1") } returns flowOf(mockChannels)
        every { repository.observeRecents(any()) } returns flowOf(emptyList())
        every { streamPlayer.playerState } returns MutableStateFlow(com.fastiptv.player.PlayerState.Idle)
        every { sessionManager.getCachedStreamFormat() } returns "ts"
        coEvery { repository.getShortEpg(any()) } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tuneToChannel plays stream and shows banner`() = runTest {
        val viewModel = LiveTvViewModel(repository, streamPlayer, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        val channel = mockChannels[0]
        viewModel.tuneToChannel(channel)

        verify { streamPlayer.playLiveStream(101, "ts", "Star Plus HD") }
        assertEquals(channel, viewModel.currentChannel.value)
        assertTrue(viewModel.isBannerVisible.value)
    }

    @Test
    fun `nextChannel cycles forward through channels`() = runTest {
        val viewModel = LiveTvViewModel(repository, streamPlayer, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.tuneToChannel(mockChannels[0])
        viewModel.nextChannel()

        assertEquals(mockChannels[1], viewModel.currentChannel.value)
        verify { streamPlayer.playLiveStream(102, "ts", "Sony TV HD") }
    }

    @Test
    fun `previousChannel cycles backward through channels`() = runTest {
        val viewModel = LiveTvViewModel(repository, streamPlayer, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.tuneToChannel(mockChannels[0])
        viewModel.previousChannel()

        assertEquals(mockChannels[2], viewModel.currentChannel.value)
        verify { streamPlayer.playLiveStream(103, "ts", "Colors HD") }
    }

    @Test
    fun `drawer opens and closes properly`() = runTest {
        val viewModel = LiveTvViewModel(repository, streamPlayer, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openDrawer()
        assertTrue(viewModel.isDrawerOpen.value)

        viewModel.closeDrawer()
        assertFalse(viewModel.isDrawerOpen.value)
    }

    @Test
    fun `selectLanguageGroup filters to matching category`() = runTest {
        val viewModel = LiveTvViewModel(repository, streamPlayer, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectLanguageGroup("GUJARATI")
        assertEquals("2", viewModel.selectedCategory.value?.id)

        viewModel.selectLanguageGroup("SPORTS")
        assertEquals("3", viewModel.selectedCategory.value?.id)
    }
}
