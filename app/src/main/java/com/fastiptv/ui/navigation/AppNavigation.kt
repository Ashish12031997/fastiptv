package com.fastiptv.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.fastiptv.ui.components.TopNavBar
import com.fastiptv.ui.livetv.LiveTvScreen
import com.fastiptv.ui.movies.MoviesScreen
import com.fastiptv.ui.player.PlayerScreen
import com.fastiptv.ui.series.SeriesScreen
import com.fastiptv.ui.settings.SettingsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Movies.route,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isFullscreenRoute = currentRoute?.startsWith("player") == true || currentRoute?.startsWith("live_tv") == true

    val topNavFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    var isTopNavFocused by remember { mutableStateOf(false) }

    // Global Remote Back Handling:
    // If not in Fullscreen video and not already on Movies, pressing Back always safely returns to Movies (Default).
    BackHandler(enabled = !isFullscreenRoute && currentRoute != null && currentRoute != Screen.Movies.route) {
        navController.navigate(Screen.Movies.route) {
            popUpTo(Screen.Movies.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Navigation Bar (Hidden when in fullscreen player or fullscreen Live TV)
        if (!isFullscreenRoute) {
            TopNavBar(
                currentRoute = currentRoute,
                topNavFocusRequester = topNavFocusRequester,
                contentFocusRequester = contentFocusRequester,
                modifier = Modifier.onFocusChanged { isTopNavFocused = it.hasFocus },
                onNavigate = { targetScreen ->
                    val destination = if (targetScreen == Screen.LiveTv) {
                        Screen.LiveTv.createRoute()
                    } else {
                        targetScreen.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Movies.route) {
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
            startDestination = startDestination,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusProperties {
                    up = topNavFocusRequester
                }
        ) {
            // 1. Movies (Default start screen)
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
                    contentFocusRequester = contentFocusRequester,
                    isTopNavFocused = isTopNavFocused
                )
            }

            // 2. Live TV (Authentic Indian Cable Set-Top Box Experience)
            composable(
                route = Screen.LiveTv.route,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                LiveTvScreen(
                    onBackPressed = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Movies.route) {
                                popUpTo(Screen.Movies.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            // 3. Series
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
                    contentFocusRequester = contentFocusRequester,
                    isTopNavFocused = isTopNavFocused
                )
            }

            // 4. Video Player
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

            // 5. Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    topNavFocusRequester = topNavFocusRequester
                )
            }
        }
    }
}
