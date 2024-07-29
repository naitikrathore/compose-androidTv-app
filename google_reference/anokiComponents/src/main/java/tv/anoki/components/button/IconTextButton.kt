package tv.anoki.components.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Text
import tv.anoki.components.R
import tv.anoki.components.theme.DarkGrayColor
import tv.anoki.components.theme.LightWhiteColor
import tv.anoki.components.theme.WhiteColor

/**
 * The IconTextButton component uses the Button composable. It does not have a outline or solid fill by default.
 * This button component is to show text with icon.
 * The position of the icon is at start of the button by default.
 *
 * @param icon the icon that appears on the button
 * @param buttonText the string that appears on the button
 * @param onClick the lambda that the button calls when the user presses it
 * @param onFocused the lambda that the button calls when the user focus it
 * @param modifier Modifier to be applied to the Composable
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun IconTextButton(
    icon: Int,
    buttonText: String,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {

    Button(
        modifier = modifier.onFocusChanged {
            if (it.hasFocus) onFocused()
        },
        onClick = onClick,
        shape = ButtonDefaults.shape(shape = ShapeDefaults.ExtraLarge),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = LightWhiteColor,
            focusedContainerColor = WhiteColor,
            focusedContentColor = DarkGrayColor
        ),
        contentPadding = PaddingValues(
            vertical = dimensionResource(id = R.dimen.button_vertical_padding),
            horizontal = dimensionResource(id = R.dimen.button_horizontal_padding)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "button icon"
            )
            if(buttonText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.button_content_padding)))
                Text(text = buttonText)
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
fun Default_IconTextButtonPreview() {
    IconTextButton(
        icon = R.drawable.icon_play,
        buttonText = "Play Season 1 Episode 1",
        onClick = {},
        onFocused = {},
        modifier = Modifier
            .width(340.dp)
            .height(40.dp)
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun Focused_IconTextButtonPreview() {
    IconTextButton(
        icon = R.drawable.icon_play_focused,
        buttonText = "Play Season 1 Episode 1",
        onClick = {},
        onFocused = {},
        modifier = Modifier
            .width(340.dp)
            .height(40.dp)
    )
}
