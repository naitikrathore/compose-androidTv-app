package com.example.developertvcompose.screens.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.developertvcompose.R
import com.example.developertvcompose.adapter.MoviesRow
import com.example.developertvcompose.data.AppViewModel
import com.example.developertvcompose.data.Movie
import com.example.developertvcompose.data.Section
import com.example.developertvcompose.screens.dashboard.rememberChildPadding

//import com.example.developertvcompose.data.AppViewModel

@Composable
fun HomeScreen(
    isComingBackFromDifferentScreen: MutableState<Boolean>,

    onMovieClick: (movie: Movie) -> Unit,
    viewModel: AppViewModel = hiltViewModel()
) {

    val alldata by viewModel.latestData.collectAsState()
    val moviesData = Section("Section 1", alldata)
    Log.e("india", "Home")

    Column() {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .height(200.dp)

        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Transparent
                                )
                            )
                        )
                    },

                painter = painterResource(id = R.drawable.scram),
                contentDescription = null,
                contentScale = ContentScale.FillBounds

            )
        }
        Catalog(
            isComingBackFromDifferentScreen,
            moviesData,
            onMovieClick = onMovieClick,
            modifier = Modifier.fillMaxSize()
        )
    }

}

@Composable
private fun Catalog(
    isComingBackFromDifferentScreen: MutableState<Boolean>,
    movieData: Section,
    onMovieClick: (movie: Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val childPadding = rememberChildPadding()
    var selectedRow = rememberSaveable { mutableStateOf(-1) }
    Log.e("india", "HomeCat")

    LazyColumn(
//        state = lazyListState,
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = modifier
    ) {

        item(contentType = "MoviesRow") {
            MoviesRow(
                selectedRow = selectedRow.value,
                index = 0,
                isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                section = movieData,
                onMovieSelected = {
                    selectedRow.value = 0
                    onMovieClick(it)
                },
            )
        }
        item(contentType = "MoviesRow") {
            MoviesRow(
                selectedRow = selectedRow.value,
                index = 1,
                isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                section = movieData,
                onMovieSelected = {
                    selectedRow.value = 1
                    onMovieClick(it)
                },
            )
        }
        item(contentType = "MoviesRow") {
            MoviesRow(
                selectedRow = selectedRow.value,
                index = 2,
                isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                section = movieData,
                onMovieSelected = {
                    selectedRow.value = 2
                    onMovieClick(it)
                },
            )
        }
    }
}
