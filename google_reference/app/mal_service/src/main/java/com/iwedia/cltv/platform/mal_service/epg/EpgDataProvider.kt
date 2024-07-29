package com.iwedia.cltv.platform.mal_service.epg

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import kotlin.concurrent.thread

class EpgDataProvider(var context: Context): EpgDataProviderInterface {

    private val TAG = javaClass.simpleName
    protected var events = Collections.synchronizedMap(HashMap<String, ArrayList<TvEvent>>())
    private val EPG_UPDATE_TIMEOUT = 5000L
    private var epgUpdateTimer: CountDownTimer? = null
    private lateinit var epgObserver: ContentObserver

    init {
        epgObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                startEpgUpdateTimer()
            }
        }

        context.contentResolver.registerContentObserver(
            TvContract.Programs.CONTENT_URI,
            true,
            epgObserver
        )
    }

    override fun dispose() {
        stopEpgUpdateTimer()
        if (epgObserver != null) {
            context.contentResolver.unregisterContentObserver(epgObserver)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun loadIntermittentEvents() {
        // Wait for event loading
        EventsLoadedEventListener(object : IAsyncCallback {
            override fun onFailed(error: Error) {
            }

            override fun onSuccess() {
                InformationBus.informationBusEventListener.submitEvent(Events.EPG_DATA_UPDATED)
            }
        })
        loadEvents()
    }

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    open fun loadEvents() {
        CoroutineHelper.runCoroutine({
            val retEvents = HashMap<String, ArrayList<TvEvent>>()
            val contentResolver: ContentResolver = context.contentResolver
            val inputList = getInputIds(context)
            if (inputList!!.isNotEmpty()) {
                for (input in inputList) {
                    //Skip google movies events
                    if (input.contains("com.google.android.videos")) {
                        continue
                    }
                    val channelCursor = contentResolver.query(
                        TvContract.buildChannelsUriForInput(input),
                        null,
                        null,
                        null,
                        null
                    )
                    if (channelCursor!!.count > 0) {
                        channelCursor.moveToFirst()
                        do {
                            val tvChannel = createChannelFromCursor(context, channelCursor)
                            if (tvChannel != null) {
                                val uri: Uri = TvContract.buildProgramsUriForChannel(
                                    tvChannel.id.toLong()
                                )
                                //Create query
                                var epgCursor: Cursor? = null
                                epgCursor = context.contentResolver
                                    .query(uri, null, null, null, null)

                                val eventList = arrayListOf<TvEvent>()
                                if (epgCursor != null) {
                                    epgCursor.moveToFirst()
                                    var endTime = 0L
                                    var firstEvent = true
                                    while (!epgCursor.isAfterLast) {
                                        val event = createTvEventFromCursor(tvChannel, epgCursor)
                                        //Remove overlapped items
                                        if (!firstEvent && event.startTime< endTime) {
                                            firstEvent = false
                                            epgCursor.moveToNext()
                                        } else {
                                            firstEvent = false
                                            if (event.endTime > endTime) {
                                                eventList.add(event)
                                            }
                                            endTime = event.endTime
                                            epgCursor.moveToNext()
                                        }
                                    }
                                    epgCursor.close()
                                }
                                retEvents[tvChannel.getUniqueIdentifier()] = eventList
                            }
                        } while (channelCursor!!.moveToNext())
                    }
                    channelCursor!!.close()
                }
                events.clear()
                events = retEvents
                InformationBus.informationBusEventListener.submitEvent(Events.EVENTS_LOADED)
            } else {
                InformationBus.informationBusEventListener.submitEvent(Events.EVENTS_LOADED)
            }
        })
    }

    override fun getEventListByChannelStartAndEndTime(
        tvChannel: TvChannel,
        startTime: Long,
        endTime: Long,
        callback: IAsyncDataCallback<ArrayList<TvEvent>>
    ) {
        val uri: Uri = TvContract.buildProgramsUriForChannel(
            tvChannel.id.toLong(),
            startTime,
            endTime
        )
        //Create query
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver
                .query(uri, null, null, null, null)
        } catch (e: Exception) {
            //For this query system permission is necessary
            //Throw security exception
            if (callback != null) {
                callback.onFailed(java.lang.Error("No current event"))
                return
            }
        }

        if (cursor == null) {
            callback.onFailed(java.lang.Error("No current event"))
            return
        }
        var retList = ArrayList<TvEvent>()
        cursor!!.moveToFirst()
        var endTime = 0L
        var firstEvent = true
        while (!cursor.isAfterLast) {
            val cd = createTvEventFromCursor(tvChannel, cursor!!)
            //Remove overlapped items
            if (!firstEvent && cd.startTime< endTime) {
                firstEvent = false
                cursor.moveToNext()
            } else {
                firstEvent = false
                if (cd.startTime>= startTime && cd.endTime > endTime) {
                    retList.add(cd)
                }
                endTime = cd.endTime
                cursor.moveToNext()
            }
        }
        callback.onReceive(retList)
    }

    protected fun addEvent(tvEvent: TvEvent) {
        if (events.contains(tvEvent.tvChannel.getUniqueIdentifier())) {
            events[tvEvent.tvChannel.getUniqueIdentifier()]!!.add(tvEvent)
        } else {
            var list = arrayListOf<TvEvent>()
            list.add(tvEvent)
            events.put(tvEvent.tvChannel.getUniqueIdentifier(), list)
        }

    }

    @Synchronized
    override fun getEventChannelMap(): HashMap<String, ArrayList<TvEvent>> {
        var retEvents = HashMap<String, ArrayList<TvEvent>>()
        events.entries.toList().forEach { entry->
            retEvents[entry.key] = entry.value
        }
        return retEvents
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    override fun getEventList(): ArrayList<TvEvent> {
        val eventList = arrayListOf<TvEvent>()
        val toList = events.values.toList()
        toList.forEach { list ->
            eventList.addAll(list)
        }
        return eventList
    }

    /**
     * Stop epg data update timer if it is already started
     */
    private fun stopEpgUpdateTimer() {
        if (epgUpdateTimer != null) {
            epgUpdateTimer!!.cancel()
            epgUpdateTimer = null
        }
    }

    /**
     * Start epg data update timer
     */
    private fun startEpgUpdateTimer() {
        //Cancel timer if it's already started
        stopEpgUpdateTimer()

        //Start new count down timer
        epgUpdateTimer = object :
            CountDownTimer(
                EPG_UPDATE_TIMEOUT,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onFinish() {
                // Wait for event loading
                thread {
                    EventsLoadedEventListener(object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                        }

                        override fun onSuccess() {
                            InformationBus.informationBusEventListener.submitEvent(Events.EPG_DATA_UPDATED)
                        }
                    })
                    loadEvents()
                }
            }
        }
        epgUpdateTimer!!.start()
    }

    inner class EventsLoadedEventListener(var callback: IAsyncCallback?) {

        private var eventListener: Any ?= null
        init {
            InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.EVENTS_LOADED), callback = {
                eventListener = it
            }, onEventReceived = {
                callback?.onSuccess()
                InformationBus.informationBusEventListener.unregisterEventListener(eventListener!!)
            })
        }
    }
}