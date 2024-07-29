package tv.anoki.ondemand.presentation.seasons_and_episode_selection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import tv.anoki.components.card.ItemCardWithProgress
import tv.anoki.components.separator.DotSeparator
import tv.anoki.components.text.EpisodeBodyText
import tv.anoki.components.text.EpisodeInfoText
import tv.anoki.components.text.EpisodeTitleText
import tv.anoki.components.utils.disableDpadRight
import tv.anoki.components.utils.onBackPressed
import tv.anoki.components.utils.onDpadLeft
import tv.anoki.ondemand.R
import tv.anoki.ondemand.components.HorizontalSpacing20
import tv.anoki.ondemand.domain.model.series.Episode
import tv.anoki.ondemand.domain.model.series.EpisodeList

private const val TAG = "EpisodeList"

@Composable
fun EpisodeList(
    episodeList: EpisodeList,
    selectedEpisodeIndex: () -> Int,
    isFocusRequested: Boolean, // TODO BORIS: Optimisation: this parameter causes too many recompositions, think how to deal with it
    onEpisodeClicked: (Episode) -> Unit,
    onDpadLeft: () -> Unit,
    onFocused: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tvLazyListState: TvLazyListState = rememberTvLazyListState()
) {
    val focusRequesterHashMap = remember {
        HashMap<Int, FocusRequester>()
    }

    suspend fun selectAndScrollToEpisode() {
        tvLazyListState.scrollToItem(selectedEpisodeIndex())
        focusRequesterHashMap[selectedEpisodeIndex()]?.requestFocus()
    }

    LaunchedEffect(Unit) {
        if (selectedEpisodeIndex() >= 0) {
            selectAndScrollToEpisode()
        }
    }

    LaunchedEffect(isFocusRequested) {
        if (isFocusRequested) {
            selectAndScrollToEpisode()
        }
    }

    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 26.dp, bottom = 66.dp),
        pivotOffsets = PivotOffsets(
            parentFraction = 0.11f, childFraction = 0.15f
        ),
        state = tvLazyListState,
    ) {
        itemsIndexed(
            items = episodeList.items,
            key = { rowIndex, rowItem -> "$rowIndex-${rowItem._id}" }
        ) { index, episode ->
            focusRequesterHashMap[index] = remember { FocusRequester() }
            Row {
                ItemCardWithProgress(
                    modifier = modifier
                        .width(dimensionResource(id = R.dimen.vod_season_episode_image_width))
                        .height(dimensionResource(id = R.dimen.vod_season_episode_image_height))
                        .focusRequester(focusRequesterHashMap[index] ?: FocusRequester.Default)
                        .disableDpadRight()
                        .onDpadLeft(onDpadLeft)
                        .onBackPressed(onDpadLeft),
                    imagePath = episode.thumbnail,
                    progress = episode.progress,
                    onFocusChanged = { hasFocus ->
                        if (hasFocus) {
                            onFocused(index)
                        }
                    },
                    onClick = {
                        onEpisodeClicked(episode)
                    }
                )
                HorizontalSpacing20()
                Column(modifier = Modifier.width(dimensionResource(id = R.dimen.vod_season_episode_content_width))) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EpisodeInfoText(text = "S${episode.season} E${episode.episode}")
                        DotSeparator()
                        EpisodeInfoText(text = episode.runtime)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    EpisodeTitleText(text = episode.title)
                    Spacer(modifier = Modifier.height(4.dp))
                    EpisodeBodyText(text = episode.description, maxLines = 4)
                }
            }
        }
    }
}