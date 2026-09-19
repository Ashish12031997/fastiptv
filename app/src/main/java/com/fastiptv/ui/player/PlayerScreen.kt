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
    val aspectRatioMode by viewModel.aspectRatioMode.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val subtitleTracks by viewModel.subtitleTracks.collectAsState()

    val nextEpisode by viewModel.nextEpisode.collectAsState()
    val isBingeBarVisible by viewModel.isBingeBarVisible.collectAsState()
    val bingeCountdownSeconds by viewModel.bingeCountdownSeconds.collectAsState()
    val resumePrompt by viewModel.resumePrompt.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(streamId, streamType, containerExt, seriesId) {
        viewModel.initStream(streamId, streamTitle, streamType, containerExt, seriesId)
        focusRequester.requestFocus()
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
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_CHANNEL_UP,
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible && streamType == "live") {
                                viewModel.previousChannel()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible && streamType == "live") {
                                viewModel.nextChannel()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                        KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                viewModel.seekForward()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND,
                        KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                viewModel.seekBackward()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                viewModel.seekBackward()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                viewModel.seekForward()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                if (streamType == "live") {
                                    viewModel.nextChannel()
                                } else {
                                    viewModel.seekForward(30_000L)
                                }
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (!isQuickSettingsVisible && !isDiagnosticsVisible) {
                                if (streamType == "live") {
                                    viewModel.previousChannel()
                                } else {
                                    viewModel.seekBackward(30_000L)
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
                                viewModel.toggleOverlay()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (isQuickSettingsVisible) {
                                viewModel.closeQuickSettings()
                                true
                            } else if (isDiagnosticsVisible) {
                                viewModel.closeDiagnosticsHud()
                                true
                            } else if (isOverlayVisible) {
                                viewModel.toggleOverlay()
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

        // Transient Channel Switch Pill
        AnimatedVisibility(
            visible = switchNotice != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC14141F))
                    .border(1.dp, AccentBlue, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = switchNotice.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        }

        // Auto-Hiding Controls & EPG Overlay
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient & Channel Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 36.dp, vertical = 28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isLive = streamType == "live"
                        val isVod = streamType == "vod"
                        val badgeColor = if (isLive) LiveRed else AccentBlue
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                                Text(
                                    text = if (isLive) "LIVE" else if (isVod) "VOD" else "SERIES",
                                    color = badgeColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "• ${containerExt?.uppercase() ?: if (isLive) "HLS" else "MP4"}",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentChannel?.name ?: streamTitle ?: if (isLive) "Live Stream" else if (isVod) "Movie $streamId" else "Episode $streamId",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Action Buttons: Stats HUD, Quick Settings, Favorite
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stats HUD Toggle Button
                            Card(
                                onClick = { viewModel.toggleDiagnostics() },
                                modifier = Modifier.clickable { viewModel.toggleDiagnostics() },
                                colors = CardDefaults.colors(
                                    containerColor = if (isDiagnosticsVisible) AccentBlue.copy(alpha = 0.25f) else DarkSurface,
                                    focusedContainerColor = DarkSurfaceElevated
                                ),
                                border = CardDefaults.border(
                                    border = Border(border = BorderStroke(1.dp, if (isDiagnosticsVisible) AccentBlue else Color.White.copy(alpha = 0.2f))),
                                    focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
                                ),
                                shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "📊 Stats",
                                    color = if (isDiagnosticsVisible) AccentBlue else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }

                            // Quick Settings Drawer Toggle Button
                            Card(
                                onClick = { viewModel.toggleQuickSettings() },
                                modifier = Modifier.clickable { viewModel.toggleQuickSettings() },
                                colors = CardDefaults.colors(
                                    containerColor = if (isQuickSettingsVisible) AccentBlue.copy(alpha = 0.25f) else DarkSurface,
                                    focusedContainerColor = DarkSurfaceElevated
                                ),
                                border = CardDefaults.border(
                                    border = Border(border = BorderStroke(1.dp, if (isQuickSettingsVisible) AccentBlue else Color.White.copy(alpha = 0.2f))),
                                    focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
                                ),
                                shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "⚙ Settings",
                                    color = if (isQuickSettingsVisible) AccentBlue else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }

                            // Favorite Button
                            val favActive = isFavorite || currentChannel?.isFavorite == true
                            Card(
                                onClick = { viewModel.toggleFavorite() },
                                modifier = Modifier.clickable { viewModel.toggleFavorite() },
                                colors = CardDefaults.colors(
                                    containerColor = if (favActive) AccentBlue.copy(alpha = 0.2f) else DarkSurface,
                                    focusedContainerColor = DarkSurfaceElevated
                                ),
                                border = CardDefaults.border(
                                    border = Border(border = BorderStroke(1.dp, if (favActive) AccentBlue else Color.White.copy(alpha = 0.2f))),
                                    focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
                                ),
                                shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = if (favActive) "★ Favorited" else "☆ Add to Favorites",
                                    color = if (favActive) AccentBlue else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Gradient & EPG Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 36.dp, vertical = 28.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (streamType == "live") {
                            if (currentProgram != null) {
                                Text(
                                    text = "NOW PLAYING",
                                    color = AccentBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentProgram.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                if (!currentProgram.description.isNullOrBlank()) {
                                    Text(
                                        text = currentProgram.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { currentProgram.progressPercent },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = AccentBlue,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                            } else {
                                Text(
                                    text = "Program guide unavailable for this stream",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        } else {
                            // VOD & TV Series Playback Controls
                            val progress = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = streamTitle ?: "Video",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "${PlayerViewModel.formatTime(currentPositionMs)} / ${PlayerViewModel.formatTime(durationMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentBlue,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.seekBackward(60000L) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = DarkSurfaceElevated,
                                        focusedContainerColor = AccentBlue
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                ) {
                                    Text(text = "⏪ -1m", color = TextWhite, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.seekBackward(10000L) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = DarkSurfaceElevated,
                                        focusedContainerColor = AccentBlue
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                ) {
                                    Text(text = "⏪ -10s", color = TextWhite, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.togglePlayPause() },
                                    colors = ButtonDefaults.colors(
                                        containerColor = AccentBlue,
                                        focusedContainerColor = AccentBlue.copy(alpha = 0.85f)
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = if (isPlaying) "⏸ Pause" else "▶ Play",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Button(
                                    onClick = { viewModel.seekForward(10000L) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = DarkSurfaceElevated,
                                        focusedContainerColor = AccentBlue
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                ) {
                                    Text(text = "⏩ +10s", color = TextWhite, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.seekForward(60000L) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = DarkSurfaceElevated,
                                        focusedContainerColor = AccentBlue
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                ) {
                                    Text(text = "⏩ +1m", color = TextWhite, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.seekForward(300000L) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = DarkSurfaceElevated,
                                        focusedContainerColor = AccentBlue
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                ) {
                                    Text(text = "⏩ +5m", color = TextWhite, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Buffering / Loading Indicator
        if (playerState is PlayerState.Buffering || playerState is PlayerState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AccentBlue,
                    modifier = Modifier.size(54.dp),
                    strokeWidth = 4.dp
                )
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
                currentAspectRatio = aspectRatioMode,
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                onSelectAspectRatio = { viewModel.setAspectRatioMode(it) },
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
