package com.example.jetreplica.presenting

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.developertvcompose.screens.movies.MovieDetailsScreen
import com.example.jetreplica.presenting.screens.dashboard.DashboardScreen
import com.example.jetreplica.presenting.screens.ScreenRoute


@Composable
fun App(
//    onBackPressed: () ->Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ScreenRoute.Dashboard.route)
    {
        composable(ScreenRoute.Dashboard.route) {
            DashboardScreen(
                openDetailScreen = { movieId ->
                   navController.navigate(ScreenRoute.MovieDetails.createRoute(movieId))
                }
            )
            Log.e("Naitik", "App")
        }
        composable(
            route=ScreenRoute.MovieDetails.route,
        ) {
            val movieId = it.arguments?.getString("movieId")
            if (movieId != null) {
                MovieDetailsScreen(
                    movieId = movieId,
                    onBackPressed = { navController.navigateUp()},
                    goToMoviePlayer = {}
                )
            } else {
                Log.e("Error", "Movie ID is null")
            }

        }

    }


}