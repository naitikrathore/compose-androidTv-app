package tv.anoki.ondemand.components.player

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import tv.anoki.components.text.TitleText
import tv.anoki.ondemand.R

/**
 * Composable function that displays the current media time details (e.g., progress and duration) for the video player.
 *
 * @param contentProgressString String representing the current progress of the media content.
 * @param contentDurationString String representing the total duration of the media content.
 * @param modifier             Modifier for styling and positioning the time details.
 */
@Composable
fun VideoPlayerMediaTimeDetails(
    contentProgressString: String, contentDurationString: String, modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        TitleText(
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.vod_full_timer_padding_horizontal)),
            text = "$contentProgressString / $contentDurationString",
        )
    }
}