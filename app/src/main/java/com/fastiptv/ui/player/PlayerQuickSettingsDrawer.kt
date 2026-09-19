package com.fastiptv.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.fastiptv.player.AspectRatioMode
import com.fastiptv.player.PlayerTrackOption
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

@Composable
fun PlayerQuickSettingsDrawer(
    currentAspectRatio: AspectRatioMode,
    audioTracks: List<PlayerTrackOption>,
    subtitleTracks: List<PlayerTrackOption>,
    onSelectAspectRatio: (AspectRatioMode) -> Unit,
    onSelectAudioTrack: (PlayerTrackOption) -> Unit,
    onSelectSubtitleTrack: (PlayerTrackOption?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Aspect Ratio", "Audio", "Subtitles")

    Box(
        modifier = modifier
            .width(420.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
            .background(Color(0xF20F131D)) // Frosted obsidian glass
            .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "⚙",
                        fontSize = 18.sp
                    )
                    Text(
                        text = "QUICK SETTINGS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 18.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                indicator = { _, _ -> }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onFocus = { selectedTab = index },
                        onClick = { selectedTab = index }
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (isSelected) AccentBlue else TextMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Aspect Ratio
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AspectRatioMode.values().forEach { mode ->
                            val isSelected = currentAspectRatio == mode
                            DrawerOptionCard(
                                title = mode.title,
                                subtitle = when (mode) {
                                    AspectRatioMode.FIT -> "Preserves 16:9 original source geometry"
                                    AspectRatioMode.FILL -> "Stretches 4:3 content to fill 16:9 screen"
                                    AspectRatioMode.ZOOM -> "Crops letterbox bars without distortion"
                                },
                                isSelected = isSelected,
                                onClick = { onSelectAspectRatio(mode) }
                            )
                        }
                    }
                }
                1 -> {
                    // Audio Tracks
                    if (audioTracks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Single audio stream available",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(audioTracks, key = { it.id }) { track ->
                                DrawerOptionCard(
                                    title = track.label,
                                    subtitle = if (track.language != null) "Language: ${track.language}" else "Standard Track",
                                    isSelected = track.isSelected,
                                    onClick = { onSelectAudioTrack(track) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Subtitle Tracks
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "sub_off") {
                            val isOff = subtitleTracks.none { it.isSelected }
                            DrawerOptionCard(
                                title = "Off (Captions Disabled)",
                                subtitle = "Do not display subtitle stream",
                                isSelected = isOff,
                                onClick = { onSelectSubtitleTrack(null) }
                            )
                        }
                        items(subtitleTracks, key = { it.id }) { track ->
                            DrawerOptionCard(
                                title = track.label,
                                subtitle = if (track.language != null) "Language: ${track.language}" else "Embedded Track",
                                isSelected = track.isSelected,
                                onClick = { onSelectSubtitleTrack(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = if (isSelected) AccentBlue.copy(alpha = 0.15f) else DarkSurface,
            focusedContainerColor = DarkSurfaceElevated
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.dp, if (isSelected) AccentBlue else GlassBorder)),
            focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) AccentBlue else TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(AccentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
