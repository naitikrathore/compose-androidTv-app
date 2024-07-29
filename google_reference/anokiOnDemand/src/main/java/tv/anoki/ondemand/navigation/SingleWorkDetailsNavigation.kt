package tv.anoki.ondemand.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tv.anoki.ondemand.constants.StringConstants
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.presentation.details.single_work.SingleWorkDetailsScreen
import tv.anoki.ondemand.presentation.details.single_work.SingleWorkDetailsViewModel

/**
 * The component to handle single work screen navigation.
 *
 * @param viewModel the view model to handle data state of single work details screen
 * @param navController the controller to handle series screens navigation
 * @param contentId the unique id of series type content
 * @param onBackPressed the lambda that gets called when user presses RCU Back button
 * @param onNavigateToPlayer the lambda that gets called when user presses the button to play content
 */
@Composable
fun SingleWorkDetailsNavigation(
    viewModel: SingleWorkDetailsViewModel,
    contentId: String,
    onBackPressed: () -> Unit,
    navController: NavHostController = rememberNavController(),
    onNavigateToPlayer: (VODItem) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = StringConstants.ROUTE_VOD_SINGLE_WORK_DETAILS
    ) {
        composable(route = StringConstants.ROUTE_VOD_SINGLE_WORK_DETAILS) {
            SingleWorkDetailsScreen(
                viewModel = viewModel,
                contentId = contentId,
                onBackPressed = onBackPressed,
                onNavigateToPlayer = onNavigateToPlayer
            )
        }
    }
}