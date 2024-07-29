package com.example.developertvcompose.screens.video

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.developertvcompose.data.AppViewModel

object VideoPlayerScreen{
    const val MovieIdBundleKey = "movieId"
}
@Composable
fun VideoPlayerScreen(
    navController: NavController,
//    onBackPressed: () -> Unit
    viewModel: AppViewModel = hiltViewModel()
){
    Log.e("nanpk","entr")
    val backStackEntry=navController.currentBackStackEntry
    val movieId=backStackEntry?.arguments?.getString(VideoPlayerScreen.MovieIdBundleKey)

    if(movieId!=null){
        viewModel.loadMovieDetails(movieId.toLong())
    }
    val movieDetail by viewModel.currentMovieDetails.collectAsState()

    val context = LocalContext.current

    val exoPlayer =  remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem= MediaItem.fromUri("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            setMediaItem(mediaItem)
            prepare()
            playWhenReady=true
        }
    }
    DisposableEffect(
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                }
            }
        )
    ) {
        onDispose { exoPlayer.release() }
    }


}