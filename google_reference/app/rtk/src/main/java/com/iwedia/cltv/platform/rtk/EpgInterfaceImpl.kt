package com.iwedia.cltv.platform.rtk

import android.content.*
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.text.TextUtils
import android.util.Log
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.base.EpgInterfaceBaseImpl
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.rtk.provider.ChannelDataProvider
import kotlinx.coroutines.Dispatchers
import java.util.*

class EpgInterfaceImpl constructor(val context : Context?, epgDataProvider: EpgDataProviderInterface, private val channelDataProvider: ChannelDataProvider, private val timeModule:TimeInterface): com.iwedia.cltv.platform.base.EpgInterfaceBaseImpl(
    epgDataProvider, timeModule) {
    override val TAG = "EpgInterfaceImpl"
    private val SVL_ID_ANTENNA = 1
    private val SVL_ID_CABLE = 2
    private val SVL_ID_SATL_GEN = 3
    private val SVL_ID_SATL_PRE = 4
    private val SVL_ID_ANTENNA_CAM = 5
    private val SVL_ID_CABLE_CAM = 6
    private val SVL_ID_SATL_CAM = 7
    private val SVL_ID_ANTENNA_ATV = 11
    private val SVL_ID_CABLE_ATV = 12
    private val WINDOW_DAYS = 7
    private val PRINT_SERVICES = false
    private val PRINT_EVENTS = false
    private val EPG_ACQUISITION_DELAY = 60 * 1000

    private var eventRefreshTask: TimerTask? = null
    private var eventRefreshTimer: Timer? = null
    private var eventServiceIndex = 0
    private var currentONID = -1
    private var currentTSID = -1
    private var currentServiceID = -1

    init {
    }

    private fun fetchAllCurrentEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            var currentEvents = arrayListOf<TvEvent>()
            epgDataProvider.getEventChannelMap().values.forEach { events->
                if (events.isNotEmpty() && !events.get(0).tvChannel.isFastChannel()) {
                    events.forEach { event ->
                        val currentTime = timeInterface.getCurrentTime(event.tvChannel)
                        if (event.startTime <= currentTime && event.endTime >= currentTime) {
                            try {
                                currentEvents.first { it.name == event.name && it.tvChannel.channelId == event.tvChannel.channelId }
                            } catch (e: NoSuchElementException) {
                                currentEvents.add(event)
                            }
                        }
                    }
                }
            }

            if (currentEvents.size > 0) {
                callback.onReceive(currentEvents)
                return@runCoroutine
            } else {
                callback.onFailed(Error("Events not found."))
            }
        })
    }

    private fun fetchAllNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            var nextEvents = arrayListOf<TvEvent>()
            epgDataProvider.getEventChannelMap().values.forEach { events->
                if (events.isNotEmpty() && !events.get(0).tvChannel.isFastChannel()) {
                    events.forEach { event ->
                        var currentTime = timeInterface.getCurrentTime(event.tvChannel)
                        if (event.startTime >= currentTime) {
                            try {
                                nextEvents.first { it.name == event.name && it.tvChannel.channelId == event.tvChannel.channelId && it.startTime == event.startTime }
                            } catch (e: NoSuchElementException) {
                                nextEvents.add(event)
                            }
                        }

                        //Limit epg next events collecting on 200 channels
                        if (nextEvents.size > 200) {
                            callback.onReceive(nextEvents)
                            return@runCoroutine
                        }
                    }
                }
            }

            if (nextEvents.size > 0) {
                callback.onReceive(nextEvents)
                return@runCoroutine
            } else {
                callback.onFailed(Error("Events not found."))
            }
        })
    }

    override fun getAllNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
            if (PRINT_EVENTS) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllNextEvents calling super for ATSC")
            }
            fetchAllNextEvents(object: IAsyncDataCallback<ArrayList<TvEvent>>{
                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }

                override fun onReceive(data: ArrayList<TvEvent>) {
                    var newData: ArrayList<TvEvent> = arrayListOf()
                    data.forEach {
                        it.genre = null
                        newData.add(it)
                    }
                    callback.onReceive(newData)
                }
            })
    }

    private fun getCurrentATSCEvent(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>) {
        CoroutineHelper.runCoroutine({
            val currentTime = timeModule.getCurrentTime(tvChannel)
            val allEvents = epgDataProvider.getEventList()
            allEvents.forEach { event ->
                event.genre = null
                if (event.startTime <= currentTime && event.endTime >= currentTime &&
                    tvChannel.channelId == event.tvChannel.channelId
                ) {
                    callback.onReceive(event)
                    return@runCoroutine
                }
            }

            super.getEventListByChannel(tvChannel,object: IAsyncDataCallback<ArrayList<TvEvent>> {
                override fun onFailed(error: Error) {
                    callback.onFailed(Error("Events not found."))
                }

                override fun onReceive(data: ArrayList<TvEvent>) {
                    run exitForEach@{
                        data.forEach { event ->
                            event.genre = null
                            if (event.startTime <= currentTime && event.endTime >= currentTime &&
                                tvChannel.channelId == event.tvChannel.channelId
                            ) {
                                callback.onReceive(event)
                                return@exitForEach
                            }
                        }
                    }
                }
            })
            return@runCoroutine

        },Dispatchers.Main)
    }

    override fun getCurrentEvent(
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<TvEvent>
    ) {
              getCurrentATSCEvent(tvChannel,callback)
    }

    override fun getEventListByChannel(tvChannel: TvChannel, callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel: ${tvChannel.name} --- ${tvChannel.id} ")
             super.getEventListByChannel(tvChannel, callback)
             return
     }
}