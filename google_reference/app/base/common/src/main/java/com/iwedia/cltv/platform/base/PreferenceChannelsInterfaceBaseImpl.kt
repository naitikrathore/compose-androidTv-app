package com.iwedia.cltv.platform.base

import android.content.Context
import com.iwedia.cltv.platform.`interface`.PreferenceChannelsInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.TvChannel
import java.util.ArrayList

open class PreferenceChannelsInterfaceBaseImpl(context: Context, tvModule: TvInterface) : PreferenceChannelsInterface {

    override fun swapChannel(
        firstChannel: TvChannel,
        secondChannel: TvChannel,
        previousPosition: Int,
        newPosition: Int
    ): Boolean {
        return false
    }

    override fun moveChannel(
        moveChannelList: ArrayList<TvChannel>,
        previousIndex: Int,
        newIndex: Int,
        channelMap: HashMap<Int, String>
    ): Boolean {
        return false
    }

    override fun deleteAllChannels() {
    }
}