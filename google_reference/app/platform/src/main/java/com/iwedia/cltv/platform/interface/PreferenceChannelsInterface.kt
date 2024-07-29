package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.TvChannel
import java.util.ArrayList

interface PreferenceChannelsInterface {

    /**
     * Performs swap among two channels
     */
    fun swapChannel(firstChannel: TvChannel, secondChannel: TvChannel, previousPosition: Int, newPosition: Int) : Boolean

    /**
     * Performs moving the channel to new position
     */
    fun moveChannel(moveChannelList: ArrayList<TvChannel>, previousIndex: Int, newIndex: Int, channelMap: HashMap<Int, String>) : Boolean

    /**
     * Performs deletion of installed channels
     */
    fun deleteAllChannels()
}