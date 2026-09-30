package com.fastiptv.ui.series

import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Brush
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
import com.fastiptv.domain.model.Series
import com.fastiptv.domain.model.SeriesEpisode
import com.fastiptv.ui.components.CatalogCategorySidebar
import com.fastiptv.ui.components.CategoryGroupHelper
import com.fastiptv.ui.components.SeriesCard
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkBackground
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

@Composable
fun SeriesScreen(
    onEpisodeClick: (episodeId: Int, title: String?, containerExt: String?, seriesId: Int?) -> Unit,
    modifier: Modifier = Modifier,
    topNavFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    isTopNavFocused: Boolean = false,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val contentRegion by viewModel.contentRegion.collectAsState()
    val pinnedGroups by viewModel.pinnedGroups.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val seriesList by viewModel.seriesList.collectAsState()
    val selectedSeries by viewModel.selectedSeries.collectAsState()
    val seriesDetail by viewModel.seriesDetail.collectAsState()
    val isLoadingDetail by viewModel.isLoadingDetail.collectAsState()

    var selectedSeasonNumber by remember { mutableStateOf("1") }
    var focusedSeries by remember { mutableStateOf<Series?>(null) }
    var isGridFocused by remember { mutableStateOf(false) }
    var isSidebarFocused by remember { mutableStateOf(false) }

    val closeButtonFocusRequester = remember { FocusRequester() }
    val sidebarFocusRequester = contentFocusRequester ?: remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }
    var activeGridItemIndex by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(seriesList) {
        if (seriesList.isNotEmpty() && (focusedSeries == null || seriesList.none { it.id == focusedSeries?.id })) {
            focusedSeries = seriesList.first()
        }
    }

    LaunchedEffect(selectedCategory) {
        activeGridItemIndex = 0
        try {
            gridState.scrollToItem(0)
        } catch (_: Exception) {}
    }

    var hasInitialFocused by remember { mutableStateOf(false) }
    LaunchedEffect(categories) {
        if (!hasInitialFocused && categories.isNotEmpty()) {
            hasInitialFocused = true
            kotlinx.coroutines.delay(150)
            try {
                sidebarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Remote Back Handling
    BackHandler(enabled = selectedSeries != null) {
        viewModel.closeSeriesDetail()
    }

    BackHandler(enabled = selectedSeries == null && isGridFocused) {
        try {
            sidebarFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler(enabled = selectedSeries == null && !isGridFocused && !isTopNavFocused) {
        try {
            topNavFocusRequester?.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(selectedSeries) {
        if (selectedSeries != null) {
            try {
                closeButtonFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
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
                        text = "Loading TV Series...",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite
                    )
                    Text(
                        text = "If this takes a while, check Settings to verify credentials and tap Sync Content.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Modern Category Sidebar
                CatalogCategorySidebar(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { cat ->
                        viewModel.selectCategory(cat)
                    },
                    contentFocusRequester = gridFocusRequester,
                    sidebarFirstItemFocusRequester = sidebarFocusRequester,
                    topNavFocusRequester = topNavFocusRequester,
                    region = contentRegion,
                    pinnedGroups = pinnedGroups,
                    onTogglePinGroup = { groupName -> viewModel.togglePinGroup(groupName) },
                    modifier = Modifier.onFocusChanged { isSidebarFocused = it.hasFocus }
                )

                // Right Column: Header + Spotlight Preview Banner + Series Grid
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp)
                ) {
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
                                    text = parsed?.cleanName ?: "Series",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = TextWhite,
                                    fontSize = 22.sp
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
                                text = "${seriesList.size} series available",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Series Grid Container
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        if (seriesList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = AccentBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isLoadingDetail) "Loading ${parsed?.cleanName ?: "series"}..." else "No series in this category",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                }
                            }
                        } else {
                            val minWidth = 140.dp
                            val spacing = 14.dp
                            val columnCount = maxOf(1, ((maxWidth + spacing) / (minWidth + spacing)).toInt())

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnCount),
                                state = gridState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onFocusChanged { state ->
                                        isGridFocused = state.hasFocus
                                    },
                                contentPadding = PaddingValues(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(spacing),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(seriesList, key = { _, series -> "series_${series.id}" }) { index, series ->
                                    val isTargetOfGridFocus = (index == activeGridItemIndex)

                                    val itemModifier = Modifier
                                        .then(
                                            if (isTargetOfGridFocus) Modifier.focusRequester(gridFocusRequester)
                                            else Modifier
                                        )
                                        .focusProperties {
                                            if (index < columnCount && topNavFocusRequester != null) {
                                                up = topNavFocusRequester
                                            }
                                        }
                                        .onFocusChanged { state ->
                                            if (state.isFocused) {
                                                activeGridItemIndex = index
                                                focusedSeries = series
                                            }
                                        }

                                    SeriesCard(
                                        series = series,
                                        onClick = { viewModel.openSeriesDetail(series) },
                                        onFocus = { focusedSeries = it },
                                        modifier = itemModifier
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Series Detail & Episode Picker Floating Window
        AnimatedVisibility(
            visible = selectedSeries != null,
            enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 44.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                selectedSeries?.let { series ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface.copy(alpha = 0.96f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(28.dp)
                            .animateEnterExit(
                                enter = scaleIn(
                                    initialScale = 0.94f,
                                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(200)),
                                exit = scaleOut(
                                    targetScale = 0.94f,
                                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(160))
                            )
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (!series.coverUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = series.coverUrl,
                                            contentDescription = series.name,
                                            modifier = Modifier
                                                .size(60.dp, 90.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = series.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = series.genre ?: "Series",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.closeSeriesDetail() },
                                    modifier = Modifier
                                        .focusRequester(closeButtonFocusRequester)
                                        .clickable { viewModel.closeSeriesDetail() },
                                    colors = ButtonDefaults.colors(
                                        containerColor = DarkSurfaceElevated,
                                        focusedContainerColor = AccentBlue
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                ) {
                                    Text(text = "✕ Close", color = TextWhite, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (isLoadingDetail) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AccentBlue)
                                }
                            } else if (seriesDetail != null) {
                                val seasons = seriesDetail!!.seasons
                                val episodesMap = seriesDetail!!.episodes

                                // Season selector
                                if (seasons.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        items(seasons, key = { "season_${it.seasonNumber}" }) { season ->
                                            val seasonKey = season.seasonNumber.toString()
                                            val isSeasonSelected = seasonKey == selectedSeasonNumber || (selectedSeasonNumber !in episodesMap.keys && season == seasons.first())
                                            Button(
                                                onClick = { selectedSeasonNumber = seasonKey },
                                                colors = ButtonDefaults.colors(
                                                    containerColor = if (isSeasonSelected) AccentBlue else DarkSurface,
                                                    focusedContainerColor = if (isSeasonSelected) AccentBlue.copy(alpha = 0.85f) else DarkSurfaceElevated
                                                ),
                                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                                modifier = Modifier.clickable { selectedSeasonNumber = seasonKey }
                                            ) {
                                                Text(text = season.name, color = TextWhite, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                // Episodes List (Crossfade animated on season change)
                                AnimatedContent(
                                    targetState = selectedSeasonNumber,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing)) togetherWith
                                            fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing))
                                    },
                                    label = "SeasonEpisodesCrossfade",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) { seasonKey ->
                                    val currentEpisodes = episodesMap[seasonKey] ?: episodesMap.values.firstOrNull() ?: emptyList()

                                    if (currentEpisodes.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No episodes found for this season.",
                                                color = TextMuted
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(currentEpisodes, key = { "ep_${it.id}" }) { episode ->
                                                EpisodeRow(
                                                    episode = episode,
                                                    onClick = {
                                                        val epId = episode.id.toIntOrNull() ?: 0
                                                        onEpisodeClick(epId, episode.title, episode.containerExt, selectedSeries?.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Unable to load episodes for this series.",
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeRow(
    episode: SeriesEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        scale = CardDefaults.scale(focusedScale = 1.02f),
        colors = CardDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = Color(0xFF1E293B)
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(1.dp, Color(0x33FFFFFF))),
            focusedBorder = Border(border = BorderStroke(3.dp, Color(0xFF38BDF8)))
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${episode.episodeNum}",
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = episode.title,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "▶ Play",
                color = AccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
