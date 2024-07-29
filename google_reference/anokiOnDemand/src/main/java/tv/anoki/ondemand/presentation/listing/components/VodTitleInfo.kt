package tv.anoki.ondemand.presentation.listing.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import tv.anoki.components.text.HeadingText
import tv.anoki.components.text.InfoText
import tv.anoki.ondemand.R

@Composable
fun VodTitleInfo(
    title: String,
    year: String,
    timeframe: String,
    origRating: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        HeadingText(
            text = title,
            fontSize = integerResource(id = R.integer.details_page_heading_font_size).toFloat()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoText(text = year)
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.dp_12)))
            InfoText(text = timeframe)
            if (origRating.isNotEmpty()) {
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.vod_details_info_text_info_text_between_spacer_height)))
                InfoText(text = origRating)
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
fun VodTitleInfoPreview() {
    VodTitleInfo(
        title = "Loki : Original series",
        year = "2024",
        timeframe = "2h 23m",
        origRating = "PG-13"
    )
}