package com.fastiptv.ui.movies

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.fastiptv.domain.model.Movie
import com.fastiptv.ui.components.CatalogCategorySidebar
import com.fastiptv.ui.components.CategoryGroupHelper
import com.fastiptv.ui.components.MovieCard
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkBackground
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

@Composable
fun MoviesScreen(
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    topNavFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var isGridFocused by remember { mutableStateOf(false) }
    var isSidebarFocused by remember { mutableStateOf(false) }

    val sidebarFocusRequester = contentFocusRequester ?: remember { FocusRequester() }
    val gridFirstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(movies) {
        if (movies.isNotEmpty() && (focusedMovie == null || movies.none { it.id == focusedMovie?.id })) {
            focusedMovie = movies.first()
        }
    }

    // Remote Back button:
    // 1. If user is browsing in the movie grid, Back focuses the category sidebar first
    // 2. If user is browsing in the sidebar, Back focuses the TopNavBar active tab (Movies)
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
                        text = "Loading VOD Movies...",
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

                // Right Column: Header + Spotlight Preview Banner + Movie Grid
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp)
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
                                    text = parsed?.cleanName ?: "Movies",
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
                                text = "${movies.size} titles in this catalog",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Spotlight Preview Header
                    AnimatedVisibility(
                        visible = focusedMovie != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        focusedMovie?.let { movie ->
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
                                    // Movie mini poster
                                    if (!movie.posterUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = movie.posterUrl,
                                            contentDescription = movie.name,
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
                                                text = movie.name.take(2).uppercase(),
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
                                            text = movie.name,
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
                                            val rating = movie.rating?.takeIf { it != "0" && it.isNotBlank() }
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

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(DarkSurfaceElevated)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = movie.containerExt.uppercase(),
                                                    color = TextMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Text(
                                                text = "HD 1080p • Ready to stream",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onMovieClick(movie) },
                                        colors = ButtonDefaults.colors(
                                            containerColor = AccentBlue,
                                            focusedContainerColor = AccentBlue.copy(alpha = 0.85f)
                                        ),
                                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.clickable { onMovieClick(movie) }
                                    ) {
                                        Text(
                                            text = "▶ Watch",
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

                    // Movie Grid
                    if (movies.isEmpty()) {
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
                                    text = "Loading ${parsed?.cleanName ?: "movies"}...",
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
                            itemsIndexed(movies, key = { _, movie -> "movie_${movie.id}" }) { index, movie ->
                                val itemModifier = Modifier
                                    .then(
                                        if (index == 0) Modifier.focusRequester(gridFirstItemFocusRequester)
                                        else Modifier
                                    )
                                    .focusProperties {
                                        left = sidebarFocusRequester
                                    }

                                MovieCard(
                                    movie = movie,
                                    onClick = onMovieClick,
                                    onFocus = { focusedMovie = it },
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
