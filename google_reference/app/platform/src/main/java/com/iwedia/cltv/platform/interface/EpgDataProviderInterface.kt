package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent

/**
 * Epg data provider interface
 *
 * @author Dejan Nadj
 */
interface EpgDataProviderInterface {

    fun loadIntermittentEvents()
    /**
     * @return epg events list
     */
    fun getEventList(): ArrayList<TvEvent>
    fun getEventChannelMap(): HashMap<String, ArrayList<TvEvent>>
    fun getEventListByChannelStartAndEndTime(tvChannel: TvChannel, startTime: Long, endTime: Long, callback: IAsyncDataCallback<ArrayList<TvEvent>>)
    /**
     * dispose provider
     */
    fun dispose()
}