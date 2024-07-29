package tv.anoki.ondemand.components.player

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


/**
 * Composable function that displays a seek bar for a video player.
 *
 * @param contentProgressInMillis The current progress of the video in milliseconds.
 * @param contentDurationInMillis The total duration of the video in milliseconds.
 * @param contentProgress The current progress of the video content, ranging from 0 to 1.
 * @param onSeek          Callback invoked when the user seeks to a new position in the video.
 * @param state           The state of the video player controls.
 * @param modifier        Modifier for styling and positioning the seek bar.
 */
@Composable
fun VideoPlayerSeeker(
    contentProgress: Float,
    contentProgressInMillis: Long,
    contentDurationInMillis: Long,
    onSeek: (seekProgress: Float) -> Unit,
    state: VideoPlayerState,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        VideoPlayerControllerIndicator(
            contentDurationInMillis = contentDurationInMillis,
            contentProgressInMillis = contentProgressInMillis,
            progress = contentProgress,
            onSeek = onSeek,
            state = state
        )
    }
}