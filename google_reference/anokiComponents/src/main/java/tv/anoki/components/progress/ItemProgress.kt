package tv.anoki.components.progress

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import tv.anoki.components.theme.ProgressColor

/**
 * The ItemProgress component uses the LinearProgressIndicator composable. This component is to shows specified progress.
 *
 * @param progress the progress of this progress indicator, where progress value is from 0.0 to 1.0
 * @param modifier Modifier to be applied to the Composable
 */
@Composable
fun ItemProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        modifier = modifier,
        progress = progress,
        color = ProgressColor,
        trackColor = Color.White,
        strokeCap = StrokeCap.Round
    )
}