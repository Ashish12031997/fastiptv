package com.fastiptv.ui.search

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.Movie
import com.fastiptv.domain.model.Series
import com.fastiptv.ui.components.ChannelCard
import com.fastiptv.ui.components.MovieCard
import com.fastiptv.ui.components.SeriesCard
import com.fastiptv.ui.series.EpisodeRow
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkBackground
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

private val SuggestedSearches = listOf(
    "Sky", "Sport", "Cricket", "News", "HBO", "Action", "Cinema", "Comedy", "Premier", "Kids", "Music"
)

@Composable
fun SearchScreen(
    onChannelClick: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onEpisodeClick: (episodeId: Int, title: String?, containerExt: String?, seriesId: Int?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val series by viewModel.series.collectAsState()
    val matchingCategories by viewModel.matchingCategories.collectAsState()
    val selectedSeries by viewModel.selectedSeries.collectAsState()
    val seriesDetail by viewModel.seriesDetail.collectAsState()
    val isLoadingDetail by viewModel.isLoadingDetail.collectAsState()

    var selectedSeasonNumber by remember(selectedSeries) { mutableStateOf("1") }

    BackHandler(enabled = selectedSeries != null) {
        viewModel.closeSeriesDetail()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 12.dp)
        ) {
            // Search Bar & Filter Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = { Text("Search Live Channels, Movies, TV Series...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AccentBlue
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearQuery() }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = TextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                // Category Filter Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Button(
                            onClick = { viewModel.onFilterSelected(filter) },
                            colors = ButtonDefaults.colors(
                                containerColor = if (isSelected) AccentBlue else DarkSurface,
                                focusedContainerColor = if (isSelected) AccentBlue.copy(alpha = 0.85f) else DarkSurfaceElevated
                            ),
                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = filter.label,
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Quick Suggestions Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                item {
                    Text(
                        text = "Suggested:",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 4.dp, top = 6.dp)
                    )
                }
                items(SuggestedSearches) { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceElevated)
                            .clickable { viewModel.onQueryChanged(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = TextWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Content Area
            if (query.isBlank()) {
                // Empty state with guidance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = DarkSurfaceElevated,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Find Your Favorite Entertainment",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Type any channel, movie, or series name or pick a suggestion above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                // Search Results
                val hasResults = channels.isNotEmpty() || movies.isNotEmpty() || series.isNotEmpty() || matchingCategories.isNotEmpty()

                if (!hasResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No results found for \"$query\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try checking your spelling or search for another keyword.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    when (selectedFilter) {
                        SearchFilter.ALL -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(bottom = 32.dp)
                            ) {
                                if (matchingCategories.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Matching Categories (${matchingCategories.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = AccentBlue,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(matchingCategories, key = { "cat_${it.id}" }) { cat ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(DarkSurfaceElevated)
                                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(
                                                            text = cat.name,
                                                            color = TextWhite,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(AccentBlue.copy(alpha = 0.2f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = cat.type.name,
                                                                color = AccentBlue,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (channels.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Live TV Channels (${channels.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            items(channels, key = { "ch_${it.id}" }) { channel ->
                                                ChannelCard(
                                                    channel = channel,
                                                    onClick = onChannelClick
                                                )
                                            }
                                        }
                                    }
                                }

                                if (movies.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Movies (${movies.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            items(movies, key = { "mv_${it.id}" }) { movie ->
                                                MovieCard(
                                                    movie = movie,
                                                    onClick = onMovieClick
                                                )
                                            }
                                        }
                                    }
                                }

                                if (series.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "TV Series (${series.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            items(series, key = { "sr_${it.id}" }) { item ->
                                                SeriesCard(
                                                    series = item,
                                                    onClick = { viewModel.selectSeries(item) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SearchFilter.CHANNELS -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(180.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(channels, key = { "ch_grid_${it.id}" }) { channel ->
                                    ChannelCard(
                                        channel = channel,
                                        onClick = onChannelClick
                                    )
                                }
                            }
                        }
                        SearchFilter.MOVIES -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(movies, key = { "mv_grid_${it.id}" }) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = onMovieClick
                                    )
                                }
                            }
                        }
                        SearchFilter.SERIES -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(series, key = { "sr_grid_${it.id}" }) { item ->
                                    SeriesCard(
                                        series = item,
                                        onClick = { viewModel.selectSeries(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Series Episode Modal Overlay if user clicks a series in search
        if (selectedSeries != null) {
            val currentSeries = selectedSeries!!
            val detail = seriesDetail

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { viewModel.closeSeriesDetail() }
                    .padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkBackground)
                        .border(BorderStroke(1.dp, DarkSurfaceElevated), RoundedCornerShape(16.dp))
                        .clickable(enabled = false) {}
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header: Cover + Info + Close Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!currentSeries.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentSeries.coverUrl,
                                    contentDescription = currentSeries.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp, 84.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSeries.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    if (!currentSeries.genre.isNullOrBlank()) {
                                        Text(
                                            text = currentSeries.genre,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                    if (!currentSeries.rating.isNullOrBlank()) {
                                        Text(
                                            text = "★ ${currentSeries.rating}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AccentBlue
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { viewModel.closeSeriesDetail() },
                                colors = ButtonDefaults.colors(
                                    containerColor = DarkSurfaceElevated,
                                    focusedContainerColor = AccentBlue
                                ),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                            ) {
                                Text(text = "✕ Close", color = TextWhite, fontSize = 13.sp)
                            }
                        }

                        if (isLoadingDetail) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentBlue)
                            }
                        } else if (detail != null) {
                            val seasons = detail.seasons
                            val episodesMap = detail.episodes

                            if (seasons.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    items(seasons, key = { "search_season_${it.seasonNumber}" }) { season ->
                                        val seasonKey = season.seasonNumber.toString()
                                        val isSeasonSelected = seasonKey == selectedSeasonNumber || (selectedSeasonNumber !in episodesMap.keys && season == seasons.first())
                                        Button(
                                            onClick = { selectedSeasonNumber = seasonKey },
                                            colors = ButtonDefaults.colors(
                                                containerColor = if (isSeasonSelected) AccentBlue else DarkSurface,
                                                focusedContainerColor = if (isSeasonSelected) AccentBlue.copy(alpha = 0.85f) else DarkSurfaceElevated
                                            ),
                                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                        ) {
                                            Text(text = season.name, color = TextWhite, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            val currentEpisodes = episodesMap[selectedSeasonNumber] ?: episodesMap.values.firstOrNull() ?: emptyList()

                            if (currentEpisodes.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "No episodes found.", color = TextMuted)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(currentEpisodes, key = { "search_ep_${it.id}" }) { episode ->
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
                            // Error or empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Unable to load episodes",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextMuted
                                    )
                                    Button(
                                        onClick = { viewModel.selectSeries(currentSeries) },
                                        colors = ButtonDefaults.colors(containerColor = AccentBlue),
                                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                                    ) {
                                        Text(text = "Retry", color = TextWhite)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
