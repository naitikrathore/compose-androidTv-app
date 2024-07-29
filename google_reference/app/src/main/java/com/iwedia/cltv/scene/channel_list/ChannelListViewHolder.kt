package com.iwedia.cltv.scene.channel_list

import android.animation.ValueAnimator
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.components.custom_card.CustomCardChannelList

/**
 * ViewHolder for the Channel List items in a RecyclerView.
 *
 * @param view The root view of the ViewHolder.
 * @property customCardChannelList Instance of [CustomCardChannelList] for displaying channel information.
 * @property normalHolderHeight Resource ID for the height of the ViewHolder in its normal state.
 * @property expandedHolderHeight Resource ID for the height of the ViewHolder in its expanded state.
 * @property valueAnimator Animation used for expanding and collapsing the ViewHolder.
 * @property channelListUpdateTimer Timer for controlling the delay before expanding the ViewHolder.
 *
 * @author Boris Tirkajla
 */
class ChannelListViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    val customCardChannelList: CustomCardChannelList = CustomCardChannelList(view.context)
    private val normalHolderHeight = R.dimen.custom_dim_59
    private val expandedHolderHeight = R.dimen.custom_dim_88_5
    private var valueAnimator: ValueAnimator? = null
    private var channelListUpdateTimer: CountDownTimer? = null

    init {
        (view as LinearLayout).apply {
            gravity =
                Gravity.CENTER_VERTICAL // important to center the child inside the ViewHolder in order to have visually better animation
            clipChildren = false
            clipToPadding = false
            addView(customCardChannelList)
        }
    }

    fun requestFocus() {
        customCardChannelList.requestFocus()
    }

    fun updateData(
        channelListItem: ChannelListItem,
        isTvEventLocked: Boolean,
        isParentalEnabled: Boolean,
        channelType: String?,
        isScrambled: Boolean
    ) {
        customCardChannelList.updateData(
            item = channelListItem,
            isTvEventLocked = isTvEventLocked,
            isParentalEnabled = isParentalEnabled,
            channelType = channelType,
            isScrambled = isScrambled
        )
    }
}