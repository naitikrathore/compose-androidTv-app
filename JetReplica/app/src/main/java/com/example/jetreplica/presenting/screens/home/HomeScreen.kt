package com.example.developertvcompose.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.jetreplica.R
import com.example.jetreplica.data.Movie
import com.example.jetreplica.data.dummySections
import com.example.jetreplica.presenting.screens.home.ImmersiveListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    openDetailScreen: (movie: Movie) -> Unit
) {
    val lazyListState: TvLazyListState = rememberTvLazyListState()
    var selectedRow by rememberSaveable { mutableStateOf(-1) }
    var lastFocucedItemIndex by rememberSaveable { mutableStateOf(-1) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    val coroutineScope = rememberCoroutineScope()
    val immersiveListHeight =  LocalConfiguration.current.screenHeightDp.times(0.50).dp

    Column {
        ImmersiveListScreen(
            height = immersiveListHeight
        )
        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .height(150.dp)
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

        // Button to scroll to the last item
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.Main) {
                    lazyListState.animateScrollToItem(dummySections.size - 1)
                }
            }
        }) {
            Text("Scroll to Last Item")
        }

        TvLazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            pivotOffsets = PivotOffsets(0.2f, 0.0f)
        ) {
            itemsIndexed(dummySections) { Index, section ->
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )

                val lazyRowFocusRequester = remember { FocusRequester() }
                focusRequesters[Index] = lazyRowFocusRequester
                TvLazyRow(

                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .focusRequester(lazyRowFocusRequester),
                    pivotOffsets = PivotOffsets(0.5f, 0.5f)

                ) {
                    itemsIndexed(section.movieList) { itemIndex, movie ->
                        val itemFocusRequester = remember { FocusRequester() }
                        val isSelected =
                            if (selectedRow == Index && lastFocucedItemIndex == itemIndex) {
                                true
                            } else {
                                false
                            }
                        MovieCard(
                            movie = movie,
                            openDetailScreen = {
                                openDetailScreen(movie)
//                                selectedRow = Index
//                                lastFocusedItemIndex = itemIndex
                            },
                            modifier = Modifier
                                .focusRequester(itemFocusRequester)
                                .focusProperties {
                                    left = if (itemIndex == 0) {
                                        FocusRequester.Cancel
                                    } else {
                                        FocusRequester.Default
                                    }
                                    right = if (itemIndex == section.movieList.size - 1) {
                                        FocusRequester.Cancel
                                    } else {
                                        FocusRequester.Default
                                    }
                                }
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        selectedRow = Index
                                        lastFocucedItemIndex = itemIndex
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

//                if (selectedRow == Index) {
//                    LaunchedEffect(Unit) {
//                        lazyRowFocusRequester.requestFocus()
//                    }
//                }
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    openDetailScreen: (movie: Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { openDetailScreen(movie) },
        modifier = modifier
            .height(160.dp)
            .width(160.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = movie.img), // Replace with actual movie image resource
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
            )
            Text(
                text = movie.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}