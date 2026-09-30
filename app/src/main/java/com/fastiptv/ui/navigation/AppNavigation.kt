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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import com.fastiptv.ui.components.TopNavBar
import com.fastiptv.ui.livetv.LiveTvScreen
import com.fastiptv.ui.movies.MoviesScreen
import com.fastiptv.ui.player.PlayerScreen
import com.fastiptv.ui.series.SeriesScreen
import com.fastiptv.ui.settings.SettingsScreen

private fun getRouteOrder(route: String?): Int {
    if (route == null) return 0
    return when {
        route.startsWith("movies") -> 0
        route.startsWith("live_tv") -> 1
        route.startsWith("series") -> 2
        route.startsWith("settings") -> 3
        route.startsWith("player") -> 4
        else -> 0
    }
}

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
        // Top Navigation Bar (Animated slide/fade when entering or leaving fullscreen modes)
        AnimatedVisibility(
            visible = !isFullscreenRoute,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 160))
        ) {
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
                },
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val isEnteringPlayer = targetRoute?.startsWith("player") == true
                val isExitingPlayer = initialRoute?.startsWith("player") == true

                when {
                    isEnteringPlayer -> {
                        scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                        )
                    }
                    isExitingPlayer -> {
                        fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                        )
                    }
                    targetRoute?.startsWith("settings") == true -> {
                        slideInVertically(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        ) { (it * 0.05f).toInt() } + fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                        )
                    }
                    initialRoute?.startsWith("settings") == true -> {
                        fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                        )
                    }
                    else -> {
                        val initialOrder = getRouteOrder(initialRoute)
                        val targetOrder = getRouteOrder(targetRoute)
                        if (targetOrder >= initialOrder) {
                            slideInHorizontally(
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            ) { (it * 0.08f).toInt() } + fadeIn(
                                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                            )
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            ) { -(it * 0.08f).toInt() } + fadeIn(
                                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                            )
                        }
                    }
                }
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val isEnteringPlayer = targetRoute?.startsWith("player") == true
                val isExitingPlayer = initialRoute?.startsWith("player") == true

                when {
                    isExitingPlayer -> {
                        scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 180)
                        )
                    }
                    isEnteringPlayer -> {
                        fadeOut(
                            animationSpec = tween(durationMillis = 180)
                        )
                    }
                    initialRoute?.startsWith("settings") == true -> {
                        slideOutVertically(
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        ) { (it * 0.05f).toInt() } + fadeOut(
                            animationSpec = tween(durationMillis = 160)
                        )
                    }
                    targetRoute?.startsWith("settings") == true -> {
                        fadeOut(
                            animationSpec = tween(durationMillis = 180)
                        )
                    }
                    else -> {
                        val initialOrder = getRouteOrder(initialRoute)
                        val targetOrder = getRouteOrder(targetRoute)
                        if (targetOrder >= initialOrder) {
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            ) { -(it * 0.08f).toInt() } + fadeOut(
                                animationSpec = tween(durationMillis = 160)
                            )
                        } else {
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            ) { (it * 0.08f).toInt() } + fadeOut(
                                animationSpec = tween(durationMillis = 160)
                            )
                        }
                    }
                }
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val isExitingPlayer = initialRoute?.startsWith("player") == true

                when {
                    isExitingPlayer -> {
                        scaleIn(
                            initialScale = 1.04f,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                        )
                    }
                    targetRoute?.startsWith("settings") == true -> {
                        fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                        )
                    }
                    else -> {
                        val initialOrder = getRouteOrder(initialRoute)
                        val targetOrder = getRouteOrder(targetRoute)
                        if (targetOrder <= initialOrder) {
                            slideInHorizontally(
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            ) { -(it * 0.08f).toInt() } + fadeIn(
                                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                            )
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            ) { (it * 0.08f).toInt() } + fadeIn(
                                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                            )
                        }
                    }
                }
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val isExitingPlayer = initialRoute?.startsWith("player") == true

                when {
                    isExitingPlayer -> {
                        scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 180)
                        )
                    }
                    initialRoute?.startsWith("settings") == true -> {
                        slideOutVertically(
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        ) { (it * 0.05f).toInt() } + fadeOut(
                            animationSpec = tween(durationMillis = 160)
                        )
                    }
                    else -> {
                        val initialOrder = getRouteOrder(initialRoute)
                        val targetOrder = getRouteOrder(targetRoute)
                        if (targetOrder <= initialOrder) {
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            ) { (it * 0.08f).toInt() } + fadeOut(
                                animationSpec = tween(durationMillis = 160)
                            )
                        } else {
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            ) { -(it * 0.08f).toInt() } + fadeOut(
                                animationSpec = tween(durationMillis = 160)
                            )
                        }
                    }
                }
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
