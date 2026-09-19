package com.fastiptv.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Channels : Screen("channels?categoryId={categoryId}", "Live TV") {
        fun createRoute(categoryId: String? = null) =
            if (categoryId != null) "channels?categoryId=$categoryId" else "channels"
    }
    data object Player : Screen("player/{streamId}?title={title}&type={type}&ext={ext}&seriesId={seriesId}", "Player") {
        fun createRoute(
            streamId: Int,
            title: String? = null,
            type: String = "live",
            ext: String? = null,
            seriesId: Int? = null
        ): String {
            val encodedTitle = title?.let { java.net.URLEncoder.encode(it, "UTF-8") }
            return "player/$streamId?type=$type" +
                    (if (encodedTitle != null) "&title=$encodedTitle" else "") +
                    (if (ext != null) "&ext=$ext" else "") +
                    (if (seriesId != null) "&seriesId=$seriesId" else "")
        }
    }
    data object Movies : Screen("movies", "Movies")
    data object Series : Screen("series", "Series")
    data object Favorites : Screen("favorites", "Favorites")
    data object Search : Screen("search", "Search")
    data object Epg : Screen("epg", "TV Guide")
    data object Settings : Screen("settings", "Settings")
}
