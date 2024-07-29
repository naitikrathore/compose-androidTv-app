package com.iwedia.cltv.platform.t56

import android.content.Context
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.t56.provider.ChannelDataProvider
import com.iwedia.cltv.platform.t56.provider.PlatformSpecificData
import com.mediatek.twoworlds.tv.MtkTvChannelList
import com.mediatek.twoworlds.tv.MtkTvChannelListBase
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.MtkTvEvent
import com.mediatek.twoworlds.tv.MtkTvTime
import com.mediatek.twoworlds.tv.common.MtkTvChCommonBase
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase
import com.mediatek.twoworlds.tv.model.MtkTvATSCChannelInfo
import com.mediatek.twoworlds.tv.model.MtkTvChannelInfoBase
import com.mediatek.twoworlds.tv.model.MtkTvDvbChannelInfo
import com.mediatek.twoworlds.tv.model.MtkTvRatingConvert2Goo
import java.util.Timer
import java.util.TimerTask

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
    private val mtkTvChList: MtkTvChannelList = MtkTvChannelList.getInstance()
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
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigTypeBase.CFG_BS_BS_BRDCST_TYPE, 0);
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

    private fun multiTSEpgWindowUpdate(): Boolean {
        val svlID: Int = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_BS_SVL_ID)
        val filter: Int = MtkTvChCommonBase.SB_VNET_ALL
        val startTime: Long = MtkTvTime.getInstance().getBroadcastUtcTime().toSeconds()
        val duration = (WINDOW_DAYS * 24 * 60 * 60).toLong()

        if (MtkTvChannelListBase.getCurrentChannel() != null && MtkTvChannelListBase.getCurrentChannel() !is MtkTvDvbChannelInfo) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "multiTSEpgWindowUpdate: current channel is not DVB, exiting")
            return false
        }

        val currentService: MtkTvDvbChannelInfo =
            MtkTvChannelListBase.getCurrentChannel() as MtkTvDvbChannelInfo
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "multiTSEpgWindowUpdate enter :$currentService"
        )
        if (currentService != null &&
            currentServiceID != currentService.channelId
        ) {
            val serviceList: List<MtkTvChannelInfoBase> = mtkTvChList.getChannelListByFilter(
                svlID,
                filter,
                currentService.channelId,
                10,
                10
            )
            currentTSID = currentService.tsId
            currentONID = currentService.onId
            currentServiceID = currentService.channelId
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "multiTSEpgWindowUpdate Channel list size: " + serviceList.size + " start time:" + startTime + " duration:" + duration + " ONID=" + currentService.getOnId() + " TSID =" + currentService.getTsId()
            )
            if (eventRefreshTask != null) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "multiTSEpgWindowUpdate closing old refresh thread")
                eventRefreshTask!!.cancel()
                eventRefreshTimer?.cancel()
                eventRefreshTimer?.purge()
                eventRefreshTask = null
                eventRefreshTimer = null
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "multiTSEpgWindowUpdate creating default window")
            MtkTvEvent.getInstance().clearActiveWindows()
            MtkTvEvent.getInstance().setCurrentActiveWindows(serviceList, startTime, duration)
            if (PRINT_SERVICES) {
                for (service in serviceList) {
                    val dvbService: MtkTvDvbChannelInfo = service as MtkTvDvbChannelInfo
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "multiTSEpgWindowUpdate service in list :$dvbService"
                    )
                }
            }
            //because svlRecId starts from 1 and index is from 0
            eventServiceIndex = (currentService.svlRecId - 1)

            if(eventServiceIndex < 0) {
                eventServiceIndex = 0
            }
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "multiTSEpgWindowUpdate eventServiceIndex:" + eventServiceIndex + " channel number : " + currentService.getChannelNumber()
            )
            eventRefreshTimer = Timer()
            eventRefreshTask = object : TimerTask() {
                override fun run() {
                    Thread {
                        val fullServiceList: List<MtkTvChannelInfoBase> =
                            mtkTvChList.getChannelListByFilter(svlID, filter, 0, 0, 0xFFFF)
                        val totalServiceCount = fullServiceList.size
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "multiTSEpgWindowUpdate started timer total service: $totalServiceCount event service index:$eventServiceIndex"
                        )

                        if(totalServiceCount > 0) {
                            eventServiceIndex = eventServiceIndex + 10
                            if (eventServiceIndex >= totalServiceCount) {
                                eventServiceIndex = 0
                            }
                            val newStartTime: Long =
                                MtkTvTime.getInstance().getBroadcastUtcTime().toSeconds()
                            val serviceList: List<MtkTvChannelInfoBase> =
                                mtkTvChList.getChannelListByFilter(
                                    svlID,
                                    filter,
                                    fullServiceList[eventServiceIndex].getChannelId(),
                                    0,
                                    10
                                )
                            if (PRINT_SERVICES) {
                                for (service in serviceList) {
                                    val dvbService: MtkTvDvbChannelInfo =
                                        service as MtkTvDvbChannelInfo
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "multiTSEpgWindowUpdate service in list :$dvbService"
                                    )
                                }
                            }
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                "multiTSEpgWindowUpdate creating additional window from : $eventServiceIndex"
                            )
                            MtkTvEvent.getInstance().clearActiveWindows()
                            MtkTvEvent.getInstance()
                                .setCurrentActiveWindows(serviceList, newStartTime, duration)
                        }
                    }.start()
                }
            }
            eventRefreshTimer!!.schedule(
                eventRefreshTask,
                (2 * EPG_ACQUISITION_DELAY).toLong(),
                EPG_ACQUISITION_DELAY.toLong()
            )
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "multiTSEpgWindowUpdate starting thread")
        return true
    }

    private fun singleTSEpgWindowUpdate(): Boolean {
        val svlID: Int = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_BS_SVL_ID)
        val filter: Int = MtkTvChCommonBase.SB_VNET_ALL
        val startTime: Long = MtkTvTime.getInstance().getBroadcastUtcTime().toSeconds()
        val duration = (WINDOW_DAYS * 24 * 60 * 60).toLong()

        if(MtkTvChannelListBase.getCurrentChannel() == null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "singleTSEpgWindowUpdate: current channel not found, exiting")
            return false
        }

        if (MtkTvChannelListBase.getCurrentChannel() != null && MtkTvChannelListBase.getCurrentChannel() !is MtkTvDvbChannelInfo) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateEpgWindow: current channel is not DVB, exiting")
            return false
        }
        val currentService: MtkTvDvbChannelInfo =
            MtkTvChannelListBase.getCurrentChannel() as MtkTvDvbChannelInfo
        val serviceList: List<MtkTvChannelInfoBase> =
            mtkTvChList.getChannelListByFilter(svlID, filter, 0, 0, 0xFFFF)
        val filterServiceList: MutableList<MtkTvChannelInfoBase> =
            java.util.ArrayList<MtkTvChannelInfoBase>()
        if (currentService != null) {
            if (eventRefreshTask != null) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateDVBTEpgWindow closing old refresh thread")
                eventRefreshTask!!.cancel()
                eventRefreshTimer?.cancel()
                eventRefreshTimer?.purge()
                eventRefreshTask = null
                eventRefreshTimer = null
            }

            //prevent interrupting EPG acquisition on channel change on same TS
            if (currentService.getTsId() === currentTSID &&
                currentService.getOnId() === currentONID
            ) {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "updateDVBTEpgWindow: skipping, already on ONID:$currentONID TSID:$currentTSID"
                )
                return true
            }
            currentTSID = currentService.getTsId()
            currentONID = currentService.getOnId()
            currentServiceID = currentService.getChannelId()
            for (service in serviceList) {
                val dvbService: MtkTvDvbChannelInfo = service as MtkTvDvbChannelInfo
                if (dvbService.onId === currentService.onId &&
                    dvbService.tsId === currentService.tsId
                ) {
                    filterServiceList.add(service)
                }
            }
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "updateDVBTEpgWindow Channel list size: " + filterServiceList.size + " start time:" + startTime + " duration:" + duration + " ONID=" + currentService.getOnId() + " TSID =" + currentService.getTsId()
            )
            MtkTvEvent.getInstance().clearActiveWindows()
            MtkTvEvent.getInstance().setCurrentActiveWindows(filterServiceList, startTime, duration)
        }
        return true
    }

    override fun updateEpgData(applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.updateEpgData(applicationMode)
        } else {
            Thread {
                val svlID: Int =
                    MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_BS_SVL_ID)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateEpgData: svlID $svlID")
                when (svlID) {
                    SVL_ID_ANTENNA -> singleTSEpgWindowUpdate()
                    SVL_ID_CABLE -> multiTSEpgWindowUpdate()
                    SVL_ID_SATL_GEN -> singleTSEpgWindowUpdate()
                    SVL_ID_SATL_PRE -> singleTSEpgWindowUpdate()
                }
            }.start()
        }
    }

    private fun isServiceFromCurrentList( tvChannel: TvChannel) : Boolean{
        val svlID: Int = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_BS_SVL_ID)

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isServiceFromCurrentList list id : $svlID tuner type ${tvChannel.tunerType}")

        if((tvChannel.tunerType == TunerType.TERRESTRIAL_TUNER_TYPE) &&
            (svlID == SVL_ID_ANTENNA)) {
            return true
        }
        if((tvChannel.tunerType == TunerType.CABLE_TUNER_TYPE) &&
            (svlID == SVL_ID_CABLE)) {
            return true
        }
        if((tvChannel.tunerType == TunerType.SATELLITE_TUNER_TYPE) &&
            ((svlID == SVL_ID_SATL_GEN) || (svlID == SVL_ID_SATL_PRE))) {
            return true
        }

        return false
    }

    private fun getServiceEvent(tifService : TvChannel, mtkService : MtkTvChannelInfoBase?, pf: Boolean): TvEvent? {
        if (PRINT_EVENTS) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getServiceEvent channel ${tifService.name} ${tifService.id}")
        }

        if(mtkService == null) {
            return null
        }

        var event = MtkTvEvent.getInstance().getPFEventInfoByChannel(mtkService.channelId, pf)

        if(event == null) {
            return null
        }

        val ratingMapped = MtkTvRatingConvert2Goo()
        MtkTvEvent.getInstance().getEventRatingMapById(mtkService.channelId, event.eventId, ratingMapped)

        var contentRating = ""
        if (!TextUtils.isEmpty(ratingMapped.domain)
            && !TextUtils.isEmpty(ratingMapped.ratingSystem)
            && !TextUtils.isEmpty(ratingMapped.rating)) {
            contentRating = TvContentRating.createRating(
                ratingMapped.domain,
                ratingMapped.ratingSystem,
                ratingMapped.rating,
                *ratingMapped.subRating
            ).flattenToString()
        }

        if (PRINT_EVENTS) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getServiceEvent event ${event.eventTitle} ${event.eventId}")
        }

        return TvEvent(
            event.eventId,
            tifService,
            event.eventTitle,
            event.eventDetail,
            event.eventDetailExtend,
            "",
            event.startTime * 1000,
            (event.startTime + event.duration) * 1000,
            null,
            0,
            0,
            null,
            contentRating,
            false,
            isInitialChannel = false,
            providerFlag = null,
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isATSCService(service : TvChannel) : Boolean {
        if (PRINT_EVENTS) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "isATSCService ${(service.platformSpecific as PlatformSpecificData).tifTunerType}")
        }

        var usedService = service

        if(service.platformSpecific == null) {
            channelDataProvider.getChannelList().forEach findService@ { providerService ->
                if(service.id ==  providerService.id) {
                    usedService = providerService
                    return@findService
                }
            }
        }
        if(usedService.platformSpecific == null) {
            return false
        }

        if((usedService.platformSpecific as PlatformSpecificData).tifTunerType == TvContract.Channels.TYPE_ATSC_T ||
            (usedService.platformSpecific as PlatformSpecificData).tifTunerType == TvContract.Channels.TYPE_ATSC_C ||
            (usedService.platformSpecific as PlatformSpecificData).tifTunerType == TvContract.Channels.TYPE_NTSC) {
            return true
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getPresentEventByService(service : TvChannel) : TvEvent? {
        var usedService = service

        if(service.platformSpecific == null) {
            channelDataProvider.getChannelList().forEach findService@ { providerService ->
                if(service.id ==  providerService.id) {
                    usedService = providerService
                    return@findService
                }
            }
        }
        if(usedService.platformSpecific == null) {
            return null
        }

        var mtkService: MtkTvChannelInfoBase? = MtkTvChannelList.getInstance().getChannelInfoBySvlRecId((usedService.platformSpecific as PlatformSpecificData).internalServiceListID,(usedService.platformSpecific as PlatformSpecificData).internalServiceIndex)
            ?: return null

        return getServiceEvent(usedService, mtkService, true)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getFollowingEventByService(service : TvChannel) : TvEvent? {
        var usedService = service

        if(service.platformSpecific == null) {
            channelDataProvider.getChannelList().forEach findService@ { providerService ->
                if(service.id ==  providerService.id) {
                    usedService = providerService
                    return@findService
                }
            }
        }
        if(usedService.platformSpecific == null) {
            return null
        }

        var mtkService: MtkTvChannelInfoBase? = MtkTvChannelList.getInstance().getChannelInfoBySvlRecId((usedService.platformSpecific as PlatformSpecificData).internalServiceListID,(usedService.platformSpecific as PlatformSpecificData).internalServiceIndex)
            ?: return null

        return getServiceEvent(usedService, mtkService, false)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getAllPresentFallowingEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>, pf : Boolean) {
        CoroutineHelper.runCoroutine({
            var events = ArrayList<TvEvent>()

            channelDataProvider.getChannelList().forEach { tvChannel ->

                if (PRINT_EVENTS) {
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "getAllPresentFallowingEvents channel ${tvChannel.name} ${tvChannel.id}"
                    )
                }

                if (tvChannel.platformSpecific != null) {
                    var mtkService = MtkTvChannelList.getInstance().getChannelInfoBySvlRecId(
                        (tvChannel.platformSpecific as PlatformSpecificData).internalServiceListID,
                        (tvChannel.platformSpecific as PlatformSpecificData).internalServiceIndex
                    )
                    if (mtkService != null) {
                        var event =
                            MtkTvEvent.getInstance().getPFEventInfoByChannel(mtkService.channelId, pf)
                        if (event != null) {
                            val ratingMapped = MtkTvRatingConvert2Goo()
                            MtkTvEvent.getInstance()
                                .getEventRatingMapById(
                                    mtkService.channelId,
                                    event.eventId,
                                    ratingMapped
                                )

                            var contentRating = ""
                            if (!TextUtils.isEmpty(ratingMapped.domain)
                                && !TextUtils.isEmpty(ratingMapped.ratingSystem)
                                && !TextUtils.isEmpty(ratingMapped.rating)
                            ) {
                                contentRating = TvContentRating.createRating(
                                    ratingMapped.domain,
                                    ratingMapped.ratingSystem,
                                    ratingMapped.rating,
                                    *ratingMapped.subRating
                                ).flattenToString()
                            }

                            if (PRINT_EVENTS) {
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "getAllPresentFallowingEvents event ${event.eventTitle} ${event.eventId}"
                                )
                            }

                            events.add(
                                TvEvent(
                                    event.eventId,
                                    tvChannel,
                                    event.eventTitle,
                                    event.eventDetail,
                                    event.eventDetailExtend,
                                    "",
                                    event.startTime * 1000,
                                    (event.startTime + event.duration) * 1000,
                                    null,
                                    0,
                                    0,
                                    null,
                                    contentRating,
                                    false,
                                    isInitialChannel = false,
                                    providerFlag = null,
                                )
                            )
                        }
                    }
                }
            }

            if (events.size > 0) {
                callback.onReceive(events)
                return@runCoroutine
            } else {
                callback.onFailed(Error("Events not found."))
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getAllCurrentEvent(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        if(MtkTvChannelListBase.getCurrentChannel() is MtkTvATSCChannelInfo) {
            if (PRINT_EVENTS) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllCurrentEvent calling super for ATSC")
            }
            super.getAllCurrentEvent(object: IAsyncDataCallback<ArrayList<TvEvent>>{
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
        } else {
            super.getAllCurrentEvent(object: IAsyncDataCallback<ArrayList<TvEvent>>{
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

        if(MtkTvChannelListBase.getCurrentChannel() is MtkTvDvbChannelInfo) {
            getAllPresentFallowingEvents(callback,true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getAllNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        if(MtkTvChannelListBase.getCurrentChannel() is MtkTvATSCChannelInfo) {
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
        } else {
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
        if(MtkTvChannelListBase.getCurrentChannel() is MtkTvDvbChannelInfo) {
            getAllPresentFallowingEvents(callback, false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getCurrentDVBEvent(tvChannel: TvChannel,
                                   callback: IAsyncDataCallback<TvEvent>) {
        CoroutineHelper.runCoroutine({
            if (PRINT_EVENTS) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEvent enter ${tvChannel.name} ${tvChannel.id}")
            }
            if (isServiceFromCurrentList(tvChannel)) {
                val sdkEvent = getPresentEventByService(tvChannel)
                if (sdkEvent != null) {
                    callback.onReceive(sdkEvent)
                    if (PRINT_EVENTS) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEvent exit pf ${sdkEvent.name}")
                    }
                    return@runCoroutine
                } else
                {
                    if (PRINT_EVENTS) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEvent Unable to find service event")
                    }
                    callback.onFailed(Error("Unable to find service event"))
                }
            } else {
                if (PRINT_EVENTS) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentEvent bad playlist")
                }
                callback.onFailed(Error("bad playlist"))
                return@runCoroutine
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

        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getCurrentEvent(
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<TvEvent>
    ) {
          if((tvChannel.platformSpecific !=null) && ((tvChannel.platformSpecific as PlatformSpecificData).tifTunerType == TvContract.Channels.TYPE_OTHER)) {
              return super.getCurrentEvent(tvChannel, callback)
          }

          if(isATSCService(tvChannel)) {
              getCurrentATSCEvent(tvChannel,callback)
          } else {
              getCurrentDVBEvent(tvChannel,callback)
          }
    }

    override fun getEventListByChannel(tvChannel: TvChannel, callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel: ${tvChannel.name} --- ${tvChannel.id} ")
        if (tvChannel.platformSpecific!=null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel:${tvChannel.platformSpecific as PlatformSpecificData}")
            if( isATSCService(tvChannel) ||
             (tvChannel.platformSpecific as PlatformSpecificData).tifTunerType == TvContract.Channels.TYPE_OTHER) {
             super.getEventListByChannel(tvChannel, callback)
             return
         }
     }

        CoroutineHelper.runCoroutine({
            if(PRINT_EVENTS) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel enter")
            }
            super.getEventListByChannel(tvChannel, object : IAsyncDataCallback<ArrayList<TvEvent>> {
                @RequiresApi(Build.VERSION_CODES.S)
                override fun onFailed(error: Error) {
                        var data = ArrayList<TvEvent>()
                        var sdkEvent = getPresentEventByService(tvChannel)
                        if (sdkEvent != null) {
                            data.add(sdkEvent)
                        }
                        sdkEvent = getFollowingEventByService(tvChannel)
                        if (sdkEvent != null) {
                            data.add(sdkEvent)
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

                @RequiresApi(Build.VERSION_CODES.S)
                override fun onReceive(data: ArrayList<TvEvent>) {
                    val presentEvent = getPresentEventByService(tvChannel)
                    val followEvent = getFollowingEventByService(tvChannel)
                    var presentFound = false
                    var followFound = false

                    if ((presentEvent == null) || (followEvent == null) && PRINT_EVENTS) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel present or fallowing are null")
                    }

                    if ((presentEvent != null) && (followEvent != null)) {
                        for (i in 0 until data.size) {
                            if (data.get(i) != null) {
                                if (PRINT_EVENTS) {
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "getEventListByChannel event name ${(data[i]).name} id ${
                                            (data.get(i) as TvEvent).startTime
                                        } ${(data.get(i) as TvEvent).endTime} present name ${presentEvent.name} date ${presentEvent.startTime} ${presentEvent.endTime}"
                                    )
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "getEventListByChannel fallow name ${followEvent.name} date ${followEvent.startTime} ${followEvent.endTime}"
                                    )
                                }
                                if (((data[i]).startTime == presentEvent.startTime) &&
                                    (data[i].endTime == presentEvent.endTime)
                                ) {
                                    presentFound = true
                                }
                                if ((data[i].startTime == followEvent.startTime) &&
                                    (data[i].endTime == followEvent.endTime)
                                ) {
                                    followFound = true
                                }
                            }
                        }
                    }

                    if (PRINT_EVENTS) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "getEventListByChannel present found $presentFound follow found $followFound"
                        )
                    }

                    if ((!presentFound) && (!followFound)) {
                        var nativeData = ArrayList<TvEvent>()
                        if (presentEvent != null) {
                            nativeData.add(presentEvent)
                        }
                        if (followEvent != null) {
                            nativeData.add(followEvent)
                        }

                        for (i in 0 until data.size) {
                            if (data.get(i) != null) {
                                //skip over Tif junk event
                                if (!((i == 0) && ((data[i]).name == ""))) {
                                    nativeData.add(data[i])
                                } else {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel skipping TIF junk event")
                                }
                            }
                        }

                        injectNoInfoState(nativeData,tvChannel)

                        callback.onReceive(nativeData)
                    } else {
                            callback.onReceive(data)
                    }
                }
            })
            if(PRINT_EVENTS) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel exit")
            }
        })
    }

    override fun setActiveWindow(tvChannelList: MutableList<TvChannel>, startTime: Long) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setActiveWindow: tvChannelList size === ${tvChannelList.size}, startTime == $startTime")
        val serviceList = mutableListOf<MtkTvChannelInfoBase>()
        tvChannelList.forEach { item ->
            val mtkService = MtkTvChannelList.getInstance().getChannelInfoBySvlRecId(
                item.providerFlag1!!, item.providerFlag2!!)
            if(mtkService != null){
                serviceList.add(mtkService)
            }
        }
        if(serviceList.isNotEmpty()){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setActiveWindow: setting up active window")
            MtkTvEvent.getInstance().setCurrentActiveWindows(serviceList, startTime)
        }
    }

    override fun clearActiveWindow() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "clearActiveWindow: clearing active window")
        MtkTvEvent.getInstance().clearActiveWindows()
    }

    override fun getStartTimeForActiveWindow(): Long {
        return MtkTvTime.getInstance().broadcastTimeInUtcSeconds
    }
}