package com.example.developertvcompose.screens.favourite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.developertvcompose.adapter.MovieCard
import com.example.developertvcompose.adapter.PosterImage
import com.example.developertvcompose.data.AppViewModel
import com.example.developertvcompose.data.Section
import com.example.developertvcompose.screens.dashboard.rememberChildPadding


@Composable
fun FavouriteScreen(
    onMovieClick: (movieId: Long) -> Unit,
    viewModel: AppViewModel = hiltViewModel()
) {
    val alldata by viewModel.latestFav.collectAsState()
    val moviesData =Section("Section 1", alldata)
    Catalog(
        movieData=moviesData,
        onMovieClick = onMovieClick,
        modifier = Modifier.fillMaxSize()
    )
}


@Composable
private fun Catalog(
    movieData:Section,
    onMovieClick: (movieId: Long) -> Unit,
    modifier: Modifier= Modifier
) {
    val childPadding = rememberChildPadding()
    val gridState = rememberLazyGridState()
    Column (modifier = modifier.padding(horizontal = childPadding.start,childPadding.top)){
      LazyVerticalGrid(
          state = gridState,
          modifier = Modifier.fillMaxSize(),
          columns = GridCells.Fixed(6),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = PaddingValues(bottom = 30.dp)
      ) {
          items(movieData.movieList, key = {it.id}){movie->
              MovieCard(onClick = { onMovieClick(movie.id) }) {
                  PosterImage(movie = movie, modifier = Modifier.fillMaxSize())
              }
          }

      }
    }
}