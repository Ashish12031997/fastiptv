package com.fastiptv.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.fastiptv.data.api.XtreamUrlBuilder
import com.fastiptv.data.session.SessionManager
import com.fastiptv.domain.model.ServerConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreamErrorHandlerTest {

    private lateinit var urlBuilder: XtreamUrlBuilder
    private lateinit var sessionManager: SessionManager
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var connectionPool: ConnectionPool
    private lateinit var player: Player
    private lateinit var errorHandler: StreamErrorHandler

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        urlBuilder = XtreamUrlBuilder()
        sessionManager = mockk(relaxed = true)
        okHttpClient = mockk(relaxed = true)
        connectionPool = mockk(relaxed = true)
        every { okHttpClient.connectionPool } returns connectionPool

        player = mockk(relaxed = true)
        errorHandler = StreamErrorHandler(urlBuilder, sessionManager, okHttpClient)
        errorHandler.mainDispatcher = testDispatcher
        errorHandler.attachPlayer(player)

        val config = ServerConfig("test.tv", 8080, "demo", "pass")
        every { sessionManager.getCachedConfig() } returns config
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testOnChannelChangedSetsLoadingStateAndResetsRetryCount() = testScope.runTest {
        errorHandler.onChannelChanged(101, "CNN HD")

        assertEquals(101, errorHandler.currentStreamId)
        assertEquals(0, errorHandler.retryCount)

        val state = errorHandler.playerState.value
        assertTrue(state is PlayerState.Loading)
        assertEquals(101, (state as PlayerState.Loading).streamId)
        assertEquals("CNN HD", state.title)
    }

    @Test
    fun testOnPlaybackStateChangedBufferingAndPlaying() = testScope.runTest {
        errorHandler.onChannelChanged(101, "BBC One")

        errorHandler.onPlaybackStateChanged(Player.STATE_BUFFERING)
        assertTrue(errorHandler.playerState.value is PlayerState.Buffering)

        every { player.isPlaying } returns true
        errorHandler.onIsPlayingChanged(true)
        assertTrue(errorHandler.playerState.value is PlayerState.Playing)
    }

    @Test
    fun testStage1ExponentialBackoffRetries() = testScope.runTest {
        errorHandler.onChannelChanged(101, "Sports 1")
        val error = PlaybackException("Network Timeout", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)

        // Attempt 1: 1s delay
        errorHandler.onPlayerError(error)
        testDispatcher.scheduler.advanceTimeBy(1000L)
        testDispatcher.scheduler.runCurrent()
        verify(exactly = 1) { player.prepare() }
        verify(exactly = 1) { player.play() }
        assertEquals(1, errorHandler.retryCount)

        // Attempt 2: 2s delay
        errorHandler.onPlayerError(error)
        testDispatcher.scheduler.advanceTimeBy(2000L)
        testDispatcher.scheduler.runCurrent()
        verify(exactly = 2) { player.prepare() }
        verify(exactly = 2) { player.play() }
        assertEquals(2, errorHandler.retryCount)

        // Attempt 3: 4s delay
        errorHandler.onPlayerError(error)
        testDispatcher.scheduler.advanceTimeBy(4000L)
        testDispatcher.scheduler.runCurrent()
        verify(exactly = 3) { player.prepare() }
        verify(exactly = 3) { player.play() }
        assertEquals(3, errorHandler.retryCount)
    }

    @Test
    fun testStage2FormatSwitchFromM3u8ToTs() = testScope.runTest {
        errorHandler.onChannelChanged(101, "Movie Channel", initialFormat = "m3u8")
        val error = PlaybackException("Bad Manifest", null, PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED)

        // Fast-forward through Stage 1 (3 attempts)
        errorHandler.onPlayerError(error)
        testDispatcher.scheduler.advanceTimeBy(1000L)
        testDispatcher.scheduler.runCurrent()

        errorHandler.onPlayerError(error)
        testDispatcher.scheduler.advanceTimeBy(2000L)
        testDispatcher.scheduler.runCurrent()

        errorHandler.onPlayerError(error)
        testDispatcher.scheduler.advanceTimeBy(4000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(3, errorHandler.retryCount)

        // Stage 2: 4th error triggers format switch
        errorHandler.onPlayerError(error)
        testDispatcher.scheduler.runCurrent()
        assertEquals(4, errorHandler.retryCount)
        assertEquals("ts", errorHandler.currentFormat)
        verify(exactly = 1) { player.setMediaItem(any()) }
    }

    @Test
    fun testStage3FreshConnectionEvictsPool() = testScope.runTest {
        errorHandler.onChannelChanged(101, "Channel", initialFormat = "m3u8")
        val error = PlaybackException("Connection reset", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)

        // Fast-forward through Stage 1 & Stage 2
        repeat(4) {
            errorHandler.onPlayerError(error)
            testDispatcher.scheduler.advanceTimeBy(5000L)
            testDispatcher.scheduler.runCurrent()
        }
        assertEquals(4, errorHandler.retryCount)

        // 5th error triggers Stage 3
        errorHandler.onPlayerError(error)
        testDispatcher.scheduler.runCurrent()
        assertEquals(5, errorHandler.retryCount)
        verify(exactly = 1) { connectionPool.evictAll() }
        testDispatcher.scheduler.advanceTimeBy(2000L)
        testDispatcher.scheduler.runCurrent()
        verify { player.prepare() }
    }

    @Test
    fun testChannelSwitchCancelsInFlightRecovery() = testScope.runTest {
        errorHandler.onChannelChanged(101, "Old Channel")
        val error = PlaybackException("Network drop", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)

        errorHandler.onPlayerError(error)

        // User switches channel before 1s retry delay expires
        errorHandler.onChannelChanged(202, "New Channel")
        testDispatcher.scheduler.advanceTimeBy(2000L)

        // The retry for old channel 101 should NOT have fired
        verify(exactly = 0) { player.prepare() }
        assertEquals(202, errorHandler.currentStreamId)
        assertEquals(0, errorHandler.retryCount)
    }

    @Test
    fun testOnRenderedFirstFrameUpdatesStateFlow() = testScope.runTest {
        assertEquals(false, errorHandler.isFirstFrameRendered.first())

        errorHandler.onRenderedFirstFrame()
        assertEquals(true, errorHandler.isFirstFrameRendered.first())

        // Switching channel resets isFirstFrameRendered
        errorHandler.onChannelChanged(streamId = 200, title = "New Channel")
        assertEquals(false, errorHandler.isFirstFrameRendered.first())
    }
}
