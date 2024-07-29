package com.example.devjetreplica.presenting.screens.dashboard

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.devjetreplica.presenting.screens.home.HomeScreen
import com.example.devjetreplica.presenting.screens.ScreenRoute
import com.example.jetreplica.presenting.screens.recent.RecentScreen

@Composable
fun DashboardScreen(
    openDetailScreen: (movieId: Long) -> Unit
) {
    Log.e("Naitik", "DashScreenEnter")
    var isTopBarFocused by remember { mutableStateOf(false) }
    var currentDestination: String? by remember { mutableStateOf(null) }
    val navController = rememberNavController()

    val currentTopBarSelectedTabIndex by remember(currentDestination) {
        Log.e("Naitik", "DashDerived ${currentDestination}")
        derivedStateOf {
            currentDestination?.let { route ->
                TopBarTabs.indexOf(
                    when (route) {
                        ScreenRoute.Home.route -> ScreenRoute.Home
                        ScreenRoute.Recent.route -> ScreenRoute.Recent
                        ScreenRoute.Favourites.route -> ScreenRoute.Favourites
                        ScreenRoute.Search.route -> ScreenRoute.Search
                        ScreenRoute.Settings.route -> ScreenRoute.Settings
                        else -> null
                    }
                )
            } ?: 0
        }
    }

    DisposableEffect(Unit) {
        Log.e("Naitik", "DashScr Dispo")
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestination = destination.route
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            Log.e("Naitik", "DashScr onDispo")
            navController.removeOnDestinationChangedListener(listener)
        }

    }

    Box(modifier = Modifier
        .onPreviewKeyEvent { keyEvent ->
            if (keyEvent.key == Key.Back) {
                if (isTopBarFocused) {
                    TopBarFocusRequesters[0].requestFocus()
                } else {
                    TopBarFocusRequesters[currentTopBarSelectedTabIndex].requestFocus()
                }
                true
            } else {
                false
            }

        }
    ) {
        var wasTopBarFocusRequestedBefore by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if(!wasTopBarFocusRequestedBefore){
                TopBarFocusRequesters[currentTopBarSelectedTabIndex].requestFocus()
                wasTopBarFocusRequestedBefore = true
                Log.e("mooooo","wastop ${currentTopBarSelectedTabIndex}")
            }

        }
        DashboardTopBar(
            modifier = Modifier
                .onFocusChanged { isTopBarFocused = it.hasFocus },
            selectedTabIndex = currentTopBarSelectedTabIndex
        ) {
            navController.navigate(it.route){
                launchSingleTop=true
            }

        }


        Body(
            navController = navController,
            openDetailScreen = openDetailScreen

        )
    }
}

@Composable
private fun Body(
    navController: NavHostController = rememberNavController(),
    openDetailScreen: (movieId: Long) -> Unit
) {
    NavHost(
        modifier = Modifier.offset(y = 50.dp),
        navController = navController,
        startDestination = ScreenRoute.Home.route
    ) {
        composable(ScreenRoute.Home.route) {
            HomeScreen(
                openDetailScreen = {
                    openDetailScreen(it.id)
                }
            )
        }
        composable(ScreenRoute.Recent.route) {
            RecentScreen {
                openDetailScreen(it.id)
            }
        }
        composable(ScreenRoute.Favourites.route) {

        }
        composable(ScreenRoute.Search.route) {

        }
        composable(ScreenRoute.Settings.route) {

        }

    }

}