package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.media.tv.TvInputManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.ForYouInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.ForYouInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.RecommendationInterface
import com.iwedia.cltv.platform.`interface`.SchedulerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
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
import com.iwedia.cltv.platform.refplus5.provider.ChannelDataProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

internal class ForYouInterfaceImpl(
    applicationContext: Context,
    private var epgInterfaceImpl: EpgInterface,
    private var channelDataProvider: ChannelDataProvider,
    private var watchlistModule: WatchlistInterface,
    private var pvrModule: PvrInterface,
    private var utilsModule: UtilsInterface,
    private var recommendationInterface: RecommendationInterface,
    private var schedulerInterface: SchedulerInterface
) :
    ForYouInterfaceBaseImpl(applicationContext, epgInterfaceImpl, channelDataProvider,watchlistModule,pvrModule, utilsModule,recommendationInterface, schedulerInterface) {

    protected var mTvInputManager: TvInputManager

    init {
        mTvInputManager = applicationContext.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
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
                    rails.add(
                        RailItem(
                        recommendationRowCount + nowRailId,
                            utilsModule.getStringValue("on_now"),
                        events.toMutableList(),
                        RailItem.RailItemType.EVENT
                    )
                    )
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
                        utilsModule.getStringValue("up_next"),
                        events.toMutableList(),
                        RailItem.RailItemType.EVENT
                    ))
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "FOR_YOU_UP_NEXT_UPDATED")
                InformationBus.informationBusEventListener.submitEvent(Events.FOR_YOU_NOW_NEXT_UPDATED, arrayListOf(rails))
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
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

        recommendationInterface.getRecommendationRows(object :
            IAsyncDataCallback<ArrayList<RecommendationRow>> {
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
        })
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
                                                    utilsModule.getStringValue("watchlist"),
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
                                                    utilsModule.getStringValue("watchlist"),
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
                            utilsModule.getStringValue("on_now_radio"),
                            events,
                            RailItem.RailItemType.EVENT
                        )
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouRadioChannels:}")
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
                            utilsModule.getStringValue("for_you_promo"),
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
                                utilsModule.getStringValue("recorded"),
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
                                                    utilsModule.getStringValue("scheduled"),
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
                                                    utilsModule.getStringValue("scheduled"),
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun isEventCurrentlyLocked(event : TvEvent) : Boolean {
        for (rating in mTvInputManager.blockedRatings) {
            if((event.parentalRating != null) && (event.parentalRating != "null")
                && (rating.mainRating != null) && (rating.mainRating != "null")) {
                //Because MTK uses random domain for event rating and rating lock
                //just check if main rating is present
                if ((event.parentalRating as String).contains(rating.mainRating)) {
                    return true
                }
            }
        }
        return false
    }


    override fun getForYouRecordedEvents(callback: IAsyncDataCallback<ArrayList<Recording>>) {
        val retVal: ArrayList<Recording> = ArrayList()
        var counter = AtomicInteger(0)

        pvrModule.getRecordingList(object :
            IAsyncDataCallback<List<Recording>> {
            override fun onFailed(error: Error) {
                if (retVal.size > 0 && (counter.get() == retVal.size)) {
                    callback.onReceive(retVal)
                } else {
                    callback.onFailed(Error("null"))
                }
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onReceive(data: List<Recording>) {
                var data1: ArrayList<Recording> = data as ArrayList<Recording>
                if((utilsModule.getCountryPreferences(UtilsInterface.CountryPreference.HIDE_LOCKED_RECORDINGS,false) as Boolean)) {
                    var activeChannelIndex =
                        utilsModule.getPrefsValue(TvInterfaceImpl.activeChannelTag, 0) as Int
                    val channelList = channelDataProvider.getChannelList()
                    var activeChannel: TvChannel
                    if (activeChannelIndex >= channelList.size || activeChannelIndex <= 0) {
                        activeChannel = channelList[0]
                    } else {
                        activeChannel = channelList[activeChannelIndex]
                    }

                    data1.forEach { item ->
                        //remove PVR recording from list if recording channel is locked
                        var channelLocked = false
                        if(item.tvEvent?.tvChannel?.isLocked != null) {
                            channelLocked = item.tvEvent!!.tvChannel.isLocked
                        }

                        var channelUnlocked = false
                        if(activeChannel.channelId == item.tvEvent!!.tvChannel.channelId &&
                            (utilsModule as UtilsInterfaceImpl).isChannelUnlocked()) {
                            channelUnlocked = true
                        }

                        var eventParentalLocked = false
                        if(item.tvEvent != null) {
                            eventParentalLocked =  isEventCurrentlyLocked(item.tvEvent!!)
                        }

                        var parentalEnabled = mTvInputManager.isParentalControlsEnabled

                        if(((!channelLocked || channelUnlocked) && !eventParentalLocked) || !parentalEnabled) {
                            retVal.add(item)
                            counter.incrementAndGet()
                        }
                    }
                }
                else {
                    data1.forEach { item ->
                        if(item.tvEvent != null) {
                            item.isEventLocked = isEventCurrentlyLocked(item.tvEvent!!)
                        }
                        retVal.add(item)
                        counter.incrementAndGet()
                    }
                }

                if (retVal.size > 0 && (counter.get() == retVal.size)) {
                    callback.onReceive(retVal)
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
                rails.first { utilsModule.getStringValue("on_now") == it.railName }
            } catch (e: NoSuchElementException) {
                updateOnNowUpNextRails()
            }
            return true
        }
        return false
    }
}