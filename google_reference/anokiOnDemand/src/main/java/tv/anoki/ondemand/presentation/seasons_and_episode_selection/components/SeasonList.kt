package tv.anoki.ondemand.presentation.seasons_and_episode_selection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import kotlinx.coroutines.launch
import tv.anoki.components.text.InfoText
import tv.anoki.components.theme.ButtonBackgroundColor
import tv.anoki.components.theme.DefaultTextColor
import tv.anoki.components.theme.FocusedButtonBackgroundColor
import tv.anoki.components.theme.LightGrayColor
import tv.anoki.components.theme.SeasonButtonFocusedTextColor
import tv.anoki.components.theme.SelectedButtonBackgroundColor
import tv.anoki.components.utils.disableDpadLeft
import tv.anoki.components.utils.onBackPressed
import tv.anoki.components.utils.onDpadRight
import tv.anoki.ondemand.R
import tv.anoki.ondemand.domain.model.series.SeasonList

private const val TAG = "SeasonList"

@Composable
fun SeasonList(
    seasonList: SeasonList,
    selectedSeasonIndex: Int,
    isFocusRequested: Boolean,
    onMoveFocusToEpisodes: () -> Unit,
    onBackPressed: () -> Unit,
    onSeasonFocused: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val focusRequesterHashMap = remember { HashMap<Int, FocusRequester>() }

    LaunchedEffect(isFocusRequested) {
        scope.launch {
            if (isFocusRequested) focusRequesterHashMap[selectedSeasonIndex]?.requestFocus()
        }
    }

    TvLazyColumn(modifier = modifier) {
        itemsIndexed(
            items = seasonList.items,
            key = { rowIndex, rowItem -> "$rowIndex-${rowItem._id}" }
        ) { index, series ->
            focusRequesterHashMap[index] = remember { FocusRequester() }

            SeasonButton(
                modifier = Modifier
                    .padding(top = dimensionResource(id = R.dimen.dp_16))
                    .focusRequester(
                        focusRequesterHashMap[index] ?: FocusRequester.Default
                    )
                    .onDpadRight(onMoveFocusToEpisodes)
                    .onBackPressed(onBackPressed)
                    .disableDpadLeft(),
                seasonIndex = series.season,
                episodeIndex = series.episodes.count().toString(),
                isSelected = selectedSeasonIndex == index,
                onClick = onMoveFocusToEpisodes,
                onFocusChanged = { hasFocus ->
                    if (hasFocus) {
                        onSeasonFocused(index)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeasonButton(
    seasonIndex: String,
    episodeIndex: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
    onClick: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val textColor = remember {
        mutableStateOf(LightGrayColor)
    }

    Button(
        modifier = modifier
            .focusRequester(focusRequester)
            .size(
                width = dimensionResource(id = R.dimen.vod_season_button_width),
                height = dimensionResource(id = R.dimen.vod_season_button_height)
            )
            .onFocusChanged {
                if (it.hasFocus) {
                    if (!isSelected) { // this is case when user navigates from Episode list to the Buttons - avoid triggering refresh of the Button List
                        onFocusChanged(it.hasFocus)
                    }
                }
                textColor.value = if (it.hasFocus) {
                    SeasonButtonFocusedTextColor
                } else {
                    DefaultTextColor
                }
            },
        scale = ButtonDefaults.scale(
            focusedScale = 1f,
            pressedScale = 0.9f
        ),
        colors = ButtonDefaults.colors(
            containerColor =
            if (isSelected) {
                SelectedButtonBackgroundColor
            } else {
                ButtonBackgroundColor
            },
            focusedContainerColor = FocusedButtonBackgroundColor
        ),
        shape = ButtonDefaults.shape(
            RoundedCornerShape(
                dimensionResource(id = R.dimen.vod_season_button_height).div(
                    2
                )
            )
        ),
        onClick = {
            onClick()
        },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InfoText(
                text = "Season $seasonIndex", // TODO BORIS: think about moving this string to the map layer, do not hardcode it here
                color = textColor.value
            )
            InfoText(
                text = "$episodeIndex Episodes", // TODO BORIS: think about moving this string to the map layer, do not hardcode it here
                color = textColor.value
            )
        }
    }
}

@Preview
@Composable
fun SeasonButtonPreview() {
    SeasonButton(
        seasonIndex = "1",
        episodeIndex = "1",
        isSelected = true
    )
}