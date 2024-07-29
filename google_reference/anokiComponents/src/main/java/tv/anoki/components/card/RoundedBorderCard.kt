package tv.anoki.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ShapeDefaults
import tv.anoki.components.R

/**
 * The RoundedBorderCard component uses the Card composable. This card component shows border on focus.
 *
 * @param modifier Modifier to be applied to the Composable
 * @param onClick the lambda that the card calls when the user presses it
 * @param shape  CardShape defines the shape of this card's container in different interaction states
 * @param content defines the Composable content inside the Card
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RoundedBorderCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CardShape =  CardDefaults.shape(shape = ShapeDefaults.ExtraLarge),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.colors(containerColor = Color.Transparent, contentColor = Color.Transparent),
        border = CardDefaults.border(
            focusedBorder = Border(
                BorderStroke(dimensionResource(id = R.dimen.rounded_card_border_size), Color.White)
            )
        ),
        onClick = onClick
    ) {
        content()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(device = Devices.TV_1080p)
@Composable
fun Default_RoundedBorderCardPreview() {
    RoundedBorderCard(
        modifier = Modifier
            .width(275.dp)
            .height(120.dp),
        onClick = {}
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = R.drawable.sample),
            contentDescription = "image description",
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(device = Devices.TV_1080p)
@Composable
fun Focused_RoundedBorderCardPreview() {
    RoundedBorderCard(
        modifier = Modifier
            .width(275.dp)
            .height(120.dp),
        onClick = {}
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = R.drawable.sample),
            contentDescription = "image description",
            contentScale = ContentScale.Crop
        )
    }
}
