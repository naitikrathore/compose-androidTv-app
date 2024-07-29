package tv.anoki.components.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import tv.anoki.components.R

/**
 * The CircularProgressView component uses the CircularProgressIndicator composable. This component is to shows loading for infinite time.
 *
 * @param modifier Modifier to be applied to the Composable
 */
@Composable
fun CircularProgressView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.circular_progress_view_width))
                .align(Alignment.Center),
            color = Color.Gray,
            trackColor = Color.White,
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
fun CircularProgressViewPreview() {
    CircularProgressView()
}