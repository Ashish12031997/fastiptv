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

        try {
            errorHandler.onChannelChanged(streamId, title, format, streamType = "live")

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(url)
                .setMediaId(streamId.toString())

            if (format.equals("m3u8", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                mediaItemBuilder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(6_000L)
                        .setMinOffsetMs(2_000L)
                        .setMaxOffsetMs(15_000L)
                        .setMinPlaybackSpeed(0.97f)
                        .setMaxPlaybackSpeed(1.03f)
                        .build()
                )
            } else {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
                mediaItemBuilder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(4_000L)
                        .setMinOffsetMs(1_000L)
                        .setMaxOffsetMs(10_000L)
                        .setMinPlaybackSpeed(1.0f)
                        .setMaxPlaybackSpeed(1.0f)
                        .build()
                )
            }

            val mediaItem = mediaItemBuilder.build()

            exoPlayer.stop()
            exoPlayer.setMediaItem(mediaItem, /* resetPosition = */ true)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            android.util.Log.e("FastIPTV", "Error playing live stream streamId=$streamId", e)
        }
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

        try {
            errorHandler.onChannelChanged(streamId, title, containerExt, streamType = "vod")

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(streamId.toString())
                .apply {
                    when (containerExt.lowercase()) {
                        "mkv" -> setMimeType(MimeTypes.VIDEO_MATROSKA)
                        "mp4" -> setMimeType(MimeTypes.VIDEO_MP4)
                        "m3u8" -> setMimeType(MimeTypes.APPLICATION_M3U8)
                        "ts" -> setMimeType(MimeTypes.VIDEO_MP2T)
                    }
                }
                .build()

            exoPlayer.stop()
            exoPlayer.setMediaItem(mediaItem, startPositionMs)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            android.util.Log.e("FastIPTV", "Error playing VOD streamId=$streamId", e)
        }
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

        try {
            errorHandler.onChannelChanged(episodeId, title, containerExt, streamType = "series")

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(episodeId.toString())
                .apply {
                    when (containerExt.lowercase()) {
                        "mkv" -> setMimeType(MimeTypes.VIDEO_MATROSKA)
                        "mp4" -> setMimeType(MimeTypes.VIDEO_MP4)
                        "m3u8" -> setMimeType(MimeTypes.APPLICATION_M3U8)
                        "ts" -> setMimeType(MimeTypes.VIDEO_MP2T)
                    }
                }
                .build()

            exoPlayer.stop()
            exoPlayer.setMediaItem(mediaItem, startPositionMs)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            android.util.Log.e("FastIPTV", "Error playing series episode episodeId=$episodeId", e)
        }
    }

    override val currentPosition: Long
        get() = try { exoPlayer.currentPosition } catch (e: Exception) { 0L }

    override val duration: Long
        get() = try { exoPlayer.duration } catch (e: Exception) { 0L }

    override val isPlaying: Boolean
        get() = try { exoPlayer.isPlaying } catch (e: Exception) { false }

    override val isSeekable: Boolean
        get() = try { exoPlayer.isCurrentMediaItemSeekable } catch (e: Exception) { false }

    override val bufferedPosition: Long
        get() = try { exoPlayer.bufferedPosition } catch (e: Exception) { 0L }

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

    override fun retry() {
        errorHandler.manualRetry()
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
