package com.iwedia.cltv.compose.presentation.vod_details_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iwedia.cltv.compose.util.KeyAction

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VodDetailsScreen(
    navController: NavController,
    keyAction: KeyAction = KeyAction(),
    imagePath: String
) {
    val focusRequester = remember {
        FocusRequester()
    }
    
    LaunchedEffect(Unit){
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier, contentAlignment = Alignment.BottomCenter){
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester),
            model = imagePath,
            contentDescription = ""
        )

        Button(
            modifier = Modifier
                .padding(30.dp)
                .focusRequester(focusRequester)
                .onKeyEvent {
                    return@onKeyEvent keyAction.handleKeyEvent(
                        keyEvent = it.nativeKeyEvent
                    )
                },
            colors = ButtonDefaults.colors(
                focusedContainerColor = Color(red = 232, green = 234, blue = 237),
                containerColor = Color.Transparent
            ),
            onClick = { navController.popBackStack() },
            onLongClick = {}
        ) {
            Text(
                text = "Go back",
                color = Color(32, 33, 36) /*else Color(232, 234, 237)*/
            )
        }

        
    }
}