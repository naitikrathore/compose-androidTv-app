package tv.anoki.ondemand.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tv.anoki.components.button.IconButton
import tv.anoki.components.button.TextButton
import tv.anoki.components.text.TitleText
import tv.anoki.components.utils.KeyAction
import tv.anoki.components.utils.KeyHandler
import tv.anoki.components.utils.disableDpadLeft
import tv.anoki.components.utils.disableDpadRight
import tv.anoki.components.utils.handleKeyEvent
import tv.anoki.components.utils.setBackgroundColor
import tv.anoki.ondemand.R

/**
 * This component to show OnDemand screens errors
 *
 * @param exception the error object
 * @param modifier Modifier to be applied to the Composable
 * @param focusRetryButton the flag to handle focus on retry button
 * @param onRetryCalled the lambda that the retry button calls when the user presses it
 * @param onDpadUp the lambda that the retry button calls when having focus on it and user presses RCU DPAD_UP
 * @param retryFocusRequester the FocusRequester to set focus on retry button
 * @param actionComponent the custom component to render above retry button
 */
@Composable
fun VodScreenError(
    exception: Throwable?,
    modifier: Modifier = Modifier,
    focusRetryButton: Boolean = false,
    onRetryCalled: (() -> Unit)? = null,
    onDpadUp: () -> Boolean = { false },
    retryFocusRequester: FocusRequester = remember { FocusRequester() },
    actionComponent: (@Composable () -> Unit)? = null,
) {
    val errorMessage =
        exception?.message ?: stringResource(id = R.string.error_something_went_wrong)
    exception?.printStackTrace()

    Box(modifier = modifier.fillMaxSize().setBackgroundColor()) {
        Box(contentAlignment = Alignment.TopStart) {
            if (actionComponent != null) {
                actionComponent()
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (onRetryCalled != null) {
                    var isButtonFocused by remember { mutableStateOf(false) }
                    LaunchedEffect(focusRetryButton) {
                        if (focusRetryButton) {
                            retryFocusRequester.requestFocus()
                        }
                    }
                    TextButton(
                        buttonText = "Retry",
                        isButtonFocused = isButtonFocused,
                        onClick = onRetryCalled,
                        modifier = Modifier
                            .handleKeyEvent(
                                keyAction = KeyAction(
                                    actionDown = KeyHandler.Action(
                                        onDpadUp = onDpadUp
                                    )
                                )
                            )
                            .focusRequester(retryFocusRequester)
                            .onFocusChanged {
                                isButtonFocused = it.isFocused
                            }
                            .disableDpadLeft()
                            .disableDpadRight()
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.dp_20)))
                }
                TitleText(text = errorMessage)
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
fun VodScreenErrorPreview() {
    VodScreenError(null,
        actionComponent = {
            IconButton(
                icon = R.drawable.ic_back,
                onClick = { /*TODO*/ },
                modifier = Modifier.size(40.dp)
            )
        }
    )
}
