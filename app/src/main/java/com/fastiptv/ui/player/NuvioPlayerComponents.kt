package com.fastiptv.ui.player

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.player.AspectRatioMode
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.LiveRed
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

/**
 * Nuvio-inspired Top Bar with Back Button, Live/VOD badges, Title, Format tags,
 * Real-time clock & Projected End-time, and Quick action icon buttons.
 */
@Composable
fun NuvioPlayerHeader(
    title: String,
    subtitle: String?,
    streamType: String,
    containerExt: String?,
    isFavorite: Boolean,
    isDiagnosticsVisible: Boolean,
    isQuickSettingsVisible: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDiagnostics: () -> Unit,
    onToggleQuickSettings: () -> Unit,
    backFocusRequester: FocusRequester? = null,
    favoriteFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val isLive = streamType == "live"
    val isVod = streamType == "vod"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.92f),
                        Color.Black.copy(alpha = 0.65f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Back Button + Badges + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Back Circle Button
                NuvioHeaderIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBackClick,
                    focusRequester = backFocusRequester,
                    downFocusRequester = downFocusRequester
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Classification & Format chips row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLive) {
                            // Pulsing live indicator
                            val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "PulseAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LiveRed.copy(alpha = 0.2f))
                                    .border(1.dp, LiveRed.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(LiveRed.copy(alpha = pulseAlpha))
                                    )
                                    Text(
                                        text = "LIVE",
                                        color = LiveRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        } else {
                            val badgeColor = if (isVod) Color(0xFF38BDF8) else Color(0xFFA855F7)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isVod) "VOD" else "SERIES",
                                    color = badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Quality Tags
                        NuvioQualityTag(text = containerExt?.uppercase() ?: if (isLive) "HLS" else "MP4")
                        NuvioQualityTag(text = "FHD")
                        NuvioQualityTag(text = "5.1")
                    }

                    // Main Stream Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle / EPG show
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Right Group: Real-time Clock + Projected End Time + Quick Action Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Clock & Projected End Time Pill
                val clock = PlayerViewModel.formatClockTime()
                val endTime = if (!isLive && durationMs > 0L) {
                    PlayerViewModel.formatEndTime(currentPositionMs, durationMs)
                } else null

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x3310141E))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = clock,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        if (endTime != null) {
                            Text(
                                text = "• $endTime",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Favorite Button
                NuvioHeaderIconButton(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) LiveRed else TextWhite,
                    onClick = onToggleFavorite,
                    focusRequester = favoriteFocusRequester,
                    downFocusRequester = downFocusRequester
                )

                // Stats HUD Button
                NuvioHeaderIconButton(
                    icon = Icons.Rounded.Analytics,
                    contentDescription = "Stats",
                    isActive = isDiagnosticsVisible,
                    onClick = onToggleDiagnostics,
                    downFocusRequester = downFocusRequester
                )

                // Settings Drawer Button
                NuvioHeaderIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    isActive = isQuickSettingsVisible,
                    onClick = onToggleQuickSettings,
                    downFocusRequester = downFocusRequester
                )
            }
        }
    }
}

@Composable
fun NuvioQualityTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33FFFFFF))
            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NuvioHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    tint: Color = TextWhite,
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        label = "HeaderIconScale"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = CardDefaults.colors(
            containerColor = if (isActive) AccentBlue.copy(alpha = 0.35f) else Color(0x331E293B),
            focusedContainerColor = DarkSurfaceElevated
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.dp, if (isActive) AccentBlue else GlassBorder)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, Color(0xFF38BDF8)))
        ),
        shape = CardDefaults.shape(shape = CircleShape),
        modifier = modifier
            .size(42.dp)
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                if (downFocusRequester != null) {
                    down = downFocusRequester
                }
            }
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color(0xFF38BDF8) else if (isActive) AccentBlue else tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Interactive TV Seekbar with dual-track (network buffer + playback progress),
 * focusable glowing cursor/thumb, and remote D-pad Left/Right scrubbing.
 */
@Composable
fun NuvioInteractiveTvSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeekRelative: (Long) -> Unit,
    onCommitSeek: () -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val progress = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val bufferProgress = if (durationMs > 0L) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val trackHeight by animateDpAsState(
        targetValue = if (isFocused) 8.dp else 5.dp,
        label = "SeekBarHeight"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (isFocused) 18.dp else 12.dp,
        label = "ThumbSize"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp)
    ) {
        // Time row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = PlayerViewModel.formatTime(currentPositionMs),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) Color(0xFF38BDF8) else TextWhite
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (durationMs > currentPositionMs) {
                    Text(
                        text = "-${PlayerViewModel.formatTime(durationMs - currentPositionMs)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
                Text(
                    text = PlayerViewModel.formatTime(durationMs),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Focusable Track Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .focusRequester(focusRequester)
                .focusProperties {
                    if (upFocusRequester != null) up = upFocusRequester
                    if (downFocusRequester != null) down = downFocusRequester
                }
                .focusable(interactionSource = interactionSource)
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                onSeekRelative(-10_000L)
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                onSeekRelative(10_000L)
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                onCommitSeek()
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(Color.White.copy(alpha = 0.18f))
            )

            // Buffered Cache Track
            if (bufferProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferProgress)
                        .height(trackHeight)
                        .clip(RoundedCornerShape(trackHeight / 2))
                        .background(Color.White.copy(alpha = 0.38f))
                )
            }

            // Playback Progress Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                        )
                    )
            )

            // Glowing TV Cursor / Thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtLeast(0.01f))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(Color.White)
                        .then(
                            if (isFocused) {
                                Modifier.border(2.5.dp, Color(0xFF38BDF8), CircleShape)
                            } else Modifier
                        )
                )
            }
        }
    }
}

/**
 * Nuvio-style Frosted Glass Control Dock.
 * Features a prominent centered Hero Play/Pause button flanked by sleek circular vector icon buttons.
 */
@Composable
fun NuvioPlayerControlDock(
    streamType: String,
    isPlaying: Boolean,
    aspectRatioMode: AspectRatioMode,
    playbackSpeed: Float,
    isSeries: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onCyclePlaybackSpeed: () -> Unit,
    heroPlayPauseFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val isLive = streamType == "live"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xDD0D121D))
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Previous / Restart Button
            NuvioDockIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = if (isLive) "Previous Channel" else "Restart",
                onClick = onPrevious,
                upFocusRequester = upFocusRequester
            )

            // Rewind 10s (VOD only)
            if (!isLive) {
                NuvioDockIconButton(
                    icon = Icons.Rounded.Replay10,
                    contentDescription = "Rewind 10s",
                    onClick = { onSeekRelative(-10_000L) },
                    upFocusRequester = upFocusRequester
                )
            }

            // Center Hero Play/Pause Button
            HeroPlayPauseButton(
                isPlaying = isPlaying,
                onClick = onTogglePlayPause,
                focusRequester = heroPlayPauseFocusRequester,
                upFocusRequester = upFocusRequester
            )

            // Forward 10s (VOD only)
            if (!isLive) {
                NuvioDockIconButton(
                    icon = Icons.Rounded.Forward10,
                    contentDescription = "Forward 10s",
                    onClick = { onSeekRelative(10_000L) },
                    upFocusRequester = upFocusRequester
                )
            }

            // Next / Skip Button
            NuvioDockIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = if (isLive) "Next Channel" else if (isSeries) "Next Episode" else "Skip",
                onClick = onNext,
                upFocusRequester = upFocusRequester
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // Audio Tracks Button
            NuvioDockIconButton(
                icon = Icons.Rounded.Audiotrack,
                contentDescription = "Audio Tracks",
                onClick = onOpenAudioTracks,
                upFocusRequester = upFocusRequester
            )

            // Subtitle Tracks Button
            NuvioDockIconButton(
                icon = Icons.Rounded.Subtitles,
                contentDescription = "Subtitles",
                onClick = onOpenSubtitles,
                upFocusRequester = upFocusRequester
            )

            // Aspect Ratio Cycle Button
            NuvioDockIconButtonWithBadge(
                icon = Icons.Rounded.AspectRatio,
                badge = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> "FIT"
                    AspectRatioMode.FILL -> "FILL"
                    AspectRatioMode.ZOOM -> "ZOOM"
                },
                contentDescription = "Aspect Ratio: ${aspectRatioMode.title}",
                onClick = onCycleAspectRatio,
                upFocusRequester = upFocusRequester
            )

            // Playback Speed Button (VOD only)
            if (!isLive) {
                NuvioDockIconButtonWithBadge(
                    icon = Icons.Rounded.Speed,
                    badge = "${playbackSpeed}x",
                    contentDescription = "Playback Speed: ${playbackSpeed}x",
                    onClick = onCyclePlaybackSpeed,
                    upFocusRequester = upFocusRequester
                )
            }
        }
    }
}

/**
 * 62dp Hero Play/Pause Button with bright cyan focus ring and scale transform.
 */
@Composable
fun HeroPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        label = "HeroPlayScale"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = CardDefaults.colors(
            containerColor = Color(0xFF0284C7),
            focusedContainerColor = Color(0xFF0284C7)
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f))),
            focusedBorder = Border(border = BorderStroke(3.dp, Color(0xFF38BDF8)))
        ),
        shape = CardDefaults.shape(shape = CircleShape),
        modifier = modifier
            .size(62.dp)
            .scale(scale)
            .focusRequester(focusRequester)
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
            }
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0284C7), Color(0xFF2563EB))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
fun NuvioDockIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        label = "DockIconScale"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = CardDefaults.colors(
            containerColor = Color(0x331E293B),
            focusedContainerColor = DarkSurfaceElevated
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.dp, GlassBorder)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, Color(0xFF38BDF8)))
        ),
        shape = CardDefaults.shape(shape = CircleShape),
        modifier = modifier
            .size(46.dp)
            .scale(scale)
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
            }
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color(0xFF38BDF8) else TextWhite,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun NuvioDockIconButtonWithBadge(
    icon: ImageVector,
    badge: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        label = "DockBadgeIconScale"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = CardDefaults.colors(
            containerColor = Color(0x331E293B),
            focusedContainerColor = DarkSurfaceElevated
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.dp, GlassBorder)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, Color(0xFF38BDF8)))
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(23.dp)),
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
            }
    ) {
        Row(
            modifier = Modifier
                .height(46.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color(0xFF38BDF8) else TextWhite,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) Color(0xFF38BDF8) else TextMuted
            )
        }
    }
}

/**
 * Animated center transient HUD for remote feedback (Play/Pause, -10s, +10s).
 */
@Composable
fun NuvioCenterTransientHud(
    noticeText: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = noticeText != null,
        enter = scaleIn(initialScale = 0.8f) + fadeIn(),
        exit = scaleOut(targetScale = 0.8f) + fadeOut(),
        modifier = modifier
    ) {
        noticeText?.let { text ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xDD0F141F))
                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        }
    }
}

/**
 * Live TV Bottom EPG Card displaying now playing and upcoming show preview.
 */
@Composable
fun NuvioLiveBottomCard(
    currentProgram: EpgProgram?,
    channelName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0D121D))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOW PLAYING • $channelName",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (currentProgram != null) {
                Text(
                    text = currentProgram.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!currentProgram.description.isNullOrBlank()) {
                    Text(
                        text = currentProgram.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}
