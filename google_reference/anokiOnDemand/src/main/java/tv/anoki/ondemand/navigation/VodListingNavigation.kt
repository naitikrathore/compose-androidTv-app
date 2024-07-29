package tv.anoki.ondemand.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tv.anoki.ondemand.presentation.listing.VodListViewModel
import tv.anoki.ondemand.constants.StringConstants
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODType
import tv.anoki.ondemand.presentation.listing.VodListingScreen

/**
 * The component to handle OnDemand listing screen navigation.
 *
 * @param vodListViewModel the view model to handle data state of OnDemand screens
 * @param onItemClicked the lambda that gets called when user presses RCU Ok button on item
 * @param changeTrailer the lambda that gets called when user changes the focus on the items
 * @param navController the controller to handle series screens navigation
 * @param onBackPressed the lambda that gets called when user presses RCU Back button
 */
@Composable
fun VodListingNavigation(
    vodListViewModel: VodListViewModel,
    onItemClicked: (type: VODType, contentId: String) -> Unit,
    changeTrailer: (vodItem: VODItem?) -> Unit,
    onBackPressed: () -> Unit = {},
    onInternetDisconnected: () -> Unit = {},
    onInternetConnected: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = StringConstants.ROUTE_VOD_LISTING
    ) {
        composable(route = StringConstants.ROUTE_VOD_LISTING) {
            VodListingScreen(
                vodListViewModel = vodListViewModel,
                onNavigateToDetails = onItemClicked,
                moveFocusToNavigation = {
                    onBackPressed()
                    vodListViewModel.setTrailerPlaying(false)
                    vodListViewModel.setListFocused(false)
                },
                changeTrailer = {
                    vodListViewModel.setTrailerPlaying(true)
                    changeTrailer(it)
                },
                onInternetDisconnected = onInternetDisconnected,
                onInternetConnected = onInternetConnected,
            )
        }
    }

}