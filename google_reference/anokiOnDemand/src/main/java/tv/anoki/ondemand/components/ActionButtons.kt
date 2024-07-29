package tv.anoki.ondemand.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import tv.anoki.components.button.IconTextButton
import tv.anoki.components.button.IconTextProgressButton
import tv.anoki.ondemand.R
import tv.anoki.ondemand.constants.UIConstants.ACTION_BUTTON_PLAY_FROM_BEGINNING_INDEX
import tv.anoki.ondemand.constants.UIConstants.ACTION_BUTTON_PLAY_RESUME_INDEX
import tv.anoki.ondemand.constants.UIConstants.ACTION_BUTTON_PLAY_SEASON_EPISODES_INDEX

typealias ButtonIndex = Int

/**
 * This button is to play content
 *
 * @param modifier Modifier to be applied to the Composable
 * @param buttonText the string that appears on the button. Default text is 'Play'
 * @param onFocusChanged the lambda that the button calls when the user focus it
 * @param onClick the lambda that the button calls when the user presses it
 */
@Composable
fun PlayButton(
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(id = R.string.text_play),
    onFocusChanged: (ButtonIndex) -> Unit = {},
    onClick: () -> Unit = {}
) {
    IconTextButton(
        modifier = modifier
            .width(dimensionResource(id = R.dimen.details_page_button_width))
            .height(dimensionResource(id = R.dimen.details_page_button_height)),
        icon = R.drawable.icon_play_focused,
        buttonText = buttonText,
        onFocused = { onFocusChanged(ACTION_BUTTON_PLAY_RESUME_INDEX) },
        onClick = onClick
    )
}

/**
 * This button is to resume content from last watched history
 *
 * @param progress the float value to show the progress. Value is from 0.0 to 1.0
 * @param modifier Modifier to be applied to the Composable
 * @param buttonText the string that appears on the button. Default text is 'Resume Playing'
 * @param onFocusChanged the lambda that the button calls when the user focus it
 * @param onClick the lambda that the button calls when the user presses it
 */
@Composable
fun ResumeButton(
    progress: Float,
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(id = R.string.text_resume_playing),
    onFocusChanged: (ButtonIndex) -> Unit = {},
    onClick: () -> Unit = {}
) {
    IconTextProgressButton(
        modifier = modifier
            .width(dimensionResource(id = R.dimen.details_page_button_width))
            .height(dimensionResource(id = R.dimen.details_page_button_height)),
        icon = R.drawable.icon_play_focused,
        buttonText = buttonText,
        currentProgress = progress,
        onFocusChanged = { onFocusChanged(ACTION_BUTTON_PLAY_RESUME_INDEX) },
        onClick = onClick
    )
}

/**
 * This button is to play content from the beginning
 *
 * @param modifier Modifier to be applied to the Composable
 * @param onClick the lambda that the button calls when the user presses it
 * @param onFocusChanged the lambda that the button calls when the user focus it
 */
@Composable
fun PlayFromBeginningButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onFocusChanged: (ButtonIndex) -> Unit
) {
    IconTextButton(modifier = modifier
        .width(dimensionResource(id = R.dimen.details_page_button_width))
        .height(dimensionResource(id = R.dimen.details_page_button_height)),
        icon = R.drawable.icon_replay,
        buttonText = stringResource(id = R.string.text_play_from_beginning),
        onClick = onClick,
        onFocused = { onFocusChanged(ACTION_BUTTON_PLAY_FROM_BEGINNING_INDEX) })
}

/**
 * This button is to open seasons and episodes screen
 *
 * @param onFocusChanged the lambda that the button calls when the user focus it
 * @param modifier Modifier to be applied to the Composable
 * @param onClick the lambda that the button calls when the user presses it
 */
@Composable
fun SeasonsAndEpisodesButton(
    onFocusChanged: (ButtonIndex) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconTextButton(
        modifier = modifier
            .width(dimensionResource(id = R.dimen.details_page_button_width))
            .height(dimensionResource(id = R.dimen.details_page_button_height)),
        icon = R.drawable.icon_queue,
        buttonText = stringResource(id = R.string.text_seasons_and_episodes),
        onFocused = { onFocusChanged(ACTION_BUTTON_PLAY_SEASON_EPISODES_INDEX) },
        onClick = onClick
    )
}