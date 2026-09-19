package com.fastiptv.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.fastiptv.ui.components.SimplePlaceholderScreen
import com.fastiptv.ui.components.TopNavBar
import com.fastiptv.ui.epg.EpgGridScreen
import com.fastiptv.ui.favorites.FavoritesScreen
import com.fastiptv.ui.home.HomeScreen
import com.fastiptv.ui.movies.MoviesScreen
import com.fastiptv.ui.player.PlayerScreen
import com.fastiptv.ui.search.SearchScreen
import com.fastiptv.ui.series.SeriesScreen
import com.fastiptv.ui.settings.SettingsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isPlayerRoute = currentRoute?.startsWith("player") == true

    val topNavFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    // Global Remote Back Handling:
    // If not in Player and not already on Home, pressing Back always safely returns to Home.
    // This prevents accidental exits or unwanted jumps to Settings.
    BackHandler(enabled = !isPlayerRoute && currentRoute != null && currentRoute != Screen.Home.route) {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Navigation Bar (Hidden when in fullscreen player)
        if (!isPlayerRoute) {
            TopNavBar(
                currentRoute = currentRoute,
                topNavFocusRequester = topNavFocusRequester,
                contentFocusRequester = contentFocusRequester,
                onNavigate = { targetScreen ->
                    navController.navigate(targetScreen.route) {
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(contentFocusRequester)
                .focusProperties {
                    up = topNavFocusRequester
                }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onChannelClick = { channel ->
                        navController.navigate(Screen.Player.createRoute(channel.id, channel.name, "live"))
                    },
                    onRecentClick = { recent ->
                        navController.navigate(Screen.Player.createRoute(recent.streamId, recent.title, recent.type))
                    },
                    topNavFocusRequester = topNavFocusRequester,
                    contentFocusRequester = contentFocusRequester
                )
            }
            composable(
                route = Screen.Channels.route,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                HomeScreen(
                    initialCategoryId = categoryId,
                    onChannelClick = { channel ->
                        navController.navigate(Screen.Player.createRoute(channel.id, channel.name, "live"))
                    },
                    topNavFocusRequester = topNavFocusRequester,
                    contentFocusRequester = contentFocusRequester
                )
            }
            composable(Screen.Epg.route) {
                EpgGridScreen(
                    onChannelClick = { channel ->
                        navController.navigate(Screen.Player.createRoute(channel.id, channel.name, "live"))
                    },
                    topNavFocusRequester = topNavFocusRequester,
                    contentFocusRequester = contentFocusRequester
                )
            }
            composable(
                route = Screen.Player.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "fastiptv://player/{streamId}?title={title}&type={type}&ext={ext}&seriesId={seriesId}" }
                ),
                arguments = listOf(
                    navArgument("streamId") { type = NavType.IntType },
                    navArgument("title") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "live"
                    },
                    navArgument("ext") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("seriesId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val streamId = backStackEntry.arguments?.getInt("streamId") ?: 0
                val rawTitle = backStackEntry.arguments?.getString("title")
                val title = try {
                    rawTitle?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                } catch (_: Exception) {
                    rawTitle
                }
                val type = backStackEntry.arguments?.getString("type") ?: "live"
                val ext = backStackEntry.arguments?.getString("ext")
                val seriesIdArg = backStackEntry.arguments?.getInt("seriesId") ?: -1
                val seriesId = if (seriesIdArg != -1) seriesIdArg else null

                PlayerScreen(
                    streamId = streamId,
                    streamTitle = title,
                    streamType = type,
                    containerExt = ext,
                    seriesId = seriesId,
                    onBackPressed = { navController.popBackStack() }
                )
            }
            composable(Screen.Movies.route) {
                MoviesScreen(
                    onMovieClick = { movie ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                streamId = movie.id,
                                title = movie.name,
                                type = "vod",
                                ext = movie.containerExt
                            )
                        )
                    },
                    topNavFocusRequester = topNavFocusRequester,
                    contentFocusRequester = contentFocusRequester
                )
            }
            composable(Screen.Series.route) {
                SeriesScreen(
                    onEpisodeClick = { episodeId, episodeTitle, containerExt, seriesId ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                streamId = episodeId,
                                title = episodeTitle,
                                type = "series",
                                ext = containerExt,
                                seriesId = seriesId
                            )
                        )
                    },
                    topNavFocusRequester = topNavFocusRequester,
                    contentFocusRequester = contentFocusRequester
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onChannelClick = { channel ->
                        navController.navigate(Screen.Player.createRoute(channel.id, channel.name, "live"))
                    },
                    onMovieClick = { movie ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                streamId = movie.id,
                                title = movie.name,
                                type = "vod",
                                ext = movie.containerExt
                            )
                        )
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onChannelClick = { channel ->
                        navController.navigate(Screen.Player.createRoute(channel.id, channel.name, "live"))
                    },
                    onMovieClick = { movie ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                streamId = movie.id,
                                title = movie.name,
                                type = "vod",
                                ext = movie.containerExt
                            )
                        )
                    },
                    onEpisodeClick = { episodeId, episodeTitle, containerExt, seriesId ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                streamId = episodeId,
                                title = episodeTitle,
                                type = "series",
                                ext = containerExt,
                                seriesId = seriesId
                            )
                        )
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
