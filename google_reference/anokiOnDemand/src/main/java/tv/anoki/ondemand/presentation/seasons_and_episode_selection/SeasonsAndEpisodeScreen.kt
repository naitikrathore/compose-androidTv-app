package tv.anoki.ondemand.presentation.seasons_and_episode_selection

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import kotlinx.coroutines.launch
import tv.anoki.components.event_observer.EventObserver
import tv.anoki.components.loading.LoadingScreen
import tv.anoki.components.text.HeadingText
import tv.anoki.components.text.TitleText
import tv.anoki.components.utils.onBackPressed
import tv.anoki.components.utils.setBackgroundColor
import tv.anoki.ondemand.R
import tv.anoki.ondemand.components.VerticalSpacing20
import tv.anoki.ondemand.components.VodScreenError
import tv.anoki.ondemand.domain.model.series.Episode
import tv.anoki.ondemand.domain.model.series.EpisodeList
import tv.anoki.ondemand.domain.model.series.SeasonList
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.components.EpisodeList
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.components.SeasonList

private const val TAG = "SeasonsAndEpisodeScreen"

@Composable
fun SeasonsAndEpisodeScreen(
    viewModel: SeasonsAndEpisodeViewModel,
    contentId: String,
    title: String,
    onBackPressed: () -> Unit,
    onNavigateToPlayer: (episode: Episode) -> Unit,
    modifier: Modifier = Modifier,
    onDpadUp: () -> Unit = {},
    topPadding: Dp = dimensionResource(id = R.dimen.zero_dimen)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val baseUiState by viewModel.baseUiState.collectAsStateWithLifecycle()
    var resetFocusToRefresh by remember { mutableStateOf(false) }

    LaunchedEffect(baseUiState.isResumeCalled) {
        if (baseUiState.isResumeCalled) {
            viewModel.onEvent(SeasonsEpisodesScreenEvent.OnResume(contentId))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(SeasonsEpisodesScreenEvent.FetchSeries(contentId))
    }

    if (uiState.isLoading) {
        LoadingScreen(onBackPressed = onBackPressed)
    } else if (uiState.errorMessage.isNotEmpty()) {
        LaunchedEffect(Unit) {
            resetFocusToRefresh = true
        }
        VodScreenError(
            modifier = modifier.onBackPressed(onBackPressed),
            exception = Throwable(uiState.errorMessage),
            onRetryCalled = {
                resetFocusToRefresh = false
                viewModel.onEvent(SeasonsEpisodesScreenEvent.FetchSeries(contentId))
            },
            onDpadUp = {
                onDpadUp()
                resetFocusToRefresh = false
                true
            },
            focusRetryButton = resetFocusToRefresh
        )
    } else {
        SeasonsAndEpisodeContent(
            modifier = modifier,
            title = title,
            viewModel = viewModel,
            onBackPressed = onBackPressed,
            onNavigateToPlayer = onNavigateToPlayer,
            topPadding = topPadding,
        )
    }
}

@Composable
fun SeasonsAndEpisodeContent(
    viewModel: SeasonsAndEpisodeViewModel,
    title: String,
    onBackPressed: () -> Unit,
    onNavigateToPlayer: (episode: Episode) -> Unit,
    modifier: Modifier = Modifier,
    startPadding: Dp = dimensionResource(id = R.dimen.page_start_spacing),
    topPadding: Dp = dimensionResource(id = R.dimen.zero_dimen),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val episodesListUiState by viewModel.episodesListUiState.collectAsStateWithLifecycle()
    val seasonsListUiState by viewModel.seasonsListUiState.collectAsStateWithLifecycle()

    val selectedEpisodeIndex = remember {
        {
            episodesListUiState.selectedPosition
        }
    }

    val selectedSeasonIndex by remember {
        derivedStateOf {
            seasonsListUiState.selectedPosition
        }
    }

    val isFocusOnSeasonsRequested = remember {
        derivedStateOf {
            seasonsListUiState.isFocusRequested
        }
    }

    val isFocusOnEpisodesRequested = remember {
        derivedStateOf {
            episodesListUiState.isFocusRequested
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val tvLazyListState = rememberTvLazyListState()

    EventObserver(flow = viewModel.events) {event ->
        when (event) {
            Event.SeasonSelected -> {
                coroutineScope.launch {
                    tvLazyListState.animateScrollToItem(viewModel.getFirstEpisodeIndexOfSelectedSeason())
                }
            }
        }
    }

    LaunchedEffect(uiState.episodesList) {
        if(uiState.episodesList.isNotEmpty()) {
            viewModel.calculateLastWatchedEpisodeIndex()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .setBackgroundColor()
            .padding(
                start = startPadding,
                top = topPadding
            )
    ) {
        Column {
            VerticalSpacing20()
            HeadingText(
                text = title,
                fontSize = integerResource(id = R.integer.details_page_heading_font_size).toFloat(),
                modifier = Modifier.width(391.dp)
            )
            VerticalSpacing20()
            SeasonList(
                selectedSeasonIndex = selectedSeasonIndex,
                isFocusRequested = isFocusOnSeasonsRequested.value,
                seasonList = SeasonList(items = uiState.seasonList),
                onSeasonFocused = viewModel::onSeasonSelected,
                onBackPressed = {
                    onBackPressed()
                    viewModel.onBackFromSeason()
                },
                onMoveFocusToEpisodes = viewModel::requestFocusOnEpisodeList
            )
        }

        Spacer(
            modifier = Modifier
                .width(9.dp)
                .fillMaxHeight()
        )

        Column {
            TitleText(
                modifier = Modifier.padding(top = 30.dp),
                text = "Seasons ${uiState.title} Episodes",
                fontSize = 20F
            )
            EpisodeList(
                episodeList = EpisodeList(items = uiState.episodesList),
                onFocused = viewModel::onEpisodeSelected,
                selectedEpisodeIndex = selectedEpisodeIndex,
                onEpisodeClicked = {
                    onNavigateToPlayer(it)
                    viewModel.onEpisodePressed()
                },
                onDpadLeft = viewModel::requestFocusOnSeasonsList,
                isFocusRequested = isFocusOnEpisodesRequested.value,
                tvLazyListState = tvLazyListState
            )
        }
    }
}

@Preview
@Composable
fun SeasonsAndEpisodeScreenPreview() {
    SeasonsAndEpisodeContent(
        title = "",
        onBackPressed = {},
        onNavigateToPlayer = {},
        viewModel = hiltViewModel()
    )
}