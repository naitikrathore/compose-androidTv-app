package tv.anoki.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Text
import tv.anoki.components.R
import tv.anoki.components.theme.DarkGrayColor
import tv.anoki.components.theme.LightWhiteColor
import tv.anoki.components.theme.WhiteColor

/**
 * The text button component uses the Button composable. Until pressed, it appears as a text.
 * It does not have a solid fill but outline by default.
 *
 * @param buttonText the string that appears on the button.
 * @param onClick the lambda that the button calls when the user presses it.
 * @param modifier Modifier to be applied to the Composable
 * @param isButtonFocused the flag to control the focus state of the button
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TextButton(
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isButtonFocused: Boolean = false
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        border = ButtonDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = 1.dp,
                    color = WhiteColor
                ),
                inset = (-1).dp
            )
        ),
        shape = ButtonDefaults.shape(shape = ShapeDefaults.ExtraLarge),
        colors = ButtonDefaults.colors(
            containerColor = if (isButtonFocused) WhiteColor else Color.Transparent,
            contentColor = if (isButtonFocused) DarkGrayColor else LightWhiteColor,
            focusedContainerColor = if (isButtonFocused) WhiteColor else Color.Transparent,
            focusedContentColor = if (isButtonFocused) DarkGrayColor else LightWhiteColor
        ),
        contentPadding = PaddingValues(
            vertical = dimensionResource(id = R.dimen.button_vertical_padding),
            horizontal = dimensionResource(id = R.dimen.button_horizontal_padding)
        )
    ) {
        Text(text = buttonText)
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
fun Default_TextButtonPreview() {
    TextButton(
        buttonText = "Play Season 1 Episode 1",
        onClick = {},
        isButtonFocused = false,
        modifier = Modifier
            .width(340.dp)
            .height(40.dp)
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun Focused_TextButtonPreview() {
    TextButton(
        buttonText = "Play Season 1 Episode 1",
        onClick = {},
        isButtonFocused = true,
        modifier = Modifier
            .width(340.dp)
            .height(40.dp)
    )
}
