package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.PreferenceChannelsInterface
import com.iwedia.cltv.platform.model.TvChannel

class PreferenceChannelsInterfaceImpl(private val serviceImpl: IServiceAPI) :
    PreferenceChannelsInterface {
    override fun swapChannel(
        firstChannel: TvChannel,
        secondChannel: TvChannel,
        previousPosition: Int,
        newPosition: Int
    ): Boolean {
        return serviceImpl.swapChannel(
            toServiceChannel(firstChannel),
            toServiceChannel(secondChannel),
            previousPosition,
            newPosition
        )
    }

    override fun moveChannel(
        moveChannelList: ArrayList<TvChannel>,
        previousIndex: Int,
        newIndex: Int,
        channelMap: HashMap<Int, String>
    ): Boolean {
        var displayNumberList = arrayListOf<String>()
        channelMap.values.forEach { id->
            displayNumberList.add(id)
        }
        var channelList = arrayListOf<com.cltv.mal.model.entities.TvChannel>()
        moveChannelList.forEach { tvChannel ->
            channelList.add(toServiceChannel(tvChannel))
        }
        return serviceImpl.moveChannel(
            channelList.toTypedArray(),
            previousIndex,
            newIndex,
            displayNumberList.toTypedArray()
        )
    }

    override fun deleteAllChannels() {
        serviceImpl.deleteAllChannels()
    }

}