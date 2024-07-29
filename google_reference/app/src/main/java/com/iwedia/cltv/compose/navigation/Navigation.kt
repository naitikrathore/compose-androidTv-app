package com.iwedia.cltv.compose.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iwedia.cltv.compose.presentation.vod_details_screen.VodDetailsScreen
import com.iwedia.cltv.compose.presentation.vod_screen.VodContent
import com.iwedia.cltv.compose.presentation.vod_screen.VodViewModel
import com.iwedia.cltv.compose.util.KeyAction
import com.iwedia.cltv.compose.util.KeyHandler
import com.iwedia.cltv.platform.model.Constants

private const val TAG = "Navigation"

@Composable
fun Navigation(vodViewModel: VodViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.VodScreen.route) {
        composable(Screen.VodScreen.route) {
            VodContent(
                vodViewModel = vodViewModel,
                navController = navController
            )
        }
        composable(Screen.VodDetailsScreen.route, arguments = listOf(navArgument("imagePath") {
            type = NavType.StringType
        })) { navBackStackEntry ->
            Log.d(Constants.LogTag.CLTV_TAG + TAG, navBackStackEntry.arguments?.getString("imagePath").toString())
            val imagePath = navBackStackEntry.arguments?.getString("imagePath")
            imagePath?.let {
                VodDetailsScreen(
                    navController = navController,
                    imagePath = it,
                    keyAction = KeyAction(actionUp = KeyHandler.Action(onBackPressed = {
                        navController.popBackStack()
                        true
                    }))
                )
            }
        }
    }
}