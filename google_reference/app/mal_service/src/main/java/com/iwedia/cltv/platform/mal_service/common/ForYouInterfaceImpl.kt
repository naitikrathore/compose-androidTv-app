package com.iwedia.cltv.platform.mal_service.common

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.mal_service.epg.EpgDataProvider
import com.iwedia.cltv.platform.mal_service.toServiceChannel
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.RecommendationRow
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * For You interface implementation
 *
 * This interface class collect for you data from the mw
 * For You recommendation and promotion data is fetched from FAST
 * On Now and Up Next rails are fetched separated and send via InformationBus mechanism
 *
 * @author Dejan Nadj
 */
class ForYouInterfaceImpl(
    private val serviceImpl: IServiceAPI,
    private val tvInterface: TvInterface,
    private val epgInterfaceImpl: EpgInterface,
    private val epgDataProvider: EpgDataProvider,
    private val watchlistInterfaceImpl: WatchlistInterface,
    private var pvrInterfaceImpl: PvrInterface,
    private var utilsInterfaceImpl: UtilsInterface,
    private var recommendationInterfaceImpl: RecommendationInterface,
    private var schedulerInterface: SchedulerInterface
) : ForYouInterface {

    private val rails: ArrayList<RailItem> = ArrayList()
    var mRailDataList = ConcurrentHashMap<Int, RailItem>()
    private var recommendationRowData = ArrayList<RecommendationRow>()
    val railsTemp: ArrayList<RailItem> = ArrayList()
    var callbackResult: IAsyncDataCallback<ArrayList<RailItem>>? = null
    var pvrEnable: Boolean = false
    private var recommendationInvoked = false
    private var recommendationRowCount = 0
    val TAG = javaClass.simpleName
    private val nowRailId = 0
    private val nextRailId = 1
    private val watchlistRailId = 2
    private val radioRailId = 3
    private val recordedRailId = 4
    private val scheduleRecordingRailId = 5
    private val railIds = arrayListOf(
        nowRailId,
        nextRailId,
        watchlistRailId,
        radioRailId,
        recordedRailId,
        scheduleRecordingRailId
    )

    private var regionSupported = true

    init {
        var eventReceiver: Any? = null
        InformationBus.informationBusEventListener.registerEventListener(
            arrayListOf(Events.ANOKI_REGION_NOT_SUPPORTED),
            {
                eventReceiver = it
            },
            {
                regionSupported = false
                InformationBus.informationBusEventListener.unregisterEventListener(eventReceiver!!)
            })
        InformationBus.informationBusEventListener.registerEventListener(
            arrayListOf(Events.FAST_DATA_UPDATED),
            {},
            {
               recommendationRowData.clear()
            })
    }

    override fun setup() {
    }

    override fun dispose() {
    }

    override fun getAvailableRailSize(): Int {
        return rails.size
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getForYouRails(callback: IAsyncDataCallback<ArrayList<RailItem>>) {
        this.callbackResult = callback
        updateRailData()
    }

    private fun updateOnNowUpNextRails() {
        //On now
        getForYouOnNowEvents(object :
            IAsyncDataCallback<ArrayList<TvEvent>> {
            override fun onReceive(data: ArrayList<TvEvent>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateOnNowUpNextRails onReceive")
                var events = arrayListOf<TvEvent>()
                data.forEach { item ->
                    //invisible channel should not be present in for you scene
                    if (item.tvChannel.isBroadcastChannel() && item.tvChannel.isBrowsable) {
                        events.add(item)
                    }
                }
                if (events.isNotEmpty()) {
                    rails.add(RailItem(
                        recommendationRowCount + nowRailId,
                        utilsInterfaceImpl.getStringValue("on_now"),
                        events.toMutableList(),
                        RailItem.RailItemType.EVENT
                    ))
                }
                getUpNextRail()
            }

            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateOnNowUpNextRails onFailed")
                getUpNextRail()
            }
        })
    }

    private fun getUpNextRail() {
        //Up next
        getForYouNextEvents(object :
            IAsyncDataCallback<ArrayList<TvEvent>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getUpNextRail failed")
                InformationBus.informationBusEventListener.submitEvent(Events.FOR_YOU_NOW_NEXT_UPDATED, arrayListOf(rails))
            }

            override fun onReceive(data: ArrayList<TvEvent>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getUpNextRail onReceive")
                var events = arrayListOf<TvEvent>()
                data.forEach { item ->
                    //invisible channel should not be present in for you scene
                    if (item.tvChannel.isBroadcastChannel() && item.tvChannel.isBrowsable) {
                        events.add(item)
                    }
                }
                if (events.isNotEmpty()) {
                    rails.add(RailItem(
                        recommendationRowCount + nextRailId,
                        utilsInterfaceImpl.getStringValue("up_next"),
                        events.toMutableList(),
                        RailItem.RailItemType.EVENT
                    ))
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "MAL_SERVICE_FOR_YOU_UP_NEXT_UPDATED")
                InformationBus.informationBusEventListener.submitEvent(Events.FOR_YOU_NOW_NEXT_UPDATED, arrayListOf(rails))
            }
        })
    }

    private fun getForYouOnNowEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        epgInterfaceImpl.getAllCurrentEvent(callback)
    }

    private fun getForYouNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        epgInterfaceImpl.getAllNextEvents(callback)
    }

    private fun getForYouWatchlistEvents(callback: IAsyncDataCallback<ArrayList<ScheduledReminder>>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouWatchlistEvents")
        val retVal: ArrayList<ScheduledReminder> = ArrayList()
        val counter = AtomicInteger(0)
        watchlistInterfaceImpl.getWatchListCount(object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
                callback.onFailed(Error("null"))
            }

            override fun onReceive(data: Int) {
                if (data > 0) {
                    watchlistInterfaceImpl.getWatchList(object :
                        IAsyncDataCallback<MutableList<ScheduledReminder>> {


                        @RequiresApi(Build.VERSION_CODES.S)
                        override fun onReceive(data: MutableList<ScheduledReminder>) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"FORYou", "data of watchlist: ${data.size}")
                            val channelList = tvInterface.getChannelList()
                            data.forEach { scheduledReminder ->
                                //filtering channel of scheduled reminder because they can also be deleted from somewhere
                                for (i in 0 until channelList.size) {
                                    val channel: TvChannel = channelList[i]
                                    if (scheduledReminder.tvChannelId == channel.id) {
                                        retVal.add(scheduledReminder)
                                        break
                                    }
                                }

                            }
                            if (retVal.size > 0) {
                                callback.onReceive(retVal)
                            } else {
                                callback.onFailed(Error("null"))
                            }
                        }

                        override fun onFailed(error: Error) {
                            if (counter.incrementAndGet() == data) {
                                if (retVal.size > 0) {
                                    callback.onReceive(retVal)
                                } else {
                                    callback.onFailed(Error("null"))
                                }
                            }
                        }

                    })
                } else {
                    callback.onFailed(Error("null"))
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getForYouRadioChannels(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouRadioChannels")
        var radioChannels = ArrayList<TvChannel>()
        val retVal: ArrayList<TvEvent> = ArrayList()
        var counter = AtomicInteger(0)

        tvInterface.getChannelList().forEach { tvChannel ->
            if (tvChannel.isRadioChannel) {
                radioChannels.add(tvChannel)
            }
        }

        if (radioChannels.isNotEmpty()) {
            for (i in 0 until radioChannels.size) {
                epgInterfaceImpl.getCurrentEvent(
                    radioChannels.get(i),
                    object : IAsyncDataCallback<TvEvent> {

                        override fun onReceive(data: TvEvent) {
                            retVal.add(data)
                            if (counter.incrementAndGet() == radioChannels.size) {
                                if (retVal.size > 0) {
                                    callback.onReceive(retVal)
                                } else {
                                    callback.onFailed(Error("null"))
                                }
                            }
                        }

                        override fun onFailed(error: Error) {
                            if (counter.incrementAndGet() == radioChannels.size) {
                                if (retVal.size > 0) {
                                    callback.onReceive(retVal)
                                } else {
                                    callback.onFailed(Error("null"))
                                }
                            }
                        }
                    })
            }
        } else {
            callback.onFailed(Error("null"))
        }
    }

    private fun getForYouRecordedEvents(callback: IAsyncDataCallback<ArrayList<Recording>>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouRecordedEvents")
        val retVal: ArrayList<Recording> = ArrayList()
        var counter = AtomicInteger(0)

        pvrInterfaceImpl.getRecordingList(object :
            IAsyncDataCallback<List<Recording>> {
            override fun onFailed(error: Error) {
                if (retVal.size > 0 && (counter.get() == retVal.size)) {
                    callback.onReceive(retVal)
                } else {
                    callback.onFailed(Error("null"))
                }
            }

            override fun onReceive(data: List<Recording>) {
                var data1: ArrayList<Recording> = data as ArrayList<Recording>

                data1.forEach { item ->
                    retVal.add(item)
                    counter.incrementAndGet()
                }

                if (retVal.size > 0 && (counter.get() == retVal.size)) {
                    callback.onReceive(retVal)
                } else {
                    callback.onFailed(Error("null"))
                }
            }
        })
    }

    private fun getForYouScheduledRecordingEvents(callback: IAsyncDataCallback<ArrayList<ScheduledRecording>>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouScheduledRecordingEvents")
        val retVal: ArrayList<ScheduledRecording> = ArrayList()
        val counter = AtomicInteger(0)
        schedulerInterface.getScheduledRecListCount(object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
                callback.onFailed(Error("null"))
            }

            override fun onReceive(data: Int) {
                if (data > 0) {
                    schedulerInterface.getRecList(object :
                        IAsyncDataCallback<MutableList<ScheduledRecording>> {


                        @RequiresApi(Build.VERSION_CODES.S)
                        override fun onReceive(data: MutableList<ScheduledRecording>) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"FORYou", "data of recordings: ${data.size}")
                            val channelList = tvInterface.getChannelList()
                            data.forEach { scheduledRecording ->
                                //filtering channel of scheduled reminder because they can also be deleted from somewhere
                                for (i in 0 until channelList.size) {
                                    val channel: TvChannel = channelList[i]
                                    if (scheduledRecording.tvChannelId == channel.id) {
                                        retVal.add(scheduledRecording)
                                        break
                                    }
                                }

                            }
                            if (retVal.size > 0) {
                                callback.onReceive(retVal)
                            } else {
                                callback.onFailed(Error("null"))
                            }
                        }

                        override fun onFailed(error: Error) {
                            if (counter.incrementAndGet() == data) {
                                if (retVal.size > 0) {
                                    callback.onReceive(retVal)
                                } else {
                                    callback.onFailed(Error("null"))
                                }
                            }
                        }

                    })
                } else {
                    callback.onFailed(Error("null"))
                }
            }
        })
    }

    private fun checkIfReadyForRefresh(counter: AtomicInteger, counterSize: Int): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkIfReadyForRefresh: ${counter.get()} ${counterSize}")
        if (counter.incrementAndGet() == counterSize) {
            rails.clear()
            railsTemp.clear()
            mRailDataList.forEach {
                railsTemp.add(it.value)
            }
            rails.addAll(railsTemp)
            callbackResult!!.onReceive(rails)
            //Update On Now and Up Next rows
            try {
                rails.first { utilsInterfaceImpl.getStringValue("on_now") == it.railName }
            } catch (e: NoSuchElementException) {
                updateOnNowUpNextRails()
            }
            return true
        }
        return false
    }

    override fun setPvrEnabled(pvrEnabled: Boolean) {
        this.pvrEnable = pvrEnabled
    }

    fun getRecommendationChannelById(
        channelId: String,
        channels: MutableList<TvChannel>
    ): TvChannel {
        channels.forEach { channel ->
            if ((channel.displayNumber == channelId) && channel.isFastChannel()) return channel
        }
        return getFastChannel(channels)
    }

    private fun getRecommendations(callback: () -> Unit) {
        if (!ForYouInterface.ENABLE_FAST_DATA || !regionSupported) {
            callback.invoke()
            return
        }
        if (recommendationInvoked) {
            return
        }

        recommendationInvoked = true
        recommendationRowCount = 0
        if (recommendationRowData.isNotEmpty()) {
            updateRecommendations(recommendationRowData, callback)
            return
        }

        recommendationInterfaceImpl.getRecommendationRows(object :
            IAsyncDataCallback<ArrayList<RecommendationRow>> {
            override fun onFailed(error: Error) {
                callback.invoke()
                recommendationInvoked = false
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onReceive(recommendationRows: ArrayList<RecommendationRow>) {
                recommendationRowData.clear()
                recommendationRowData.addAll(recommendationRows)
                recommendationRowCount = recommendationRows.size
                updateRecommendations(recommendationRows, callback)
            }
        })
    }

    private fun updateRecommendations(recommendationRows: ArrayList<RecommendationRow>, callback: ()->Unit) {
        recommendationRowCount = recommendationRows.size
        var rowId = 0
        val channels = tvInterface.getChannelList(ApplicationMode.FAST_ONLY)
        recommendationRows.toList().forEach { recommendationRow ->
            var itemList = arrayListOf<TvEvent>()
            recommendationRow.items.toList().forEach { recommendationItem ->
                try {
                    var tvChannel =
                        if (recommendationItem.channelId != null) {
                            getRecommendationChannelById(
                                recommendationItem.channelId,
                                channels
                            )
                        } else {
                            getFastChannel(channels)
                        }
                    /**
                     * VOD data is of two types, hence checking if recommended data is of VOD or not
                     */
                    val isVod = recommendationItem.type == "series" || recommendationItem.type == "single-work"
                    val title =
                        if (recommendationItem.type == "guide") "Explore FREE TV Guide" else recommendationItem.title

                    if (recommendationItem.type == "guide") {
                        tvChannel = tvChannel.copy()
                        tvChannel.ordinalNumber = 0
                    }
                    /**
                     * TvEvent id default value is 0 and for VOD data it is contentId.
                     * We are using this field to handle navigation for VOD separately from Discovery
                     */
                    itemList.add(
                        TvEvent(
                            if(isVod) recommendationItem.contentId.toInt() else 0,
                            tvChannel,
                            title,
                            recommendationItem.description,
                            recommendationItem.description,
                            recommendationItem.thumbnail,
                            recommendationItem.startTimeEpoch * 1000L,
                            (recommendationItem.startTimeEpoch * 1000L) + (recommendationItem.durationSec * 1000L),
                            arrayListOf(),
                            0,
                            0,
                            recommendationItem.type,
                            "",
                            false,
                            false,
                            0,
                            "",
                            ""
                        )
                    )
                } catch (E: Exception) {
                    println(E)
                }
            }
            itemList.sortBy { tvEvent -> tvEvent.tvChannel.ordinalNumber }
            mRailDataList[rowId] = RailItem(
                rowId,
                recommendationRow.name,
                itemList.toMutableList(),
                RailItem.RailItemType.EVENT
            )
            rowId++
        }
        recommendationInvoked = false
        callback.invoke()
    }

    private fun getFastChannel(channelList: MutableList<TvChannel>): TvChannel {
        var tvChannel: TvChannel? = null
        channelList.forEach { channel ->
            if (channel.isFastChannel()) {
                tvChannel = channel
                return@forEach
            }
        }
        return tvChannel!!
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    override fun updateRailData() {
        try {
            mRailDataList.clear()
        } catch (E: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateRailData: ${E.printStackTrace()}")
            mRailDataList = ConcurrentHashMap<Int, RailItem>()
        }
        thread {
            getRecommendations {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getRecommendations recommendationRowCount: ${recommendationRowCount}")
                val railSize = recommendationRowCount + railIds.size - 2
                val counter = AtomicInteger(recommendationRowCount)
                railsTemp.clear()

                //Watchlist
                getForYouWatchlistEvents(object :
                    IAsyncDataCallback<ArrayList<ScheduledReminder>> {
                    override fun onFailed(error: Error) {
                        checkIfReadyForRefresh(counter, railSize)
                    }

                    override fun onReceive(data: ArrayList<ScheduledReminder>) {
                        val eventList = ArrayList<TvEvent>()
                        val index = AtomicInteger(0)
                        data.forEach { scheduledReminder ->
                            epgInterfaceImpl.getEventByNameAndStartTime(
                                scheduledReminder.name,
                                scheduledReminder.startTime,
                                scheduledReminder.tvChannelId!!,
                                object : IAsyncDataCallback<TvEvent> {
                                    override fun onReceive(tvEvent: TvEvent) {
                                        eventList.add(tvEvent)
                                        if (index.incrementAndGet() >= data.size) {
                                            mRailDataList[recommendationRowCount + watchlistRailId] =
                                                RailItem(
                                                    recommendationRowCount + watchlistRailId,
                                                    utilsInterfaceImpl.getStringValue("watchlist"),
                                                    eventList.toMutableList(),
                                                    RailItem.RailItemType.EVENT
                                                )
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouWatchlistEvents:}")
                                            checkIfReadyForRefresh(counter, railSize)
                                        }
                                    }

                                    override fun onFailed(error: Error) {
                                        if (index.incrementAndGet() >= data.size) {
                                            mRailDataList[recommendationRowCount + watchlistRailId] =
                                                RailItem(
                                                    recommendationRowCount + watchlistRailId,
                                                    utilsInterfaceImpl.getStringValue("watchlist"),
                                                    eventList.toMutableList(),
                                                    RailItem.RailItemType.EVENT
                                                )
                                            checkIfReadyForRefresh(counter, railSize)
                                        }
                                    }
                                })
                        }
                    }
                })

                getForYouRadioChannels(object :
                    IAsyncDataCallback<ArrayList<TvEvent>> {
                    override fun onFailed(error: Error) {
                        checkIfReadyForRefresh(counter, railSize)
                    }

                    override fun onReceive(data: ArrayList<TvEvent>) {
                        var events = mutableListOf<Any>()
                        data.forEach { item ->
                            //invisible channel should not be present in for you scene
                            if (item.tvChannel.isBrowsable) {
                                events.add(item)
                            }
                        }
                        mRailDataList[recommendationRowCount + radioRailId] = RailItem(
                            recommendationRowCount + radioRailId,
                            utilsInterfaceImpl.getStringValue("on_now_radio"),
                            events,
                            RailItem.RailItemType.EVENT
                        )
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouRadioChannels:}")
                        checkIfReadyForRefresh(counter, railSize)
                    }
                })

                //Recorded
                if (pvrEnable) {
                    getForYouRecordedEvents(object :
                        IAsyncDataCallback<ArrayList<Recording>> {

                        override fun onFailed(error: Error) {
                            if (checkIfReadyForRefresh(counter, railSize)) {
                                callbackResult?.onReceive(rails)
                            }
                        }

                        override fun onReceive(data: ArrayList<Recording>) {
                            mRailDataList[recommendationRowCount + recordedRailId] = RailItem(
                                recommendationRowCount + recordedRailId,
                                utilsInterfaceImpl.getStringValue("recorded"),
                                data.toMutableList(),
                                RailItem.RailItemType.RECORDING
                            )

                            if (checkIfReadyForRefresh(counter, railSize)) {
                                callbackResult?.onReceive(rails)
                            }
                        }
                    })
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouRecordedEvents:}")
                    if (checkIfReadyForRefresh(counter, railSize)) {
                        callbackResult?.onReceive(rails)
                    }
                }

                getForYouScheduledRecordingEvents(object :
                    IAsyncDataCallback<ArrayList<ScheduledRecording>> {
                    override fun onFailed(error: Error) {
                        checkIfReadyForRefresh(counter, railSize)
                    }

                    override fun onReceive(data: ArrayList<ScheduledRecording>) {
                        val recordings = ArrayList<TvEvent>()
                        val index = AtomicInteger(0)
                        data.forEach { scheduledRecording ->
                            epgInterfaceImpl.getEventByNameAndStartTime(
                                scheduledRecording.name,
                                scheduledRecording.scheduledDateStart,
                                scheduledRecording.tvChannel!!.id,
                                object : IAsyncDataCallback<TvEvent> {
                                    override fun onReceive(tvEvent: TvEvent) {
                                        recordings.add(tvEvent)
                                        if (index.incrementAndGet() >= data.size) {
                                            mRailDataList[recommendationRowCount + scheduleRecordingRailId] =
                                                RailItem(
                                                    recommendationRowCount + scheduleRecordingRailId,
                                                    utilsInterfaceImpl.getStringValue("scheduled"),
                                                    recordings.toMutableList(),
                                                    RailItem.RailItemType.EVENT
                                                )
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouScheduledRecordingEvents:}")
                                            checkIfReadyForRefresh(counter, railSize)
                                        }
                                    }

                                    override fun onFailed(error: Error) {
                                        if (index.incrementAndGet() >= data.size) {
                                            mRailDataList[recommendationRowCount + scheduleRecordingRailId] =
                                                RailItem(
                                                    recommendationRowCount + scheduleRecordingRailId,
                                                    utilsInterfaceImpl.getStringValue("scheduled"),
                                                    recordings.toMutableList(),
                                                    RailItem.RailItemType.EVENT
                                                )
                                            checkIfReadyForRefresh(counter, railSize)
                                        }
                                    }
                                }
                            )
                        }
                    }
                })
            }
        }
    }
}