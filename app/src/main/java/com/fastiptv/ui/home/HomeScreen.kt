package com.fastiptv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.ui.components.CatalogCategorySidebar
import com.fastiptv.ui.components.CategoryGroupHelper
import com.fastiptv.ui.components.ChannelCard
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkBackground
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.LiveRed
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onChannelClick: (Channel) -> Unit,
    onRecentClick: (RecentItem) -> Unit = {},
    modifier: Modifier = Modifier,
    initialCategoryId: String? = null,
    topNavFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val focusedChannel by viewModel.focusedChannel.collectAsState()
    val focusedProgram by viewModel.focusedProgram.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var isGridFocused by remember { mutableStateOf(false) }
    var isSidebarFocused by remember { mutableStateOf(false) }

    val sidebarFocusRequester = contentFocusRequester ?: remember { FocusRequester() }
    val gridFirstItemFocusRequester = remember { FocusRequester() }
    val spotlightWatchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(initialCategoryId) {
        if (!initialCategoryId.isNullOrBlank()) {
            viewModel.selectCategoryById(initialCategoryId)
        }
    }

    // Remote Back button:
    // 1. If user is browsing in the channel grid, Back focuses the category sidebar first
    // 2. If user is browsing in the sidebar, Back focuses the TopNavBar active tab (Home)
    BackHandler(enabled = isGridFocused) {
        try {
            sidebarFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler(enabled = isSidebarFocused && !isGridFocused) {
        try {
            topNavFocusRequester?.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = AccentBlue,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading Live Channels...",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite
                    )
                    Text(
                        text = "Syncing live channels from provider playlist...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Modern Category Sidebar with in-sidebar search & country/group chips
                CatalogCategorySidebar(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { cat ->
                        viewModel.selectCategory(cat)
                    },
                    contentFocusRequester = gridFirstItemFocusRequester,
                    sidebarFirstItemFocusRequester = sidebarFocusRequester,
                    topNavFocusRequester = topNavFocusRequester,
                    modifier = Modifier.onFocusChanged { isSidebarFocused = it.hasFocus }
                )

                // Right Column: Header + Live Spotlight Preview Banner + Channel Grid
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    // Category Header with Clean Name & Count
                    val parsed = remember(selectedCategory) {
                        selectedCategory?.let { CategoryGroupHelper.parse(it) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = parsed?.cleanName ?: "Live Channels",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = TextWhite,
                                    fontSize = 20.sp
                                )
                                if (parsed != null && parsed.groupName != "GENERAL") {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AccentBlue.copy(alpha = 0.2f))
                                            .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = parsed.groupName,
                                            color = AccentBlue,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${channels.size} Channels Available",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Spotlight Preview Banner
                    focusedChannel?.let { ch ->
                        HomeSpotlightHeader(
                            channel = ch,
                            programTitle = focusedProgram?.title,
                            programStartSec = focusedProgram?.startTimestamp,
                            programEndSec = focusedProgram?.endTimestamp,
                            watchFocusRequester = spotlightWatchFocusRequester,
                            gridFocusRequester = gridFirstItemFocusRequester,
                            onWatchClick = { onChannelClick(ch) },
                            onToggleFavorite = { viewModel.toggleFavorite(ch) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Channel Grid
                    if (channels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No live channels found in this category.",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    isGridFocused = focusState.hasFocus
                                }
                        ) {
                            itemsIndexed(channels, key = { _, ch -> ch.id }) { index, ch ->
                                val itemModifier = if (index == 0) {
                                    Modifier
                                        .focusRequester(gridFirstItemFocusRequester)
                                        .focusProperties {
                                            left = sidebarFocusRequester
                                            up = spotlightWatchFocusRequester
                                        }
                                } else {
                                    Modifier
                                }

                                ChannelCard(
                                    channel = ch,
                                    onClick = { onChannelClick(ch) },
                                    onFocus = { viewModel.setFocusedChannel(ch) },
                                    modifier = itemModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSpotlightHeader(
    channel: Channel,
    programTitle: String?,
    programStartSec: Long?,
    programEndSec: Long?,
    watchFocusRequester: FocusRequester,
    gridFocusRequester: FocusRequester,
    onWatchClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "home_pulse_spotlight")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        onClick = onWatchClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onWatchClick() },
        scale = CardDefaults.scale(focusedScale = 1.05f),
        colors = CardDefaults.colors(
            containerColor = DarkSurface.copy(alpha = 0.9f),
            focusedContainerColor = DarkSurfaceElevated
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.dp, GlassBorder)),
            focusedBorder = Border(border = BorderStroke(3.dp, Color(0xFF60A5FA)))
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Channel Logo / Initials Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = channel.name.take(3).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = AccentBlue,
                        fontSize = 18.sp
                    )
                }
            }

            // Channel & Airing Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // LIVE pulsing badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LiveRed.copy(alpha = 0.2f))
                            .border(1.dp, LiveRed.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "LIVE",
                                color = LiveRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Currently Airing Program Info
                if (!programTitle.isNullOrBlank()) {
                    Text(
                        text = programTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Progress bar & Time range if start & end are provided
                    if (programStartSec != null && programEndSec != null && programEndSec > programStartSec) {
                        val nowSec = System.currentTimeMillis() / 1000
                        val progress = ((nowSec - programStartSec).toFloat() / (programEndSec - programStartSec).toFloat())
                            .coerceIn(0f, 1f)

                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val startTimeStr = timeFormat.format(Date(programStartSec * 1000))
                        val endTimeStr = timeFormat.format(Date(programEndSec * 1000))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                progress = { progress },
                                color = AccentBlue,
                                trackColor = DarkSurfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                            Text(
                                text = "$startTimeStr - $endTimeStr",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Broadcast stream ready • Tap to watch live",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWatchClick,
                    modifier = Modifier
                        .focusRequester(watchFocusRequester)
                        .focusProperties {
                            down = gridFocusRequester
                        }
                        .clickable { onWatchClick() },
                    colors = ButtonDefaults.colors(
                        containerColor = AccentBlue,
                        focusedContainerColor = AccentBlue.copy(alpha = 0.85f)
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = "▶ Watch Live",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Button(
                    onClick = onToggleFavorite,
                    modifier = Modifier.clickable { onToggleFavorite() },
                    colors = ButtonDefaults.colors(
                        containerColor = DarkSurfaceElevated,
                        focusedContainerColor = DarkSurfaceElevated.copy(alpha = 0.8f)
                    ),
                    border = ButtonDefaults.border(
                        border = Border(border = BorderStroke(1.dp, GlassBorder)),
                        focusedBorder = Border(border = BorderStroke(1.5.dp, AccentBlue))
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = if (channel.isFavorite) "★ Favorited" else "☆ Favorite",
                        fontWeight = FontWeight.SemiBold,
                        color = if (channel.isFavorite) Color(0xFFFFD700) else TextWhite,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
