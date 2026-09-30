package com.fastiptv.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

interface StreamPlayerController {
    val playerState: StateFlow<PlayerState>
    val currentPosition: Long
    val duration: Long
    val isPlaying: Boolean
    val isSeekable: Boolean
    val isFirstFrameRendered: StateFlow<Boolean>
    val bufferedPosition: Long get() = 0L

    fun getPlayer(): Player
    fun playLiveStream(streamId: Int, format: String = "m3u8", title: String? = null)
    fun playVod(streamId: Int, containerExt: String = "mp4", title: String? = null, startPositionMs: Long = 0L)
    fun playSeriesEpisode(episodeId: Int, containerExt: String = "mkv", title: String? = null, startPositionMs: Long = 0L)
    fun seekTo(positionMs: Long)
    fun play()
    fun pause()
    fun resume()
    fun retry()
    fun stop()
    fun release()
}
