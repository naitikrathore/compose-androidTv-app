package com.iwedia.cltv.components
import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.scene.channel_list.ChannelListItem

/**
 * DiffUtilCallback used in RecyclerView.Adapter for item recycling during list refreshing
 *
 * @author Dejan Nadj
 */
class DiffUtilCallback (oldList: List<Any>, newList: List<Any>) :
    DiffUtil.Callback() {
    private val mOldList: List<Any>
    private val mNewList: List<Any>
    override fun getOldListSize(): Int {
        return mOldList.size
    }

    override fun getNewListSize(): Int {
        return mNewList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (mOldList.size < oldItemPosition && mNewList.size < newItemPosition) {
            if (mOldList[oldItemPosition] is ChannelListItem) {
                return (mOldList[oldItemPosition] as ChannelListItem).channel.channelId == (mNewList[newItemPosition] as ChannelListItem).channel.channelId
            } else if (mOldList[oldItemPosition] is TvEvent) {
                return (mOldList[oldItemPosition] as TvEvent).id == (mNewList[newItemPosition] as TvEvent).id
            } else if (mOldList[oldItemPosition] is RailItem) {
                return (mOldList[oldItemPosition] as RailItem).railName == (mNewList[newItemPosition] as RailItem).railName
            } else if (mOldList[oldItemPosition] is TvChannel) {
                return (mOldList[oldItemPosition] as TvChannel).channelId == (mNewList[newItemPosition] as TvChannel).channelId
            }
        }
        return false
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (mOldList.size < oldItemPosition && mNewList.size < newItemPosition) {
            if (mOldList[oldItemPosition] is ChannelListItem) {
                val oldItem: ChannelListItem = mOldList[oldItemPosition] as ChannelListItem
                val newItem: ChannelListItem = mNewList[newItemPosition] as ChannelListItem
                return oldItem.channel.channelId == newItem.channel.channelId
            } else if (mOldList[oldItemPosition] is TvEvent) {
                val oldItem: TvEvent = mOldList[oldItemPosition] as TvEvent
                val newItem: TvEvent = mNewList[newItemPosition] as TvEvent
                return oldItem.id == newItem.id
            } else if (mOldList[oldItemPosition] is RailItem) {
                val oldItem: RailItem = mOldList[oldItemPosition] as RailItem
                val newItem: RailItem = mNewList[newItemPosition] as RailItem
                return oldItem.railName == newItem.railName
            } else if (mOldList[oldItemPosition] is TvChannel) {
                val oldItem: TvChannel = mOldList[oldItemPosition] as TvChannel
                val newItem: TvChannel = mNewList[newItemPosition] as TvChannel
                return oldItem.channelId == newItem.channelId
            }
        }
        return false
    }

    @Nullable
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        // Implement method if you're going to use ItemAnimator
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }

    init {
        mOldList = oldList
        mNewList = newList
    }
}