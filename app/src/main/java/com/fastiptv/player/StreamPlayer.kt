package com.fastiptv.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import com.fastiptv.data.api.XtreamUrlBuilder
import com.fastiptv.data.session.SessionManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamPlayer @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val errorHandler: StreamErrorHandler,
    private val urlBuilder: XtreamUrlBuilder,
    private val sessionManager: SessionManager
) : StreamPlayerController {
    override val playerState: StateFlow<PlayerState> = errorHandler.playerState
    override val isFirstFrameRendered: StateFlow<Boolean> = errorHandler.isFirstFrameRendered

    init {
        errorHandler.attachPlayer(exoPlayer)
    }

    override fun getPlayer(): ExoPlayer = exoPlayer

    override fun playLiveStream(streamId: Int, format: String, title: String?) {
        val config = sessionManager.getCachedConfig() ?: return
        val url = urlBuilder.buildLiveStreamUrl(config, streamId, format)
        android.util.Log.d("FastIPTV", "playLiveStream URL: $url (format=$format)")

        errorHandler.onChannelChanged(streamId, title, format, streamType = "live")

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(streamId.toString())
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(3_000)
                    .setMinPlaybackSpeed(0.97f)
                    .setMaxPlaybackSpeed(1.03f)
                    .build()
            )
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun playVod(
        streamId: Int,
        containerExt: String,
        title: String?,
        startPositionMs: Long
    ) {
        val config = sessionManager.getCachedConfig() ?: return
        val url = urlBuilder.buildVodUrl(config, streamId, containerExt)
        android.util.Log.d("FastIPTV", "playVod URL: $url")

        errorHandler.onChannelChanged(streamId, title, containerExt, streamType = "vod")

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(streamId.toString())
            .build()

        exoPlayer.setMediaItem(mediaItem, startPositionMs)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun playSeriesEpisode(
        episodeId: Int,
        containerExt: String,
        title: String?,
        startPositionMs: Long
    ) {
        val config = sessionManager.getCachedConfig() ?: return
        val url = urlBuilder.buildSeriesEpisodeUrl(config, episodeId, containerExt)
        android.util.Log.d("FastIPTV", "playSeriesEpisode URL: $url")

        errorHandler.onChannelChanged(episodeId, title, containerExt, streamType = "series")

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(episodeId.toString())
            .build()

        exoPlayer.setMediaItem(mediaItem, startPositionMs)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override val currentPosition: Long
        get() = try { exoPlayer.currentPosition } catch (e: Exception) { 0L }

    override val duration: Long
        get() = try { exoPlayer.duration } catch (e: Exception) { 0L }

    override val isPlaying: Boolean
        get() = try { exoPlayer.isPlaying } catch (e: Exception) { false }

    override val isSeekable: Boolean
        get() = try { exoPlayer.isCurrentMediaItemSeekable } catch (e: Exception) { false }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun resume() {
        exoPlayer.play()
    }

    /**
     * Halts playback and resets state. Critical for IPTV providers enforcing max_connections = 1.
     */
    override fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        errorHandler.resetState()
    }

    override fun release() {
        exoPlayer.release()
        errorHandler.resetState()
    }
}
