package tv.anoki.ondemand.presentation.player

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import tv.anoki.components.button.IconButton
import tv.anoki.components.loading.LoadingScreen
import tv.anoki.components.text.HeadingText
import tv.anoki.components.utils.actionUpOnDpadCenter
import tv.anoki.components.utils.actionUpOnDpadDown
import tv.anoki.components.utils.actionUpOnDpadLeft
import tv.anoki.components.utils.actionUpOnDpadRight
import tv.anoki.components.utils.actionUpOnDpadUp
import tv.anoki.components.utils.onBackPressed
import tv.anoki.ondemand.R
import tv.anoki.ondemand.components.VodScreenError
import tv.anoki.ondemand.components.player.VideoPlayerMainFrame
import tv.anoki.ondemand.components.player.VideoPlayerMediaDetails
import tv.anoki.ondemand.components.player.VideoPlayerMediaTimeDetails
import tv.anoki.ondemand.components.player.VideoPlayerOverlay
import tv.anoki.ondemand.components.player.VideoPlayerSeeker
import tv.anoki.ondemand.components.player.VideoPlayerState
import tv.anoki.ondemand.components.player.rememberVideoPlayerState
import java.util.Locale
import java.util.concurrent.TimeUnit


/**
 * Displays an error screen with a title and a back button.
 *
 * @param title The title to display.
 * @param onBackPressed The action to perform when the back button is pressed.
 * @param modifier The modifier to be applied to the Box.
 */
@Composable
fun PlayerError(title: String, onBackPressed: () -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        modifier = modifier.padding(
            top = dimensionResource(id = R.dimen.full_page_top_spacing),
            start = dimensionResource(id = R.dimen.page_start_spacing)
        ),
        contentAlignment = Alignment.TopStart
    ) {
        Row {
            IconButton(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.vod_back_button_size))
                    .focusRequester(focusRequester),
                onClick = { onBackPressed() },
                icon = R.drawable.ic_back,
                isButtonFocused = true
            )
            HeadingText(
                text = title,
                fontSize = dimensionResource(id = R.dimen.vod_details_heading_text_font_size).value,
                maxLines = 1,
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.dp_16))
            )
        }
    }
}

/**
 * Displays the video player screen based on the current UI state.
 *
 * @param videoPlayerScreenViewModel The view model for the video player screen.
 * @param onBackPressed The action to perform when the back button is pressed.
 * @param onClickPlay The action to perform when the play button is clicked.
 * @param onClickPause The action to perform when the pause button is clicked.
 * @param onSeek The action to perform when seeking.
 */
@Composable
fun VideoPlayerScreen(
    videoPlayerScreenViewModel: VideoPlayerScreenViewModel,
    onBackPressed: () -> Unit,
    onClickPlay: () -> Unit,
    onClickPause: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    val uiState by videoPlayerScreenViewModel.videoState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is VODPlayerUiState.Loading -> {
            LoadingScreen(onBackPressed = onBackPressed)
        }
        is VODPlayerUiState.Error -> {
            VodScreenError(exception = s.exception) {
                PlayerError(title = s.loadingData, onBackPressed = onBackPressed)
            }
        }
        is VODPlayerUiState.Ready -> {
            VideoPlayerScreenContentWithoutExoplayer(
                videoPlayerScreenViewModel = videoPlayerScreenViewModel,
                onBackPressed = onBackPressed,
                onClickPlay = onClickPlay,
                onClickPause = onClickPause,
                onSeek = onSeek
            )
        }
    }
}

/**
 * Displays the content of the video player screen without ExoPlayer.
 *
 * @param videoPlayerScreenViewModel The view model for the video player screen.
 * @param onBackPressed The action to perform when the back button is pressed.
 * @param onClickPlay The action to perform when the play button is clicked.
 * @param onClickPause The action to perform when the pause button is clicked.
 * @param onSeek The action to perform when seeking.
 * @param modifier The modifier to be applied to the Box.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreenContentWithoutExoplayer(
    videoPlayerScreenViewModel: VideoPlayerScreenViewModel,
    onBackPressed: () -> Unit,
    onClickPlay: () -> Unit,
    onClickPause: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val playerUiState by videoPlayerScreenViewModel.playerUiState.collectAsStateWithLifecycle()
    val videoPlayerState = rememberVideoPlayerState(coroutineScope = rememberCoroutineScope())
    val contentDurationInMillis = playerUiState.contentDuration
    val contentProgressInMillis = playerUiState.contentCurrentPosition
    val contentProgress by remember(contentProgressInMillis, contentDurationInMillis) {
        derivedStateOf { contentProgressInMillis.toFloat() / contentDurationInMillis }
    }

    Box(
        modifier
            .onBackPressed {
                if (videoPlayerState.controlsVisible.not()) {
                    onBackPressed()
                } else {
                    videoPlayerState.hideControls()
                }
            }
            .actionUpOnDpadUp { videoPlayerState.showControls() }
            .actionUpOnDpadDown { videoPlayerState.showControls() }
            .actionUpOnDpadLeft { videoPlayerState.showControls() }
            .actionUpOnDpadRight { videoPlayerState.showControls() }
            .actionUpOnDpadCenter { videoPlayerState.showControls() }
            .focusable()
    ) {
        val focusRequester = remember { FocusRequester() }
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            state = videoPlayerState,
            isPlaying = playerUiState.isPlaying,
            controls = {
                VideoPlayerControls(
                    focusRequester = focusRequester,
                    isPlaying = playerUiState.isPlaying,
                    contentProgressInMillis = playerUiState.contentCurrentPosition,
                    contentDurationInMillis = playerUiState.contentDuration,
                    contentProgress = contentProgress,
                    title = playerUiState.title,
                    rating = playerUiState.origRating,
                    state = videoPlayerState,
                    onClickPlay = onClickPlay,
                    onClickPause = onClickPause,
                    onSeek = onSeek
                )
            }
        )
    }
}

/**
 * Displays the controls for the video player.
 *
 * @param focusRequester The focus requester for managing focus.
 * @param isPlaying Whether the video is currently playing.
 * @param title The title of the video.
 * @param contentProgressInMillis The current progress of the video in milliseconds.
 * @param contentDurationInMillis The total duration of the video in milliseconds.
 * @param contentProgress The progress of the video as a fraction of the total duration.
 * @param onClickPlay The action to perform when the play button is clicked.
 * @param onClickPause The action to perform when the pause button is clicked.
 * @param onSeek The action to perform when seeking.
 * @param state The state of the video player.
 * @param modifier The modifier to be applied to the controls.
 */
@Composable
fun VideoPlayerControls(
    focusRequester: FocusRequester,
    isPlaying: Boolean,
    title: String,
    rating: String,
    contentProgressInMillis: Long,
    contentDurationInMillis: Long,
    contentProgress: Float,
    onClickPlay: () -> Unit,
    onClickPause: () -> Unit,
    onSeek: (seekProgress: Float) -> Unit,
    state: VideoPlayerState,
    modifier: Modifier = Modifier
) {
    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) {
            onClickPlay()
        } else {
            onClickPause()
        }
    }

    val contentProgressString by remember(contentProgressInMillis) {
        derivedStateOf { converter(contentProgressInMillis) }
    }

    val contentDurationString by remember(contentDurationInMillis) {
        derivedStateOf { converter(contentDurationInMillis) }
    }

    VideoPlayerMainFrame(
        mediaDetails = {
            VideoPlayerMediaDetails(
                focusRequester = focusRequester,
                onPlayPauseToggle = onPlayPauseToggle,
                isPlaying = isPlaying,
                title = title,
                rating = rating
            )
        },
        seeker = {
            VideoPlayerSeeker(contentProgressInMillis = contentProgressInMillis, contentDurationInMillis = contentDurationInMillis, contentProgress = contentProgress, onSeek = onSeek, state = state)
        },
        timeDetails = {
            VideoPlayerMediaTimeDetails(
                contentProgressString = contentProgressString,
                contentDurationString = contentDurationString
            )
        },
        modifier = modifier
    )
}

/**
 * Converts the given time in milliseconds to a formatted string.
 *
 * @param millis The time in milliseconds.
 * @return The formatted time string.
 */
private fun converter(millis: Long) = if (TimeUnit.MILLISECONDS.toHours(millis) > 0) {
    String.format(
        locale = Locale.ENGLISH,
        "%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
    )
} else {
    String.format(
        locale = Locale.ENGLISH,
        "%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
    )
}