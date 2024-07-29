package com.example.jetreplica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import com.example.jetreplica.presenting.App
import com.example.jetreplica.ui.theme.JetReplicaTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetReplicaTheme {
               Box(
                   modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
               ){
                   App(
//                       onBackPressed=onBackPressedDispatcher::onBackPressed,
                   )
               }
            }
        }
    }
}
