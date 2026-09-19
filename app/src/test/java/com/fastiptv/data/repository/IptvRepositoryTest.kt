package com.fastiptv.data.repository

import androidx.room.withTransaction
import com.fastiptv.data.api.XtreamApi
import com.fastiptv.data.db.AppDatabase
import com.fastiptv.data.db.dao.CategoryDao
import com.fastiptv.data.db.dao.ChannelDao
import com.fastiptv.data.db.dao.ChannelWithFavorite
import com.fastiptv.data.db.dao.FavoriteDao
import com.fastiptv.data.db.dao.RecentDao
import com.fastiptv.data.db.dao.SeriesDao
import com.fastiptv.data.db.dao.VodDao
import com.fastiptv.data.db.entity.RecentEntity
import com.fastiptv.data.model.CategoryDto
import com.fastiptv.data.model.LiveStreamDto
import com.fastiptv.domain.model.CategoryType
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.RecentItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IptvRepositoryTest {

    private lateinit var api: XtreamApi
    private lateinit var database: AppDatabase
    private lateinit var channelDao: ChannelDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var recentDao: RecentDao
    private lateinit var vodDao: VodDao
    private lateinit var seriesDao: SeriesDao
    private lateinit var repository: IptvRepositoryImpl

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        database = mockk(relaxed = true)
        channelDao = mockk(relaxed = true)
        categoryDao = mockk(relaxed = true)
        favoriteDao = mockk(relaxed = true)
        recentDao = mockk(relaxed = true)
        vodDao = mockk(relaxed = true)
        seriesDao = mockk(relaxed = true)

        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction(any<suspend () -> Any>()) } coAnswers {
            secondArg<suspend () -> Any>().invoke()
        }

        repository = IptvRepositoryImpl(
            api = api,
            database = database,
            channelDao = channelDao,
            categoryDao = categoryDao,
            favoriteDao = favoriteDao,
            recentDao = recentDao,
            vodDao = vodDao,
            seriesDao = seriesDao
        )
    }

    @Test
    fun testObserveChannelsByCategoryMapsDomainAndEmits() = runTest {
        val cached = listOf(
            ChannelWithFavorite(
                streamId = 101,
                name = "BBC One",
                iconUrl = "http://logo.png",
                categoryId = "1",
                epgChannelId = "bbc1",
                streamType = "live",
                isFavorite = true
            )
        )
        every { channelDao.observeChannelsByCategory("1") } returns flowOf(cached)

        val channels = repository.observeChannelsByCategory("1").first()
        assertEquals(1, channels.size)
        assertEquals(101, channels[0].id)
        assertEquals("BBC One", channels[0].name)
        assertTrue(channels[0].isFavorite)
    }

    @Test
    fun testObserveRecentsMapsDomainAndEmits() = runTest {
        val cached = listOf(
            RecentEntity(
                streamId = 101,
                type = "live",
                title = "BBC One",
                iconUrl = "http://logo.png",
                lastWatched = 1000L
            )
        )
        every { recentDao.observeRecent(any()) } returns flowOf(cached)

        val recents = repository.observeRecents().first()
        assertEquals(1, recents.size)
        assertEquals(101, recents[0].streamId)
        assertEquals("BBC One", recents[0].title)
    }

    @Test
    fun testRecordRecentCallsDaoInsert() = runTest {
        val item = RecentItem(
            streamId = 101,
            type = "live",
            title = "BBC One",
            iconUrl = null,
            lastWatched = 5000L
        )
        val result = repository.recordRecent(item)
        assertTrue(result.isSuccess)
        coVerify { recentDao.insertRecent(match { it.streamId == 101 && it.title == "BBC One" }) }
    }

    @Test
    fun testSyncChannelsReconcilesAndPurgesOrphans() = runTest {
        val remoteStreams = listOf(
            LiveStreamDto(
                streamId = 201,
                name = "CNN News",
                streamIcon = "http://cnn.png",
                categoryId = "1"
            )
        )
        coEvery { api.getLiveStreams(any(), "1") } returns remoteStreams

        val result = repository.syncChannels("1")
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: AssertionError("Unknown error")
        }
        assertTrue(result.isSuccess)

        coVerify { channelDao.deleteByCategory("1") }
        coVerify { channelDao.upsertAll(any()) }
    }

    @Test
    fun testToggleFavoriteInsertsWhenNotFavorite() = runTest {
        coEvery { favoriteDao.isFavorite(301) } returns false

        val channel = Channel(301, "ESPN", null, "1", null, isFavorite = false)
        val result = repository.toggleFavorite(channel)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        coVerify { favoriteDao.insert(any()) }
    }

    @Test
    fun testToggleFavoriteDeletesWhenAlreadyFavorite() = runTest {
        coEvery { favoriteDao.isFavorite(301) } returns true

        val channel = Channel(301, "ESPN", null, "1", null, isFavorite = true)
        val result = repository.toggleFavorite(channel)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
        coVerify { favoriteDao.delete(301) }
    }

    @Test
    fun testGetChannelByIdQueriesDao() = runTest {
        val channel = ChannelWithFavorite(
            streamId = 501,
            name = "Sky Sports",
            iconUrl = null,
            categoryId = "sports",
            epgChannelId = null,
            streamType = "live",
            isFavorite = true
        )
        coEvery { channelDao.getChannelById(501) } returns channel

        val result = repository.getChannelById(501)
        assertEquals("Sky Sports", result?.name)
        assertTrue(result?.isFavorite == true)
    }

    @Test
    fun testGetShortEpgParsesListings() = runTest {
        val shortEpgDto = com.fastiptv.data.model.ShortEpgDto(
            epgListings = listOf(
                com.fastiptv.data.model.EpgListingDto(
                    id = "prog1",
                    title = "Premier League Live",
                    start = "2024-01-01 12:00:00",
                    end = "2024-01-01 14:00:00",
                    description = "Live match",
                    nowPlaying = 1
                )
            )
        )
        coEvery { api.getShortEpg(any(), 501) } returns shortEpgDto

        val result = repository.getShortEpg(501)
        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertEquals(1, list?.size)
        assertEquals("Premier League Live", list?.get(0)?.title)
        assertTrue(list?.get(0)?.isNowPlaying == true)
    }
}
