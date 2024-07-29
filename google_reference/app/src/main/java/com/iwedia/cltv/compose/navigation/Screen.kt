package com.iwedia.cltv.compose.navigation

sealed class Screen(val route: String) {
    object VodScreen: Screen("vod_screen")
    object VodDetailsScreen : Screen("vod_details_screen/{imagePath}") {
        fun createRoute(imagePath: String) = "vod_details_screen/$imagePath"
    }
}