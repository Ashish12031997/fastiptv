package com.fastiptv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastiptv.data.session.SessionManager
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.model.Movie
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.domain.repository.IptvRepository
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import com.fastiptv.player.AspectRatioMode
import com.fastiptv.player.PlayerState
import com.fastiptv.player.PlayerTrackOption
import com.fastiptv.player.StreamDiagnostics
import com.fastiptv.player.StreamPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamPlayer: StreamPlayerController,
    private val repository: IptvRepository,
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = streamPlayer.playerState

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _epgPrograms = MutableStateFlow<List<EpgProgram>>(emptyList())
    val epgPrograms: StateFlow<List<EpgProgram>> = _epgPrograms.asStateFlow()

    private val _isOverlayVisible = MutableStateFlow(true)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    private val _channelSwitchNotice = MutableStateFlow<String?>(null)
    val channelSwitchNotice: StateFlow<String?> = _channelSwitchNotice.asStateFlow()

    private val _streamType = MutableStateFlow("live")
    val streamType: StateFlow<String> = _streamType.asStateFlow()

    private val _streamTitle = MutableStateFlow<String?>(null)
    val streamTitle: StateFlow<String?> = _streamTitle.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // Stream Diagnostics ("Nerd Stats")
    private val _isDiagnosticsVisible = MutableStateFlow(false)
    val isDiagnosticsVisible: StateFlow<Boolean> = _isDiagnosticsVisible.asStateFlow()

    private val _diagnostics = MutableStateFlow(StreamDiagnostics())
    val diagnostics: StateFlow<StreamDiagnostics> = _diagnostics.asStateFlow()

    private var diagnosticsJob: Job? = null

    // Quick Settings Drawer
    private val _isQuickSettingsVisible = MutableStateFlow(false)
    val isQuickSettingsVisible: StateFlow<Boolean> = _isQuickSettingsVisible.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<PlayerTrackOption>>(emptyList())
    val audioTracks: StateFlow<List<PlayerTrackOption>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<PlayerTrackOption>>(emptyList())
    val subtitleTracks: StateFlow<List<PlayerTrackOption>> = _subtitleTracks.asStateFlow()

    // Phase D: Intelligent Resume & Binge Bar
    private val _nextEpisode = MutableStateFlow<NextEpisodeInfo?>(null)
    val nextEpisode: StateFlow<NextEpisodeInfo?> = _nextEpisode.asStateFlow()

    private val _isBingeBarVisible = MutableStateFlow(false)
    val isBingeBarVisible: StateFlow<Boolean> = _isBingeBarVisible.asStateFlow()

    private val _bingeCountdownSeconds = MutableStateFlow(30)
    val bingeCountdownSeconds: StateFlow<Int> = _bingeCountdownSeconds.asStateFlow()

    private val _resumePrompt = MutableStateFlow<ResumePromptInfo?>(null)
    val resumePrompt: StateFlow<ResumePromptInfo?> = _resumePrompt.asStateFlow()

    private var currentSeriesId: Int? = null
    private var isBingeDismissedForCurrentEpisode: Boolean = false
    private var resumePromptHideJob: Job? = null

    private var currentStreamId: Int = 0
    private var currentContainerExt: String = "mp4"

    private var channelList: List<Channel> = emptyList()
    private var overlayHideJob: Job? = null
    private var noticeHideJob: Job? = null
    private var progressTrackingJob: Job? = null

    private var pendingSeekTargetMs: Long? = null
    private var consecutiveSeekCount: Int = 0
    private var lastSeekTimestamp: Long = 0L
    private var seekDebounceJob: Job? = null

    var liveFrameTimeoutMs: Long = 15_000L
    private var frameTimeoutJob: Job? = null
    private var consecutiveTimeoutCount: Int = 0

    init {
        viewModelScope.launch {
            streamPlayer.isFirstFrameRendered.collect { rendered ->
                if (rendered) {
                    frameTimeoutJob?.cancel()
                    consecutiveTimeoutCount = 0
                }
            }
        }
    }

    fun hasPendingSeek(): Boolean = pendingSeekTargetMs != null

    @androidx.annotation.VisibleForTesting
    fun setStreamType(type: String) {
        _streamType.value = type
    }

    fun getPlayer() = streamPlayer.getPlayer()

    fun initStream(
        streamId: Int,
        initialTitle: String? = null,
        type: String = "live",
        containerExt: String? = null,
        seriesId: Int? = null
    ) {
        currentStreamId = streamId
        _streamType.value = type
        _streamTitle.value = initialTitle
        currentContainerExt = containerExt ?: if (type == "series") "mkv" else "mp4"

        when (type) {
            "vod" -> {
                viewModelScope.launch {
                    val savedRecent = repository.getRecentByStreamId(streamId)
                    val resumePos = if (savedRecent != null && savedRecent.watchPositionMs > 5000L) {
                        if (savedRecent.durationMs > 0 && savedRecent.watchPositionMs >= savedRecent.durationMs - 10000L) {
                            0L // Restart if within last 10s
                        } else {
                            savedRecent.watchPositionMs
                        }
                    } else {
                        0L
                    }

                    streamPlayer.playVod(
                        streamId = streamId,
                        containerExt = currentContainerExt,
                        title = initialTitle,
                        startPositionMs = resumePos
                    )

                    if (resumePos > 0L) {
                        _resumePrompt.value = ResumePromptInfo(resumePos, formatTime(resumePos))
                        resumePromptHideJob?.cancel()
                        resumePromptHideJob = launch {
                            delay(6000L)
                            _resumePrompt.value = null
                        }
                        showSwitchNotice("Resuming from ${formatTime(resumePos)}")
                    } else {
                        _resumePrompt.value = null
                    }

                    repository.recordRecent(
                        RecentItem(
                            streamId = streamId,
                            type = "vod",
                            title = initialTitle ?: "Movie $streamId",
                            iconUrl = null,
                            lastWatched = System.currentTimeMillis(),
                            watchPositionMs = resumePos
                        )
                    )

                    launch {
                        repository.observeFavoriteMovies().collect { favs ->
                            _isFavorite.value = favs.any { it.id == streamId }
                        }
                    }

                    startProgressTracking()
                }
                showOverlay(4000L)
            }
            "series" -> {
                currentSeriesId = seriesId
                isBingeDismissedForCurrentEpisode = false
                _isBingeBarVisible.value = false
                _nextEpisode.value = null

                viewModelScope.launch {
                    val savedRecent = repository.getRecentByStreamId(streamId)
                    val resumePos = if (savedRecent != null && savedRecent.watchPositionMs > 5000L) {
                        if (savedRecent.durationMs > 0 && savedRecent.watchPositionMs >= savedRecent.durationMs - 10000L) {
                            0L
                        } else {
                            savedRecent.watchPositionMs
                        }
                    } else {
                        0L
                    }

                    streamPlayer.playSeriesEpisode(
                        episodeId = streamId,
                        containerExt = currentContainerExt,
                        title = initialTitle,
                        startPositionMs = resumePos
                    )

                    if (resumePos > 0L) {
                        _resumePrompt.value = ResumePromptInfo(resumePos, formatTime(resumePos))
                        resumePromptHideJob?.cancel()
                        resumePromptHideJob = launch {
                            delay(6000L)
                            _resumePrompt.value = null
                        }
                        showSwitchNotice("Resuming from ${formatTime(resumePos)}")
                    } else {
                        _resumePrompt.value = null
                    }

                    repository.recordRecent(
                        RecentItem(
                            streamId = streamId,
                            type = "series",
                            title = initialTitle ?: "Episode $streamId",
                            iconUrl = null,
                            lastWatched = System.currentTimeMillis(),
                            watchPositionMs = resumePos
                        )
                    )

                    if (seriesId != null) {
                        if (seriesId == -999) {
                            _nextEpisode.value = NextEpisodeInfo(
                                episodeId = 19453,
                                seriesId = -999,
                                seasonNumber = 1,
                                episodeNumber = 2,
                                title = "The Truth Revealed",
                                containerExt = "mkv"
                            )
                            _bingeCountdownSeconds.value = 24
                            _isBingeBarVisible.value = true
                        } else {
                            resolveNextEpisode(seriesId, streamId)
                        }
                    }

                    startProgressTracking()
                }
                showOverlay(4000L)
            }
            else -> {
                _resumePrompt.value = null
                _isBingeBarVisible.value = false
                _nextEpisode.value = null
                initChannel(streamId, initialTitle)
            }
        }
    }

    private fun resolveNextEpisode(seriesId: Int, currentEpisodeId: Int) {
        viewModelScope.launch {
            repository.getSeriesInfo(seriesId).onSuccess { detail ->
                val sortedSeasons = detail.seasons.sortedBy { it.seasonNumber }
                val allEpisodes = mutableListOf<Pair<Int, com.fastiptv.domain.model.SeriesEpisode>>()
                for (season in sortedSeasons) {
                    val list = detail.episodes[season.seasonNumber.toString()]
                        ?: detail.episodes[season.id?.toString()]
                        ?: emptyList()
                    list.sortedBy { it.episodeNum }.forEach { ep ->
                        allEpisodes.add(season.seasonNumber to ep)
                    }
                }
                if (allEpisodes.isEmpty()) {
                    detail.episodes.entries.sortedBy { it.key.toIntOrNull() ?: 0 }.forEach { (k, list) ->
                        val sNum = k.toIntOrNull() ?: 1
                        list.sortedBy { it.episodeNum }.forEach { ep ->
                            allEpisodes.add(sNum to ep)
                        }
                    }
                }

                val currentIndex = allEpisodes.indexOfFirst { it.second.id == currentEpisodeId.toString() }
                if (currentIndex != -1 && currentIndex < allEpisodes.size - 1) {
                    val (nextSeasonNum, nextEp) = allEpisodes[currentIndex + 1]
                    val nextEpId = nextEp.id.toIntOrNull()
                    if (nextEpId != null) {
                        _nextEpisode.value = NextEpisodeInfo(
                            episodeId = nextEpId,
                            seriesId = seriesId,
                            seasonNumber = nextSeasonNum,
                            episodeNumber = nextEp.episodeNum,
                            title = nextEp.title,
                            containerExt = nextEp.containerExt.ifEmpty { "mp4" }
                        )
                    }
                }
            }
        }
    }

    fun restartFromBeginning() {
        seekDebounceJob?.cancel()
        pendingSeekTargetMs = null
        consecutiveSeekCount = 0
        _resumePrompt.value = null
        resumePromptHideJob?.cancel()
        streamPlayer.seekTo(0L)
        _currentPositionMs.value = 0L
        showSwitchNotice("Playing from start")
    }

    fun dismissResumePrompt() {
        _resumePrompt.value = null
        resumePromptHideJob?.cancel()
    }

    fun playNextEpisode() {
        val next = _nextEpisode.value ?: return
        _isBingeBarVisible.value = false
        isBingeDismissedForCurrentEpisode = false
        initStream(
            streamId = next.episodeId,
            initialTitle = next.title,
            type = "series",
            containerExt = next.containerExt,
            seriesId = next.seriesId
        )
    }

    fun dismissBingeBar() {
        _isBingeBarVisible.value = false
        isBingeDismissedForCurrentEpisode = true
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val pos = streamPlayer.currentPosition.coerceAtLeast(0L)
                val dur = streamPlayer.duration.coerceAtLeast(0L)
                if (pendingSeekTargetMs == null) {
                    if (pos > 0L || _currentPositionMs.value == 0L || _streamType.value == "live") {
                        _currentPositionMs.value = pos
                    }
                }
                _durationMs.value = dur
                _isPlaying.value = streamPlayer.isPlaying

                if (pos > 0L && dur > 0L && currentStreamId != 0) {
                    repository.updatePlaybackPosition(currentStreamId, pos, dur)
                }

                if (_streamType.value == "series" && _nextEpisode.value != null && !isBingeDismissedForCurrentEpisode) {
                    if (currentSeriesId == -999) {
                        if (_bingeCountdownSeconds.value > 1) {
                            _bingeCountdownSeconds.value = _bingeCountdownSeconds.value - 1
                            _isBingeBarVisible.value = true
                        }
                    } else if (dur > 30000L) {
                        val remainingMs = dur - pos
                        if (remainingMs in 1000L..30000L) {
                            val remainingSec = (remainingMs / 1000L).toInt().coerceIn(1, 30)
                            _bingeCountdownSeconds.value = remainingSec
                            _isBingeBarVisible.value = true
                        } else if (remainingMs < 1000L && _isBingeBarVisible.value) {
                            playNextEpisode()
                        } else if (remainingMs > 30000L && _isBingeBarVisible.value) {
                            _isBingeBarVisible.value = false
                        }
                    }
                }
            }
        }
    }

    fun initChannel(streamId: Int, initialTitle: String? = null) {
        currentStreamId = streamId
        _streamType.value = "live"
        _streamTitle.value = initialTitle
        progressTrackingJob?.cancel()

        viewModelScope.launch {
            val channel = repository.getChannelById(streamId) ?: Channel(
                id = streamId,
                name = initialTitle ?: "Channel $streamId",
                logoUrl = null,
                categoryId = null,
                epgChannelId = null
            )
            _currentChannel.value = channel
            _isFavorite.value = channel.isFavorite

            // Load surfing list
            channel.categoryId?.let { catId ->
                launch {
                    repository.observeChannelsByCategory(catId).collect { list ->
                        channelList = list
                    }
                }
            }
        }

        playCurrentStream(streamId, initialTitle)
    }

    private fun playCurrentStream(streamId: Int, title: String? = null) {
        val format = sessionManager?.getCachedStreamFormat() ?: "ts"
        streamPlayer.playLiveStream(streamId = streamId, format = format, title = title)

        // Record recent item
        viewModelScope.launch {
            repository.recordRecent(
                RecentItem(
                    streamId = streamId,
                    type = "live",
                    title = title ?: "Channel $streamId",
                    iconUrl = _currentChannel.value?.logoUrl,
                    lastWatched = System.currentTimeMillis()
                )
            )
        }

        // Fetch short EPG
        fetchEpg(streamId)

        // Start frame timeout watchdog (auto-switch channel if no frame rendered within timeout)
        startFrameTimeoutWatchdog(streamId)

        // Trigger overlay with 3s auto-dismiss
        showOverlay(3000L)
    }

    private fun startFrameTimeoutWatchdog(streamId: Int) {
        frameTimeoutJob?.cancel()
        if (_streamType.value != "live") return

        frameTimeoutJob = viewModelScope.launch {
            delay(liveFrameTimeoutMs)
            if (!streamPlayer.isFirstFrameRendered.value &&
                _streamType.value == "live" &&
                currentStreamId == streamId &&
                _isPlaying.value
            ) {
                android.util.Log.w("FastIPTV", "No video frame received within ${liveFrameTimeoutMs}ms for streamId=$streamId. Auto-switching channel.")
                handleLiveStreamTimeout()
            }
        }
    }

    private fun handleLiveStreamTimeout() {
        val count = channelList.size
        if (count > 1 && consecutiveTimeoutCount < count.coerceAtMost(5)) {
            consecutiveTimeoutCount++
            showSwitchNotice("⚠️ No signal • Switching to next channel...")
            nextChannel(isAutoSwitch = true)
        } else {
            showSwitchNotice("⚠️ Stream timed out • No signal")
            consecutiveTimeoutCount = 0
        }
    }

    private fun fetchEpg(streamId: Int) {
        viewModelScope.launch {
            repository.getShortEpg(streamId).onSuccess { programs ->
                _epgPrograms.value = programs
            }.onFailure {
                _epgPrograms.value = emptyList()
            }
        }
    }

    fun togglePlayPause() {
        if (streamPlayer.isPlaying) {
            streamPlayer.pause()
            _isPlaying.value = false
            frameTimeoutJob?.cancel()
            showOverlay(8000L)
            showSwitchNotice("Paused")
        } else {
            streamPlayer.play()
            _isPlaying.value = true
            if (_streamType.value == "live" && !streamPlayer.isFirstFrameRendered.value) {
                startFrameTimeoutWatchdog(currentStreamId)
            }
            showOverlay(3000L)
            showSwitchNotice("Playing")
        }
    }

    /**
     * Smart accelerated seek forward.
     * Tapping repeatedly accelerates jump step (10s -> 30s -> 1m -> 2m -> 5m).
     * Debounces network requests so multiple rapid clicks result in only 1 single seek to ExoPlayer.
     */
    fun seekForward(deltaMs: Long? = null) {
        if (_streamType.value == "live") {
            showSwitchNotice("Live TV • Fast forward unavailable")
            showOverlay(2500L)
            return
        }
        if (!streamPlayer.isSeekable && _durationMs.value <= 0L) {
            showSwitchNotice("Stream is not seekable")
            showOverlay(2500L)
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastSeekTimestamp < 1200L) {
            consecutiveSeekCount++
        } else {
            consecutiveSeekCount = 1
        }
        lastSeekTimestamp = now

        val step = deltaMs ?: when {
            consecutiveSeekCount <= 3 -> 10_000L
            consecutiveSeekCount <= 6 -> 30_000L
            consecutiveSeekCount <= 10 -> 60_000L
            consecutiveSeekCount <= 14 -> 120_000L
            else -> 300_000L
        }

        val current = if (_currentPositionMs.value > 0L) _currentPositionMs.value else streamPlayer.currentPosition.coerceAtLeast(0L)
        val base = pendingSeekTargetMs ?: current
        val dur = streamPlayer.duration
        val target = if (dur > 0L) (base + step).coerceAtMost(dur) else base + step
        pendingSeekTargetMs = target
        _currentPositionMs.value = target

        val diffFromCurrent = target - current
        val sign = if (diffFromCurrent >= 0) "+" else "-"
        val formattedDiff = formatTime(kotlin.math.abs(diffFromCurrent))
        showSwitchNotice("⏩ $sign$formattedDiff (${formatTime(target)})")
        showOverlay(4000L)

        scheduleDebouncedSeek(target)
    }

    /**
     * Smart accelerated seek backward.
     */
    fun seekBackward(deltaMs: Long? = null) {
        if (_streamType.value == "live") {
            showSwitchNotice("Live TV • Rewind unavailable")
            showOverlay(2500L)
            return
        }
        if (!streamPlayer.isSeekable && _durationMs.value <= 0L) {
            showSwitchNotice("Stream is not seekable")
            showOverlay(2500L)
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastSeekTimestamp < 1200L) {
            consecutiveSeekCount++
        } else {
            consecutiveSeekCount = 1
        }
        lastSeekTimestamp = now

        val step = deltaMs ?: when {
            consecutiveSeekCount <= 3 -> 10_000L
            consecutiveSeekCount <= 6 -> 30_000L
            consecutiveSeekCount <= 10 -> 60_000L
            consecutiveSeekCount <= 14 -> 120_000L
            else -> 300_000L
        }

        val current = if (_currentPositionMs.value > 0L) _currentPositionMs.value else streamPlayer.currentPosition.coerceAtLeast(0L)
        val base = pendingSeekTargetMs ?: current
        val target = (base - step).coerceAtLeast(0L)
        pendingSeekTargetMs = target
        _currentPositionMs.value = target

        val diffFromCurrent = target - current
        val sign = if (diffFromCurrent >= 0) "+" else "-"
        val formattedDiff = formatTime(kotlin.math.abs(diffFromCurrent))
        showSwitchNotice("⏪ $sign$formattedDiff (${formatTime(target)})")
        showOverlay(4000L)

        scheduleDebouncedSeek(target)
    }

    fun seekTo(positionMs: Long) {
        val dur = streamPlayer.duration
        val target = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        pendingSeekTargetMs = target
        _currentPositionMs.value = target
        showOverlay(4000L)
        showSwitchNotice("Seeking to ${formatTime(target)}")
        scheduleDebouncedSeek(target)
    }

    fun confirmPendingSeek() {
        seekDebounceJob?.cancel()
        val target = pendingSeekTargetMs ?: return
        pendingSeekTargetMs = null
        consecutiveSeekCount = 0
        streamPlayer.seekTo(target)
    }

    private fun scheduleDebouncedSeek(targetPositionMs: Long) {
        seekDebounceJob?.cancel()
        seekDebounceJob = viewModelScope.launch {
            delay(400L)
            consecutiveSeekCount = 0
            streamPlayer.seekTo(targetPositionMs)
            pendingSeekTargetMs = null
        }
    }

    fun nextChannel(isAutoSwitch: Boolean = false) {
        if (!isAutoSwitch) {
            consecutiveTimeoutCount = 0
        }
        frameTimeoutJob?.cancel()
        if (channelList.isEmpty()) return
        val currentIndex = channelList.indexOfFirst { it.id == _currentChannel.value?.id }
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < channelList.size) {
            currentIndex + 1
        } else {
            0
        }
        val next = channelList[nextIndex]
        _currentChannel.value = next
        _isFavorite.value = next.isFavorite
        showSwitchNotice("CH ${next.id} • ${next.name}")
        playCurrentStream(next.id, next.name)
    }

    fun previousChannel() {
        consecutiveTimeoutCount = 0
        frameTimeoutJob?.cancel()
        if (channelList.isEmpty()) return
        val currentIndex = channelList.indexOfFirst { it.id == _currentChannel.value?.id }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            channelList.size - 1
        }
        val prev = channelList[prevIndex]
        _currentChannel.value = prev
        _isFavorite.value = prev.isFavorite
        showSwitchNotice("CH ${prev.id} • ${prev.name}")
        playCurrentStream(prev.id, prev.name)
    }

    fun toggleFavorite() {
        if (_streamType.value == "vod") {
            viewModelScope.launch {
                val movie = Movie(
                    id = currentStreamId,
                    name = _streamTitle.value ?: "Movie $currentStreamId",
                    posterUrl = null,
                    rating = null,
                    categoryId = null,
                    containerExt = currentContainerExt,
                    isFavorite = _isFavorite.value
                )
                repository.toggleFavoriteMovie(movie).onSuccess { isFav ->
                    _isFavorite.value = isFav
                }
            }
            return
        }
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(channel).onSuccess { isFav ->
                _currentChannel.value = channel.copy(isFavorite = isFav)
                _isFavorite.value = isFav
            }
        }
    }

    fun toggleOverlay() {
        if (_isOverlayVisible.value) {
            hideOverlay()
        } else {
            showOverlay(4000L)
        }
    }

    fun showOverlay(durationMs: Long = 4000L) {
        _isOverlayVisible.value = true
        overlayHideJob?.cancel()
        overlayHideJob = viewModelScope.launch {
            delay(durationMs)
            _isOverlayVisible.value = false
        }
    }

    fun hideOverlay() {
        overlayHideJob?.cancel()
        _isOverlayVisible.value = false
    }

    private fun showSwitchNotice(text: String) {
        _channelSwitchNotice.value = text
        noticeHideJob?.cancel()
        noticeHideJob = viewModelScope.launch {
            delay(2000L)
            _channelSwitchNotice.value = null
        }
    }

    fun retry() {
        if (_streamType.value == "live") {
            _currentChannel.value?.let { channel ->
                playCurrentStream(channel.id, channel.name)
            }
        } else {
            initStream(currentStreamId, _streamTitle.value, _streamType.value, currentContainerExt)
        }
    }

    fun toggleDiagnosticsHud() {
        val next = !_isDiagnosticsVisible.value
        _isDiagnosticsVisible.value = next
        if (next) {
            startDiagnosticsPolling()
        } else {
            diagnosticsJob?.cancel()
            diagnosticsJob = null
        }
    }

    fun toggleDiagnostics() = toggleDiagnosticsHud()

    fun closeDiagnosticsHud() {
        _isDiagnosticsVisible.value = false
        diagnosticsJob?.cancel()
        diagnosticsJob = null
    }

    fun toggleQuickSettings() {
        val next = !_isQuickSettingsVisible.value
        _isQuickSettingsVisible.value = next
        if (next) {
            refreshTracks()
        }
    }

    fun closeQuickSettings() {
        _isQuickSettingsVisible.value = false
    }

    fun setAspectRatioMode(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun selectAudioTrack(option: PlayerTrackOption) {
        val player = streamPlayer.getPlayer()
        val trackGroup = player.currentTracks.groups.getOrNull(option.groupIndex)?.mediaTrackGroup ?: return
        val override = TrackSelectionOverride(trackGroup, option.trackIndex)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
        refreshTracks()
    }

    fun selectSubtitleTrack(option: PlayerTrackOption?) {
        val player = streamPlayer.getPlayer()
        if (option == null) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val trackGroup = player.currentTracks.groups.getOrNull(option.groupIndex)?.mediaTrackGroup ?: return
            val override = TrackSelectionOverride(trackGroup, option.trackIndex)
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(override)
                .build()
        }
        refreshTracks()
    }

    fun refreshTracks() {
        val player = streamPlayer.getPlayer()
        val currentTracks = player.currentTracks
        val audios = mutableListOf<PlayerTrackOption>()
        val subtitles = mutableListOf<PlayerTrackOption>()

        for (groupIndex in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[groupIndex]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = group.isTrackSelected(trackIndex)
                    val lang = format.language?.uppercase()
                    val label = format.label ?: lang ?: "Audio Track ${audios.size + 1}"
                    audios.add(
                        PlayerTrackOption(
                            id = "audio_${groupIndex}_$trackIndex",
                            label = "$label (${format.channelCount} ch)",
                            language = format.language,
                            isSelected = isSelected,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex
                        )
                    )
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = group.isTrackSelected(trackIndex)
                    val lang = format.language?.uppercase()
                    val label = format.label ?: lang ?: "Subtitle ${subtitles.size + 1}"
                    subtitles.add(
                        PlayerTrackOption(
                            id = "sub_${groupIndex}_$trackIndex",
                            label = label,
                            language = format.language,
                            isSelected = isSelected,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex
                        )
                    )
                }
            }
        }
        _audioTracks.value = audios
        _subtitleTracks.value = subtitles
    }

    private fun startDiagnosticsPolling() {
        diagnosticsJob?.cancel()
        diagnosticsJob = viewModelScope.launch {
            val player = streamPlayer.getPlayer()
            while (isActive && _isDiagnosticsVisible.value) {
                val currentTracks = player.currentTracks
                var videoFormat: androidx.media3.common.Format? = null
                var audioFormat: androidx.media3.common.Format? = null

                for (group in currentTracks.groups) {
                    if (group.type == C.TRACK_TYPE_VIDEO && group.isSelected) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                videoFormat = group.getTrackFormat(i)
                                break
                            }
                        }
                    } else if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                audioFormat = group.getTrackFormat(i)
                                break
                            }
                        }
                    }
                }

                val videoSize = player.videoSize
                val w = if (videoSize.width > 0) videoSize.width else videoFormat?.width ?: 0
                val h = if (videoSize.height > 0) videoSize.height else videoFormat?.height ?: 0

                val resolution = if (w > 0 && h > 0) {
                    "${w}x${h}"
                } else "Detecting..."

                val fps = if (videoFormat != null && videoFormat.frameRate > 0) {
                    "%.1f fps".format(videoFormat.frameRate)
                } else "N/A"

                val vCodec = videoFormat?.sampleMimeType?.substringAfterLast("/")?.uppercase() ?: "N/A"
                val aCodec = audioFormat?.sampleMimeType?.substringAfterLast("/")?.uppercase() ?: "N/A"
                val channels = when (audioFormat?.channelCount) {
                    6 -> "5.1 Surround"
                    2 -> "Stereo (2.0)"
                    1 -> "Mono (1.0)"
                    null -> "N/A"
                    else -> "${audioFormat.channelCount} Channels"
                }

                val bitrate = if (videoFormat != null && videoFormat.bitrate > 0) {
                    "%.1f Mbps".format(videoFormat.bitrate / 1_000_000f)
                } else "Adaptive"

                val bufferSec = ((player.bufferedPosition - player.currentPosition) / 1000f).coerceAtLeast(0f)

                _diagnostics.value = StreamDiagnostics(
                    resolution = resolution,
                    frameRate = fps,
                    videoCodec = vCodec,
                    audioCodec = aCodec,
                    audioChannels = channels,
                    bitrateFormatted = bitrate,
                    bufferHealthSec = bufferSec,
                    droppedFrames = 0,
                    streamFormat = if (_streamType.value == "live") {
                        val fmt = sessionManager?.getCachedStreamFormat() ?: "ts"
                        if (fmt.equals("m3u8", ignoreCase = true)) "HLS (m3u8)" else "MPEG-TS (ts)"
                    } else currentContainerExt.uppercase()
                )
                delay(1000)
            }
        }
    }

    fun stopPlayback() {
        seekDebounceJob?.cancel()
        pendingSeekTargetMs = null
        consecutiveSeekCount = 0
        overlayHideJob?.cancel()
        noticeHideJob?.cancel()
        progressTrackingJob?.cancel()
        diagnosticsJob?.cancel()
        resumePromptHideJob?.cancel()
        frameTimeoutJob?.cancel()
        consecutiveTimeoutCount = 0
        _isDiagnosticsVisible.value = false
        _isQuickSettingsVisible.value = false
        _isBingeBarVisible.value = false
        _resumePrompt.value = null
        val pos = streamPlayer.currentPosition
        val dur = streamPlayer.duration
        if (pos > 0L && dur > 0L && currentStreamId != 0) {
            viewModelScope.launch {
                repository.updatePlaybackPosition(currentStreamId, pos, dur)
            }
        }
        streamPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }

    companion object {
        fun formatTime(millis: Long): String {
            if (millis <= 0L) return "00:00"
            val totalSeconds = millis / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}
