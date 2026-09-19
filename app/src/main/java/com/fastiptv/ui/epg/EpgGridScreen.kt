package com.fastiptv.ui.epg

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.ui.components.CatalogCategorySidebar
import com.fastiptv.ui.components.CategoryGroupHelper
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
fun EpgGridScreen(
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    topNavFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    viewModel: EpgGridViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val timeSlots by viewModel.timeSlots.collectAsState()
    val programsMap by viewModel.programsMap.collectAsState()
    val selectedProgram by viewModel.selectedProgram.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val horizontalGridScrollState = rememberScrollState()
    val gridFirstItemFocusRequester = remember { FocusRequester() }
    val sidebarFocusRequester = contentFocusRequester ?: remember { FocusRequester() }
    val spotlightWatchFocusRequester = remember { FocusRequester() }

    var isGridFocused by remember { mutableStateOf(false) }
    var isSidebarFocused by remember { mutableStateOf(false) }
    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var focusedProgram by remember { mutableStateOf<EpgProgram?>(null) }

    // Keep spotlight updated when channels or programs change
    LaunchedEffect(selectedCategory) {
        focusedChannel = null
        focusedProgram = null
    }

    LaunchedEffect(channels) {
        if (channels.isNotEmpty() && (focusedChannel == null || channels.none { it.id == focusedChannel?.id })) {
            focusedChannel = channels.first()
        }
    }

    LaunchedEffect(focusedChannel, programsMap) {
        focusedChannel?.let { ch ->
            val list = programsMap[ch.id] ?: emptyList()
            val nowSec = System.currentTimeMillis() / 1000
            val current = list.firstOrNull { it.startTimestamp <= nowSec && it.endTimestamp >= nowSec }
                ?: list.firstOrNull()
            focusedProgram = current
        }
    }

    // Remote Back button:
    // 1. If user is browsing inside the EPG grid, Back returns focus to the category sidebar
    // 2. If user is browsing inside the sidebar, Back focuses the TopNavBar active tab (TV Guide)
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
                    CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading TV Guide...",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite
                    )
                    Text(
                        text = "Fetching live channels and electronic program guide...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Modern Glassmorphic Category Sidebar with Search & Group Selector
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

                // Right Column: Category Header + EPG Spotlight Bar + Timeline Ruler + Matrix Grid
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .onFocusChanged { isGridFocused = it.hasFocus }
                ) {
                    // Category Header Title & Badges
                    val parsed = remember(selectedCategory) {
                        selectedCategory?.let { CategoryGroupHelper.parse(it) }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = parsed?.cleanName ?: "TV Guide",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                fontSize = 18.sp
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
                            text = "${channels.size} Channels in Guide",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // TV Guide Spotlight Bar (Channel & Focused Program Quick Actions)
                    focusedChannel?.let { ch ->
                        EpgSpotlightBar(
                            channel = ch,
                            program = focusedProgram,
                            watchFocusRequester = spotlightWatchFocusRequester,
                            gridFocusRequester = gridFirstItemFocusRequester,
                            onWatchNow = { onChannelClick(ch) },
                            onShowDetails = { prg -> viewModel.selectProgram(ch, prg) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // Timeline Header Ruler
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xE6101420))
                            .border(width = 1.dp, color = GlassBorder)
                            .padding(vertical = 8.dp)
                    ) {
                        // Fixed Left Gutter (Channels Label)
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .padding(start = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "CHANNELS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                letterSpacing = 1.sp,
                                fontSize = 12.sp
                            )
                        }

                        // Scrollable Time Intervals
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalGridScrollState)
                        ) {
                            timeSlots.forEach { slot ->
                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = slot.formattedTime,
                                        color = TextWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // Channels & Programs Matrix
                    if (isLoading && channels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(44.dp))
                        }
                    } else if (channels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No channels available for this category.",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                                ChannelGridRow(
                                    channel = channel,
                                    programs = programsMap[channel.id],
                                    timeSlots = timeSlots,
                                    scrollState = horizontalGridScrollState,
                                    modifier = if (index == 0) Modifier.focusRequester(gridFirstItemFocusRequester) else Modifier,
                                    upFocusRequester = if (index == 0) spotlightWatchFocusRequester else null,
                                    sidebarFocusRequester = sidebarFocusRequester,
                                    onChannelClick = { onChannelClick(channel) },
                                    onProgramClick = { program ->
                                        viewModel.selectProgram(channel, program)
                                    },
                                    onRowFocused = { ch, prg ->
                                        focusedChannel = ch
                                        focusedProgram = prg
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Program Detail Modal
        AnimatedVisibility(
            visible = selectedProgram != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedProgram?.let { (channel, program) ->
                EpgProgramDetailModal(
                    channel = channel,
                    program = program,
                    onWatchNow = {
                        viewModel.dismissProgramDetail()
                        onChannelClick(channel)
                    },
                    onDismiss = { viewModel.dismissProgramDetail() }
                )
            }
        }
    }
}

@Composable
private fun EpgSpotlightBar(
    channel: Channel,
    program: EpgProgram?,
    watchFocusRequester: FocusRequester,
    gridFocusRequester: FocusRequester,
    onWatchNow: () -> Unit,
    onShowDetails: (EpgProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onWatchNow,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable { onWatchNow() },
        colors = CardDefaults.colors(
            containerColor = DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceElevated
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.dp, GlassBorder)),
            focusedBorder = Border(border = BorderStroke(1.5.dp, AccentBlue))
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Channel info + Now Playing Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Channel Initials / Mini Logo Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A2234)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    } else {
                        Text(
                            text = channel.name.take(3).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(LiveRed)
                        )
                    }

                    if (program != null) {
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val startStr = timeFormat.format(Date(program.startTimestamp * 1000L))
                        val endStr = timeFormat.format(Date(program.endTimestamp * 1000L))
                        Text(
                            text = "${program.title} • $startStr - $endStr",
                            color = AccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Live Stream Ready",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Right: Watch & Details Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWatchNow,
                    modifier = Modifier
                        .focusRequester(watchFocusRequester)
                        .focusProperties {
                            down = gridFocusRequester
                        }
                        .clickable { onWatchNow() },
                    colors = ButtonDefaults.colors(
                        containerColor = AccentBlue,
                        focusedContainerColor = AccentBlue.copy(alpha = 0.85f)
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "▶ Watch Now",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                if (program != null) {
                    Button(
                        onClick = { onShowDetails(program) },
                        modifier = Modifier.clickable { onShowDetails(program) },
                        colors = ButtonDefaults.colors(
                            containerColor = DarkSurfaceElevated
                        ),
                        border = ButtonDefaults.border(
                            border = Border(border = BorderStroke(1.dp, GlassBorder)),
                            focusedBorder = Border(border = BorderStroke(1.5.dp, AccentBlue))
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "ℹ Details",
                            fontWeight = FontWeight.Medium,
                            color = TextWhite,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelGridRow(
    channel: Channel,
    programs: List<EpgProgram>?,
    timeSlots: List<EpgTimeSlot>,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    sidebarFocusRequester: FocusRequester? = null,
    onChannelClick: () -> Unit,
    onProgramClick: (EpgProgram) -> Unit,
    onRowFocused: (Channel, EpgProgram?) -> Unit = { _, _ -> }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(width = 0.5.dp, color = Color(0x26FFFFFF))
            .background(Color(0xFF0B0E14)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel Info Header (Left Column)
        Card(
            onClick = onChannelClick,
            modifier = modifier
                .width(180.dp)
                .height(72.dp)
                .clickable { onChannelClick() }
                .focusProperties {
                    if (upFocusRequester != null) {
                        up = upFocusRequester
                    }
                    if (sidebarFocusRequester != null) {
                        left = sidebarFocusRequester
                    }
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        val active = programs?.firstOrNull { it.isNowPlaying } ?: programs?.firstOrNull()
                        onRowFocused(channel, active)
                    }
                },
            colors = CardDefaults.colors(
                containerColor = DarkSurface,
                focusedContainerColor = DarkSurfaceElevated
            ),
            border = CardDefaults.border(
                border = Border(border = BorderStroke(0.5.dp, GlassBorder)),
                focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
            ),
            shape = CardDefaults.shape(shape = RoundedCornerShape(0.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Channel Initials Box
                val initials = channel.name.take(3).uppercase()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2536)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    } else {
                        Text(
                            text = initials,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "LIVE HD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LiveRed
                    )
                }
            }
        }

        // Horizontal Program Blocks
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (programs == null || programs.isEmpty()) {
                val isUpdating = programs == null
                // Placeholder Broadcast Block covering the timeline window
                val totalWidth = (timeSlots.size * 180).dp
                Card(
                    onClick = onChannelClick,
                    modifier = Modifier
                        .width(totalWidth)
                        .height(64.dp)
                        .padding(horizontal = 4.dp)
                        .clickable { onChannelClick() }
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onRowFocused(channel, null)
                            }
                        },
                    colors = CardDefaults.colors(
                        containerColor = DarkSurfaceElevated.copy(alpha = 0.5f),
                        focusedContainerColor = DarkSurfaceElevated
                    ),
                    border = CardDefaults.border(
                        border = Border(border = BorderStroke(1.dp, GlassBorder)),
                        focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
                    ),
                    shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isUpdating) Color(0xFFFFB300) else LiveRed)
                        )
                        Text(
                            text = if (isUpdating) "Updating guide..." else "Live Broadcast (No Guide Available)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
            } else {
                programs.forEach { program ->
                    val durationSec = (program.endTimestamp - program.startTimestamp).coerceAtLeast(900L)
                    val widthDp = ((durationSec / 1800f) * 180f).coerceIn(100f, 720f).dp

                    Card(
                        onClick = { onProgramClick(program) },
                        modifier = Modifier
                            .width(widthDp)
                            .height(64.dp)
                            .padding(horizontal = 4.dp)
                            .clickable { onProgramClick(program) }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onRowFocused(channel, program)
                                }
                            },
                        colors = CardDefaults.colors(
                            containerColor = if (program.isNowPlaying) Color(0xFF161F33) else DarkSurface,
                            focusedContainerColor = DarkSurfaceElevated
                        ),
                        border = CardDefaults.border(
                            border = Border(
                                border = BorderStroke(
                                    1.dp,
                                    if (program.isNowPlaying) AccentBlue.copy(alpha = 0.8f) else GlassBorder
                                )
                            ),
                            focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
                        ),
                        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (program.isNowPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(LiveRed)
                                    )
                                    Text(
                                        text = "NOW",
                                        color = LiveRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                                val startStr = timeFormat.format(Date(program.startTimestamp * 1000L))
                                val endStr = timeFormat.format(Date(program.endTimestamp * 1000L))
                                Text(
                                    text = "$startStr - $endStr",
                                    color = if (program.isNowPlaying) AccentBlue else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = program.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgProgramDetailModal(
    channel: Channel,
    program: EpgProgram,
    onWatchNow: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xF2121724))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(28.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (program.isNowPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                            Text(
                                text = "CURRENTLY AIRING",
                                color = LiveRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "UPCOMING PROGRAM",
                                color = AccentBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = channel.name,
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = program.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val startStr = timeFormat.format(Date(program.startTimestamp * 1000L))
                val endStr = timeFormat.format(Date(program.endTimestamp * 1000L))
                val durationMin = ((program.endTimestamp - program.startTimestamp) / 60).coerceAtLeast(1)
                Text(
                    text = "$startStr – $endStr • $durationMin mins",
                    color = AccentBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = program.description?.takeIf { it.isNotBlank() }
                        ?: "No synopsis available for this broadcast schedule.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onWatchNow,
                        colors = ButtonDefaults.colors(
                            containerColor = AccentBlue,
                            focusedContainerColor = AccentBlue.copy(alpha = 0.85f)
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onWatchNow() }
                    ) {
                        Text(
                            text = "▶ Watch Channel",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = DarkSurfaceElevated
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(text = "Close", color = TextWhite)
                    }
                }
            }
        }
    }
}
