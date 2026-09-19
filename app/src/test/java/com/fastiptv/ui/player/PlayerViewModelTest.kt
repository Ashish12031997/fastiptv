package com.fastiptv.ui.player

import androidx.media3.common.Player
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.model.Movie
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.domain.model.Series
import com.fastiptv.domain.model.SeriesDetail
import com.fastiptv.domain.model.SeriesEpisode
import com.fastiptv.domain.model.SeriesSeason
import com.fastiptv.domain.repository.IptvRepository
import com.fastiptv.player.PlayerState
import com.fastiptv.player.StreamPlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var streamPlayer: FakeStreamPlayer
    private lateinit var repository: FakeIptvRepository
    private lateinit var viewModel: PlayerViewModel

    class FakeStreamPlayer : StreamPlayerController {
        override val playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
        override var currentPosition: Long = 0L
        override var duration: Long = 0L
        override var isPlaying: Boolean = true
        override var isSeekable: Boolean = true
        override val isFirstFrameRendered = MutableStateFlow(false)

        var lastPlayedLiveStreamId: Int? = null
        var lastPlayedLiveFormat: String? = null
        var lastPlayedLiveTitle: String? = null

        var lastPlayedVodStreamId: Int? = null
        var lastPlayedVodExt: String? = null
        var lastPlayedVodTitle: String? = null
        var lastPlayedVodStartPos: Long? = null

        var lastPlayedSeriesEpisodeId: Int? = null
        var lastPlayedSeriesExt: String? = null
        var lastPlayedSeriesTitle: String? = null
        var lastPlayedSeriesStartPos: Long? = null

        var lastSeekPos: Long? = null
        var seekCount: Int = 0
        var playCount: Int = 0
        var pauseCount: Int = 0
        var stopCount: Int = 0

        override fun getPlayer(): Player {
            throw UnsupportedOperationException("Not needed in unit tests")
        }

        override fun playLiveStream(streamId: Int, format: String, title: String?) {
            lastPlayedLiveStreamId = streamId
            lastPlayedLiveFormat = format
            lastPlayedLiveTitle = title
        }

        override fun playVod(streamId: Int, containerExt: String, title: String?, startPositionMs: Long) {
            lastPlayedVodStreamId = streamId
            lastPlayedVodExt = containerExt
            lastPlayedVodTitle = title
            lastPlayedVodStartPos = startPositionMs
        }

        override fun playSeriesEpisode(episodeId: Int, containerExt: String, title: String?, startPositionMs: Long) {
            lastPlayedSeriesEpisodeId = episodeId
            lastPlayedSeriesExt = containerExt
            lastPlayedSeriesTitle = title
            lastPlayedSeriesStartPos = startPositionMs
        }

        override fun seekTo(positionMs: Long) {
            seekCount++
            lastSeekPos = positionMs
            currentPosition = positionMs
        }

        override fun play() {
            playCount++
            isPlaying = true
        }

        override fun pause() {
            pauseCount++
            isPlaying = false
        }

        override fun resume() {
            play()
        }

        override fun stop() {
            stopCount++
            isPlaying = false
        }

        override fun release() {}
    }

    class FakeIptvRepository : IptvRepository {
        var channelByIdMap = mutableMapOf<Int, Channel>()
        var categoryChannelsMap = mutableMapOf<String, List<Channel>>()
        var recordedRecents = mutableListOf<RecentItem>()
        var favoriteToggleResult: Boolean = true

        override fun observeLiveCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeChannelsByCategory(categoryId: String): Flow<List<Channel>> =
            flowOf(categoryChannelsMap[categoryId] ?: emptyList())
        override fun observeFavorites(): Flow<List<Channel>> = flowOf(emptyList())
        override fun observeRecents(limit: Int): Flow<List<RecentItem>> = flowOf(emptyList())
        override fun searchChannels(query: String): Flow<List<Channel>> = flowOf(emptyList())

        var recentsByStreamIdMap = mutableMapOf<Int, RecentItem>()
        var updatedPositions = mutableListOf<Triple<Int, Long, Long>>()

        override suspend fun getChannelById(streamId: Int): Channel? = channelByIdMap[streamId]
        override suspend fun getRecentByStreamId(streamId: Int): RecentItem? = recentsByStreamIdMap[streamId]
        override suspend fun updatePlaybackPosition(streamId: Int, positionMs: Long, durationMs: Long): Result<Unit> {
            updatedPositions.add(Triple(streamId, positionMs, durationMs))
            return Result.success(Unit)
        }
        override suspend fun getShortEpg(streamId: Int): Result<List<EpgProgram>> = Result.success(emptyList())
        override suspend fun recordRecent(recent: RecentItem): Result<Unit> {
            recordedRecents.add(recent)
            return Result.success(Unit)
        }
        override suspend fun syncLiveCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncChannels(categoryId: String): Result<Unit> = Result.success(Unit)
        override suspend fun toggleFavorite(channel: Channel): Result<Boolean> = Result.success(favoriteToggleResult)

        override fun observeVodCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeMoviesByCategory(categoryId: String): Flow<List<Movie>> = flowOf(emptyList())
        override fun searchMovies(query: String): Flow<List<Movie>> = flowOf(emptyList())
        override suspend fun syncVodCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncMovies(categoryId: String): Result<Unit> = Result.success(Unit)

        override fun observeSeriesCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeSeriesByCategory(categoryId: String): Flow<List<Series>> = flowOf(emptyList())
        var seriesDetailResult: SeriesDetail? = null

        override fun searchSeries(query: String): Flow<List<Series>> = flowOf(emptyList())
        override suspend fun syncSeriesCategories(): Result<Unit> = Result.success(Unit)
        override suspend fun syncSeries(categoryId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getSeriesInfo(seriesId: Int): Result<SeriesDetail> =
            Result.success(seriesDetailResult ?: SeriesDetail(emptyList(), emptyMap()))
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        streamPlayer = FakeStreamPlayer()
        repository = FakeIptvRepository()

        viewModel = PlayerViewModel(
            streamPlayer = streamPlayer,
            repository = repository
        )
    }

    @After
    fun tearDown() {
        viewModel.stopPlayback()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitChannelPlaysStreamAndRecordsRecent() = runTest(testDispatcher) {
        val channel = Channel(101, "BBC One", null, "1", null)
        repository.channelByIdMap[101] = channel

        viewModel.initChannel(101, "BBC One")
        advanceTimeBy(100L)

        assertEquals(101, streamPlayer.lastPlayedLiveStreamId)
        assertEquals("ts", streamPlayer.lastPlayedLiveFormat)
        assertEquals("BBC One", streamPlayer.lastPlayedLiveTitle)
        assertEquals(1, repository.recordedRecents.size)
        assertEquals(101, repository.recordedRecents[0].streamId)
        assertEquals(101, viewModel.currentChannel.value?.id)
        viewModel.stopPlayback()
    }

    @Test
    fun testChannelSurfingCyclesNextAndPrevious() = runTest(testDispatcher) {
        val ch1 = Channel(101, "BBC One", null, "news", null)
        val ch2 = Channel(102, "BBC Two", null, "news", null)
        repository.channelByIdMap[101] = ch1
        repository.categoryChannelsMap["news"] = listOf(ch1, ch2)

        viewModel.initChannel(101, "BBC One")
        advanceTimeBy(100L)

        // Flip to next
        viewModel.nextChannel()
        assertNotNull(viewModel.channelSwitchNotice.value)
        advanceTimeBy(100L)

        assertEquals(102, viewModel.currentChannel.value?.id)
        assertEquals(102, streamPlayer.lastPlayedLiveStreamId)
        assertEquals("BBC Two", streamPlayer.lastPlayedLiveTitle)

        // Flip to previous
        viewModel.previousChannel()
        advanceTimeBy(100L)

        assertEquals(101, viewModel.currentChannel.value?.id)
        assertEquals(101, streamPlayer.lastPlayedLiveStreamId)
        assertEquals("BBC One", streamPlayer.lastPlayedLiveTitle)
        viewModel.stopPlayback()
    }

    @Test
    fun testToggleFavoriteUpdatesCurrentChannel() = runTest(testDispatcher) {
        val channel = Channel(101, "BBC One", null, null, null, isFavorite = false)
        repository.channelByIdMap[101] = channel
        repository.favoriteToggleResult = true

        viewModel.initChannel(101, "BBC One")
        advanceTimeBy(100L)

        viewModel.toggleFavorite()
        advanceTimeBy(100L)

        assertTrue(viewModel.currentChannel.value?.isFavorite == true)
        viewModel.stopPlayback()
    }

    @Test
    fun testStopPlaybackCallsPlayerStop() {
        viewModel.stopPlayback()
        assertEquals(1, streamPlayer.stopCount)
    }

    @Test
    fun testInitStreamWithVodPlaysVodAndRecordsRecent() = runTest(testDispatcher) {
        viewModel.initStream(streamId = 51303, initialTitle = "Inception", type = "vod", containerExt = "mp4")
        advanceTimeBy(100L)

        assertEquals(51303, streamPlayer.lastPlayedVodStreamId)
        assertEquals("mp4", streamPlayer.lastPlayedVodExt)
        assertEquals("Inception", streamPlayer.lastPlayedVodTitle)
        assertEquals(0L, streamPlayer.lastPlayedVodStartPos)
        assertEquals(1, repository.recordedRecents.size)
        assertEquals(51303, repository.recordedRecents.first().streamId)
        assertEquals("vod", repository.recordedRecents.first().type)
        assertEquals("Inception", repository.recordedRecents.first().title)
        viewModel.stopPlayback()
    }

    @Test
    fun testInitStreamWithSeriesPlaysSeriesAndRecordsRecent() = runTest(testDispatcher) {
        viewModel.initStream(streamId = 30020, initialTitle = "Fauda S01E12", type = "series", containerExt = "mp4")
        advanceTimeBy(100L)

        assertEquals(30020, streamPlayer.lastPlayedSeriesEpisodeId)
        assertEquals("mp4", streamPlayer.lastPlayedSeriesExt)
        assertEquals("Fauda S01E12", streamPlayer.lastPlayedSeriesTitle)
        assertEquals(0L, streamPlayer.lastPlayedSeriesStartPos)
        assertEquals(1, repository.recordedRecents.size)
        assertEquals(30020, repository.recordedRecents.first().streamId)
        assertEquals("series", repository.recordedRecents.first().type)
        viewModel.stopPlayback()
    }

    @Test
    fun testVodStreamResumesFromSavedPosition() = runTest(testDispatcher) {
        val savedRecent = RecentItem(
            streamId = 777,
            type = "vod",
            title = "Gladiator",
            iconUrl = null,
            lastWatched = 1000L,
            watchPositionMs = 45000L,
            durationMs = 120000L
        )
        repository.recentsByStreamIdMap[777] = savedRecent

        viewModel.initStream(streamId = 777, initialTitle = "Gladiator", type = "vod", containerExt = "mp4")
        advanceTimeBy(100L)

        assertEquals(777, streamPlayer.lastPlayedVodStreamId)
        assertEquals(45000L, streamPlayer.lastPlayedVodStartPos)
        viewModel.stopPlayback()
    }

    @Test
    fun testLiveStreamFastForwardGuarded() = runTest(testDispatcher) {
        streamPlayer.isFirstFrameRendered.value = true
        streamPlayer.currentPosition = 50000L
        streamPlayer.duration = 100000L

        viewModel.seekForward(10000L)
        advanceTimeBy(500L)

        // Seeking on live stream must be ignored and not dispatched to streamPlayer
        assertEquals(0, streamPlayer.seekCount)
        assertEquals(null, streamPlayer.lastSeekPos)
    }

    @Test
    fun testSeekForwardAndBackward() = runTest(testDispatcher) {
        viewModel.setStreamType("vod")
        streamPlayer.currentPosition = 50000L
        streamPlayer.duration = 100000L

        viewModel.seekForward(10000L)
        advanceTimeBy(500L)
        assertEquals(60000L, streamPlayer.lastSeekPos)

        viewModel.seekBackward(10000L)
        advanceTimeBy(500L)
        assertEquals(50000L, streamPlayer.lastSeekPos)
    }

    @Test
    fun testRapidSeekingDebouncesAndAccelerates() = runTest(testDispatcher) {
        viewModel.setStreamType("vod")
        streamPlayer.currentPosition = 0L
        streamPlayer.duration = 3600000L // 1 hour

        // Tap 1: step 10s -> pending 10s
        viewModel.seekForward()
        // Tap 2: step 10s -> pending 20s
        viewModel.seekForward()
        // Tap 3: step 10s -> pending 30s
        viewModel.seekForward()
        // Tap 4: accelerated to 30s -> pending 60s
        viewModel.seekForward()

        // Before debounce timer expires, streamPlayer.seekTo should NOT have been called yet
        assertEquals(0, streamPlayer.seekCount)
        assertEquals(60000L, viewModel.currentPositionMs.value)

        // Advance debounce time
        advanceTimeBy(500L)

        // Exactly one seek dispatched to player
        assertEquals(1, streamPlayer.seekCount)
        assertEquals(60000L, streamPlayer.lastSeekPos)
    }

    @Test
    fun testConfirmPendingSeekDispatchesImmediately() = runTest(testDispatcher) {
        viewModel.setStreamType("vod")
        streamPlayer.currentPosition = 10000L
        streamPlayer.duration = 3600000L

        viewModel.seekForward(10000L)
        assertTrue(viewModel.hasPendingSeek())
        assertEquals(0, streamPlayer.seekCount)

        viewModel.confirmPendingSeek()
        assertEquals(1, streamPlayer.seekCount)
        assertEquals(20000L, streamPlayer.lastSeekPos)
        assertEquals(false, viewModel.hasPendingSeek())
    }

    @Test
    fun testTogglePlayPause() {
        streamPlayer.isPlaying = true
        viewModel.togglePlayPause()
        assertEquals(1, streamPlayer.pauseCount)

        streamPlayer.isPlaying = false
        viewModel.togglePlayPause()
        assertEquals(1, streamPlayer.playCount)
    }

    @Test
    fun testVodResumePromptAppearsAndRestartSeeksToStart() = runTest {
        val savedRecent = RecentItem(
            streamId = 777,
            type = "vod",
            title = "Gladiator",
            iconUrl = null,
            lastWatched = 1000L,
            watchPositionMs = 45000L,
            durationMs = 120000L
        )
        repository.recentsByStreamIdMap[777] = savedRecent

        viewModel.initStream(streamId = 777, initialTitle = "Gladiator", type = "vod", containerExt = "mp4")
        advanceTimeBy(100L)

        assertNotNull(viewModel.resumePrompt.value)
        assertEquals(45000L, viewModel.resumePrompt.value?.resumedPositionMs)

        viewModel.restartFromBeginning()
        assertEquals(null, viewModel.resumePrompt.value)
        assertEquals(0L, streamPlayer.lastSeekPos)
        viewModel.stopPlayback()
    }

    @Test
    fun testSeriesNextEpisodeResolutionAndBingeBar() = runTest {
        val season1 = SeriesSeason(id = 1, name = "Season 1", seasonNumber = 1, episodeCount = 2, coverUrl = null)
        val ep1 = SeriesEpisode(id = "101", episodeNum = 1, title = "Pilot", containerExt = "mp4")
        val ep2 = SeriesEpisode(id = "102", episodeNum = 2, title = "The Next Chapter", containerExt = "mp4")
        repository.seriesDetailResult = SeriesDetail(
            seasons = listOf(season1),
            episodes = mapOf("1" to listOf(ep1, ep2))
        )

        viewModel.initStream(
            streamId = 101,
            initialTitle = "Pilot",
            type = "series",
            containerExt = "mp4",
            seriesId = 42
        )
        advanceTimeBy(200L)

        assertNotNull(viewModel.nextEpisode.value)
        assertEquals(102, viewModel.nextEpisode.value?.episodeId)
        assertEquals("The Next Chapter", viewModel.nextEpisode.value?.title)

        // Near end of playback: 15 seconds remaining
        streamPlayer.duration = 60000L
        streamPlayer.currentPosition = 45000L
        advanceTimeBy(1100L)

        assertEquals(true, viewModel.isBingeBarVisible.value)
        assertEquals(15, viewModel.bingeCountdownSeconds.value)

        // Dismiss binge bar
        viewModel.dismissBingeBar()
        assertEquals(false, viewModel.isBingeBarVisible.value)

        // Play next episode directly
        viewModel.playNextEpisode()
        advanceTimeBy(100L)
        assertEquals(102, streamPlayer.lastPlayedSeriesEpisodeId)
        viewModel.stopPlayback()
    }

    @Test
    fun testLiveStreamTimeoutSwitchesToNextChannelWhenNoFrameRendered() = runTest(testDispatcher) {
        val ch1 = Channel(1, "News 1", null, "cat1", null)
        val ch2 = Channel(2, "News 2", null, "cat1", null)
        repository.channelByIdMap[1] = ch1
        repository.channelByIdMap[2] = ch2
        repository.categoryChannelsMap["cat1"] = listOf(ch1, ch2)

        viewModel.liveFrameTimeoutMs = 5000L
        viewModel.initChannel(1, "News 1")
        advanceTimeBy(200L)

        assertEquals(1, viewModel.currentChannel.value?.id)
        assertEquals(false, streamPlayer.isFirstFrameRendered.value)

        // Advance past timeout (5000ms)
        advanceTimeBy(5100L)

        // Should have auto-switched to channel 2
        assertEquals(2, viewModel.currentChannel.value?.id)
        assertEquals(2, streamPlayer.lastPlayedLiveStreamId)
        viewModel.stopPlayback()
    }

    @Test
    fun testLiveStreamDoesNotSwitchWhenFirstFrameRenderedBeforeTimeout() = runTest(testDispatcher) {
        val ch1 = Channel(1, "News 1", null, "cat1", null)
        val ch2 = Channel(2, "News 2", null, "cat1", null)
        repository.channelByIdMap[1] = ch1
        repository.channelByIdMap[2] = ch2
        repository.categoryChannelsMap["cat1"] = listOf(ch1, ch2)

        viewModel.liveFrameTimeoutMs = 5000L
        viewModel.initChannel(1, "News 1")
        advanceTimeBy(200L)

        assertEquals(1, viewModel.currentChannel.value?.id)

        // First frame rendered at 2000ms
        advanceTimeBy(2000L)
        streamPlayer.isFirstFrameRendered.value = true
        advanceTimeBy(200L)

        // Advance well past the 5000ms timeout mark
        advanceTimeBy(6000L)

        // Channel should remain 1
        assertEquals(1, viewModel.currentChannel.value?.id)
        viewModel.stopPlayback()
    }

    @Test
    fun testVodDoesNotTriggerFrameTimeout() = runTest(testDispatcher) {
        viewModel.liveFrameTimeoutMs = 5000L
        viewModel.initStream(
            streamId = 50,
            initialTitle = "Action Movie",
            type = "vod"
        )
        advanceTimeBy(200L)

        advanceTimeBy(6000L)

        // Remains on vod stream 50
        assertEquals(50, streamPlayer.lastPlayedVodStreamId)
        viewModel.stopPlayback()
    }
}
