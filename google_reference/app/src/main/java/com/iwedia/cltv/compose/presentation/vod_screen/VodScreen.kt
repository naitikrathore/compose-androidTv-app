package com.iwedia.cltv.compose.presentation.vod_screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyHorizontalGrid
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.iwedia.cltv.compose.navigation.Screen
import com.iwedia.cltv.compose.presentation.util.components.ComposableCard
import com.iwedia.cltv.compose.util.KeyAction
import com.iwedia.cltv.compose.util.KeyHandler
import com.iwedia.cltv.platform.model.TvEvent
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val TAG = "ReferenceVodWidget"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VodScreen(
    modifier: Modifier = Modifier,
    state: VodState,
    testButtonKeyAction: KeyAction = KeyAction(),
    composableCardKeyAction: KeyAction = KeyAction(),
    onFocusChangedFromTestButton: (Boolean) -> Unit,
    onFocusChangedFromRailItem: (Int) -> Unit,
    onCardClick: (String) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {

    val focusRequester = remember {
        FocusRequester()
    }

    val context = LocalContext.current // TODO remove this after Toast is implemented in proper way

    LaunchedEffect(state.isFocusRequested) {
        if (state.isFocusRequested) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(state.toastMessage) { // TODO BORIS this is not a way how to deal with Toast messages.
        if (state.toastMessage.isNotBlank()) {
            Toast.makeText(
                context,
                state.toastMessage,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = 100.dp)
    ) {
        val isButtonSelected = remember { mutableStateOf(false) }
        Button(
            modifier = Modifier
                .onKeyEvent {
                    return@onKeyEvent testButtonKeyAction.handleKeyEvent(
                        keyEvent = it.nativeKeyEvent
                    )
                }
                .focusRequester(focusRequester)
                .onFocusChanged {
                    onFocusChangedFromTestButton(it.hasFocus)
                    isButtonSelected.value = it.hasFocus
                },
            colors = ButtonDefaults.colors(
                focusedContainerColor = Color(red = 232, green = 234, blue = 237),
                containerColor = Color.Transparent
            ),
            onClick = onClick,
            onLongClick = onLongClick
        ) {
            Text(
                text = "Get data",
                color = if (isButtonSelected.value) Color(32, 33, 36) else Color(232, 234, 237)
            )
        }

        TvLazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = TvGridCells.Fixed(1)
        ) {
            items(state.items) { railItem ->

                TvLazyHorizontalGrid(
                    rows = TvGridCells.Fixed(1),
                    modifier = modifier
                        .height(150.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(railItem.rail as ArrayList<TvEvent>) { index, tvEvent ->
                        ComposableCard(
                            tvEvent = tvEvent,
                            onClick = {
                                onCardClick(tvEvent.imagePath!!)
                            },
                            keyAction = composableCardKeyAction,
                            onFocusChanged = { hasFocus ->
                                if (hasFocus) onFocusChangedFromRailItem(index)
                            }
                        )
                    }
                }


            }
        }

    }
}

@Composable
fun VodContent(
    vodViewModel: VodViewModel,
    navController: NavController
) {
    val state by vodViewModel.state.collectAsState()

    VodScreen(
        state = state,
        onFocusChangedFromTestButton = {
            if (!it) vodViewModel.releaseFocus()
        },
        onFocusChangedFromRailItem = {
            vodViewModel.rememberFocusedItemIndex(it)
        },
        testButtonKeyAction = KeyAction(
            actionDown = KeyHandler.Action(
                onDpadRight = {true},
                onDpadLeft = {true}
            ),
            actionUp = KeyHandler.Action(
                onBackPressed = {true } // TODO this have to be handled to behave as DPAD_UP
            )
        ),
        composableCardKeyAction = KeyAction(
            actionUp = KeyHandler.Action(
                onBackPressed = {
                    vodViewModel.requestFocus()
                    true
                }
            )
        ),
        onCardClick = { imagePath ->
            val encodedUrl = URLEncoder.encode(imagePath, StandardCharsets.UTF_8.toString())

            navController.navigate(route = Screen.VodDetailsScreen.createRoute(encodedUrl))
        },
        onClick = {
            vodViewModel.onClick()
        })
}

