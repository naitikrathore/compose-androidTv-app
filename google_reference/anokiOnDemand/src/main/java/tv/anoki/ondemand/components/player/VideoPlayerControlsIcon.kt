package tv.anoki.ondemand.components.player

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import tv.anoki.components.utils.disableDpadLeft
import tv.anoki.components.utils.disableDpadRight
import tv.anoki.components.utils.disableDpadUp
import tv.anoki.ondemand.R

/**
 * A composable function that displays an icon with customized styling and interaction behavior.
 *
 * @param icon The drawable resource ID of the icon to be displayed.
 * @param modifier The modifier to be applied to the Surface.
 * @param contentDescription A description of the icon for accessibility purposes.
 * @param onClick The callback to be invoked when the icon is clicked.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayerControlsIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .padding(dimensionResource(id = R.dimen.vod_full_icon_focus_padding))
            .disableDpadLeft()
            .disableDpadRight()
            .disableDpadUp()
            .fillMaxSize(),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            disabledContainerColor = Color.Transparent,
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.vod_full_icon_padding)),
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = LocalContentColor.current
        )
    }
}
