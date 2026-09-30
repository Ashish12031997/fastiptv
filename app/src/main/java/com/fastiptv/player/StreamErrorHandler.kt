package com.fastiptv.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.fastiptv.data.api.XtreamUrlBuilder
import com.fastiptv.data.session.SessionManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamErrorHandler @Inject constructor(
    private val urlBuilder: XtreamUrlBuilder,
    private val sessionManager: SessionManager,
    private val okHttpClient: OkHttpClient
) : Player.Listener {

    private var player: Player? = null
    private var recoveryJob: Job? = null
    var retryCount: Int = 0
        private set
    var currentFormat: String = "m3u8"
        private set
    var currentStreamId: Int = 0
        private set
    private var currentTitle: String? = null

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _isFirstFrameRendered = MutableStateFlow(false)
    val isFirstFrameRendered: StateFlow<Boolean> = _isFirstFrameRendered.asStateFlow()

    var mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    var currentStreamType: String = "live"
        private set

    fun attachPlayer(player: Player) {
        this.player = player
    }

    fun onChannelChanged(
        streamId: Int,
        title: String? = null,
        initialFormat: String = "m3u8",
        streamType: String = "live"
    ) {
        recoveryJob?.cancel()
        recoveryJob = null
        currentStreamId = streamId
        currentTitle = title
        currentFormat = initialFormat
        currentStreamType = streamType
        retryCount = 0
        _isFirstFrameRendered.value = false
        _playerState.value = PlayerState.Loading(streamId, title)
    }

    fun resetState() {
        recoveryJob?.cancel()
        recoveryJob = null
        retryCount = 0
        _isFirstFrameRendered.value = false
        _playerState.value = PlayerState.Idle
    }

    override fun onRenderedFirstFrame() {
        _isFirstFrameRendered.value = true
        if (player?.isPlaying == true || _playerState.value is PlayerState.Buffering || _playerState.value is PlayerState.Loading || _playerState.value is PlayerState.Reconnecting) {
            _playerState.value = PlayerState.Playing(currentStreamId, currentTitle)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            _playerState.value = PlayerState.Playing(currentStreamId, currentTitle)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                if (_playerState.value !is PlayerState.Reconnecting) {
                    _playerState.value = PlayerState.Buffering(currentStreamId)
                }
            }
            Player.STATE_READY -> {
                if (player?.isPlaying == true) {
                    _playerState.value = PlayerState.Playing(currentStreamId, currentTitle)
                }
            }
            Player.STATE_ENDED -> {
                _playerState.value = PlayerState.Idle
            }
            Player.STATE_IDLE -> {
                // Handled in onPlayerError or explicit stop
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        android.util.Log.e("FastIPTV", "onPlayerError: streamId=$currentStreamId, type=$currentStreamType, errorCode=${error.errorCode}, error=${error.message}", error)
        val activePlayer = player ?: return
        recoveryJob?.cancel()
        recoveryJob = CoroutineScope(mainDispatcher).launch {
            executeRecoveryPipeline(activePlayer, error)
        }
    }

    fun manualRetry() {
        val activePlayer = player ?: return
        recoveryJob?.cancel()
        recoveryJob = null
        retryCount = 0
        _playerState.value = PlayerState.Reconnecting(
            streamId = currentStreamId,
            attempt = 1,
            maxAttempts = 3,
            message = "Retrying stream..."
        )
        okHttpClient.connectionPool.evictAll()
        activePlayer.prepare()
        activePlayer.play()
    }

    private suspend fun executeRecoveryPipeline(player: Player, error: PlaybackException) {
        // Special case: Behind live window (common in HLS live streams)
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            android.util.Log.w("FastIPTV", "Behind live window detected for streamId=$currentStreamId, snapping back to live edge")
            _playerState.value = PlayerState.Buffering(currentStreamId)
            player.seekToDefaultPosition()
            player.prepare()
            player.play()
            return
        }

        when {
            // Stage 1: Retry same URL (3 attempts with exponential backoff 1s -> 2s -> 4s)
            retryCount < 3 -> {
                retryCount++
                _playerState.value = PlayerState.Reconnecting(
                    streamId = currentStreamId,
                    attempt = retryCount,
                    maxAttempts = 3,
                    message = "Reconnecting stream (Attempt $retryCount/3)..."
                )
                val delayMs = 1000L * (1 shl (retryCount - 1))
                delay(delayMs)
                player.prepare()
                player.play()
            }

            // Stage 2: Switch stream format (.m3u8 <-> .ts) for live, or re-prepare
            retryCount == 3 -> {
                retryCount++
                _playerState.value = PlayerState.Reconnecting(
                    streamId = currentStreamId,
                    attempt = retryCount,
                    maxAttempts = 5,
                    message = "Switching format & reconnecting..."
                )
                if (currentStreamType == "live") {
                    currentFormat = if (currentFormat == "m3u8") "ts" else "m3u8"
                    val config = sessionManager.getCachedConfig()
                    if (config != null && config.isValid) {
                        val newUrl = urlBuilder.buildLiveStreamUrl(config, currentStreamId, currentFormat)
                        val mimeType = if (currentFormat.equals("m3u8", ignoreCase = true)) {
                            MimeTypes.APPLICATION_M3U8
                        } else {
                            MimeTypes.VIDEO_MP2T
                        }
                        val newItemBuilder = MediaItem.Builder()
                            .setUri(newUrl)
                            .setMediaId(currentStreamId.toString())
                            .setMimeType(mimeType)

                        if (currentFormat.equals("m3u8", ignoreCase = true)) {
                            newItemBuilder.setLiveConfiguration(
                                MediaItem.LiveConfiguration.Builder()
                                    .setTargetOffsetMs(6_000L)
                                    .setMinOffsetMs(2_000L)
                                    .setMaxOffsetMs(15_000L)
                                    .setMinPlaybackSpeed(0.97f)
                                    .setMaxPlaybackSpeed(1.03f)
                                    .build()
                            )
                        } else {
                            newItemBuilder.setLiveConfiguration(
                                MediaItem.LiveConfiguration.Builder()
                                    .setTargetOffsetMs(4_000L)
                                    .setMinOffsetMs(1_000L)
                                    .setMaxOffsetMs(10_000L)
                                    .setMinPlaybackSpeed(1.0f)
                                    .setMaxPlaybackSpeed(1.0f)
                                    .build()
                            )
                        }
                        val newItem = newItemBuilder.build()
                        player.setMediaItem(newItem)
                        player.prepare()
                        player.play()
                    } else {
                        emitError(error, stage = 2)
                    }
                } else {
                    player.prepare()
                    player.play()
                }
            }

            // Stage 3: Fresh connection (flush connection pool and retry)
            retryCount == 4 -> {
                retryCount++
                _playerState.value = PlayerState.Reconnecting(
                    streamId = currentStreamId,
                    attempt = retryCount,
                    maxAttempts = 5,
                    message = "Resetting network connection..."
                )
                okHttpClient.connectionPool.evictAll()
                delay(2000L)
                player.prepare()
                player.play()
            }

            // Stage 4: Give up, notify user, auto-retry in 15s
            else -> {
                emitError(error, stage = 4)
                delay(15_000L)
                retryCount = 0
                player.prepare()
                player.play()
            }
        }
    }

    private fun emitError(error: PlaybackException, stage: Int) {
        val message = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Network connection failed"
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Server returned HTTP error"
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Hardware video decoder failed"
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Stream data corrupted"
            else -> error.message ?: "Stream playback failed"
        }
        _playerState.value = PlayerState.Error(
            streamId = currentStreamId,
            message = message,
            canRetry = true,
            stage = stage
        )
    }
}
