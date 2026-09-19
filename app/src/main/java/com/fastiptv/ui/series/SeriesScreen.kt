package com.fastiptv.ui.series

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
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
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
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
    val gridFirstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(seriesList) {
        if (seriesList.isNotEmpty() && (focusedSeries == null || seriesList.none { it.id == focusedSeries?.id })) {
            focusedSeries = seriesList.first()
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

    BackHandler(enabled = selectedSeries == null && isSidebarFocused && !isGridFocused) {
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
                    contentFocusRequester = gridFirstItemFocusRequester,
                    sidebarFirstItemFocusRequester = sidebarFocusRequester,
                    topNavFocusRequester = topNavFocusRequester,
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Spotlight Preview Header
                    AnimatedVisibility(
                        visible = focusedSeries != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        focusedSeries?.let { series ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(86.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF141926),
                                                Color(0xFF0F1420)
                                            )
                                        )
                                    )
                                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Series mini cover
                                    if (!series.coverUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = series.coverUrl,
                                            contentDescription = series.name,
                                            modifier = Modifier
                                                .width(46.dp)
                                                .height(66.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .width(46.dp)
                                                .height(66.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DarkSurfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = series.name.take(2).uppercase(),
                                                color = AccentBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = series.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            modifier = Modifier.padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val rating = series.rating?.takeIf { it != "0" && it.isNotBlank() }
                                            if (rating != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFE5A00D).copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "★ $rating",
                                                        color = Color(0xFFFFC107),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            if (!series.genre.isNullOrBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(DarkSurfaceElevated)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = series.genre,
                                                        color = TextMuted,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "Series • Seasons & Episodes available",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.openSeriesDetail(series) },
                                        colors = ButtonDefaults.colors(
                                            containerColor = AccentBlue,
                                            focusedContainerColor = AccentBlue.copy(alpha = 0.85f)
                                        ),
                                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.clickable { viewModel.openSeriesDetail(series) }
                                    ) {
                                        Text(
                                            text = "View Episodes",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Series Grid
                    if (seriesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = AccentBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading ${parsed?.cleanName ?: "series"}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .onFocusChanged { state ->
                                    isGridFocused = state.hasFocus
                                },
                            contentPadding = PaddingValues(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(seriesList, key = { _, series -> "series_${series.id}" }) { index, series ->
                                val itemModifier = Modifier
                                    .then(
                                        if (index == 0) Modifier.focusRequester(gridFirstItemFocusRequester)
                                        else Modifier
                                    )
                                    .focusProperties {
                                        left = sidebarFocusRequester
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

        // Series Detail & Episode Picker Overlay
        if (selectedSeries != null) {
            val series = selectedSeries!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(32.dp)
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

                        // Episodes List
                        val currentEpisodes = episodesMap[selectedSeasonNumber] ?: episodesMap.values.firstOrNull() ?: emptyList()

                        if (currentEpisodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No episodes found for this season.",
                                    color = TextMuted
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
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

@Composable
fun EpisodeRow(
    episode: SeriesEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = DarkSurfaceElevated
        ),
        border = CardDefaults.border(
            focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
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
