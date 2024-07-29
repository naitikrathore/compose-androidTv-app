package com.example.thinkcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.thinkcompose.Routing.ThinkRouting
import com.example.thinkcompose.ui.theme.ThinkComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThinkComposeTheme {
               val navHostController= rememberNavController()
                ThinkRouting(navHostController =navHostController )
//                CategoryScreen()
//                SplashScreen()
            }
        }
    }
}