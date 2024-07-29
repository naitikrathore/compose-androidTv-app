package com.iwedia.cltv.scene.channel_list

import android.annotation.SuppressLint
import android.os.Build
import android.os.CountDownTimer
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.components.custom_card.ClickListener
import com.iwedia.cltv.components.custom_card.CustomCardChannelList
import com.iwedia.cltv.components.DiffUtilCallback
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.components.custom_card.FocusListener
import com.iwedia.cltv.components.custom_card.LongClickListener
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import java.util.*


/**
 * ChannelListAdapter
 * @author Boris Tirkajla
 */
class ChannelListAdapter : RecyclerView.Adapter<ChannelListViewHolder>() {

    //Items
    private var items = mutableListOf<ChannelListItem>()
    private var lastSelectedViewHolder: ChannelListViewHolder? = null

    //Channel list adapter listener
    var channelListAdapterListener: ChannelListAdapterListener? = null

    //Holders hash map
    private var holders: HashMap<Int, ChannelListViewHolder> = HashMap()

    //DiffUtil DiffResult instance
    private var diffResult: DiffUtil.DiffResult? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelListViewHolder {
        val linearLayout = LinearLayout(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        return ChannelListViewHolder(linearLayout)
    }

    override fun onViewRecycled(holder: ChannelListViewHolder) {
        holder.customCardChannelList.updateUnfocusedState()
        super.onViewRecycled(holder)
    }

    override fun onViewAttachedToWindow(holder: ChannelListViewHolder) {
        holder.customCardChannelList.updateUnfocusedState()
        super.onViewAttachedToWindow(holder)
    }

    override fun onBindViewHolder(holder: ChannelListViewHolder, position: Int) {
        val channelListItem = items[position]
        if (holders.contains(position)) {
            holders.remove(position)
        }
        holders[position] = holder

        holder.updateData(
            channelListItem = channelListItem,
            isTvEventLocked = channelListAdapterListener!!.isEventLocked(channelListItem.event),
            isParentalEnabled = channelListAdapterListener!!.isParentalEnabled(),
            channelType = channelListAdapterListener!!.getChannelSourceType(channelListItem.channel),
            isScrambled = channelListAdapterListener!!. isScrambled()
        )
        holder.customCardChannelList.apply {
            setFocusListener(object : FocusListener {
                override fun onFocusChanged(hasFocus: Boolean) {
                    if (hasFocus) {
                        lastSelectedViewHolder = holder
                        triggerItemSelected(position)
                        channelListAdapterListener!!.setSpeechText(
                            holder.customCardChannelList.getText()
                        )
                    }
                }
            })

            setClickListener(object : ClickListener {
                override fun onClick() {
                    channelListAdapterListener?.onItemClick(position)
                }
            })

            setLongClickListener(object : LongClickListener {
                override fun onLongClick() {
                    channelListAdapterListener!!.onItemLongClick(position)
                }
            })

            setKeyListener(object : CustomCardChannelList.KeyListener {
                override fun onFirstDpadRightPressed() {
                    channelListAdapterListener!!.showChannelInformation(position)
                }

                override fun onSecondDpadRightPressed() {
                    channelListAdapterListener?.onKeyRight()
                }

                override fun onDpadLeftPressed() {
                    channelListAdapterListener!!.hideChannelInformation()
                }

                override fun onDpadDownPressed() {
                    if (holder.absoluteAdapterPosition != itemCount - 1) { // last item
                        channelListAdapterListener!!.hideChannelInformation()
                    }
                }

                override fun onDpadUpPressed() {
                    channelListAdapterListener!!.onKeyUp(position)
                    channelListAdapterListener!!.hideChannelInformation()
                }

                override fun onDpadChannelUpPressed() {
                    channelListAdapterListener?.onChannelUpPressed()
                    channelListAdapterListener!!.hideChannelInformation()
                }

                override fun onDpadChannelDownPressed() {
                    channelListAdapterListener?.onChannelDownPressed()
                    channelListAdapterListener!!.hideChannelInformation()
                }

                override fun onCaptionsPressed() {
                    if (channelListItem.isCurrentChannel) {
                        channelListAdapterListener?.onCCPressed()
                    }
                }

                override fun onBackPressed() {
                    channelListAdapterListener?.onBackPressed()
                }
            })
        }
    }

    /**
     * Iem selected count down timer
     * Triggers getAdapterPosition listener with 500ms delay
     * Added in order to avoid multiple selected item details info refreshing during fast scrolling
     */
    private var itemSelectedTimer: CountDownTimer? = null
    private fun triggerItemSelected(position: Int) {
        itemSelectedTimer?.cancel()
        itemSelectedTimer = object :
            CountDownTimer(
                500,
                500
            ) {
            override fun onTick(millisUntilFinished: Long) {}

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                channelListAdapterListener?.onItemSelected(position)
            }
        }
        itemSelectedTimer!!.start()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateLockSkipIcons(position: Int, holder: ChannelListViewHolder) {
        if (position < items.size) {
            val item = items[position]
            holder.customCardChannelList.updateData(
                item = item,
                isTvEventLocked = channelListAdapterListener!!.isEventLocked(item.event),
                isParentalEnabled = channelListAdapterListener!!.isParentalEnabled(),
                channelType = channelListAdapterListener?.getChannelSourceType(item.channel) ?: "",
                isScrambled = channelListAdapterListener!!.isScrambled()
            )
        }
    }

    //Refresh
    fun refresh(adapterItems: MutableList<ChannelListItem>) {
        val diffCallback = DiffUtilCallback(this.items, adapterItems)
        diffResult = DiffUtil.calculateDiff(diffCallback)
        this.items.clear()
        this.items.addAll(adapterItems)
        diffResult?.dispatchUpdatesTo(this)
    }

    fun update(data: ChannelListItem) {
        items.forEachIndexed { index, it ->
            if (it.channel == data.channel) {
                it.event = data.event
                notifyItemChanged(index)
            }
        }
    }

    fun removeElement(position: Int) {
        if (items[position] != null) {
            items.removeAt(position)
            notifyDataSetChanged()
        }
    }

    fun refreshCurrentChannel(position: Int) {
        if (position >= 0 && position < items.size) {
            items[position].isCurrentChannel = true
            notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearList() {
        itemSelectedTimer?.cancel()
        if (items.isNotEmpty()) items.clear()
        if (holders.isNotEmpty()) holders.clear()
        notifyDataSetChanged()
    }

    fun dispose() {
        channelListAdapterListener = null

        itemSelectedTimer?.cancel()
        itemSelectedTimer = null

        if (items.isNotEmpty()) items.clear()
        if (holders.isNotEmpty()) holders.clear()
    }

    //Channel list adapter listener
    interface ChannelListAdapterListener: TTSSetterInterface {
        fun getChannelSourceType(tvChannel: TvChannel): String
        fun onItemClick(position: Int)
        fun onKeyUp(currentPosition: Int): Boolean
        fun onKeyRight(): Boolean
        fun onItemLongClick(position: Int)
        fun onItemSelected(position: Int)
        fun onCCPressed()
        fun onChannelUpPressed()
        fun onChannelDownPressed()
        fun isEventLocked(tvEvent: TvEvent?): Boolean
        fun isParentalEnabled(): Boolean
        fun showChannelInformation(position: Int)
        fun hideChannelInformation()
        fun onBackPressed()
        fun isScrambled(): Boolean
    }
}