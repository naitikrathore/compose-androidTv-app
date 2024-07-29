package com.example.thinkcompose.Routing

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.thinkcompose.CategoryScreen
import com.example.thinkcompose.FinalScreen
import com.example.thinkcompose.SplashScreen
import com.example.thinkcompose.ThinkDetailScreen

@Composable
fun ThinkRouting(navHostController: NavHostController) {
    NavHost(navController = navHostController, startDestination = ThinkSealed.splashScreen.route)
    {
        composable(ThinkSealed.categoryScreen.route) {
            CategoryScreen(navHostController)
        }
        composable(ThinkSealed.splashScreen.route) {
            SplashScreen(navHostController)
        }
        composable(ThinkSealed.detailScreen.route + "/{title}"){
             val title=it.arguments?.getString("title")
             ThinkDetailScreen(navHostController,title)
        }
        composable(ThinkSealed.finalScreen.route + "/{item}"){
            val item=it.arguments?.getString("item")
            FinalScreen(navHostController,item)
        }
    }
}