package tv.anoki.ondemand.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Composable function that Handles the visibility and animation of the controls.
 *
 * @param isPlaying      Indicates whether the video is currently playing.
 * @param modifier       Modifier for styling and positioning the overlay.
 * @param state          The state of the video player controls.
 * @param focusRequester FocusRequester used to request focus for the overlay.
 * @param centerButton   Composable function for displaying a center button in the overlay.
 * @param controls       Composable function for displaying the video player controls.
 */
@Composable
fun VideoPlayerOverlay(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    state: VideoPlayerState = rememberVideoPlayerState(coroutineScope = rememberCoroutineScope()),
    focusRequester: FocusRequester = remember { FocusRequester() },
    centerButton: @Composable () -> Unit = {},
    controls: @Composable () -> Unit = {}
) {

    LaunchedEffect(state.controlsVisible) {
        if (state.controlsVisible) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            state.showControls(seconds = Int.MAX_VALUE)
        } else {
            state.showControls()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(state.controlsVisible, Modifier, fadeIn(), fadeOut()) {
            CinematicBackground(Modifier.fillMaxSize())
        }

        Column {
            AnimatedVisibility(
                state.controlsVisible,
                Modifier,
                slideInVertically { it },
                slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 56.dp)
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    controls()
                }
            }
        }
        centerButton()
    }
}

@Composable
fun CinematicBackground(modifier: Modifier = Modifier) {
    Spacer(
        modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = 0.1f),
                    Color.Black.copy(alpha = 0.8f)
                )
            )
        )
    )
}

@Preview(device = "id:tv_4k")
@Composable
private fun VideoPlayerOverlayPreview() {
    Box(Modifier.fillMaxSize()) {
        VideoPlayerOverlay(
            isPlaying = false,
            modifier = Modifier.align(Alignment.BottomCenter),
            controls = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.Blue)
                )
            },
            centerButton = {
                Box(
                    Modifier
                        .size(88.dp)
                        .background(Color.Green)
                )
            }
        )
    }
}