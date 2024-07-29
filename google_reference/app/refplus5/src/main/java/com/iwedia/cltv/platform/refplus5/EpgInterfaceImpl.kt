package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.media.tv.TvContract
import android.media.tv.TvInputService
import android.os.Bundle
import android.util.Log
import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.refplus5.provider.ChannelDataProvider
import com.iwedia.cltv.platform.refplus5.provider.PlatformSpecificData
import com.mediatek.dtv.tvinput.client.eventinfo.EventDownloadManager
import com.mediatek.dtv.tvinput.client.eventinfo.EventInfoManager


internal class EpgInterfaceImpl constructor(val channelDataProvider : ChannelDataProvider, val context : Context, epgDataProvider: EpgDataProviderInterface, timeInterface: TimeInterface, val utilsInterfaceImpl : UtilsInterfaceImpl): com.iwedia.cltv.platform.base.EpgInterfaceBaseImpl(epgDataProvider,timeInterface) {

    private var hideLockedServices : Boolean = false
    private var infoManager: EventInfoManager? = null
    private val TAG_E = "EpgInterfaceImpl"

    private val KEY_EVENT_ID = "KEY_EVENT_ID"
    private val KEY_START_TIME_UTC_MILLIS = "KEY_START_TIME_UTC_MILLIS"
    private val KEY_END_TIME_UTC_MILLIS = "KEY_END_TIME_UTC_MILLIS"
    private val KEY_SHORT_DESCRIPTION = "KEY_SHORT_DESCRIPTION"
    private val KEY_LONG_DESCRIPTION = "KEY_LONG_DESCRIPTION"
    private val KEY_HAS_CAPTION = "KEY_HAS_CAPTION"
    private val KEY_SCRAMBLED = "KEY_SCRAMBLED"
    private val KEY_TITLE = "KEY_TITLE"
    private val KEY_RUNNING_STATUS = "KEY_RUNNING_STATUS"
    private val KEY_EVENT_TYPE = "KEY_EVENT_TYPE"
    private val KEY_CONTENT_RATING = "KEY_CONTENT_RATING"
    private val KEY_CA_SYSTEM_IDS = "KEY_CA_SYSTEM_IDS"
    private val KEY_GUIDANCE_TEXT = "KEY_GUIDANCE_TEXT"
    private val KEY_BROADCAST_GENRE = "KEY_BROADCAST_GENRE"
    private val pfInput = "com.mediatek.dtv.tvinput.dvbtuner/.DvbTvInputService/HW0"
    private var eventDownloadManager: EventDownloadManager? = null

    private fun bundleToTvEvent(bundle: Bundle, channelDbId: Long) : TvEvent {
        val eventId: Int = bundle.getInt(KEY_EVENT_ID)
        val startTimeUtcMills: Long = bundle.getLong(KEY_START_TIME_UTC_MILLIS)
        val endTimeUtcMills: Long = bundle.getLong(KEY_END_TIME_UTC_MILLIS)
        val shortDes: String = bundle.getString(KEY_SHORT_DESCRIPTION, "")
        val longDes: String = bundle.getString(KEY_LONG_DESCRIPTION, "")
        val title: String = bundle.getString(KEY_TITLE, "")
        val rating: String = bundle.getString(KEY_CONTENT_RATING, "")
        val genre: String = bundle.getString(KEY_BROADCAST_GENRE, "")
        val hasCaption: Boolean = bundle.getBoolean(KEY_HAS_CAPTION) // need todo
        val scrambled: Int = bundle.getInt(KEY_SCRAMBLED) // need todo
        val status: Int = bundle.getInt(KEY_RUNNING_STATUS) // need todo
        val eventType: Int = bundle.getInt(KEY_EVENT_TYPE) // need todo
        val systemIds: Int = bundle.getInt(KEY_CA_SYSTEM_IDS) // need todo
        val guidanceText: String = bundle.getString(KEY_GUIDANCE_TEXT, "")

        var tvChannel = TvChannel()

        for (channel in channelDataProvider.getChannelList()) {
            if(!(channel.isFastChannel()) && (channel.channelId == channelDbId)) {
                tvChannel = channel
            }
        }

        return TvEvent(eventId, tvChannel, title, shortDes, longDes, "",
            startTimeUtcMills, endTimeUtcMills, null,
            0, 0, null, rating, false,
            isInitialChannel = false, providerFlag = null, genre = genre, subGenre = null, isSchedulable = true,tvEventId = eventId
        )
    }

    init {
            hideLockedServices = utilsInterfaceImpl.getCountryPreferences(UtilsInterface.CountryPreference.HIDE_LOCKED_SERVICES_IN_EPG,false) as Boolean
            infoManager = EventInfoManager(context)
            eventDownloadManager = EventDownloadManager(context);
            val params = Bundle()
            params.putInt("KEY_PRIORITY_HINT_USE_CASE_TYPE", TvInputService.PRIORITY_HINT_USE_CASE_TYPE_BACKGROUND)
            eventDownloadManager?.initEventDownloadSession(pfInput, params)
            eventDownloadManager?.setEventDownloadListener( object :
                EventDownloadManager.EventDownloadListener {
                override fun onCompleted(status: Bundle) {
                }
            })
    }


    override fun getAllCurrentEvent(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            var allEvents = epgDataProvider.getEventList()
            var currentEvents = arrayListOf<TvEvent>()
            allEvents.forEach { event ->
                val currentTime = timeInterface.getCurrentTime(event.tvChannel)
                if (event.startTime <= currentTime && event.endTime >= currentTime) {

                    try {
                        if (infoManager?.getPresentEventInfo(pfInput, event.tvChannel.channelId) != null
                        ) {
                            var presentEvent = bundleToTvEvent(
                                infoManager?.getPresentEventInfo(
                                    pfInput,
                                    event.tvChannel.channelId
                                )!!, event.tvChannel.channelId
                            )
                            currentEvents.add(presentEvent)
                        } else {
                            currentEvents.add(event)
                        }

                    } catch (e: NoSuchElementException) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG_E,"[getAllCurrentEvent] Exception $e")
                    }
                }
            }
            for (channel in channelDataProvider.getChannelList()) {
                var serviceFound = false
                for(event in currentEvents) {
                    if(event.tvChannel.id == channel.id) {
                        serviceFound = true
                        break
                    }
                }
                if(!serviceFound) {
                    var presentEvent = infoManager?.getPresentEventInfo(pfInput, channel.channelId)
                    if(presentEvent != null) {
                        var currentEvent = bundleToTvEvent(presentEvent, channel.channelId)
                        currentEvents.add(currentEvent)
                    }
                }
            }
            if (hideLockedServices) {
                var repackData = ArrayList<TvEvent>()
                for (event in currentEvents) {
                    if (!event.tvChannel.isLocked) {
                        repackData.add(event)
                    }
                }
                callback.onReceive(repackData)
            } else {
                if (currentEvents.size > 0) {
                    callback.onReceive(currentEvents)
                    return@runCoroutine
                } else {
                    callback.onFailed(Error("Events not found."))
                }
            }
        })
    }

    override fun getAllNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            val nextEvents = arrayListOf<TvEvent>()
            val allEvents = epgDataProvider.getEventList()
            allEvents.sortBy  { it.startTime }

            for (event in allEvents){
                val tvChannel = event.tvChannel
                if (tvChannel.isBroadcastChannel()
                    && !(hideLockedServices && tvChannel.isLocked)
                    && event.startTime >= timeInterface.getCurrentTime(tvChannel)
                ) {
                        val followEvent = infoManager?.getFollowingEventInfo(pfInput, tvChannel.channelId)
                        if(followEvent!=null){
                            val followingEvent = bundleToTvEvent(followEvent, tvChannel.channelId)
                            if(followingEvent.tvEventId == event.tvEventId){
                                nextEvents.add(followingEvent)
                            }else{
                                nextEvents.add(event)
                            }

                        }else{
                            nextEvents.add(event)
                        }

                        //Limit epg next events collecting on 200 channels
                        if (nextEvents.size > 200) {
                            callback.onReceive(nextEvents)
                            return@runCoroutine
                        }
                }
            }

            for (tvChannel in channelDataProvider.getChannelList()) {
                if (tvChannel.isBroadcastChannel()
                    && !(hideLockedServices && tvChannel.isLocked)
                ) {
                    var serviceFound = false
                    for(event in nextEvents) {
                        if(event.tvChannel.id == tvChannel.id) {
                            serviceFound = true
                            break
                        }
                    }
                    if(!serviceFound) {
                        val fallowEvent = infoManager?.getFollowingEventInfo(pfInput, tvChannel.channelId)
                        if(fallowEvent != null) {
                            val followingEvent = bundleToTvEvent(fallowEvent, tvChannel.channelId)
                            nextEvents.add(followingEvent)

                            //Limit epg next events collecting on 200 channels
                            if (nextEvents.size > 200) {
                                nextEvents.sortBy { it.startTime }
                                callback.onReceive(nextEvents)
                                return@runCoroutine
                            }
                        }
                    }
                }
            }

            if (nextEvents.size > 0) {
                nextEvents.sortBy { it.startTime }
                callback.onReceive(nextEvents)
            } else {
                callback.onFailed(Error("Events not found."))
            }
        })
    }
    @Synchronized
    override fun getCurrentEvent(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>) {
        super.getCurrentEvent(tvChannel, object : IAsyncDataCallback<TvEvent> {
            override fun onFailed(error: Error) {
                getPresentEvent(null,tvChannel,callback)
            }

            override fun onReceive(data: TvEvent) {
                getPresentEvent(data,tvChannel,callback)
            }
        })
    }

    private fun getPresentEvent(
        scheduledTvEvent: TvEvent?,
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<TvEvent>
    ) {
        var presentEvent: TvEvent? = null

        if (infoManager?.getPresentEventInfo(pfInput, tvChannel.channelId) != null) {
            presentEvent = bundleToTvEvent(
                infoManager?.getPresentEventInfo(
                    pfInput,
                    tvChannel.channelId
                )!!, tvChannel.channelId
            )
        }
        if (presentEvent != null) {
            callback.onReceive(presentEvent)
        } else {
            if (scheduledTvEvent != null) {
                callback.onReceive(scheduledTvEvent)
            } else {
                callback.onFailed(Error("Event for current channel not found."))
            }
        }
    }

    override fun getEventListByChannel(tvChannel: TvChannel, callback: IAsyncDataCallback<java.util.ArrayList<TvEvent>>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel: ${tvChannel.name} --- ${tvChannel.id} ")
        CoroutineHelper.runCoroutine({
            if(tvChannel.isFastChannel()) {
                super.getEventListByChannel(tvChannel, callback)
            } else {
                super.getEventListByChannel(
                tvChannel,
                object : IAsyncDataCallback<java.util.ArrayList<TvEvent>> {
                    override fun onFailed(error: Error) {
                        var data = java.util.ArrayList<TvEvent>()
                        var event: TvEvent

                        var sdkEvent =
                            infoManager?.getPresentEventInfo(pfInput, tvChannel.channelId)
                        if (sdkEvent != null) {
                            event = bundleToTvEvent(sdkEvent, tvChannel.channelId)
                            data.add(event)
                        }

                        sdkEvent =
                            infoManager?.getFollowingEventInfo(pfInput, tvChannel.channelId)
                        if (sdkEvent != null) {
                            event = bundleToTvEvent(sdkEvent, tvChannel.channelId)
                            data.add(event)
                        }

                        if (data.size == 0) {
                            Thread {
                                callback.onFailed(error)
                            }.start()
                        } else {
                            Thread {
                                callback.onReceive(data)
                            }.start()
                        }
                    }

                    override fun onReceive(data: java.util.ArrayList<TvEvent>) {
                        var presentEvent: TvEvent? = null
                        var followEvent: TvEvent? = null

                        var sdkEvent =
                            infoManager?.getPresentEventInfo(pfInput, tvChannel.channelId)
                        if (sdkEvent != null) {
                            presentEvent = bundleToTvEvent(sdkEvent!!, tvChannel.channelId)
                        }

                        sdkEvent =
                            infoManager?.getFollowingEventInfo(pfInput, tvChannel.channelId)
                        if (sdkEvent != null) {
                            followEvent = bundleToTvEvent(sdkEvent!!, tvChannel.channelId)
                        }
                        var presentFound = false
                        var followFound = false
                        val finalList = mutableListOf<TvEvent>()
                        for (i in 0 until data.size) {
                            if (data.get(i) != null) {
                                if (presentEvent != null  && data[i].tvEventId == presentEvent.tvEventId) {
                                    finalList.add(presentEvent)
                                    presentFound = true
                                } else if (followEvent != null && data[i].tvEventId == followEvent.tvEventId) {
                                    finalList.add(followEvent)
                                    followFound = true
                                } else {
                                    finalList.add(data[i])
                                }
                            }
                        }
                        if ((!presentFound) && (!followFound) &&
                            (presentEvent != null) && (followEvent != null)
                        ) {
                            var eventMap = epgDataProvider.getEventChannelMap()
                            var nativeData = java.util.ArrayList<TvEvent>()

                            if (presentEvent != null) {
                                nativeData.add(presentEvent)
                            }
                            if (followEvent != null) {
                                nativeData.add(followEvent)
                            }

                            eventMap[tvChannel.getUniqueIdentifier()]?.forEach {
                                nativeData.add(it)
                            }

                            injectNoInfoState(nativeData, tvChannel)

                            callback.onReceive(nativeData)
                        } else {
                            callback.onReceive(finalList as java.util.ArrayList)
                        }
                    }
                })
            }
        })
    }

    @Synchronized
    fun checkIfBarkerChannel(tvChannel : TvChannel) : Boolean {
        var channelListId = (tvChannel.platformSpecific as PlatformSpecificData).channelListId
        var internalProviderId = (tvChannel.platformSpecific as PlatformSpecificData).internalProviderId

        val bundle = Bundle()

        if (channelListId != null && eventDownloadManager != null) {
            bundle.putString("KEY_SERVICE_LIST_IDS", channelListId);
            bundle.putString("KEY_SERVICE_RECORD_ID", internalProviderId);

            if (eventDownloadManager != null) {
                try {
                    var value = eventDownloadManager!!.isBarkerOrSequentialDownloadByChannel(bundle);
                    if(value == 1) {
                        return true
                    }
                }catch (E: Exception){
                    E.printStackTrace()
                    return false
                }
            }
        }
        return false
    }
    @Synchronized
    fun tuneToBarkerChannel(tvChannel : TvChannel) {
        try {
            val mUri = TvContract.buildChannelUri(tvChannel.id.toLong())
            eventDownloadManager?.startTuningMultiplex(mUri)
        } catch(e : Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "tuneToBarkerChannel exception ${e.message}")
        }
    }

    @Synchronized
    fun cancelBarkerChannel() {
        eventDownloadManager?.cancel()
        eventDownloadManager?.release()
        eventDownloadManager = null
        eventDownloadManager = EventDownloadManager(context);
        val params = Bundle()
        params.putInt("KEY_PRIORITY_HINT_USE_CASE_TYPE", TvInputService.PRIORITY_HINT_USE_CASE_TYPE_BACKGROUND)
        eventDownloadManager?.initEventDownloadSession(pfInput, params)
    }
}