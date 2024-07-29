package com.example.devjetreplica.presenting.screens

sealed class ScreenRoute(
    open val route: String,
    open val isTabitem: Boolean = false,
) {
    object Dashboard : ScreenRoute("dashboard")
    object Home : ScreenRoute("Home", isTabitem = true)
    object Recent : ScreenRoute("Recent", isTabitem = true)
    object Favourites : ScreenRoute("Favourites", isTabitem = true)
    object Search : ScreenRoute("Search", isTabitem = true)
    object Settings : ScreenRoute("Settings", isTabitem = true)
    object VideoPlayer : ScreenRoute("VideoPlayer")
    object MovieDetails : ScreenRoute("movieDetails/{movieId}") {
        fun createRoute(movieId: Long) = "movieDetails/$movieId"
    }


    companion object {
        val TabRoutes: List<ScreenRoute> = listOf(Home, Recent, Favourites, Search, Settings)
    }
}

