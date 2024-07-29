package tv.anoki.ondemand.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.dimensionResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tv.anoki.ondemand.R
import tv.anoki.ondemand.constants.StringConstants
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.helper.buildSeasonsEpisodesScreenRoute
import tv.anoki.ondemand.presentation.details.series.SeriesDetailsScreen
import tv.anoki.ondemand.presentation.details.series.SeriesDetailsViewModel
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.SeasonsAndEpisodeScreen
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.SeasonsAndEpisodeViewModel

/**
 * The component to handle series screens navigation.
 *
 * @param seriesDetailsViewModel the view model to handle data state of series details screen
 * @param seasonsAndEpisodeViewModel the view model to handle data state of seasons and episodes screen
 * @param navController the controller to handle series screens navigation
 * @param contentId the unique id of series type content
 * @param onBackPressed the lambda that gets called when user presses RCU Back button
 * @param onNavigateToPlayer the lambda that gets called when user presses the button to play content
 */
@Composable
fun SeriesNavigation(
    seriesDetailsViewModel: SeriesDetailsViewModel,
    seasonsAndEpisodeViewModel: SeasonsAndEpisodeViewModel,
    navController: NavHostController,
    contentId: String,
    onBackPressed: () -> Unit,
    onNavigateToPlayer: (seriesMetaData: VODItem) -> Unit
) {
    NavHost(
        navController = navController, startDestination = StringConstants.ROUTE_VOD_SERIES_DETAILS
    ) {
        composable(route = StringConstants.ROUTE_VOD_SERIES_DETAILS) {
            SeriesDetailsScreen(
                viewModel = seriesDetailsViewModel,
                contentId = contentId,
                onNavigateToSeasons = { item ->
                    navController.navigate(
                        buildSeasonsEpisodesScreenRoute(
                            StringConstants.ROUTE_VOD_SEASONS_AND_EPISODES,
                            item.contentId,
                            item.title,
                            item.resumeFromSeason,
                            item.resumeFromEpisode
                        )
                    )
                },
                onBackPressed = onBackPressed,
                onNavigateToPlayer = onNavigateToPlayer
            )
        }
        composable(
            route = StringConstants.ROUTE_VOD_SEASONS_AND_EPISODES,
            arguments = listOf(navArgument("contentId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("resumeFromSeason") { type = NavType.IntType },
                navArgument("resumeFromEpisode") { type = NavType.IntType })
        ) {

            LaunchedEffect(Unit) {
                seasonsAndEpisodeViewModel.setupInitialSeasonAndEpisodeIndex(
                    seasonIndex = it.arguments?.getInt("resumeFromSeason") ?: 1,
                    episodeIndex = it.arguments?.getInt("resumeFromEpisode") ?: 1
                )
            }

            SeasonsAndEpisodeScreen(
                viewModel = seasonsAndEpisodeViewModel,
                contentId = it.arguments?.getString("contentId")!!,
                title = it.arguments?.getString("title")!!,
                onBackPressed = {
                    navController.navigateUp()
                },
                onNavigateToPlayer = onNavigateToPlayer,
                topPadding = dimensionResource(id = R.dimen.zero_dimen)
            )
        }
    }
}