package tv.anoki.components.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * The RenderCropImage component uses the AsyncImage composable.
 *
 * @param imageUrl the image url to render image
 * @param modifier the Modifier to be applied to this card
 * @param imageDescription optional text to describe what this image represents
 * @param alignment optional alignment parameter used to render image in the given bounds defined by the width and height
 */
@Composable
fun RenderCropImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageDescription: String? = null,
    alignment: Alignment = Alignment.Center
) {
    RenderCoilImage(
        modifier = modifier,
        imageUrl = imageUrl,
        imageDescription = imageDescription,
        contentScale = ContentScale.Crop,
        alignment = alignment
    )
}

/**
 * The RenderCropImage component uses the AsyncImage composable.
 *
 * @param imageUrl the image url to render image
 * @param modifier Modifier to be applied to the Composable
 * @param imageDescription optional text to describe what this image represents
 * @param contentScale optional scale parameter used to determine the aspect ratio scaling to be used
 * @param alignment optional alignment parameter used to render image in the given bounds defined by the width and height
 */
@Composable
fun RenderScaleImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    RenderCoilImage(
        modifier = modifier,
        imageUrl = imageUrl,
        imageDescription = imageDescription,
        contentScale = contentScale,
        alignment = alignment
    )
}

@Composable
private fun RenderCoilImage(
    imageUrl: String,
    contentScale: ContentScale,
    alignment: Alignment,
    modifier: Modifier = Modifier,
    imageDescription: String? = null
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .build(),
        contentDescription = imageDescription,
        modifier = modifier
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                }
            },
        alignment = alignment,
        contentScale = contentScale
    )
}



