package com.fastiptv.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Movies : Screen("movies", "Movies")
    data object LiveTv : Screen("live_tv?categoryId={categoryId}", "Live TV") {
        fun createRoute(categoryId: String? = null) =
            if (categoryId != null) "live_tv?categoryId=$categoryId" else "live_tv"
    }
    data object Series : Screen("series", "Series")
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
    data object Settings : Screen("settings", "Settings")
}
