package tv.anoki.ondemand.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import tv.anoki.components.image.RenderScaleImage
import tv.anoki.components.theme.GradientColor

/**
 * This component is to render background image on details screen
 *
 * @param url the string to render image from the server
 * @param modifier Modifier to be applied to the Composable
 */
@Composable
fun DetailsBackgroundImage(
    url: String,
    modifier: Modifier = Modifier,
) {
    Box {
        RenderScaleImage(
            imageUrl = url,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // TODO This need to be updated and waiting for UX developer to provide actual values
        val hGradient = Brush.horizontalGradient(
            colorStops = arrayOf(
                0F to GradientColor.copy(alpha = 0.99F),
                0.5F to GradientColor.copy(alpha = 0.95F),
                1F to GradientColor.copy(alpha = 0.7F)
            ),
            startX = LocalConfiguration.current.screenWidthDp.div(5).toFloat(),
            endX = LocalConfiguration.current.screenWidthDp.div(1.8).toFloat()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(hGradient)
        )
    }
}
