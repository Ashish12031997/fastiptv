package com.fastiptv.ui.epg

import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.CategoryType
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.repository.IptvRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpgGridViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: IptvRepository
    private lateinit var viewModel: EpgGridViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        every { repository.observeLiveCategories() } returns flowOf(
            listOf(Category("1", "News", CategoryType.LIVE))
        )
        every { repository.observeChannelsByCategory(any()) } returns flowOf(
            listOf(Channel(101, "CNN", null, "1", null))
        )
        io.mockk.coEvery { repository.getShortEpg(any()) } returns Result.success(emptyList())

        viewModel = EpgGridViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSelectAndDismissProgramDetail() = runTest(testDispatcher) {
        val channel = Channel(101, "CNN", null, "1", null)
        val program = EpgProgram("p1", "Newsroom Live", "Breaking news", 1000L, 2000L)

        assertNull(viewModel.selectedProgram.value)

        viewModel.selectProgram(channel, program)

        val selected = viewModel.selectedProgram.value
        assertNotNull(selected)
        assertEquals(channel.id, selected!!.first.id)
        assertEquals(program.id, selected.second.id)
        assertEquals("Newsroom Live", selected.second.title)

        viewModel.dismissProgramDetail()

        assertNull(viewModel.selectedProgram.value)
    }

    @Test
    fun testObservesLiveCategoriesAndSelectsFirst() = runTest(testDispatcher) {
        val categories = viewModel.categories.first { it.isNotEmpty() }
        assertEquals(1, categories.size)
        assertEquals("News", categories[0].name)
    }
}
