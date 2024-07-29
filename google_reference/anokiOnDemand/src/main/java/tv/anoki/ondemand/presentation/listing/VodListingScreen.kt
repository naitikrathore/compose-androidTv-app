package tv.anoki.ondemand.presentation.listing

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import tv.anoki.components.constants.LogConstants
import tv.anoki.components.loading.LoadingScreen
import tv.anoki.components.shimmer.shimmer
import tv.anoki.components.utils.onBackPressed
import tv.anoki.framework.ui.MultipleEventsCutterImpl
import tv.anoki.ondemand.R
import tv.anoki.ondemand.components.VodScreenError
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODType
import tv.anoki.ondemand.domain.model.VideoOnDemandsItemsList
import tv.anoki.ondemand.presentation.listing.components.RecommendedItemDetails
import tv.anoki.ondemand.presentation.listing.components.VideoOnDemandsRow

@Composable
fun VodListingScreen(
    vodListViewModel: VodListViewModel,
    onNavigateToDetails: (type: VODType, contentId: String) -> Unit,
    changeTrailer: (vodItem: VODItem?) -> Unit,
    moveFocusToNavigation: () -> Unit,
    onInternetDisconnected: () -> Unit,
    onInternetConnected: () -> Unit,
    modifier: Modifier = Modifier,
    vodListDataViewModel: VodListDataViewModel = hiltViewModel()

) {
    val isInternetAvailable by vodListViewModel.isInternetAvailable.collectAsStateWithLifecycle()
    LaunchedEffect(isInternetAvailable) {
        if (isInternetAvailable) {
            vodListDataViewModel.refreshListingData()
        }
    }
    val shouldReloadData = vodListViewModel.shouldReloadListData().collectAsStateWithLifecycle().value
    LaunchedEffect(shouldReloadData) {
        if(shouldReloadData) {
            vodListDataViewModel.refreshListingData()
        }
    }
    val videoOnDemandsDataState by vodListDataViewModel.vodItemsFlow.collectAsStateWithLifecycle(
        VodListDataUiState.Loading()
    )

    Box(modifier = modifier.onBackPressed { moveFocusToNavigation() }) {
        if (!isInternetAvailable) {
            onInternetDisconnected()
            moveFocusToNavigation()
            vodListViewModel.setFocusedRowIndex(-1)
        }
        else {
            onInternetConnected()
            when (val dataState = videoOnDemandsDataState) {
                is VodListDataUiState.Loading -> {
                    vodListViewModel.setDataLoaded(false)
                    LoadingScreen()
                }

                is VodListDataUiState.Error -> {
                    val moveFocusDown =
                        vodListViewModel.isFocusRequested.collectAsStateWithLifecycle().value

                    LaunchedEffect(Unit) {
                        if (vodListViewModel.isDataLoaded.value.not()) {
                            vodListViewModel.setDataLoaded(true)
                        }
                    }

                    VodScreenError(
                        exception = dataState.exception,
                        onRetryCalled = {
                            vodListDataViewModel.refreshListingData()
                            moveFocusToNavigation()
                        },
                        onDpadUp = {
                            moveFocusToNavigation()
                            true
                        },
                        focusRetryButton = moveFocusDown
                    )
                }

                is VodListDataUiState.Ready -> {
                    LaunchedEffect(Unit) {
                        if (vodListViewModel.isDataLoaded.value.not()) {
                            vodListViewModel.setDataLoaded(true)
                        }
                    }

                    OnDemandContent(
                        vodListViewModel = vodListViewModel,
                        onPlayableClick = {
                            onNavigateToDetails(it.type, it.contentId)
                        },
                        moveFocusToNavigation = moveFocusToNavigation,
                        videoOnDemandsList = VideoOnDemandsItemsList(items = dataState.data.toMutableList()),
                        changeTrailer = changeTrailer
                    )
                }
            }
        }
    }
}

@Composable
fun OnDemandContent(
    changeTrailer: (vodItem: VODItem?) -> Unit,
    onPlayableClick: (playable: VODItem) -> Unit,
    videoOnDemandsList: VideoOnDemandsItemsList,
    moveFocusToNavigation: () -> Unit,
    modifier: Modifier = Modifier,
    vodListViewModel: VodListViewModel = hiltViewModel(),
    multipleEventsCutter: MultipleEventsCutterImpl = remember { MultipleEventsCutterImpl() }
) {
    val immersiveListHeight = LocalConfiguration.current.screenHeightDp.times(0.50f).dp
    val tvLazyListState: TvLazyListState = rememberTvLazyListState()

    /**
     * The map to store all focusRequester of items so that we can programmatically handle focus switch between rows.
     * We are handling this way to not loose focus while scrolling fast
     */
    val focusRequesterMap = remember { mutableMapOf<Int, MutableMap<Int, FocusRequester>>() }
    val focusRowItemIndexMap = remember { mutableMapOf<Int, Int>() }
    var focusedRowIndex by remember { mutableIntStateOf(-1) }

    /**
     * first parameter is current focused row index
     * second parameter is next focus row index
     */
    var nextRowIndexPair: Pair<Int, Int>? by remember { mutableStateOf(Pair(-1, 0)) }

    /**
     * The function gets triggered when user presses RCU DPAD_DOWN on OnDemand tab.
     * This function set focus on last focused item or first item of the first row.
     */
    LaunchedEffect(Unit) {
        vodListViewModel.isFocusRequested.collect {
            if (vodListViewModel.isFocusRequested.value) {
                vodListViewModel.setListFocused(true)
                val lastFocusedRowIndex =
                    if (focusedRowIndex < 0) tvLazyListState.firstVisibleItemIndex else focusedRowIndex
                val nextItemIndex = focusRowItemIndexMap[lastFocusedRowIndex] ?: 0
                try {
                    val focusRequester = focusRequesterMap[lastFocusedRowIndex]?.get(nextItemIndex)
                    focusRequester?.requestFocus()
                } catch (e: Exception) {
                    //Log.d("Level-A", "Crash: $lastFocusedRowIndex == $nextItemIndex")
                    val focusRequester = focusRequesterMap[tvLazyListState.firstVisibleItemIndex]?.get(nextItemIndex)
                    focusRequester?.requestFocus()
                }
            } else {
                if (vodListViewModel.isTrailerPlaying.value.not()) {
                    changeTrailer.invoke(null)
                }
            }
        }
    }

    Column(modifier = modifier) {
        RecommendedItemDetails(
            vodListViewModel = vodListViewModel,
            defaultVODItem = videoOnDemandsList.items[0].items[0],
            height = immersiveListHeight
        )

        TvLazyColumn(
            modifier = Modifier
                .background(color = colorResource(R.color.background))
                .height(immersiveListHeight)
                .fillMaxSize()
                .testTag("OnDemand"),
            state = tvLazyListState,
            pivotOffsets = PivotOffsets(0.2f, 0.0f)
        ) {
            itemsIndexed(
                items = videoOnDemandsList.items,
                key = { rowIndex, rowItem -> "$rowIndex-${rowItem._id}" }) { rowIndex, rowItem ->
                val rowListState = rememberTvLazyListState()
                VideoOnDemandsRow(
                    listState = rowListState,
                    modifier = Modifier
                        .onPreviewKeyEvent { keyEvent ->
                            if (rowListState.isScrollInProgress && (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP || keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)) {
                                return@onPreviewKeyEvent true
                            } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                                && focusRowItemIndexMap[focusedRowIndex] == videoOnDemandsList.items[rowIndex].items.size.minus(1)) {
                                return@onPreviewKeyEvent true
                            } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                multipleEventsCutter.processEvent {
                                    nextRowIndexPair = Pair(focusedRowIndex, focusedRowIndex.plus(1))

                                    val currentCIndex = nextRowIndexPair!!.first
                                    val nextCIndex = nextRowIndexPair!!.second
                                    //Log.d("Level-A", "Composed: $focusedRowIndex == $nextRowIndexPair")
                                    try {
                                        val nextItemIndex = focusRowItemIndexMap[nextCIndex] ?: 0
                                        val focusRequester = focusRequesterMap[nextCIndex]?.get(nextItemIndex)
                                        focusRequester?.requestFocus()
                                    } catch (e: Exception) {
                                        //Log.d("Level-A", "Crash: $focusedRowIndex == $nextRowIndexPair")
                                        e.printStackTrace()
                                        val currentItemIndex = focusRowItemIndexMap[currentCIndex] ?: 0
                                        val focusRequester = focusRequesterMap[currentCIndex]?.get(currentItemIndex)
                                        focusRequester?.requestFocus()
                                    }
                                }
                                return@onPreviewKeyEvent true
                            } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                var consumeFocusEvent = false
                                if (focusedRowIndex == 0) {
                                    nextRowIndexPair = null
                                    focusedRowIndex = -1
                                    vodListViewModel.setFocusedRowIndex(-1)
                                    moveFocusToNavigation()
                                } else if (focusedRowIndex > 0) {
                                    consumeFocusEvent = true
                                    multipleEventsCutter.processEvent {
                                        nextRowIndexPair = Pair(focusedRowIndex, focusedRowIndex.minus(1))

                                        val currentCIndex = nextRowIndexPair!!.first
                                        val nextCIndex = nextRowIndexPair!!.second
                                        //Log.d("Level-A", "Composed: $focusedRowIndex == $nextRowIndexPair")
                                        try {
                                            val nextItemIndex = focusRowItemIndexMap[nextCIndex] ?: 0
                                            val focusRequester = focusRequesterMap[nextCIndex]?.get(nextItemIndex)
                                            focusRequester?.requestFocus()
                                        } catch (e: Exception) {
                                            //Log.d("Level-A", "Crash: $focusedRowIndex == $nextRowIndexPair")
                                            e.printStackTrace()
                                            val currentItemIndex = focusRowItemIndexMap[currentCIndex] ?: 0
                                            val focusRequester = focusRequesterMap[currentCIndex]?.get(currentItemIndex)
                                            focusRequester?.requestFocus()
                                        }

                                    }
                                }
                                return@onPreviewKeyEvent consumeFocusEvent
                            }

                            return@onPreviewKeyEvent false
                        },
                    item = rowItem,
                    rowIndex = rowIndex,
                    onItemLaunched = { rIndex, itemIndex, focusRequester ->
                        var map: MutableMap<Int, FocusRequester>? =
                            focusRequesterMap[rIndex]
                        if (map.isNullOrEmpty()) {
                            map = mutableMapOf()
                        }

                        map[itemIndex] = focusRequester
                        focusRequesterMap[rIndex] = map
                    },
                    onItemFocused = { rIndex, rItemIndex ->
                        focusedRowIndex = rIndex
                        focusRowItemIndexMap[rIndex] = rItemIndex

                        vodListViewModel.setFocusedRowIndex(rIndex)
                        vodListViewModel.setSelectedBackgroundImage(
                            videoOnDemands = rowItem.items[rItemIndex],
                            url = rowItem.items[rItemIndex].thumbnail, isFullscreen = true
                        )
                        changeTrailer.invoke(rowItem.items[rItemIndex])
                    },
                    onUpdateLastFocusedItem = { name, index ->
                        vodListViewModel.setLastFocusedItem(name, index)
                    },
                    onPlayableClick = onPlayableClick,
                    vodListViewModel = vodListViewModel
                )
            }

            item(key = "OnDemandBottom") {
                Spacer(
                    modifier = Modifier.padding(
                        bottom = LocalConfiguration.current.screenHeightDp.dp.times(0.25f)
                    )
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun Loading() {
    Column {

        Box(Modifier.weight(5.5f)) {
            Row(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .shimmer()
            ) {
                colorResource(R.color.surfaceVariant)
                Modifier
                    .weight(1f)
            }

        }

        Spacer(
            Modifier
                .weight(0.4f)
                .fillMaxWidth()
        )

        Row(
            Modifier
                .weight(0.35f)
                .padding(
                    start = dimensionResource(id = R.dimen.vod_listing_loading_row_start_padding),
                )
                .shimmer()
        ) {
            colorResource(R.color.surfaceVariant)
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.15f)
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.vod_listing_loading_rounded_corner)))
        }

        Spacer(
            Modifier
                .weight(0.3f)
                .fillMaxWidth()
        )

        Row(
            Modifier
                .weight(2.4f)
                .padding(
                    start = dimensionResource(id = R.dimen.vod_listing_loading_row_fourth_start_padding)
                )
                .shimmer()
        ) {
            colorResource(R.color.surfaceVariant)
            Modifier
                .weight(1.1f)
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.vod_listing_loading_rounded_corner)))
            Spacer(
                Modifier
                    .weight(0.15f)
                    .fillMaxWidth()
            )
            colorResource(R.color.surfaceVariant)
            Modifier
                .weight(1.1f)
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.vod_listing_loading_rounded_corner)))
            Spacer(
                Modifier
                    .weight(0.15f)
                    .fillMaxWidth()
            )
            colorResource(R.color.surfaceVariant)
            Modifier
                .weight(1.15f)
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.vod_listing_loading_rounded_corner)))
            Spacer(
                Modifier
                    .weight(0.1f)
                    .fillMaxWidth()
            )
            colorResource(R.color.surfaceVariant)
            Modifier
                .weight(1.17f)
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.vod_listing_loading_rounded_corner)))
            Spacer(
                Modifier
                    .weight(0.08f)
                    .fillMaxWidth()
            )
        }

        Spacer(
            Modifier
                .weight(0.55f)
                .fillMaxWidth()
        )

        Row(
            Modifier
                .weight(0.35f)
                .padding(
                    start = dimensionResource(id = R.dimen.vod_listing_loading_row_start_padding),
                )
                .shimmer()
        ) {
            colorResource(R.color.surfaceVariant)
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.15f)
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.vod_listing_loading_rounded_corner)))
        }

        Spacer(
            Modifier
                .weight(0.15f)
                .fillMaxWidth()
        )
    }
}