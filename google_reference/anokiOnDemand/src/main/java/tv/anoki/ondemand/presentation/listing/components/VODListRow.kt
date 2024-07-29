@file:OptIn(ExperimentalTvMaterial3Api::class)

package tv.anoki.ondemand.presentation.listing.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import tv.anoki.components.card.RoundedBorderCard
import tv.anoki.components.image.RenderScaleImage
import tv.anoki.components.progress.ItemProgress
import tv.anoki.components.text.BodyText
import tv.anoki.components.text.HeadingText
import tv.anoki.components.text.InfoText
import tv.anoki.components.text.TitleText
import tv.anoki.components.theme.DefaultTextColor
import tv.anoki.components.theme.FocusedTextColor
import tv.anoki.ondemand.R
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODItems
import tv.anoki.ondemand.domain.model.VODType
import tv.anoki.ondemand.presentation.listing.VodListViewModel

@Composable
fun VideoOnDemandsRow(
    item: VODItems,
    rowIndex: Int,
    listState: TvLazyListState,
    vodListViewModel: VodListViewModel,
    onPlayableClick: (playable: VODItem) -> Unit,
    onItemLaunched: (rIndex: Int, itemIndex: Int, focusRequester: FocusRequester) -> Unit,
    onItemFocused: (focusedRowIndex: Int, focusedItemIndex: Int) -> Unit,
    onUpdateLastFocusedItem: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    startPadding: Dp = dimensionResource(id = R.dimen.page_start_spacing),
    padding8: Dp = dimensionResource(id = R.dimen.dp_8),
    padding16: Dp = dimensionResource(id = R.dimen.dp_16)
) {
    Column(modifier = modifier) {
        VideoOnDemandsRowTitle(
            item,
            rowIndex,
            startPadding,
            vodListViewModel
        )
        TvLazyRow(
            modifier = Modifier.padding(top = padding16, bottom = padding16),
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(padding8),
            pivotOffsets = PivotOffsets(0.05f, 0f)
        ) {
            item { Spacer(modifier = Modifier.padding(start = startPadding.minus(padding16))) }

            itemsIndexed(
                items = item.items,
                key = { index, item -> "$index-${item._id}" }) { index, item ->
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    onItemLaunched(rowIndex, index, focusRequester)
                }
                VideoOnDemandsItemCard(
                    item = item,
                    rowIndex = rowIndex,
                    itemIndex = index,
                    onItemFocused = onItemFocused,
                    onUpdateLastFocusedItem = onUpdateLastFocusedItem,
                    onPlayableClick = onPlayableClick,
                    focusRequester = focusRequester,
                    vodListViewModel = vodListViewModel
                )
            }

            item { Spacer(modifier = Modifier.padding(start = padding16)) }
        }
    }
}

@Composable
fun VideoOnDemandsRowTitle(
    item: VODItems,
    rowIndex: Int,
    startPadding: Dp,
    vodListViewModel: VodListViewModel,
    modifier: Modifier = Modifier
) {
    val isRowFocused =
        vodListViewModel.isRowFocused(rowIndex).collectAsStateWithLifecycle().value
    Box(
        modifier = modifier
            .height(dimensionResource(id = R.dimen.vod_listing_row_title_box_height))
            .padding(start = startPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        val scale by animateFloatAsState(
            targetValue = if (isRowFocused) 1.2f else 1f,
            label = "TextScaleAnimation"
        )
        val textColor by animateColorAsState(
            targetValue = if (isRowFocused) FocusedTextColor else DefaultTextColor,
            label = "TextColorAnimation"
        )

        TitleText(
            text = item.name.lowercase().replaceFirstChar { it.uppercase() },
            color = textColor,
            modifier = Modifier.scale(scale),
            fontWeight = if (isRowFocused) FontWeight.Medium else FontWeight.Light
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoOnDemandsItemCard(
    item: VODItem,
    rowIndex: Int,
    itemIndex: Int,
    vodListViewModel: VodListViewModel,
    focusRequester: FocusRequester,
    onPlayableClick: (playable: VODItem) -> Unit,
    onItemFocused: (focusedRowIndex: Int, focusedItemIndex: Int) -> Unit,
    onUpdateLastFocusedItem: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSize: Size = Size(
        width = dimensionResource(id = R.dimen.vod_details_card_width).value,
        height = dimensionResource(id = R.dimen.vod_details_card_height).value
    )
) {
    val shouldFocusCard =
        vodListViewModel.shouldFocusCard(item._id, rowIndex, itemIndex).collectAsStateWithLifecycle().value

    LaunchedEffect(shouldFocusCard) {
        //Log.d("Level-C", "shouldFocusCard: $shouldFocusCard $rowIndex:$itemIndex")
        if (shouldFocusCard) {
            focusRequester.requestFocus()
        }
    }

    RoundedBorderCard(
        modifier = modifier
            .size(
                width = itemSize.width.dp,
                height = itemSize.height.dp
            )
            .onFocusChanged {
                if (it.hasFocus) {
                    onItemFocused(rowIndex, itemIndex)
                    onUpdateLastFocusedItem(item._id, itemIndex)
                    if (vodListViewModel.isContentWatched.value) {
                        vodListViewModel.setContentIsWatched(false)
                    }
                }
            }
            .focusRequester(focusRequester),
        shape = CardDefaults.shape(shape = ShapeDefaults.Medium),
        onClick = { onPlayableClick(item) }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            RenderScaleImage(
                imageUrl = item.thumbnail,
                contentScale = ContentScale.Crop
            )
            if (item.progress > 0) {
                ItemProgress(
                    modifier = Modifier
                        .padding(horizontal = 22.dp)
                        .height(4.dp),
                    progress = item.progress
                )
            }
            /**
             * Below box is to check performance of listing
             */
//            Box (modifier = Modifier.background(color = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))).size(80.dp))
        }
    }
}

@Composable
fun RecommendedItemDetails(
    vodListViewModel: VodListViewModel,
    defaultVODItem: VODItem,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val selectedBackgroundImage =
        vodListViewModel.selectedBackgroundImage.collectAsStateWithLifecycle().value
    val videoOnDemands: VODItem = selectedBackgroundImage.third ?: defaultVODItem
    val isTrailerPlaying = vodListViewModel.isTrailerPlaying.collectAsStateWithLifecycle().value
    val isVideoReadyToPlay = vodListViewModel.isVideoReadyToPlay.collectAsStateWithLifecycle().value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (!isVideoReadyToPlay || !isTrailerPlaying) {
            RenderScaleImage(
                modifier = Modifier.fillMaxSize(),
                imageUrl = videoOnDemands.thumbnail,
                imageDescription = "",
                alignment = Alignment.BottomEnd
            )
        }
        BackgroundShades(colorResource(R.color.background))
        VODDetails(videoOnDemands, height)
    }
}

@Composable
private fun VODDetails(
    vodItem: VODItem,
    height: Dp,
    startPadding: Dp = dimensionResource(id = R.dimen.page_start_spacing),
    topPadding: Dp = dimensionResource(id = R.dimen.page_top_spacing)
) {
    Box(
        modifier = Modifier
            .height(height)
            .padding(
                start = startPadding,
                top = topPadding
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.55f)
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.vod_details_logo_heading_between_spacer_height)))
            HeadingText(
                text = vodItem.title,
                fontSize = dimensionResource(id = R.dimen.vod_details_heading_text_font_size).value,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.vod_details_heading_row_between_spacer_height)))
            Row {
                InfoText(text = vodItem.year)
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.vod_details_info_text_info_text_between_spacer_height)))
                when (vodItem.type) {
                    VODType.SINGLE_WORK ->
                        InfoText(text = vodItem.runtime)

                    VODType.SERIES ->
                        InfoText(text = "Season ${vodItem.numSeasons}")
                }
                if (vodItem.origRating.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.vod_details_info_text_info_text_between_spacer_height)))
                    InfoText(text = vodItem.origRating)
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.vod_details_row_body_text_between_spacer_height)))
            BodyText(
                text = vodItem.description,
                maxLines = if (vodItem.cast.isNullOrEmpty()) 3 else 2
            )
            vodItem.cast?.let {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.dp_16)))
                BodyText(
                    text = stringResource(id = R.string.text_cast_info, it),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BackgroundShades(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Horizontal gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color, color, Color.Transparent)
                    )
                )
        )
    }
}
