package tv.anoki.ondemand.components.player

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Manages the visibility of video player controls.
 *
 * @param hideSeconds The number of seconds to wait before hiding the controls.
 * @param coroutineScope The coroutine scope used for launching coroutines.
 */
class VideoPlayerState internal constructor(
    @IntRange(from = 0)
    private val hideSeconds: Int = 6,
    val coroutineScope: CoroutineScope
) {
    private var _controlsVisible by mutableStateOf(true)
    val controlsVisible get() = _controlsVisible

    private val countDownTimer = MutableStateFlow(value = hideSeconds)

    /**
     * Initializes the VideoPlayerState.
     * Starts a timer to automatically hide controls after a period of inactivity.
     */
    init {
        coroutineScope.launch {
            countDownTimer.collectLatest { time ->
                if (time > 0) {
                    _controlsVisible = true
                    delay(1000)
                    countDownTimer.emit(countDownTimer.value - 1)
                } else {
                    _controlsVisible = false
                }
            }
        }
    }

    /**
     * Shows the video player controls for a specified duration.
     *
     * @param seconds The duration in seconds for which to show the controls.
     */
    fun showControls(seconds: Int = hideSeconds) {
        coroutineScope.launch {
            countDownTimer.emit(seconds)
        }
    }

    /**
     * Hides the video player controls.
     */
    fun hideControls() {
        coroutineScope.launch {
            countDownTimer.emit(0)
        }
    }
}

/**
 * Create and remember a [VideoPlayerState] instance. Useful when trying to control the state of
 * the [VideoPlayerOverlay]-related composable.
 * @return A remembered instance of [VideoPlayerState].
 * */
@Composable
fun rememberVideoPlayerState(coroutineScope: CoroutineScope) =
    remember { VideoPlayerState(coroutineScope = coroutineScope) }

