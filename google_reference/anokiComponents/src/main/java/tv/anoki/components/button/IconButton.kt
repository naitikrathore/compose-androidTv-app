package tv.anoki.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ShapeDefaults
import tv.anoki.components.R
import tv.anoki.components.theme.DarkGrayColor
import tv.anoki.components.theme.LightWhiteColor
import tv.anoki.components.theme.LighterGrayColor
import tv.anoki.components.theme.WhiteColor

/**
 * The icon button component uses the Button composable. Until pressed, it appears as a icon.
 * It does not have a outline but solid fill by default.
 *
 * @param icon the icon that appears on the button
 * @param onClick the lambda that the button calls when the user presses it
 * @param modifier Modifier to be applied to the Composable
 * @param isButtonFocused the flag to control the focus state of the button
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun IconButton(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isButtonFocused: Boolean = false
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = ButtonDefaults.shape(shape = ShapeDefaults.ExtraLarge),
        colors = ButtonDefaults.colors(
            containerColor = if (isButtonFocused) WhiteColor else DarkGrayColor,
            contentColor = if (isButtonFocused) DarkGrayColor else LightWhiteColor,
            focusedContainerColor = if (isButtonFocused) WhiteColor else Color.Transparent,
            focusedContentColor = if (isButtonFocused) DarkGrayColor else LightWhiteColor
        ),
        contentPadding = PaddingValues(all = 8.dp)
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = icon),
            contentDescription = "button icon"
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
fun Default_IconButtonPreview() {
    IconButton(
        icon = R.drawable.icon_replay,
        onClick = {},
        isButtonFocused = false,
        modifier = Modifier.size(80.dp)
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
fun Focused_IconButtonPreview() {
    IconButton(
        icon = R.drawable.icon_replay,
        onClick = {},
        isButtonFocused = true,
        modifier = Modifier.size(80.dp)
    )
}
