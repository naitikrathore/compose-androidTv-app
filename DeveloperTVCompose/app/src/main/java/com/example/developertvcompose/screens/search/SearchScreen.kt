package com.example.developertvcompose.screens.search

import android.app.DownloadManager.Query
import android.app.appsearch.SearchResult
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.example.developertvcompose.adapter.MoviesRow
import com.example.developertvcompose.data.AppViewModel
import com.example.developertvcompose.data.Movie
import com.example.developertvcompose.data.Section
import com.example.developertvcompose.screens.dashboard.rememberChildPadding
import com.google.common.collect.Queues

@Composable
fun SearchScreen(
    onMovieClick: (movie: Movie) -> Unit,
    viewModel: AppViewModel = hiltViewModel()
) {

    val movieList by viewModel.latestData.collectAsState()
    var searchQuery by remember { mutableStateOf(" ") }
    val filteredMovies = movieList.filter {
        Log.e("akuu","${it.title}  $searchQuery")
        it.title.contains(searchQuery, ignoreCase = true)
    }
    Log.d("akuu", "SearchScreen: ${filteredMovies.toString()}")

    SearchResult(
        movieList = filteredMovies,
        onSearchQuery = { searchQuery = it },
        searchQuery = searchQuery,
        onMovieClick = onMovieClick
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchResult(
    movieList: List<Movie>,
    onSearchQuery: (String) -> Unit,
    searchQuery: String,
    onMovieClick: (movie: Movie) -> Unit,
    lazyColumnState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    Log.d("akuu", "SearchScreen: ${movieList.toString()}")
    val childPadding = rememberChildPadding()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        BasicTextField(
            value = searchQuery,
            onValueChange = {query -> onSearchQuery(query)},
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    innerTextField()
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search for movies, shows and more… ",
                            modifier = Modifier.alpha(0.5f)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onKeyEvent {
                    if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (it.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                focusManager.moveFocus(FocusDirection.Down)

                            }

                            KeyEvent.KEYCODE_DPAD_UP -> {
                                focusManager.moveFocus(FocusDirection.Up)

                            }

                            KeyEvent.KEYCODE_BACK -> {
                                focusManager.moveFocus(FocusDirection.Exit)
                            }
                        }
                    }
                    true
                },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            )
        )
        LazyColumn(
            state = lazyColumnState,
            modifier = modifier
        ) {
//            item{
//                MoviesRow(
//                   modifier = Modifier
//                       .fillMaxSize()
//                       .padding(top = childPadding.top * 2),
//                    isComingBackFromDifferentScreen = i,
//                    section = Section("ti", movieList)
//                ){
//                    onMovieClick(it)
//                }
//            }
        }
    }
}