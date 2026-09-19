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
        if (player?.isPlaying == true || _playerState.value is PlayerState.Buffering || _playerState.value is PlayerState.Loading) {
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
                _playerState.value = PlayerState.Buffering(currentStreamId)
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
        android.util.Log.e("FastIPTV", "onPlayerError: streamId=$currentStreamId, type=$currentStreamType, error=${error.message}", error)
        val activePlayer = player ?: return
        recoveryJob?.cancel()
        recoveryJob = CoroutineScope(mainDispatcher).launch {
            executeRecoveryPipeline(activePlayer, error)
        }
    }

    private suspend fun executeRecoveryPipeline(player: Player, error: PlaybackException) {
        when {
            // Stage 1: Retry same URL (3 attempts with exponential backoff 1s -> 2s -> 4s)
            retryCount < 3 -> {
                retryCount++
                _playerState.value = PlayerState.Buffering(currentStreamId)
                val delayMs = 1000L * (1 shl (retryCount - 1))
                delay(delayMs)
                player.prepare()
                player.play()
            }

            // Stage 2: Switch stream format (.m3u8 <-> .ts) for live, or re-prepare
            retryCount == 3 -> {
                retryCount++
                _playerState.value = PlayerState.Buffering(currentStreamId)
                if (currentStreamType == "live") {
                    currentFormat = if (currentFormat == "m3u8") "ts" else "m3u8"
                    val config = sessionManager.getCachedConfig()
                    if (config != null && config.isValid) {
                        val newUrl = urlBuilder.buildLiveStreamUrl(config, currentStreamId, currentFormat)
                        val newItem = MediaItem.Builder()
                            .setUri(newUrl)
                            .setMediaId(currentStreamId.toString())
                            .build()
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
                _playerState.value = PlayerState.Buffering(currentStreamId)
                okHttpClient.connectionPool.evictAll()
                delay(2000L)
                player.prepare()
                player.play()
            }

            // Stage 4: Give up, notify user, auto-retry in 30s
            else -> {
                emitError(error, stage = 4)
                delay(30_000L)
                retryCount = 0
                player.prepare()
                player.play()
            }
        }
    }

    private fun emitError(error: PlaybackException, stage: Int) {
        val message = error.message ?: "Stream playback failed"
        _playerState.value = PlayerState.Error(
            streamId = currentStreamId,
            message = message,
            canRetry = true,
            stage = stage
        )
    }
}
