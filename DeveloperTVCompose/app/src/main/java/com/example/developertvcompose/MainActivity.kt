package com.example.developertvcompose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import com.example.developertvcompose.data.AppViewModel
//import com.example.developertvcompose.data.AppViewModel
import com.example.developertvcompose.data.Movie
import com.example.developertvcompose.theme.JetStreamTheme
import com.example.developertvcompose.ui.theme.DeveloperTVComposeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JetStreamTheme {
                val viewModel: AppViewModel = hiltViewModel()
                val movies = listOf(
                    Movie(1, "Batman", img = R.drawable.srambled_poster),
                    Movie(2, "Superman", img = R.drawable.movie2),
                    Movie(3, "Spiderman", img = R.drawable.movie3),
                    Movie(4, "Iron Man", img = R.drawable.movie4),
                    Movie(5, "Wonder Woman", img = R.drawable.movie5),
                    Movie(6, "Black Panther", img = R.drawable.movie1),
                    Movie(7, "Thor", img = R.drawable.movie2),
                    Movie(8, "Captain America", img = R.drawable.movie3),
                    Movie(9, "Deadpool", img = R.drawable.movie4),
                    Movie(10, "Guardians of the Galaxy", img = R.drawable.movie5),
                    Movie(11, "The Avengers", img = R.drawable.movie4),
                    Movie(12, "Aquaman", img = R.drawable.movie3),
                    Movie(13, "The Dark Knight", img = R.drawable.movie2),
                    Movie(14, "Avatar", img = R.drawable.movie1),
                    Movie(15, "Jurassic Park", img = R.drawable.movie2),
                    Movie(16, "Avengers: End Game", img = R.drawable.movie8),
                    Movie(17, "A Wednesday", img = R.drawable.movie9),
                    Movie(18, "Thor", img = R.drawable.movie7),
                    Movie(19, "Marshion", img = R.drawable.movie8),
                    Movie(20, "Oblivioun", img = R.drawable.movie9),
                    Movie(21, "Movie 21", img = R.drawable.movie1),
                    Movie(22, "Movie 22", img = R.drawable.movie2),
                    Movie(23, "Movie 23", img = R.drawable.movie3),
                    Movie(24, "Movie 24", img = R.drawable.movie4),
                    Movie(25, "Movie 25", img = R.drawable.movie5),
                    Movie(26, "Movie 26", img = R.drawable.movie1),
                    Movie(27, "Movie 27", img = R.drawable.movie2),
                    Movie(28, "Movie 28", img = R.drawable.movie3),
                    Movie(29, "Movie 29", img = R.drawable.movie4),
                    Movie(30, "Movie 30", img = R.drawable.movie5),
                    Movie(31, "Movie 31", img = R.drawable.movie4),
                    Movie(32, "Movie 32", img = R.drawable.movie3),
                    Movie(33, "Movie 33", img = R.drawable.movie2),
                    Movie(34, "Movie 34", img = R.drawable.movie1),
                    Movie(35, "Movie 35", img = R.drawable.movie2),
                    Movie(36, "Movie 36", img = R.drawable.movie8),
                    Movie(37, "Movie 37", img = R.drawable.movie9),
                    Movie(38, "Movie 38", img = R.drawable.movie7),
                    Movie(39, "Movie 39", img = R.drawable.movie8),
                    Movie(40, "Movie 40", img = R.drawable.movie9)
                )
                val latest_data by viewModel.latestData.collectAsState()

                if(latest_data.isEmpty()){
                    movies.forEach{ movie->
                        viewModel.insertData(movie)
                    }
                }
                Log.e("moView","${latest_data.isEmpty()}")

                Box (
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ){
                    CompositionLocalProvider(
                        LocalContentColor provides  MaterialTheme.colorScheme.onSurface
                    ) {
                        App(
                            onBackPressed = onBackPressedDispatcher::onBackPressed
                        )
                    }


                }

            }
        }
    }
}

