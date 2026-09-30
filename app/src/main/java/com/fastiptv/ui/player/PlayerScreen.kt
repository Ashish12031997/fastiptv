package com.fastiptv.ui.player

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fastiptv.player.PlayerState
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.LiveRed
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamId: Int,
    streamTitle: String?,
    streamType: String = "live",
    containerExt: String? = null,
    seriesId: Int? = null,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val epgPrograms by viewModel.epgPrograms.collectAsState()
    val isOverlayVisible by viewModel.isOverlayVisible.collectAsState()
    val switchNotice by viewModel.channelSwitchNotice.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    val isDiagnosticsVisible by viewModel.isDiagnosticsVisible.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val isQuickSettingsVisible by viewModel.isQuickSettingsVisible.collectAsState()
    val quickSettingsTab by viewModel.quickSettingsTab.collectAsState()
    val aspectRatioMode by viewModel.aspectRatioMode.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val subtitleTracks by viewModel.subtitleTracks.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val bufferedPositionMs by viewModel.bufferedPositionMs.collectAsState()

    val nextEpisode by viewModel.nextEpisode.collectAsState()
    val isBingeBarVisible by viewModel.isBingeBarVisible.collectAsState()
    val bingeCountdownSeconds by viewModel.bingeCountdownSeconds.collectAsState()
    val resumePrompt by viewModel.resumePrompt.collectAsState()

    val rootFocusRequester = remember { FocusRequester() }
    val headerBackFocusRequester = remember { FocusRequester() }
    val headerFavoriteFocusRequester = remember { FocusRequester() }
    val seekBarFocusRequester = remember { FocusRequester() }
    val heroPlayPauseFocusRequester = remember { FocusRequester() }

    LaunchedEffect(streamId, streamType, containerExt, seriesId) {
        viewModel.initStream(streamId, streamTitle, streamType, containerExt, seriesId)
        try {
            rootFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(isOverlayVisible) {
        if (isOverlayVisible) {
            kotlinx.coroutines.delay(100L)
            try {
                heroPlayPauseFocusRequester.requestFocus()
            } catch (_: Exception) {
                try {
                    rootFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        } else {
            try {
                rootFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    BackHandler {
        if (isQuickSettingsVisible) {
            viewModel.closeQuickSettings()
        } else if (isDiagnosticsVisible) {
            viewModel.closeDiagnosticsHud()
        } else if (isBingeBarVisible) {
            viewModel.dismissBingeBar()
        } else if (resumePrompt != null) {
            viewModel.dismissResumePrompt()
        } else if (isOverlayVisible) {
            viewModel.toggleOverlay()
        } else {
            viewModel.stopPlayback()
            onBackPressed()
        }
    }

    val currentProgram = epgPrograms.firstOrNull { it.isNowPlaying } ?: epgPrograms.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    if (isOverlayVisible) {
                        viewModel.showOverlay(4000L)
                    }

                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_CHANNEL_UP,
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                if (!isOverlayVisible) {
                                    if (streamType == "live") {
                                        viewModel.previousChannel()
                                        true
                                    } else {
                                        viewModel.showOverlay(4000L)
                                        true
                                    }
                                } else false
                            } else false
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                if (!isOverlayVisible) {
                                    if (streamType == "live") {
                                        viewModel.nextChannel()
                                        true
                                    } else {
                                        viewModel.showOverlay(4000L)
                                        true
                                    }
                                } else false
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                        KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                viewModel.seekForward(10_000L)
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND,
                        KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                viewModel.seekBackward(10_000L)
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                if (!isOverlayVisible) {
                                    if (streamType != "live") {
                                        viewModel.seekBackward(10_000L)
                                        true
                                    } else {
                                        viewModel.showOverlay(4000L)
                                        true
                                    }
                                } else false
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                if (!isOverlayVisible) {
                                    if (streamType != "live") {
                                        viewModel.seekForward(10_000L)
                                        true
                                    } else {
                                        viewModel.showOverlay(4000L)
                                        true
                                    }
                                } else false
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                if (streamType == "live") {
                                    viewModel.nextChannel()
                                } else {
                                    viewModel.seekForward(10_000L)
                                }
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                if (streamType == "live") {
                                    viewModel.previousChannel()
                                } else {
                                    viewModel.seekBackward(10_000L)
                                }
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            viewModel.togglePlayPause()
                            true
                        }
                        KeyEvent.KEYCODE_MENU,
                        KeyEvent.KEYCODE_SETTINGS -> {
                            viewModel.toggleQuickSettings()
                            true
                        }
                        KeyEvent.KEYCODE_INFO,
                        KeyEvent.KEYCODE_PROG_GREEN -> {
                            viewModel.toggleDiagnostics()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (viewModel.hasPendingSeek()) {
                                viewModel.confirmPendingSeek()
                                true
                            } else if (!isQuickSettingsVisible) {
                                if (!isOverlayVisible) {
                                    viewModel.showOverlay(4000L)
                                    true
                                } else false
                            } else false
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (isQuickSettingsVisible) {
                                viewModel.closeQuickSettings()
                                true
                            } else if (isDiagnosticsVisible) {
                                viewModel.closeDiagnosticsHud()
                                true
                            } else if (isBingeBarVisible) {
                                viewModel.dismissBingeBar()
                                true
                            } else if (resumePrompt != null) {
                                viewModel.dismissResumePrompt()
                                true
                            } else if (isOverlayVisible) {
                                viewModel.hideOverlay()
                                true
                            } else {
                                viewModel.stopPlayback()
                                onBackPressed()
                                true
                            }
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Video Surface
        AndroidView(
            factory = { context ->
                val view = android.view.LayoutInflater.from(context).inflate(com.fastiptv.R.layout.view_player, null) as PlayerView
                view.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = viewModel.getPlayer()
                    keepScreenOn = true
                    isFocusable = false
                    setOnClickListener { viewModel.toggleOverlay() }
                    setEnableComposeSurfaceSyncWorkaround(true)
                    resizeMode = aspectRatioMode.modeInt
                }
            },
            update = { view ->
                view.resizeMode = aspectRatioMode.modeInt
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable { viewModel.toggleOverlay() }
        )

        // Nuvio Animated Center Transient HUD
        NuvioCenterTransientHud(
            noticeText = switchNotice,
            modifier = Modifier.align(Alignment.Center)
        )

        // Nuvio Auto-Hiding Controls & Header Overlay
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                NuvioPlayerHeader(
                    title = currentChannel?.name ?: streamTitle ?: if (streamType == "live") "Live Stream" else if (streamType == "vod") "Movie $streamId" else "Episode $streamId",
                    subtitle = if (streamType == "live") currentProgram?.title else null,
                    streamType = streamType,
                    containerExt = containerExt,
                    isFavorite = isFavorite || currentChannel?.isFavorite == true,
                    isDiagnosticsVisible = isDiagnosticsVisible,
                    isQuickSettingsVisible = isQuickSettingsVisible,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onBackClick = {
                        viewModel.stopPlayback()
                        onBackPressed()
                    },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onToggleDiagnostics = { viewModel.toggleDiagnostics() },
                    onToggleQuickSettings = { viewModel.toggleQuickSettings() },
                    backFocusRequester = headerBackFocusRequester,
                    favoriteFocusRequester = headerFavoriteFocusRequester,
                    downFocusRequester = if (streamType != "live") seekBarFocusRequester else heroPlayPauseFocusRequester,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Bottom Section: Seekbar + Control Dock or Live TV EPG Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (streamType == "live") {
                        NuvioLiveBottomCard(
                            currentProgram = currentProgram,
                            channelName = currentChannel?.name ?: streamTitle ?: "Live TV"
                        )
                    } else {
                        NuvioInteractiveTvSeekBar(
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            bufferedPositionMs = bufferedPositionMs,
                            onSeekRelative = { deltaMs -> viewModel.seekTo(currentPositionMs + deltaMs) },
                            onCommitSeek = { viewModel.confirmPendingSeek() },
                            focusRequester = seekBarFocusRequester,
                            upFocusRequester = headerBackFocusRequester,
                            downFocusRequester = heroPlayPauseFocusRequester
                        )
                    }

                    NuvioPlayerControlDock(
                        streamType = streamType,
                        isPlaying = isPlaying,
                        aspectRatioMode = aspectRatioMode,
                        playbackSpeed = playbackSpeed,
                        isSeries = streamType == "series",
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSeekRelative = { deltaMs ->
                            if (deltaMs > 0) viewModel.seekForward(deltaMs) else viewModel.seekBackward(-deltaMs)
                        },
                        onPrevious = {
                            if (streamType == "live") viewModel.previousChannel() else viewModel.restartFromBeginning()
                        },
                        onNext = {
                            if (streamType == "live") viewModel.nextChannel() else if (streamType == "series") viewModel.playNextEpisode() else viewModel.seekForward(30_000L)
                        },
                        onOpenAudioTracks = { viewModel.openAudioSettings() },
                        onOpenSubtitles = { viewModel.openSubtitleSettings() },
                        onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                        onCyclePlaybackSpeed = { viewModel.cyclePlaybackSpeed() },
                        heroPlayPauseFocusRequester = heroPlayPauseFocusRequester,
                        upFocusRequester = if (streamType != "live") seekBarFocusRequester else headerBackFocusRequester
                    )
                }
            }
        }

        // Buffering / Loading / Reconnecting Indicator
        if (playerState is PlayerState.Buffering || playerState is PlayerState.Loading || playerState is PlayerState.Reconnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (playerState is PlayerState.Reconnecting) {
                    val rec = playerState as PlayerState.Reconnecting
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xE60F172A))
                            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Column {
                                Text(
                                    text = "Reconnecting (${rec.attempt}/${rec.maxAttempts})...",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = rec.message ?: "Restoring connection...",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        color = AccentBlue,
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 4.dp
                    )
                }
            }
        }

        // Error State Overlay
        if (playerState is PlayerState.Error) {
            val error = playerState as PlayerState.Error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(32.dp)
                ) {
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = LiveRed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = error.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.retry() },
                            colors = ButtonDefaults.colors(
                                containerColor = AccentBlue,
                                focusedContainerColor = AccentBlue.copy(alpha = 0.8f)
                            )
                        ) {
                            Text("Retry Now", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.stopPlayback()
                                onBackPressed()
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = DarkSurfaceElevated
                            )
                        ) {
                            Text("Back", color = TextWhite)
                        }
                    }
                }
            }
        }

        // Real-Time Stream Diagnostics ("Nerd Stats") HUD
        AnimatedVisibility(
            visible = isDiagnosticsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 90.dp, end = 32.dp)
        ) {
            StreamDiagnosticsHud(
                diagnostics = diagnostics,
                onClose = { viewModel.closeDiagnosticsHud() }
            )
        }

        // In-Player Quick Settings Side Drawer
        AnimatedVisibility(
            visible = isQuickSettingsVisible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            PlayerQuickSettingsDrawer(
                initialTab = quickSettingsTab,
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
                onSelectSubtitleTrack = { viewModel.selectSubtitleTrack(it) },
                onClose = { viewModel.closeQuickSettings() }
            )
        }

        // Intelligent VOD / Series Resume Prompt Pill
        AnimatedVisibility(
            visible = resumePrompt != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isOverlayVisible) 170.dp else 40.dp)
        ) {
            resumePrompt?.let { prompt ->
                ResumePromptOverlay(
                    promptInfo = prompt,
                    onRestart = { viewModel.restartFromBeginning() },
                    onDismiss = { viewModel.dismissResumePrompt() }
                )
            }
        }

        // Next Episode Auto-Play Binge Bar Card
        AnimatedVisibility(
            visible = isBingeBarVisible && nextEpisode != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = if (isOverlayVisible) 170.dp else 40.dp)
        ) {
            nextEpisode?.let { next ->
                BingeBarOverlay(
                    nextEpisode = next,
                    countdownSeconds = bingeCountdownSeconds,
                    onPlayNext = { viewModel.playNextEpisode() },
                    onDismiss = { viewModel.dismissBingeBar() }
                )
            }
        }
    }
}
