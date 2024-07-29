package com.example.developertvcompose

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.developertvcompose.Routing.ScreenRoute
import com.example.developertvcompose.screens.dashboard.DashboardScreen
import com.example.developertvcompose.screens.movies.MovieDetailsScreen
import com.example.developertvcompose.screens.video.VideoPlayerScreen

@Composable
fun App(
    onBackPressed: () -> Unit
) {
    val navController = rememberNavController()
    var isComingBackFromDifferentScreen = remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = ScreenRoute.Dashboard()
    ) {
        composable(route = ScreenRoute.Dashboard()) {
            Log.e("india", "Dash")
            DashboardScreen(
                openDetailScreen = { movieId ->
                    navController.navigate(
                        ScreenRoute.MovieDetails.withArgs(movieId)
                    )
                },
                onBackPressed = onBackPressed,
                isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                resetIsComingBackFromDifferentScreen = {
                    isComingBackFromDifferentScreen.value = false
                }
            )
        }

        composable(
            route = ScreenRoute.MovieDetails(),
            arguments = listOf(
                navArgument(MovieDetailsScreen.MovieIdBundleKey) {
                    type = NavType.StringType
                }
            )
        ) {
            MovieDetailsScreen(
                navController = navController,
                onBackPressed = {
                    Log.e("india", "onback")
                    if (navController.navigateUp()) {
                        isComingBackFromDifferentScreen.value = true
                    }
                },
                goToMoviePlayer = {
                    Log.e("nanpk", "trig")
                    navController.navigate(ScreenRoute.VideoPlayer())
                }
            )
        }

        composable(
            route = ScreenRoute.VideoPlayer(),
        ) {
            VideoPlayerScreen(navController = navController)
        }


    }
}

