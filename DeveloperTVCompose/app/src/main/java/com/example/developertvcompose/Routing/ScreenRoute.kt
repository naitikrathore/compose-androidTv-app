package com.example.developertvcompose.Routing

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.developertvcompose.screens.movies.MovieDetailsScreen
import com.example.developertvcompose.screens.video.VideoPlayerScreen

enum class ScreenRoute(
    private val args: List<String>? = null,
    val isTabItem: Boolean = false,
    val tabIcon: ImageVector? = null
) {
    Home(isTabItem = true),
    Recent(isTabItem = true),
    Favourites(isTabItem = true),
    Search(isTabItem = true, tabIcon = Icons.Default.Search),
    Settings(isTabItem = true, tabIcon = Icons.Default.Settings),
    MovieDetails(listOf(MovieDetailsScreen.MovieIdBundleKey)),
    VideoPlayer,
    Dashboard;

    operator fun invoke(): String {
        val argList = StringBuilder()
        args?.let { nnArgs ->
            nnArgs.forEach { arg -> argList.append("/{$arg}") }
        }
        return name + argList
    }

    fun withArgs(vararg args: Any): String {
        val destination = StringBuilder()
        args.forEach { arg -> destination.append("/$arg") }
        return name + destination
    }
}
