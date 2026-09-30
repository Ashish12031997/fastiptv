package com.fastiptv.ui.movies

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
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
import kotlinx.coroutines.delay

@Composable
fun MoviesScreen(
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    topNavFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    isTopNavFocused: Boolean = false,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val contentRegion by viewModel.contentRegion.collectAsState()
    val pinnedGroups by viewModel.pinnedGroups.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var isGridFocused by remember { mutableStateOf(false) }
    var isSidebarFocused by remember { mutableStateOf(false) }

    val sidebarFocusRequester = contentFocusRequester ?: remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }
    var activeGridItemIndex by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(movies) {
        if (movies.isNotEmpty() && (focusedMovie == null || movies.none { it.id == focusedMovie?.id })) {
            focusedMovie = movies.first()
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
            delay(150)
            try {
                sidebarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Remote Back button:
    // 1. If user is browsing in the movie grid, Back focuses the category sidebar first
    // 2. If user is in the sidebar and topNav is not focused, Back focuses the TopNavBar active tab (Movies)
    // 3. When TopNavBar is already focused, BackHandler does not intercept, letting the app exit cleanly.
    BackHandler(enabled = isGridFocused) {
        try {
            sidebarFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler(enabled = !isGridFocused && !isTopNavFocused) {
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
                    contentFocusRequester = gridFocusRequester,
                    sidebarFirstItemFocusRequester = sidebarFocusRequester,
                    topNavFocusRequester = topNavFocusRequester,
                    region = contentRegion,
                    pinnedGroups = pinnedGroups,
                    onTogglePinGroup = { groupName -> viewModel.togglePinGroup(groupName) },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Movie Grid Container
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        if (movies.isEmpty()) {
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
                                        text = if (isLoading) "Loading ${parsed?.cleanName ?: "movies"}..." else "No movies in this category",
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
                                itemsIndexed(movies, key = { _, movie -> "movie_${movie.id}" }) { index, movie ->
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
                                                focusedMovie = movie
                                            }
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
}
