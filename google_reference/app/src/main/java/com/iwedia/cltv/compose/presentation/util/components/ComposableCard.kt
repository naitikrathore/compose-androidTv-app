package com.iwedia.cltv.compose.presentation.util.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import coil.compose.AsyncImage
import com.iwedia.cltv.compose.util.KeyAction
import com.iwedia.cltv.platform.model.TvEvent

object ComposableCard {
    val width = 178.dp
    val height = 113.dp
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ComposableCard(
    modifier: Modifier = Modifier,
    tvEvent: TvEvent?,
    keyAction: KeyAction = KeyAction(),
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {

    val focusRequester = remember {
        FocusRequester()
    }

    Card(
        modifier = modifier
            .padding(15.dp)
            .onKeyEvent {
                return@onKeyEvent keyAction.handleKeyEvent(it.nativeKeyEvent)
            }
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .focusRequester(focusRequester)
            .wrapContentSize(),
        glow = CardDefaults.glow(
            focusedGlow = Glow(Color.White, 12.dp)
        ),
        onClick = onClick,
        border = CardDefaults.border(
            focusedBorder = Border(
                BorderStroke(2.dp, Color.White)
            )
        )
    ) {
        AsyncImage(
            modifier = Modifier
                .width(ComposableCard.width),
            model = tvEvent?.imagePath, contentDescription = tvEvent?.name
        )
    }

}