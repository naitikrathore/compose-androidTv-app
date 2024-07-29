package com.example.developertvcompose.screens.movies

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.jetreplica.data.dummyMovies

@Composable
fun MovieDetailsScreen(
    movieId: String,
    onBackPressed: () -> Unit,
    goToMoviePlayer: () -> Unit,
) {
    Log.e("india","Detail")
    BackHandler (onBack = onBackPressed)
//    val navBackStackEntry = navController.currentBackStackEntry
//    val movieDetail=navBackStackEntry?.arguments?.getString(MovieDetailsScreen.MovieIdBundleKey)
//    Log.e("nanpak","$movieId")

    val movieDetail = remember { dummyMovies.find { it.id == movieId.toLong() } }

    val playButtonFocusRequester = FocusRequester()

    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = movieDetail!!.img),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
        )
        Column(
            modifier = Modifier
                .padding(top = 80.dp, start = 60.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x40000000), Color(0x90000000)),
                    ),
                    shape = CircleShape
                )
                .padding(16.dp)
                .width(400.dp)
                .height(300.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 130.dp),
                text = movieDetail!!.title,
                fontSize = 50.sp,
                style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "In a mystical realm where magic and reality intertwine, a young adventurer named Lila discovers a hidden forest filled with mythical creatures and ancient secrets.",
                style = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.padding(start = 10.dp,end = 10.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Button(
                    onClick = {
                        goToMoviePlayer()
                        movieDetail.isRecent =1
                        },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .focusRequester(playButtonFocusRequester)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        fontWeight = FontWeight.Bold,
                    )
                }
                val isFavourite = remember { mutableStateOf(movieDetail.isFav == 1) }
                Button(
                    onClick = {
                        if(isFavourite.value)
                          movieDetail.isFav=0
                        else
                            movieDetail.isFav=1
                        val newFavStatus = !isFavourite.value
                        isFavourite.value=newFavStatus
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if(isFavourite.value) "Unfavourite" else "Favouirite")
                }
            }
        }
    }
}