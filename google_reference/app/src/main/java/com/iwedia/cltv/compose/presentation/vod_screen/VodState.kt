package com.iwedia.cltv.compose.presentation.vod_screen

import androidx.compose.runtime.Stable
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem

@Stable
data class VodState(
    val isFocusRequested: Boolean = false,
    val focusedItemIndex: Int = 0, // initially first index should be focused
    val toastMessage: String = "", // TODO BORIS - this is not good way to handle Toast, refer to the video...
    val items: List<RailItem> = emptyList()
)