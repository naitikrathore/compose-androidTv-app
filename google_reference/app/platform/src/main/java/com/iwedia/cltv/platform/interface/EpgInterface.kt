package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent

interface EpgInterface  {

    fun setup()

    fun dispose()

    fun getCurrentEvent(
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<TvEvent>
    )

    fun updateEpgData(applicationMode: ApplicationMode = ApplicationMode.DEFAULT)

    fun getEvent(index: Int, callback: IAsyncDataCallback<TvEvent>)

    fun getEventById(id: Int, callback: IAsyncDataCallback<TvEvent>)

    fun getEventByNameAndStartTime(name: String, startTime: Long?, channelId: Int, callback: IAsyncDataCallback<TvEvent>)

    fun getEventList(callback: IAsyncDataCallback<ArrayList<TvEvent>>)

    fun getEventListByChannel(
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<ArrayList<TvEvent>>
    )

    fun getEventListByChannelAndTime(
        tvChannel: TvChannel,
        startTime: Long,
        endTime: Long,
        callback: IAsyncDataCallback<ArrayList<TvEvent>>
    )

    fun getAllCurrentEvent(callback: IAsyncDataCallback<ArrayList<TvEvent>>)

    fun getAllNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>)

    /**
     * this function gives the next event for a particular channel
     */
    fun getNextEventByChannel(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>)
    fun setActiveWindow(tvChannelList: MutableList<TvChannel>, startTime: Long)
    fun clearActiveWindow()
    fun getStartTimeForActiveWindow(): Long
}
