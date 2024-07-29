package com.example.thinkcompose

import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.thinkcompose.Routing.ThinkSealed
import com.example.thinkcompose.ui.theme.primaryColor
import com.example.thinkcompose.ui.theme.purpleColor
import java.util.logging.Handler

@Composable
fun SplashScreen(navHostController: NavHostController) {
    Surface() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = primaryColor),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 50.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(35.dp),
                    strokeWidth = 5.dp,
                    color = purpleColor
                )
            }
            android.os.Handler(Looper.getMainLooper()).postDelayed(Runnable {
                navHostController.navigate(ThinkSealed.categoryScreen.route)
            },3000)
        }
    }
}
