package com.iwedia.cltv.components

import androidx.recyclerview.widget.DiffUtil
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.scene.channel_list.ChannelListItem

/**
 * DiffUtilItemCallback used in RecyclerView.Adapter for item recycling during list refreshing
 *
 * @author Dejan Nadj
 */
class DiffUtilItemCallback<T : Any>:
    DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        if (oldItem is ChannelListItem) {
            return oldItem.channel.channelId == (newItem as ChannelListItem).channel.channelId
        } else if (oldItem is TvEvent) {
            return oldItem.id == (newItem as TvEvent).id
        } else if (oldItem is RailItem) {
            return oldItem.id == (newItem as RailItem).id
        }
        return false
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        if (oldItem is ChannelListItem) {
            return oldItem.channel.channelId == (newItem as ChannelListItem).channel.channelId
        } else if (oldItem is TvEvent) {
            return oldItem.id == (newItem as TvEvent).id
        } else if (oldItem is RailItem) {
            return oldItem.id == (newItem as RailItem).id
        }
        return false
    }
}