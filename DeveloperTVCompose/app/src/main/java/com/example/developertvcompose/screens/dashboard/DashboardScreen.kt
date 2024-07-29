package com.example.developertvcompose.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.developertvcompose.Routing.ScreenRoute
import com.example.developertvcompose.screens.favourite.FavouriteScreen
import com.example.developertvcompose.screens.home.HomeScreen
import com.example.developertvcompose.screens.recent.RecentScreen
import com.example.developertvcompose.screens.search.SearchScreen
import com.example.developertvcompose.screens.settings.SettingsScreen

@Immutable
data class Padding(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
)

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)


@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}


@Composable
fun DashboardScreen(
    openDetailScreen: (movieId: Long) -> Unit,
    isComingBackFromDifferentScreen: MutableState<Boolean>,
    resetIsComingBackFromDifferentScreen: () -> Unit,
    onBackPressed: () -> Unit
) {

    val focusManager = LocalFocusManager.current
    val navController = rememberNavController()

    var isTopBarFocused by remember { mutableStateOf(false) }

    var currentDestination: String? by remember { mutableStateOf(null) }
    val currentTopBarSelectedTabIndex by remember(currentDestination) {
        derivedStateOf {
            currentDestination?.let { TopBarTabs.indexOf(ScreenRoute.valueOf(it)) } ?: 0
        }
    }


    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestination = destination.route
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    BackPressHandleArea(
        onBackPressed = {
//                TopBarFocusRequesters[currentTopBarSelectedTabIndex].requestFocus()
            if (currentTopBarSelectedTabIndex == 0) {
                onBackPressed()
            } else if (!isTopBarFocused) {
                TopBarFocusRequesters[currentTopBarSelectedTabIndex].requestFocus()
            } else {
                TopBarFocusRequesters[0].requestFocus()
            }
        }
    ) {
        var wasTopBarFocusRequestedBefore by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (!wasTopBarFocusRequestedBefore) {
                TopBarFocusRequesters[currentTopBarSelectedTabIndex].requestFocus()
                wasTopBarFocusRequestedBefore = true
            }
        }
//        val isTopBarVisible=0
//        val topBarYOffsetPx by animateIntAsState(
//            targetValue = isTopBarVisible,
//            animationSpec = tween(),
//            label = "",
//            finishedListener = {
//                if (isComingBackFromDifferentScreen) {
//                    focusManager.moveFocus(FocusDirection.Down)
//                    resetIsComingBackFromDifferentScreen()
//                }
//            }
//        )
        DashboardTopBar(
            modifier = Modifier
//                .offset { IntOffset(x = 0, y = topBarYOffsetPx) }
                .onFocusChanged { isTopBarFocused = it.hasFocus }
                .padding(
                    horizontal = ParentPadding.calculateStartPadding(
                        LocalLayoutDirection.current
                    ) + 8.dp
                )
                .padding(
                    top = ParentPadding.calculateTopPadding(),
                    bottom = ParentPadding.calculateBottomPadding()
                ),
            selectedTabIndex = currentTopBarSelectedTabIndex
        ) { screen ->
            navController.navigate(screen()) {
                if (screen == TopBarTabs[0])
                    popUpTo(TopBarTabs[0].invoke())
                launchSingleTop = true
            }
        }

        Body(
            isComingBackFromDifferentScreen=isComingBackFromDifferentScreen,
            openDetailScreen = openDetailScreen,
            navController = navController,
            modifier = Modifier.offset(y = 50.dp),
        )
    }
}

@Composable
private fun BackPressHandleArea(

    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = Box(
    modifier = Modifier
        .onPreviewKeyEvent {
            if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                onBackPressed()
                true
            } else {
                false
            }
        }
        .then(modifier),
    content = content
)

@Composable
private fun Body(
    isComingBackFromDifferentScreen: MutableState<Boolean>,
    openDetailScreen: (movieId: Long) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) =
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = ScreenRoute.Home(),
    ) {
        composable(ScreenRoute.Home()) {
//            LaunchedEffect(Unit) {
//                isComingBackFromDifferentScreen.value=false
//            }
            HomeScreen(
                isComingBackFromDifferentScreen,
                onMovieClick = { selectedMovie ->
                    openDetailScreen(selectedMovie.id)
                },
            )
        }
        composable(ScreenRoute.Recent()) {
            RecentScreen(
                onMovieClick = {

                }

            )
        }
        composable(ScreenRoute.Favourites()) {
            FavouriteScreen(
                onMovieClick = {

                }
            )
        }
        composable(ScreenRoute.Search()) {
            SearchScreen(onMovieClick = {

            })
        }
        composable(ScreenRoute.Settings()) {
            SettingsScreen()
        }

    }
