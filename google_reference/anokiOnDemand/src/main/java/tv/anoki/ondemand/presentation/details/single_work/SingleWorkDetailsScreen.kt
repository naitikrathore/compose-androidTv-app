package tv.anoki.ondemand.presentation.details.single_work

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import tv.anoki.components.utils.setBackgroundColor
import tv.anoki.ondemand.R
import tv.anoki.ondemand.components.DetailsBackgroundImage
import tv.anoki.ondemand.components.PlayButton
import tv.anoki.ondemand.components.PlayFromBeginningButton
import tv.anoki.ondemand.components.ResumeButton
import tv.anoki.ondemand.components.VerticalSpacing20
import tv.anoki.ondemand.components.VodScreenError
import tv.anoki.ondemand.constants.UIConstants
import tv.anoki.ondemand.domain.model.VODType
import tv.anoki.ondemand.domain.model.single_work.SingleWork
import tv.anoki.ondemand.presentation.details.UiState
import tv.anoki.ondemand.presentation.listing.components.VodTitleInfo

@Composable
fun SingleWorkDetailsScreen(
    viewModel: SingleWorkDetailsViewModel,
    contentId: String,
    onBackPressed: () -> Unit,
    onNavigateToPlayer: (singleWork: SingleWork) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = dimensionResource(id = R.dimen.zero_dimen)
) {
    var resetFocusToRefresh by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val baseUiState = viewModel.baseUiState.collectAsStateWithLifecycle().value

    /**
     * The launch effect to make api call only once after screen is rendered
     */
    LaunchedEffect(Unit) { // used to request focus after moving back from the player screen (from another Activity)
        viewModel.getSingleWorkData(contentId)
    }

    /**
     * The launch effect to make api call after screen is resumed
     */
    LaunchedEffect(baseUiState.isResumeCalled) { // used to request focus after moving back from the player screen (from another Activity)
        if (baseUiState.isResumeCalled) {
            viewModel.getSingleWorkData(contentId)
        }
    }

    Box(
        modifier = modifier
            .onBackPressed(onBackPressed)
            .setBackgroundColor()
    ) {
        when (val dataState = uiState) {
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
                        viewModel.getSingleWorkData(contentId)
                    },
                    onDpadUp = {
                        resetFocusToRefresh = false
                        true
                    },
                    focusRetryButton = resetFocusToRefresh
                )
            }

            is UiState.Ready -> {
                SingleWorkDetailsContent(
                    data = dataState.data,
                    onNavigateToPlayer = onNavigateToPlayer,
                    topPadding = topPadding,
                    lastFocusedButton = baseUiState.lastFocusedButtonIndex,
                    isResumeCalled = baseUiState.isResumeCalled,
                    setLastFocusedButtonIndex = viewModel::setLastFocusedButtonIndex
                )
            }
        }
    }
}

@Composable
private fun SingleWorkDetailsContent(
    data: SingleWork,
    onNavigateToPlayer: (singleWork: SingleWork) -> Unit,
    lastFocusedButton: Int,
    setLastFocusedButtonIndex: (index: Int) -> Unit,
    isResumeCalled: Boolean,
    modifier: Modifier = Modifier,
    startPadding: Dp = dimensionResource(id = R.dimen.page_start_spacing),
    topPadding: Dp = dimensionResource(id = R.dimen.zero_dimen),
    playFromBeginningBtnFocusRequester: FocusRequester = remember { FocusRequester() },
    playBtnFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    /**
     * The function to set focus on last focused action button
     */
    val requestFocus: () -> Unit = {
        when (lastFocusedButton) {
            UIConstants.ACTION_BUTTON_PLAY_FROM_BEGINNING_INDEX -> {
                playFromBeginningBtnFocusRequester.requestFocus()
            }

            UIConstants.ACTION_BUTTON_PLAY_RESUME_INDEX -> {
                playBtnFocusRequester.requestFocus()
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
                .fillMaxHeight()
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
                timeframe = data.runtime,
                origRating = data.origRating
            )
            VerticalSpacing20()
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                BodyText(text = data.description)
                VerticalSpacing20()

                data.cast?.let {
                    BodyText(text = stringResource(id = R.string.text_cast_info, it))
                    VerticalSpacing20()
                }

                if (data.resumeFromSec > 0) {
                    ResumeButton(
                        modifier = Modifier
                            .focusRequester(playBtnFocusRequester)
                            .disableDpadLeft()
                            .disableDpadRight(),
                        onFocusChanged = setLastFocusedButtonIndex,
                        progress = data.progress,
                        onClick = {
                            onNavigateToPlayer(data)
                        }
                    )

                    VerticalSpacing20()

                    PlayFromBeginningButton(
                        modifier = Modifier
                            .focusRequester(playFromBeginningBtnFocusRequester)
                            .disableDpadLeft()
                            .disableDpadRight()
                            .disableDpadDown(),
                        onClick = {
                            onNavigateToPlayer(
                                data.copy(resumeFromSec = 0)
                            )
                            setLastFocusedButtonIndex(UIConstants.ACTION_BUTTON_PLAY_RESUME_INDEX)
                        },
                        onFocusChanged = setLastFocusedButtonIndex
                    )
                } else {
                    PlayButton(
                        modifier = Modifier
                            .focusRequester(playBtnFocusRequester)
                            .disableDpadLeft()
                            .disableDpadRight()
                            .disableDpadDown(),
                        onFocusChanged = setLastFocusedButtonIndex,
                        onClick = {
                            onNavigateToPlayer(data)
                        }
                    )
                }
                VerticalSpacing20()
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
fun SingleWorkDetailsContentPreview() {
    SingleWorkDetailsContent(
        data = SingleWork(
            durationSec = 2700,
            contentId = "82040",
            title = "American Barbarian",
            description = "Satire, dark comedy, and a rock n’ roll soundtrack give American Barbarian a tragicomic vibe completely relevant for the absurd times we live in.",
            thumbnail = "https://d15jbrb5d6zfm4.cloudfront.net/images/3p-1675-1711-1699354743-thumbnail.jpg",
            rating = "2",
            origRating = "",
            year = "2018",
            genre = "Drama",
            director = "Paul L. Carr",
            cast = "Zoe Pike,Karen O. Fort,Ayala Leyser,Arin Mulvaney,Zarinah Ali,Christian JJ Anderson,Isabelle M. Carr",
            runtime = "45m",
            language = "EN",
            vodPlaybackUrl = "https://playback-stage.aistrm.net:5206/vod_playback/82040/anoki.m3u8",
            trailerUrl = "",
            type = VODType.SINGLE_WORK,
            resumeFromSec = 0,
            progress = 0.4F
        ),
        onNavigateToPlayer = { _ -> },
        lastFocusedButton = 0,
        setLastFocusedButtonIndex = {},
        isResumeCalled = false
    )
}

