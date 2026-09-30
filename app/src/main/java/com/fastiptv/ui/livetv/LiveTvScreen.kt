package com.fastiptv.ui.livetv

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.CategoryType
import com.fastiptv.domain.model.Channel
import com.fastiptv.player.PlayerState
import com.fastiptv.ui.components.CategoryGroupHelper
import com.fastiptv.ui.theme.AccentBlue
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
fun LiveTvScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val isBannerVisible by viewModel.isBannerVisible.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val currentEpgProgram by viewModel.currentEpgProgram.collectAsState()
    val switchNotice by viewModel.switchNotice.collectAsState()

    val rootFocusRequester = remember { FocusRequester() }
    val drawerCategoryFocusRequester = remember { FocusRequester() }
    val drawerChannelFocusRequester = remember { FocusRequester() }
    var isFocusedInChannels by remember { mutableStateOf(false) }

    // Request initial root focus for remote key events
    LaunchedEffect(Unit) {
        try {
            rootFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Auto-focus drawer when opened
    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) {
            isFocusedInChannels = false
            delay(100)
            try {
                drawerCategoryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            try {
                rootFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    BackHandler(enabled = isDrawerOpen && isFocusedInChannels) {
        try {
            drawerCategoryFocusRequester.requestFocus()
            isFocusedInChannels = false
        } catch (_: Exception) {}
    }

    BackHandler(enabled = isDrawerOpen && !isFocusedInChannels) {
        viewModel.closeDrawer()
    }

    BackHandler(enabled = !isDrawerOpen) {
        onBackPressed()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayer()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val code = keyEvent.nativeKeyEvent.keyCode
                    when (code) {
                        KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!isDrawerOpen) {
                                viewModel.previousChannel()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!isDrawerOpen) {
                                viewModel.nextChannel()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!isDrawerOpen) {
                                viewModel.openDrawer()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (!isDrawerOpen) {
                                viewModel.toggleBanner()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Full-screen Video Surface
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
                    setOnClickListener { viewModel.toggleBanner() }
                    setEnableComposeSurfaceSyncWorkaround(true)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable { viewModel.toggleBanner() }
        )

        // 2. Buffering / Loading Indicator
        if (playerState is PlayerState.Buffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LiveRed)
                    )
                    Text(
                        text = "Buffering Live Stream...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Indian Cable TV Bottom Banner (OSD)
        AnimatedVisibility(
            visible = isBannerVisible && !isDrawerOpen,
            enter = fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            currentChannel?.let { channel ->
                IndianCableBottomBanner(
                    channel = channel,
                    currentProgram = currentEpgProgram,
                    onLeftClick = { viewModel.openDrawer() }
                )
            }
        }

        // 4. Semi-Transparent 2-Column Channel Guide Drawer (Tata Play / Airtel DTH Style)
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(560.dp)
        ) {
            IndianCableSideDrawer(
                categories = categories,
                selectedCategory = selectedCategory,
                channels = channels,
                currentChannelId = currentChannel?.id,
                onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                onSelectChannel = { ch ->
                    viewModel.tuneToChannel(ch)
                    viewModel.closeDrawer()
                },
                onCloseDrawer = { viewModel.closeDrawer() },
                categoryFocusRequester = drawerCategoryFocusRequester,
                channelListFocusRequester = drawerChannelFocusRequester,
                onChannelFocused = { isFocusedInChannels = it }
            )
        }
    }
}

/**
 * Authentic Indian Cable TV / DTH Bottom Info Banner (Tata Play / Airtel style)
 */
@Composable
fun IndianCableBottomBanner(
    channel: Channel,
    currentProgram: com.fastiptv.domain.model.EpgProgram?,
    onLeftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTime = remember {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xE6050914),
                        Color(0xF5080E1E)
                    )
                )
            )
            .padding(horizontal = 36.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Big Channel Number + Logo + Name + Category
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Channel Number Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE5A00D))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${channel.id}",
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Channel Logo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = channel.name.take(2).uppercase(),
                            color = AccentBlue,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }

                // Channel Name & Program Info
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // LIVE Red Dot Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LiveRed)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Program title or category
                    Text(
                        text = currentProgram?.title ?: "Live Broadcast • Indian Regional & National Feed",
                        color = Color(0xFF93C5FD),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Group: Time + Quick Guide Hint
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                        .clickable { onLeftClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "◀ LEFT: Channel List  •  [Back]: Menu",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Semi-Transparent 2-Column Side Channel Guide (Tata Play / Airtel DTH Style)
 * Column 1 (Left): Categories / Genres
 * Column 2 (Right): Channels in the selected category
 */
@Composable
fun IndianCableSideDrawer(
    categories: List<Category>,
    selectedCategory: Category?,
    channels: List<Channel>,
    currentChannelId: Int?,
    onSelectCategory: (Category) -> Unit,
    onSelectChannel: (Channel) -> Unit,
    onCloseDrawer: () -> Unit,
    categoryFocusRequester: FocusRequester,
    channelListFocusRequester: FocusRequester,
    onChannelFocused: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var debounceCategoryJob by remember { mutableStateOf<Job?>(null) }
    val categoryListState = rememberLazyListState()
    val channelListState = rememberLazyListState()

    val allCategories = remember(categories) {
        val recentCat = Category(
            id = "RECENT",
            name = "⭐ Recent",
            type = CategoryType.LIVE
        )
        listOf(recentCat) + categories
    }

    val selectedCategoryIndex = remember(allCategories, selectedCategory) {
        val idx = allCategories.indexOfFirst { it.id == selectedCategory?.id }
        if (idx >= 0) idx else 0
    }

    val currentChannelIndex = remember(channels, currentChannelId) {
        val idx = channels.indexOfFirst { it.id == currentChannelId }
        if (idx >= 0) idx else 0
    }

    LaunchedEffect(selectedCategoryIndex) {
        if (selectedCategoryIndex in allCategories.indices) {
            try {
                categoryListState.scrollToItem((selectedCategoryIndex - 2).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(selectedCategory?.id) {
        try {
            channelListState.scrollToItem(0)
        } catch (_: Exception) {}
    }

    LaunchedEffect(currentChannelIndex) {
        if (currentChannelIndex in channels.indices && channels.any { it.id == currentChannelId }) {
            try {
                channelListState.scrollToItem((currentChannelIndex - 2).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }


    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xF50A0E1A))
            .border(width = 1.dp, color = GlassBorder)
    ) {
        // COLUMN 1: Categories (Left, 220.dp)
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(Color(0x99070A12))
                .border(width = 1.dp, color = GlassBorder)
                .padding(top = 16.dp, bottom = 12.dp, start = 12.dp, end = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CATEGORIES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentBlue,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${allCategories.size}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                state = categoryListState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(allCategories, key = { _, cat -> cat.id }) { index, cat ->
                    val isSelected = cat.id == selectedCategory?.id
                    var isCatFocused by remember { mutableStateOf(false) }
                    val shouldAttachFocus = index == selectedCategoryIndex

                    val parsed = remember(cat) { CategoryGroupHelper.parse(cat) }

                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .then(
                            if (shouldAttachFocus) Modifier.focusRequester(categoryFocusRequester) else Modifier
                        )
                        .onFocusChanged { state ->
                            isCatFocused = state.isFocused
                            if (state.isFocused) {
                                onChannelFocused(false)
                                if (cat.id != selectedCategory?.id) {
                                    debounceCategoryJob?.cancel()
                                    debounceCategoryJob = coroutineScope.launch {
                                        delay(150L)
                                        onSelectCategory(cat)
                                    }
                                }
                            }
                        }
                        .focusProperties {
                            right = channelListFocusRequester
                        }

                    Card(
                        onClick = {
                            onSelectCategory(cat)
                            try {
                                channelListFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                        },
                        modifier = itemModifier,
                        scale = CardDefaults.scale(focusedScale = 1.04f),
                        colors = CardDefaults.colors(
                            containerColor = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color.Transparent,
                            focusedContainerColor = Color(0xFF1D4ED8)
                        ),
                        border = CardDefaults.border(
                            border = Border(
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color.Transparent
                                )
                            ),
                            focusedBorder = Border(border = BorderStroke(3.dp, Color(0xFF60A5FA)))
                        ),
                        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            // Active Indicator Pill
                            Box(
                                modifier = Modifier
                                    .width(if (isCatFocused) 4.dp else 3.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when {
                                            isCatFocused -> Color(0xFF60A5FA)
                                            isSelected -> AccentBlue
                                            else -> Color.Transparent
                                        }
                                    )
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (cat.id == "RECENT") "⭐ Recent" else parsed.cleanName,
                                    fontSize = if (isCatFocused) 13.sp else 12.sp,
                                    fontWeight = when {
                                        isCatFocused -> FontWeight.Black
                                        isSelected -> FontWeight.Bold
                                        else -> FontWeight.Medium
                                    },
                                    color = when {
                                        isCatFocused -> Color.White
                                        isSelected -> TextWhite
                                        else -> TextMuted
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (cat.id != "RECENT" && parsed.groupName != "GENERAL") {
                                    Text(
                                        text = parsed.groupName,
                                        fontSize = 9.sp,
                                        color = if (isCatFocused) Color(0xFF93C5FD) else AccentBlue.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        // COLUMN 2: Channels (Right, 340.dp)
        Column(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
                .padding(top = 16.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedCategory?.name ?: "Channels",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${channels.size} channels",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "◀ [Back] to TV",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No channels in this category",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    state = channelListState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                        val isCurrent = channel.id == currentChannelId
                        var isItemFocused by remember { mutableStateOf(false) }
                        val targetFocusIndex = if (currentChannelIndex in channels.indices && channels.any { it.id == currentChannelId }) currentChannelIndex else 0
                        val shouldAttachFocus = index == targetFocusIndex

                        val itemModifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .then(
                                if (shouldAttachFocus) Modifier.focusRequester(channelListFocusRequester) else Modifier
                            )
                            .onFocusChanged {
                                isItemFocused = it.isFocused
                                if (it.isFocused) {
                                    onChannelFocused(true)
                                }
                            }
                            .focusProperties {
                                left = categoryFocusRequester
                            }

                        Card(
                            onClick = { onSelectChannel(channel) },
                            modifier = itemModifier,
                            scale = CardDefaults.scale(focusedScale = 1.04f),
                            colors = CardDefaults.colors(
                                containerColor = if (isCurrent) Color(0xFF1E3A8A).copy(alpha = 0.6f) else DarkSurface,
                                focusedContainerColor = Color(0xFF1D4ED8)
                            ),
                            border = CardDefaults.border(
                                border = Border(border = BorderStroke(1.dp, if (isCurrent) Color(0xFF38BDF8) else GlassBorder)),
                                focusedBorder = Border(border = BorderStroke(3.dp, Color(0xFF60A5FA)))
                            ),
                            shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Channel Number (Single line bold gold)
                                Text(
                                    text = "${channel.id}",
                                    color = if (isItemFocused) Color(0xFFFFD700) else Color(0xFFE5A00D),
                                    fontSize = if (channel.id.toString().length > 4) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    modifier = Modifier.widthIn(min = 40.dp, max = 58.dp)
                                )

                                // Channel Logo
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!channel.logoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = channel.logoUrl,
                                            contentDescription = channel.name,
                                            modifier = Modifier.size(28.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text(
                                            text = channel.name.take(2).uppercase(),
                                            color = AccentBlue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Channel Name
                                Text(
                                    text = channel.name,
                                    color = if (isItemFocused || isCurrent) Color.White else Color(0xFFE2E8F0),
                                    fontSize = 14.sp,
                                    fontWeight = if (isItemFocused || isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
