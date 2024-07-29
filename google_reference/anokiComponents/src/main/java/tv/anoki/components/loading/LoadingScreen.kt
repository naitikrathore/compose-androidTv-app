package tv.anoki.components.loading

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import tv.anoki.components.constants.LogConstants
import tv.anoki.components.progress.CircularProgressView
import tv.anoki.components.theme.BackgroundColor
import tv.anoki.components.utils.onBackPressed
import tv.anoki.components.utils.setBackgroundColor

private const val TAG = "LoadingViews"

/**
 * Internal implementation of a loading screen Composable.
 * Displays a circular progress indicator and optionally requests focus when composed.
 *
 * @param modifier Modifier to be applied to the Composable.
 * @param isFocusRequested Flag indicating whether focus should be requested when composed. Default is false.
 * @param onBackPressed Callback to be invoked when the back button is pressed. Default is an empty lambda.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LoadingScreenInternal(
    modifier: Modifier = Modifier,
    isFocusRequested: Boolean = false,
    backgroundColor: Color = BackgroundColor,
    onBackPressed: () -> Unit = {}
) {

    val focusRequester = remember {
        if (isFocusRequested) FocusRequester() else null
    }

    LaunchedEffect(Unit) {
        focusRequester?.requestFocus()
    }

    Surface(
        onClick = {/*don't handle click event*/},
        modifier
            .clickable(enabled = false) {}
            .focusRequester(focusRequester ?: FocusRequester())
            .onBackPressed {
                Log.d(LogConstants.CLTV_TAG + TAG, "LoadingScreenInternal: Back Button Pressed from the loading screen")
                onBackPressed()
            }
            .fillMaxSize()
            .setBackgroundColor(),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = backgroundColor,
            contentColor = backgroundColor,
            focusedContainerColor = backgroundColor,
            focusedContentColor = backgroundColor,
            pressedContainerColor = backgroundColor,
            pressedContentColor = backgroundColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = backgroundColor

        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f) // Disables scaling for focused state in ClickableSurface component.
    ) {
        CircularProgressView()
    }
}

/**
 * Displays a loading screen without handling back button presses.
 * Use this function to present a loading screen in your UI.
 *
 * @param modifier The modifier to be applied to the Composable.
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    LoadingScreenInternal(
        modifier = modifier
    )
}

/**
 * Displays a loading screen and handles back button presses.
 * Use this function to present a loading screen that also handles back button presses.
 *
 * @param modifier The modifier to be applied to the Composable.
 * @param onBackPressed The callback to be invoked when the back button is pressed.
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit
) {
    LoadingScreenInternal(
        modifier = modifier, isFocusRequested = true, onBackPressed = onBackPressed
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun LoadingScreenPreview() {
    LoadingScreen()
}

@Preview(device = Devices.TV_1080p)
@Composable
fun LoadingScreenWithBackPressedPreview() {
    LoadingScreen {}
}