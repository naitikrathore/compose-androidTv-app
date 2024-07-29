package com.example.jetreplica.presenting.screens.recent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.example.developertvcompose.screens.home.MovieCard
import com.example.jetreplica.data.Movie
import com.example.jetreplica.data.dummyMovies

@Composable
fun RecentScreen(
    onMovieClick: (movie: Movie) -> Unit,
) {
    val recentData = dummyMovies.filter { it.isRecent == 1 }
    var lastItemFocused by rememberSaveable { mutableStateOf(-1) }
//    val focusRequester = remember { mutableMapOf<Int, FocusRequester>() }

    Column() {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(recentData) { index, movie ->
                val itemFocusRequester = remember { FocusRequester() }
                val isSelected = if (index == lastItemFocused) {
                    true
                } else {
                    false
                }
                MovieCard(
                    movie = movie,
                    openDetailScreen = onMovieClick,
                    modifier = Modifier
                        .focusRequester(itemFocusRequester)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                lastItemFocused = index
                            }
                        }
                )
                if (isSelected) {
                    LaunchedEffect(Unit) {

                      itemFocusRequester.requestFocus()
                    }
                }
            }

        }

    }

}