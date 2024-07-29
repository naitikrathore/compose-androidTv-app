package tv.anoki.ondemand.presentation.details.series

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tv.anoki.components.loading.LoadingScreen
import tv.anoki.components.text.BodyText
import tv.anoki.components.utils.disableDpadDown
import tv.anoki.components.utils.disableDpadLeft
import tv.anoki.components.utils.disableDpadRight
import tv.anoki.components.utils.onBackPressed
import tv.anoki.ondemand.R
import tv.anoki.ondemand.components.DetailsBackgroundImage
import tv.anoki.ondemand.components.PlayButton
import tv.anoki.ondemand.components.PlayFromBeginningButton
import tv.anoki.ondemand.components.ResumeButton
import tv.anoki.ondemand.components.SeasonsAndEpisodesButton
import tv.anoki.ondemand.components.VerticalSpacing20
import tv.anoki.ondemand.components.VodScreenError
import tv.anoki.ondemand.constants.UIConstants
import tv.anoki.ondemand.domain.model.VODType
import tv.anoki.ondemand.domain.model.series.SeriesMetadata
import tv.anoki.ondemand.helper.buildNumSeasonsString
import tv.anoki.ondemand.presentation.details.UiState
import tv.anoki.ondemand.presentation.listing.components.VodTitleInfo

@Composable
fun SeriesDetailsScreen(
    viewModel: SeriesDetailsViewModel,
    contentId: String,
    onNavigateToSeasons: (item: SeriesMetadata) -> Unit,
    onBackPressed: () -> Unit,
    onNavigateToPlayer: (seriesMetadata: SeriesMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    var resetFocusToRefresh by remember { mutableStateOf(false) }
    val vodDataState by viewModel.seriesMetadataFlow.collectAsStateWithLifecycle()
    val baseUiState = viewModel.baseUiState.collectAsStateWithLifecycle().value

    /**
     * The launch effect to make api call only once after screen is rendered
     */
    LaunchedEffect(Unit) {
        /*Triggers the fetching of series metadata from the view model when the screen is composed.
        This is particularly important for features like "continue watching" to ensure that the latest
        data is fetched and displayed. */
        viewModel.getSeriesMetaData(contentId)
    }

    /**
     * The launch effect to make api call after screen is resumed
     */
    LaunchedEffect(baseUiState.isResumeCalled) { // used to request focus after moving back from the player screen (from another Activity)
        if (baseUiState.isResumeCalled) {
            viewModel.getSeriesMetaData(contentId)
        }
    }

    Box(modifier = modifier.onBackPressed(onBackPressed)) {
        when (val dataState = vodDataState) {
            is UiState.Loading -> {
                LoadingScreen(onBackPressed = onBackPressed)
            }

            is UiState.Error -> {
                /**
                 * The launch effect to set focus on reload button
                 */
                LaunchedEffect(Unit) {
                    resetFocusToRefresh = true
                }
                VodScreenError(
                    exception = dataState.exception,
                    onRetryCalled = {
                        resetFocusToRefresh = false
                        viewModel.getSeriesMetaData(contentId)
                    },
                    onDpadUp = {
                        resetFocusToRefresh = false
                        true
                    },
                    focusRetryButton = resetFocusToRefresh
                )
            }

            is UiState.Ready -> {
                SeriesDetailsContent(
                    data = dataState.data,
                    onNavigateToSeasons = onNavigateToSeasons,
                    onNavigateToPlayer = onNavigateToPlayer,
                    lastFocusedButton = baseUiState.lastFocusedButtonIndex,
                    isResumeCalled = baseUiState.isResumeCalled,
                    setLastFocusedButtonIndex = viewModel::setLastFocusedButtonIndex
                )
            }
        }
    }
}

@Composable
private fun SeriesDetailsContent(
    data: SeriesMetadata,
    onNavigateToSeasons: (item: SeriesMetadata) -> Unit,
    onNavigateToPlayer: (seriesMetadata: SeriesMetadata) -> Unit,
    lastFocusedButton: Int,
    setLastFocusedButtonIndex: (index: Int) -> Unit,
    isResumeCalled: Boolean,
    modifier: Modifier = Modifier,
    startPadding: Dp = dimensionResource(id = R.dimen.page_start_spacing),
    topPadding: Dp = dimensionResource(id = R.dimen.zero_dimen),
    playResumeBtnFocusRequester: FocusRequester = remember { FocusRequester() },
    seriesEpisodeBtnFocusRequester: FocusRequester = remember { FocusRequester() },
    playFromBeginningBtnFocusRequester: FocusRequester = remember { FocusRequester() }
) {

    /**
     * The function to set focus on last focused action button
     */
    val requestFocus: () -> Unit = {
        when (lastFocusedButton) {
            UIConstants.ACTION_BUTTON_PLAY_FROM_BEGINNING_INDEX -> {
                playFromBeginningBtnFocusRequester.requestFocus()
            }

            UIConstants.ACTION_BUTTON_PLAY_SEASON_EPISODES_INDEX -> {
                seriesEpisodeBtnFocusRequester.requestFocus()
            }

            UIConstants.ACTION_BUTTON_PLAY_RESUME_INDEX -> {
                playResumeBtnFocusRequester.requestFocus()
            }
        }
    }

    /**
     * The function to set focus on last focused action button after the screen is rendered
     */
    LaunchedEffect(Unit) {
        requestFocus()
    }

    /**
     * The function to set focus on last focused action button on isResumeCalled flag set to true
     */
    LaunchedEffect(isResumeCalled) {
        if (isResumeCalled) {
            requestFocus()
        }
    }

    Box {
        DetailsBackgroundImage(url = data.thumbnail)
        Column(
            modifier = modifier
                .width(dimensionResource(id = R.dimen.details_page_content_width))
                .padding(
                    start = startPadding,
                    top = topPadding
                )
        ) {
            VerticalSpacing20()
            VodTitleInfo(
                title = data.title,
                year = data.year,
                timeframe = buildNumSeasonsString(data.numSeasons),
                origRating = data.origRating
            )
            VerticalSpacing20()
            BodyText(text = data.description)
            VerticalSpacing20()

            data.cast?.let {
                BodyText(text = stringResource(id = R.string.text_cast_info, it))
                VerticalSpacing20()
            }

            if (data.resumeFromSec > 0) {
                ResumeButton(
                    modifier = Modifier
                        .disableDpadLeft()
                        .disableDpadRight()
                        .focusRequester(playResumeBtnFocusRequester),
                    onFocusChanged = setLastFocusedButtonIndex,
                    buttonText = stringResource(
                        id = R.string.text_resume_x_season_x_episode,
                        data.resumeFromSeason,
                        data.resumeFromEpisode
                    ),
                    progress = data.progress,
                    onClick = {
                        onNavigateToPlayer(data)
                    }
                )

                VerticalSpacing20()

                PlayFromBeginningButton(
                    modifier = Modifier
                        .disableDpadLeft()
                        .disableDpadRight()
                        .focusRequester(playFromBeginningBtnFocusRequester),
                    onClick = {
                        onNavigateToPlayer(data.copy(resumeFromSec = 0))
                        setLastFocusedButtonIndex(UIConstants.ACTION_BUTTON_PLAY_RESUME_INDEX)
                    },
                    onFocusChanged = setLastFocusedButtonIndex
                )
            } else {
                PlayButton(
                    modifier = Modifier
                        .disableDpadLeft()
                        .disableDpadRight()
                        .focusRequester(playResumeBtnFocusRequester),
                    onFocusChanged = setLastFocusedButtonIndex,
                    buttonText = stringResource(
                        id = R.string.text_play_x_season_x_episode,
                        data.resumeFromSeason,
                        data.resumeFromEpisode
                    ),
                    onClick = {
                        onNavigateToPlayer(data)
                    }
                )
            }

            VerticalSpacing20()

            SeasonsAndEpisodesButton(
                modifier = Modifier
                    .disableDpadDown()
                    .disableDpadLeft()
                    .disableDpadRight()
                    .focusRequester(seriesEpisodeBtnFocusRequester),
                onFocusChanged = setLastFocusedButtonIndex,
                onClick = {
                    onNavigateToSeasons(data)
                }
            )
        }
    }
}


@Preview(device = Devices.TV_1080p)
@Composable
fun SeriesDetailsContentPreview() {
    SeriesDetailsContent(
        data = SeriesMetadata(
            contentId = "82040",
            title = "American Barbarian",
            description = "Satire, dark comedy, and a rock n’ roll soundtrack give American Barbarian a tragicomic vibe completely relevant for the absurd times we live in.",
            thumbnail = "https://d15jbrb5d6zfm4.cloudfront.net/images/3p-1675-1711-1699354743-thumbnail.jpg",
            rating = "2",
            origRating = "",
            year = "2018",
            genre = "Drama",
            runtime = "45m",
            language = "EN",
            vodPlaybackUrl = "https://playback-stage.aistrm.net:5206/vod_playback/82040/anoki.m3u8",
            trailerUrl = "",
            type = VODType.SERIES,
            cast = "Zoe Pike,Karen O. Fort,Ayala Leyser,Arin Mulvaney,Zarinah Ali,Christian JJ Anderson,Isabelle M. Carr",
            numSeasons = 1,
            resumeFromEpisode = 1,
            resumeFromSeason = 2,
            episodePlaybackUrl = "",
            resumeFromSec = 0,
            progress = 0.4F,
            episodeDurationSec = 1012
        ),
        onNavigateToSeasons = {},
        onNavigateToPlayer = { _ -> },
        lastFocusedButton = 0,
        setLastFocusedButtonIndex = {},
        isResumeCalled = false
    )
}

