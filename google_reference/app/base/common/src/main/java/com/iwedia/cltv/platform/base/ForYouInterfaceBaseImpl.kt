package com.iwedia.cltv.platform.base

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.ForYouInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.model.Constants
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
 * For You Interface Base Implementation
 */
open class ForYouInterfaceBaseImpl(
    val context: Context,
    private var epgInterfaceImpl: EpgInterface,
    private var channelDataProvider: ChannelDataProviderInterface,
    private var watchlistInterfaceImpl: WatchlistInterface,
    private var pvrInterfaceImpl: PvrInterface,
    private var utilsInterfaceImpl: UtilsInterface,
    private var recommendationInterfaceImpl: RecommendationInterface,
    private var schedulerInterface: SchedulerInterface
) : ForYouInterface {

    protected val rails: ArrayList<RailItem> = ArrayList()
    var mRailDataList = ConcurrentHashMap<Int, RailItem>()
    val railsTemp: ArrayList<RailItem> = ArrayList()
    var callbackResult: IAsyncDataCallback<ArrayList<RailItem>>? = null
    var pvrEnable: Boolean = false
    protected var recommendationInvoked = false
    protected var recommendationRowCount = 0
    val TAG = javaClass.simpleName
    protected val nowRailId = 0
    protected val nextRailId = 1
    protected val watchlistRailId = 2
    protected val radioRailId = 3
    protected val promoRailId = 4
    protected val recordedRailId = 5
    protected val scheduleRecordingRailId = 6
    protected val railIds = arrayListOf(
        nowRailId,
        nextRailId,
        watchlistRailId,
        radioRailId,
        promoRailId,
        recordedRailId,
        scheduleRecordingRailId
    )

    protected var regionSupported = true
    init {
        var eventReceiver: Any ?= null
        InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.ANOKI_REGION_NOT_SUPPORTED), {
            eventReceiver = it
        }, {
            regionSupported = false
            InformationBus.informationBusEventListener.unregisterEventListener(eventReceiver!!)
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

    protected fun getForYouOnNowEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        epgInterfaceImpl.getAllCurrentEvent(callback)
    }

    protected fun getForYouWatchlistEvents(callback: IAsyncDataCallback<ArrayList<ScheduledReminder>>) {
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
                            val channelList = channelDataProvider.getChannelList()
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

    protected fun getForYouNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        epgInterfaceImpl.getAllNextEvents(callback)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    protected fun getForYouRadioChannels(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        var radioChannels = ArrayList<TvChannel>()
        val retVal: ArrayList<TvEvent> = ArrayList()
        var counter = AtomicInteger(0)

        channelDataProvider.getChannelList().forEach { tvChannel ->
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

    protected open fun getForYouRecordedEvents(callback: IAsyncDataCallback<ArrayList<Recording>>) {
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

    protected fun getForYouScheduledRecordingEvents(callback: IAsyncDataCallback<ArrayList<ScheduledRecording>>) {
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
                            val channelList = channelDataProvider.getChannelList()
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

    @RequiresApi(Build.VERSION_CODES.S)
    protected fun getForYouPromoChannels(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        val retVal: ArrayList<TvEvent> = ArrayList()
        channelDataProvider.getChannelList().forEach { tvChannel ->
            val appLinkIntentUri = tvChannel.appLinkIntentUri
            val appLinkText = tvChannel.appLinkText

            if (appLinkText.isNotEmpty() && appLinkIntentUri.isNotEmpty()) {
                val tvEvent = TvEvent.createAppLinkCardEvent(tvChannel)

                retVal.add(tvEvent)
            }
        }
        if (retVal.size > 0) {
            callback.onReceive(retVal)
        } else {
            callback.onFailed(Error("null"))
        }
    }

    private fun checkIfReadyForRefresh(counter: AtomicInteger, counterSize: Int): Boolean {
        if (counter.incrementAndGet() == counterSize) {
            rails.clear()
            railsTemp.clear()
            mRailDataList.forEach {
                railsTemp.add(it.value)
            }
            rails.addAll(railsTemp)
            callbackResult!!.onReceive(rails)
            return true
        }
        return false
    }

    override fun setPvrEnabled(pvrEnabled: Boolean) {
        this.pvrEnable = pvrEnabled
    }

    fun getRecommendationChannelById(channelId: String,channels: MutableList<TvChannel>): TvChannel {
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
        recommendationInterfaceImpl.getRecommendationRows(object : IAsyncDataCallback<ArrayList<RecommendationRow>> {
            override fun onFailed(error: Error) {
                callback.invoke()
                recommendationInvoked = false
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onReceive(recommendationRows: ArrayList<RecommendationRow>) {
                recommendationRowCount = recommendationRows.size
                var rowId = 0
                val channels = channelDataProvider.getChannelList()
                recommendationRows.toList().forEach { recommendationRow ->
                     var itemList = arrayListOf<TvEvent>()
                        recommendationRow.items.toList().forEach { recommendationItem ->
                            try {
                                var tvChannel =
                                    if (recommendationItem.channelId != null) {
                                        getRecommendationChannelById(recommendationItem.channelId, channels)
                                    } else
                                    {
                                        getFastChannel(channels)
                                    }
                                val title =
                                    if (recommendationItem.type == "guide") "Explore FREE TV Guide" else recommendationItem.title

                                if (recommendationItem.type == "guide") {
                                    tvChannel = tvChannel.copy()
                                    tvChannel.ordinalNumber = 0
                                }
                                itemList.add(
                                    TvEvent(
                                        0,
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
        })
    }

    protected fun getFastChannel(channelList : MutableList<TvChannel>): TvChannel {
        var tvChannel : TvChannel? = null
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
                val railSize = recommendationRowCount + railIds.size
                val counter = AtomicInteger(recommendationRowCount)
                railsTemp.clear()

                //On now
                getForYouOnNowEvents(object :
                    IAsyncDataCallback<ArrayList<TvEvent>> {
                    override fun onReceive(data: ArrayList<TvEvent>) {
                        var events = arrayListOf<TvEvent>()
                        data.forEach { item->
                            //invisible channel should not be present in for you scene
                            if (item.tvChannel.isBroadcastChannel() && item.tvChannel.isBrowsable) {
                                events.add(item)
                            }
                        }
                        if (events.isNotEmpty()) {
                            mRailDataList[recommendationRowCount + nowRailId] = RailItem(
                                recommendationRowCount + nowRailId,
                                utilsInterfaceImpl.getStringValue("on_now"),
                                events.toMutableList(),
                                RailItem.RailItemType.EVENT
                            )
                        }

                        checkIfReadyForRefresh(counter, railSize)
                    }

                    override fun onFailed(error: Error) {
                        checkIfReadyForRefresh(counter, railSize)
                    }
                })

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
                                scheduledReminder.name, scheduledReminder.startTime, scheduledReminder.tvChannelId!!,
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

                //Up next
                getForYouNextEvents(object :
                    IAsyncDataCallback<ArrayList<TvEvent>> {
                    override fun onFailed(error: Error) {
                        checkIfReadyForRefresh(counter, railSize)
                    }

                    override fun onReceive(data: ArrayList<TvEvent>) {
                        var events = arrayListOf<TvEvent>()
                        data.forEach { item->
                            //invisible channel should not be present in for you scene
                            if (item.tvChannel.isBroadcastChannel() && item.tvChannel.isBrowsable) {
                                events.add(item)
                            }
                        }
                        if (events.isNotEmpty()) {
                            mRailDataList[recommendationRowCount + nextRailId] = RailItem(
                                recommendationRowCount + nextRailId,
                                utilsInterfaceImpl.getStringValue("up_next"),
                                events.toMutableList(),
                                RailItem.RailItemType.EVENT
                            )
                        }

                        checkIfReadyForRefresh(counter, railSize)
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
                        checkIfReadyForRefresh(counter, railSize)
                    }
                })

                getForYouPromoChannels(object :
                    IAsyncDataCallback<ArrayList<TvEvent>> {
                    override fun onFailed(error: Error) {
                        checkIfReadyForRefresh(counter, railSize)
                    }

                    override fun onReceive(data: ArrayList<TvEvent>) {
                        mRailDataList[recommendationRowCount + promoRailId] = RailItem(
                            recommendationRowCount + promoRailId,
                            utilsInterfaceImpl.getStringValue("for_you_promo"),
                            data.toMutableList(),
                            RailItem.RailItemType.EVENT
                        )
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
                                scheduledRecording.name, scheduledRecording.scheduledDateStart, scheduledRecording.tvChannel!!.id,
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