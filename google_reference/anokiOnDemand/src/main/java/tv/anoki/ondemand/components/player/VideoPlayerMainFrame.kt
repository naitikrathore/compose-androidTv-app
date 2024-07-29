package tv.anoki.ondemand.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tv.anoki.ondemand.R

/**
 * Composable function that defines the main frame layout for the video player UI.
 *
 * @param mediaDetails  Composable function to display media details (e.g., title, description).
 * @param seeker        Composable function for seeking within the media.
 * @param timeDetails   Composable function to display time details (e.g., current time, duration).
 * @param modifier      Modifier for styling and positioning the main frame.
 */
@Composable
fun VideoPlayerMainFrame(
    mediaDetails: @Composable () -> Unit,
    seeker: @Composable () -> Unit,
    timeDetails: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = dimensionResource(id = R.dimen.vod_controls_padding_horizontal),
                )
                .align(Alignment.BottomEnd)
        ) {
            mediaDetails()
            Spacer(modifier = Modifier.padding(dimensionResource(id = R.dimen.vod_controls_space_padding)))
            seeker()
            Spacer(modifier = Modifier.padding(dimensionResource(id = R.dimen.vod_controls_space_padding)))
            timeDetails()
        }
    }
}


@Preview(device = "id:tv_4k")
@Composable
private fun MediaPlayerMainFramePreviewLayout() {
    VideoPlayerMainFrame(
        mediaDetails = {
            Box(
                Modifier
                    .border(2.dp, Color.Red)
                    .background(Color.LightGray)
                    .fillMaxWidth()
                    .height(64.dp)
            )
        },
        seeker = {
            Box(
                Modifier
                    .border(2.dp, Color.Red)
                    .background(Color.LightGray)
                    .fillMaxWidth()
                    .height(16.dp)
            )
        },
        timeDetails = {
            Box(
                Modifier
                    .border(2.dp, Color.Red)
                    .background(Color.LightGray)
                    .fillMaxWidth()
                    .height(64.dp)
            )
        },
    )
}

@Preview(device = "id:tv_4k")
@Composable
private fun MediaPlayerMainFramePreviewLayoutWithoutMore() {
    VideoPlayerMainFrame(mediaDetails = {
        Box(
            Modifier
                .border(2.dp, Color.Red)
                .background(Color.LightGray)
                .fillMaxWidth()
                .height(64.dp)
        )
    }, seeker = {
        Box(
            Modifier
                .border(2.dp, Color.Red)
                .background(Color.LightGray)
                .fillMaxWidth()
                .height(16.dp)
        )
    }, timeDetails = {
        Box(
            Modifier
                .border(2.dp, Color.Red)
                .background(Color.LightGray)
                .fillMaxWidth()
                .height(64.dp)
        )
    })
}