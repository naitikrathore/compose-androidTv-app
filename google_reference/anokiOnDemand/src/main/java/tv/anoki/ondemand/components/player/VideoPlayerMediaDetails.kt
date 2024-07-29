package tv.anoki.ondemand.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.sp
import tv.anoki.components.text.HeadingInlineTextContent
import tv.anoki.ondemand.R

/**
 * Composable function that displays media details such as play/pause button and title for the video player.
 *
 * @param focusRequester      FocusRequester to request focus for the play/pause button.
 * @param onPlayPauseToggle   Callback to toggle between play and pause states.
 * @param isPlaying           Boolean indicating if the media is currently playing.
 * @param title               Title of the media content.
 * @param rating              Rating of the media content.
 * @param modifier            Modifier for styling and positioning the media details.
 */
@Composable
fun VideoPlayerMediaDetails(
    focusRequester: FocusRequester,
    onPlayPauseToggle: (Boolean) -> Unit,
    isPlaying: Boolean,
    title: String,
    rating: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.vod_full_icon_padding_horizontal))
                .size(dimensionResource(id = R.dimen.vod_full_play_pause_icon_size))
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(
                    width = dimensionResource(id = tv.anoki.components.R.dimen.rounded_card_border_size),
                    color = Color.White,
                    shape = CircleShape
                )
        ) {
            VideoPlayerControlsIcon(
                modifier = Modifier.focusRequester(focusRequester),
                icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                onClick = { onPlayPauseToggle(!isPlaying) }
            )
        }
        HeadingInlineTextContent(
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.vod_full_heading_padding_left)),
            text = title,
            fontSize = dimensionResource(id = R.dimen.vod_full_screen_title_font_size).value,
            maxLines = 1,
            inlineText = rating
        )
    }
}