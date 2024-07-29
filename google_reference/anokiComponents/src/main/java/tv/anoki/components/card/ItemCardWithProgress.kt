package tv.anoki.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import tv.anoki.components.R
import tv.anoki.components.image.RenderScaleImage
import tv.anoki.components.progress.ItemProgress

/**
 * The ItemCardWithProgress component uses the Card composable. This card component is to show image and progress value.
 *
 * @param progress the float value to show the progress. Value is from 0 to 1
 * @param imagePath the image url to render image
 * @param modifier Modifier to be applied to the Composable
 * @param onClick the lambda that the card calls when the user presses it
 * @param onFocusChanged the lambda that the card calls when the user changes focus on it
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ItemCardWithProgress(
    progress: Float,
    imagePath: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {}
) {
    Card(
        modifier = modifier
            .onFocusChanged {
                onFocusChanged(it.hasFocus)
            },
        shape = CardDefaults.shape(RoundedCornerShape(dimensionResource(id = R.dimen.item_card_with_progress_corner_radius))),
        border = CardDefaults.border(
            focusedBorder = Border(
                BorderStroke(dimensionResource(id = R.dimen.rounded_card_border_size), Color.White),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.item_card_with_progress_corner_radius))
            )
        ),
        onClick = onClick
    ) {
        Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.BottomCenter) {
            RenderScaleImage(
                imageUrl = imagePath,
                contentScale = ContentScale.FillBounds
            )
            if (progress > 0) {
                ItemProgress(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.vod_progress_bar_horizontal_padding))
                        .height(dimensionResource(id = R.dimen.vod_progress_bar_height)),
                    progress = progress
                )
            }
        }
    }
}

@Preview
@Composable
fun ItemCardPreview() {
    ItemCardWithProgress(
        imagePath = "",
        progress = 0.5f
    )
}