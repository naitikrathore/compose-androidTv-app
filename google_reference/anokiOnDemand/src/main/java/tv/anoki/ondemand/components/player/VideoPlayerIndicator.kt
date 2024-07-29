package tv.anoki.ondemand.components.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.DpSize
import tv.anoki.components.theme.LightGrayColor
import tv.anoki.components.theme.ProgressColor
import tv.anoki.components.theme.WhiteColor
import tv.anoki.components.utils.actionUpOnDpadCenter
import tv.anoki.components.utils.disableDpadDown
import tv.anoki.components.utils.onDpadLeft
import tv.anoki.components.utils.onDpadRight
import tv.anoki.ondemand.R

/**
 * A composable function that displays a slider indicator for controlling video playback progress.
 *
 * @param contentProgressInMillis The current progress of the video content in milliseconds.
 * @param contentDurationInMillis The total duration of the video content in milliseconds.
 * @param progress The current progress of the slider as a float value between 0.0 and 1.0.
 * @param onSeek A callback function invoked when seeking to a new position.
 * @param state The current state of the video player.
 * @param modifier The modifier to be applied to the slider.
 * @param modifier focusManager FocusManager to manage focus state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerControllerIndicator(
    contentProgressInMillis: Long,
    contentDurationInMillis: Long,
    progress: Float,
    onSeek: (seekProgress: Float) -> Unit,
    state: VideoPlayerState,
    modifier: Modifier = Modifier,
    focusManager: FocusManager = LocalFocusManager.current,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var currentPosition by remember { mutableStateOf(contentProgressInMillis) }
    val color by rememberUpdatedState(
        newValue = SliderDefaults.colors(
            activeTrackColor = if (isFocused) WhiteColor else ProgressColor,
            thumbColor = WhiteColor,
            inactiveTrackColor = LightGrayColor
        )
    )
    var seekProgress by remember { mutableStateOf(progress) }

    val animatedIndicatorHeight by animateDpAsState(
        targetValue = dimensionResource(id = R.dimen.vod_full_player_indicator_height).times(1f),
        label = "SeekIndicator"
    )

    LaunchedEffect(isFocused, progress) {
        seekProgress = progress
        currentPosition = contentProgressInMillis
    }

    Slider(
        colors = color,
        value = if (isFocused) seekProgress else progress,
        onValueChange = { newPosition ->
            if (isFocused) {
                seekProgress = newPosition
            }
        },
        onValueChangeFinished = {
        },
        thumb = {
            if (isFocused) {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = color,
                    thumbSize = DpSize(
                        dimensionResource(id = R.dimen.vod_full_slider_thumb_size),
                        dimensionResource(id = R.dimen.vod_full_slider_thumb_size)
                    )
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(animatedIndicatorHeight)
            .onDpadLeft {
                if (isFocused) {
                    state.showControls(seconds = 10)
                    currentPosition = (currentPosition - 10000L).coerceAtLeast(0L)
                    seekProgress = (currentPosition / contentDurationInMillis.toFloat())
                    onSeek(seekProgress)
                }
            }
            .onDpadRight {
                if (isFocused) {
                    state.showControls(seconds = 10)
                    currentPosition = (currentPosition + 10000L).coerceAtMost(contentDurationInMillis)
                    seekProgress = (currentPosition / contentDurationInMillis.toFloat())
                    onSeek(seekProgress)
                }
            }
            .actionUpOnDpadCenter {
                focusManager.moveFocus(FocusDirection.Up)
                state.showControls(seconds = 6)
            }
            .disableDpadDown()
            .focusable(interactionSource = interactionSource)
    )
}
