package com.iwedia.cltv.sdk.handlers

import android.util.Log
import com.iwedia.cltv.sdk.TifDataProvider
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.entities.ReferenceTvEvent
import core_entities.Error
import data_type.GList
import data_type.GLong
import handlers.DataProvider
import handlers.EpgHandler
import listeners.AsyncDataReceiver

/**
 * ReferenceEpgHandler
 *
 * @author Aleksandar Milojevic
 */
class ReferenceEpgHandler : EpgHandler<ReferenceTvChannel, ReferenceTvEvent> {

    private val TAG = javaClass.simpleName

    constructor(dataProvider: DataProvider<*>) : super(dataProvider)

    fun getAllCurrentEvent(callback: AsyncDataReceiver<MutableList<ReferenceTvEvent>>) {
        var currentTime = GLong(System.currentTimeMillis().toString())
        (dataProvider as TifDataProvider).loadCurrentEpgData(object : AsyncDataReceiver<HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>> {
            override fun onReceive(data: HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>) {
                var retList = mutableListOf<ReferenceTvEvent>()
                for ((key, value) in data) {
                    value.value.forEach { event ->
                        if (event.startDate.value.toLong() <= currentTime.value.toLong() && event.endDate.value.toLong() >= currentTime.value.toLong()) {
                            retList.add(event)
                        }
                    }
                }
                if (retList.size > 0) {
                    callback.onReceive(retList)
                } else {
                    callback.onFailed(Error(404, "Events not found."))
                }
            }
            override fun onFailed(error: Error?) {
                callback.onFailed(error)
            }
        })
    }


    fun getAllNextEvents(callback: AsyncDataReceiver<MutableList<ReferenceTvEvent>>) {
        var currentTime = GLong(System.currentTimeMillis().toString())
        (dataProvider as TifDataProvider).loadCurrentEpgData(object : AsyncDataReceiver<HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>> {
            override fun onReceive(data: HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>) {
                var retList = mutableListOf<ReferenceTvEvent>()
                for ((key, value) in data) {
                    var events = ArrayList<ReferenceTvEvent>()
                    events.addAll(value.value)
                    for (i in 0 until events.size - 1) {
                        var event = events[i]
                        if (event.startDate.value.toLong() <= currentTime.value.toLong() && event.endDate.value.toLong() >= currentTime.value.toLong()) {
                            if (i + 1 < events.size) {
                                retList.add(events[i + 1])
                            }
                            break
                        }
                    }
                }
                if (retList.size > 0) {
                    callback.onReceive(retList)
                } else {
                    callback.onFailed(Error(404, "Events not found."))
                }
            }
            override fun onFailed(error: Error?) {
                callback.onFailed(error)
            }
        })
    }

    fun getCurrentEPGEvent(
        startTime: Long,
        endTime: Long,
        callback: AsyncDataReceiver<HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>>
    ) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEPGEvent : ")
        (dataProvider as TifDataProvider).loadCurrentEpgEventsData(
            startTime,
            endTime,
            object : AsyncDataReceiver<HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>> {
                override fun onReceive(data: HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEPGEvent onReceive: data size ${data.size} ")
                    callback.onReceive(data)

                }

                override fun onFailed(error: Error?) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEPGEvent onFailed: ")
                    callback.onFailed(error)
                }
            })
    }
    /*
    loads the epg data for a given channel list
     */
    fun getEpgDataForChannelList(
        channelList: MutableList<ReferenceTvChannel>,
        startTime: Long,
        endTime: Long,
        callback: AsyncDataReceiver<LinkedHashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>>
    ) {
        (dataProvider as TifDataProvider).loadEpgDataForChannelList(
            channelList,
            startTime,
            endTime,
            object : AsyncDataReceiver<LinkedHashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>> {
                override fun onReceive(data: LinkedHashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEPGEvent onReceive: data size ${data.size} ")
                    callback.onReceive(data)

                }

                override fun onFailed(error: Error?) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEPGEvent onFailed: ")
                    callback.onFailed(error)
                }
            })
    }

}