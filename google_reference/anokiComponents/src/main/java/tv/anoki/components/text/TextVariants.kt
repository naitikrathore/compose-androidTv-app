@file:OptIn(ExperimentalFoundationApi::class, ExperimentalTvMaterial3Api::class)

package tv.anoki.components.text

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.anoki.components.theme.AppFontFamily
import tv.anoki.components.theme.LightGrayColor
import tv.anoki.components.theme.LightWhiteColor
import tv.anoki.components.theme.WhiteColor

/**
 * The HeadingText component uses the Text composable.
 *
 * @param text  the text to be displayed
 * @param modifier Modifier to be applied to the Composable
 * @param fontSize the size to be set to composable
 * @param fontWeight the font weight to be set to composable
 * @param maxLines the maximum number of lines to be displayed
 */
@Composable
fun HeadingText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 48F,
    fontWeight: FontWeight = FontWeight.Black,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .basicMarquee(),
        color = LightWhiteColor,
        fontSize = TextUnit(value = fontSize, type = TextUnitType.Sp),
        fontFamily = AppFontFamily,
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = fontWeight),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun HeadingTextPreview() {
    Column {
        HeadingText(text = "Loki Original series")
        HeadingText(
            text = "Loki Original series",
            fontSize = 32F
        )
        HeadingText(
            text = "Loki, the God of Mischief, steps out of his brother's shadow",
            modifier = Modifier.width(480.dp),
            maxLines = 1
        )
        HeadingText(
            text = "Loki, the God of Mischief, steps out of his brother's shadow",
            modifier = Modifier.width(480.dp),
            fontSize = 32F,
            maxLines = 1
        )
    }
}

/**
 * The HeadingText component uses the Text composable.
 *
 * @param text  the text to be displayed
 * @param inlineText the text to be displayed at the end of textview
 * @param modifier Modifier to be applied to the Composable
 * @param fontSize the size to be set to composable
 * @param fontWeight the font weight to be set to composable
 * @param maxLines the maximum number of lines to be displayed
 */
@Composable
fun HeadingInlineTextContent(
    text: String,
    inlineText: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 48F,
    fontWeight: FontWeight = FontWeight.Medium,
    maxLines: Int = Int.MAX_VALUE
) {
    val inlineContentId = "inlineContent"
    val textStr = buildAnnotatedString {
        append(text)
        // Append a placeholder string "[inlineText]" and attach an annotation "inlineContent" on it.
        appendInlineContent(inlineContentId, "[inlineText]")
    }

    val inlineContent = mapOf(
        Pair(
            // This tells the [BasicText] to replace the placeholder string "[inlineText]" by
            // the composable given in the [InlineTextContent] object.
            inlineContentId,
            InlineTextContent(
                // Placeholder tells text layout the expected size and vertical alignment of
                // children composable.
                Placeholder(
                    width = 2.em,
                    height = 0.6.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                )
            ) {
                // This [Box] will fill maximum size, which is specified by the [Placeholder]
                // above. Notice the width and height in [Placeholder] are specified in TextUnit,
                // and are converted into pixel by text layout.
                Box(modifier = Modifier.fillMaxSize().padding(start = 10.dp)) {
                    HeadingText(
                        text = inlineText,
                        fontSize = 14F,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    )

    BasicText(
        text = textStr,
        modifier = modifier
            .fillMaxWidth()
            .basicMarquee(),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = fontWeight, color = LightWhiteColor, fontFamily = AppFontFamily).copy(fontSize = TextUnit(value = fontSize, type = TextUnitType.Sp)),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        inlineContent = inlineContent
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun HeadingInlineTextContentPreview() {
    Column {
        HeadingInlineTextContent(text = "Loki Original series", inlineText = "TV-EPG")
        HeadingInlineTextContent(
            text = "Loki, the God of Mischief, steps out of his brother's shadow",
            modifier = Modifier.width(480.dp),
            maxLines = 1,
            inlineText = "TV-EPG"
        )
    }
}


/**
 * The TitleText component uses the Text composable.
 *
 * @param text  the text to be displayed
 * @param modifier Modifier to be applied to the Composable
 * @param color the color to be set to composable
 * @param fontSize the size to be set to composable
 * @param fontWeight the font weight to be set to composable
 */
@Composable
fun TitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LightWhiteColor,
    fontSize: Float = 16F,
    fontWeight: FontWeight = FontWeight.Medium
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = TextUnit(value = fontSize, type = TextUnitType.Sp),
        fontFamily = AppFontFamily,
        lineHeight = TextUnit(value = 40F, type = TextUnitType.Sp),
        letterSpacing = TextUnit(value = 0.1F, type = TextUnitType.Sp),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = fontWeight)
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun TitleTextPreview() {
    TitleText(
        text = "Top Picks For You"
    )
}

/**
 * The InfoText component uses the Text composable.
 *
 * @param text  the text to be displayed
 * @param modifier Modifier to be applied to the Composable
 * @param color the color to be set to composable
 * @param fontWeight the font weight to be set to composable
 */
@Composable
fun InfoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LightGrayColor,
    fontWeight: FontWeight = FontWeight.Medium
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = TextUnit(value = 14F, type = TextUnitType.Sp),
        fontFamily = AppFontFamily,
        letterSpacing = TextUnit(value = 0.25F, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20F, type = TextUnitType.Sp),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = fontWeight)
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun InfoTextPreview() {
    Row {
        InfoText(
            text = "2021"
        )
        Spacer(modifier = Modifier.width(12.dp))
        InfoText(
            text = "4 Seasons"
        )
    }
}

/**
 * The EpisodeInfoText component uses the Text composable.
 *
 * @param text  the text to be displayed
 * @param modifier Modifier to be applied to the Composable
 * @param fontWeight the weight to be set to composable
 */
@Composable
fun EpisodeInfoText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Medium
) {
    Text(
        text = text,
        modifier = modifier,
        color = LightGrayColor,
        fontSize = TextUnit(value = 12F, type = TextUnitType.Sp),
        fontFamily = AppFontFamily,
        letterSpacing = TextUnit(value = 0.1F, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20F, type = TextUnitType.Sp),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = fontWeight)
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun EpisodeInfoTextPreview() {
    Row {
        EpisodeInfoText(text = "S1 E1")
        Spacer(modifier = Modifier.width(12.dp))
        EpisodeInfoText(text = "o")
        Spacer(modifier = Modifier.width(12.dp))
        EpisodeInfoText(text = "9 Jun 2021")
        Spacer(modifier = Modifier.width(12.dp))
        EpisodeInfoText(text = "o")
        Spacer(modifier = Modifier.width(12.dp))
        EpisodeInfoText(text = "53m")
    }
}

/**
 * The EpisodeTitleText component uses the Text composable.
 *
 * @param text  the text to be displayed
 * @param modifier Modifier to be applied to the Composable
 */
@Composable
fun EpisodeTitleText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = WhiteColor,
        fontSize = TextUnit(value = 14F, type = TextUnitType.Sp),
        fontFamily = AppFontFamily,
        letterSpacing = TextUnit(value = 0.1F, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20F, type = TextUnitType.Sp),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium)
    )
}

/**
 * The BodyText component uses the Text composable.
 *
 * @param text  the text to be displayed
 * @param modifier Modifier to be applied to the Composable
 * @param maxLines the maximum number of lines to be displayed
 * @param fontWeight the font weight to be set to composable
 */
@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = LightGrayColor,
        fontSize = TextUnit(value = 16F, type = TextUnitType.Sp),
        fontFamily = AppFontFamily,
        letterSpacing = TextUnit(value = 0.25F, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 24F, type = TextUnitType.Sp),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = fontWeight),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun BodyTextPreview() {
    Column {
        BodyText(
            modifier = Modifier.width(480.dp),
            text = "Loki, the God of Mischief, steps out of his brother's shadow to embark on an adventure that takes place after the events of \"Avengers: Endgame.\", Loki, the God of Mischief, steps out of his brother's shadow to embark on an adventure that takes place after the events of \"Avengers: Endgame.\""
        )
        BodyText(
            modifier = Modifier.width(480.dp),
            text = "Loki, the God of Mischief, steps out of his brother's shadow to embark on an adventure that takes place after the events of \"Avengers: Endgame.\", Loki, the God of Mischief, steps out of his brother's shadow to embark on an adventure that takes place after the events of \"Avengers: Endgame.\"",
            maxLines = 3
        )
    }
}

/**
 * The EpisodeBodyText component uses the Text composable.
 *
 * @param text  the text to be displayed
 * @param modifier Modifier to be applied to the Composable
 * @param maxLines the maximum number of lines to be displayed
 */
@Composable
fun EpisodeBodyText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = WhiteColor,
        fontSize = TextUnit(value = 12F, type = TextUnitType.Sp),
        fontFamily = AppFontFamily,
        lineHeight = TextUnit(value = 16F, type = TextUnitType.Sp),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "This needs to be updated or removed as per CLTV requirement."
)
@Composable
fun StandardCardTitle(
    text: String,
    color: Color,
    isViewFocused: Boolean,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.W400
) {
    val textAlpha by animateFloatAsState(
        targetValue = if (isViewFocused) 1f else 0f,
        label = "textAlpha"
    )

    Text(
        text = text,
        modifier = modifier
            .alpha(textAlpha)
            .fillMaxWidth()
            .padding(start = 5.dp, bottom = 4.dp),
        color = color.copy(alpha = 0.7f),
        textAlign = TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = fontWeight)
    )
}
