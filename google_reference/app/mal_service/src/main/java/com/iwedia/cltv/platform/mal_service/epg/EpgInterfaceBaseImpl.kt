package com.iwedia.cltv.platform.mal_service.epg

import android.util.Log
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.mal_service.fromServiceChannel
import com.iwedia.cltv.platform.mal_service.fromServiceTvEvent
import com.iwedia.cltv.platform.mal_service.toServiceChannel
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import java.util.Calendar
import java.util.Date

open class EpgInterfaceBaseImpl constructor(var epgDataProvider: EpgDataProviderInterface, var serviceImpl: IServiceAPI) :
    EpgInterface {

    private var hideLockedServices : Boolean = false

    open val TAG = "EpgInterfaceBaseImpl"
    override fun setup() {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    init {
        hideLockedServices = serviceImpl.getCountryPreferences(com.cltv.mal.model.entities.CountryPreference.HIDE_LOCKED_SERVICES_IN_EPG,false) as Boolean
    }

    @Synchronized
    override fun getCurrentEvent(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>) {
        val currentTime = serviceImpl.getCurrentTimeByChannel(toServiceChannel(tvChannel))
        val allEvents = arrayListOf<TvEvent>()

        var serviceEvent = serviceImpl.getUncachedCurrentEvent(toServiceChannel(tvChannel))
        if(serviceEvent != null) {
            var event = fromServiceTvEvent(serviceEvent)
            callback.onReceive(event)
            return
        }

        try {
            allEvents.addAll(epgDataProvider.getEventChannelMap()[tvChannel.getUniqueIdentifier()] as Collection<TvEvent>)
            var eventFound = false
            run loop@{
                allEvents.forEach { event ->
                    if (event.startTime <= currentTime && event.endTime >= currentTime) {
                        callback.onReceive(event)
                        eventFound = true
                        return@loop
                    }
                }
            }
            if (!eventFound) {
                callback.onFailed(Error("Event for current channel not found."))
            }
        } catch (e: Exception) {
            callback.onFailed(Error("Event for current channel not found."))
        }

    }

    override fun updateEpgData(applicationMode: ApplicationMode) {
        epgDataProvider.loadIntermittentEvents()
    }

    override fun getEvent(index: Int, callback: IAsyncDataCallback<TvEvent>) {
        CoroutineHelper.runCoroutine({
            var event = epgDataProvider.getEventList().get(index)
            if (event != null) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEvent by index: event = $event")
                callback.onReceive(event)
            } else {
                callback.onFailed(Error("getEvent Cannot find event by index: $index"))
            }
            return@runCoroutine
        })
    }

    //SKLONITI?????
    override fun getEventById(id: Int, callback: IAsyncDataCallback<TvEvent>) {
        CoroutineHelper.runCoroutine({
            var eventList = epgDataProvider.getEventList()
            if (eventList != null) {
                eventList.forEach { event ->
                    if (event.id == id) {
                        callback.onReceive(event)
                        return@runCoroutine
                    }
                }
            } else {
                callback.onFailed(Error("Cannot find event by id: $id"))
                return@runCoroutine
            }
        })
    }

    override fun getEventByNameAndStartTime(name: String, startTime: Long?, channelId: Int, callback: IAsyncDataCallback<TvEvent>) {
        CoroutineHelper.runCoroutine({
            val eventList = epgDataProvider.getEventList()
            eventList.forEach { event ->
                if (event.name == name && event.startTime == startTime && event.tvChannel.id == channelId) {
                    callback.onReceive(event)
                    return@runCoroutine
                }
            }
            callback.onFailed(Error("Events not found."))
            return@runCoroutine
        })
    }

    //SKLONITI?????
    override fun getEventList(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            val eventList = epgDataProvider.getEventList()
            if (eventList.isNotEmpty()) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventList event list successfully got")
                callback.onReceive(eventList)
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventList event list is null.")
                callback.onFailed(Error("Cannot get event list"))
            }
        })
    }

    override fun getEventListByChannel(
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<ArrayList<TvEvent>>
    ) {

        CoroutineHelper.runCoroutine({
            var eventMap = epgDataProvider.getEventChannelMap()
            var channelEventList : ArrayList<TvEvent> = arrayListOf()

            eventMap[tvChannel.getUniqueIdentifier()]?.forEach{
                channelEventList.add(it)
            }

            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventListByChannel: ${tvChannel.name} ---id:  ${tvChannel.id} size: ${channelEventList.size}")
            var presentEvent: TvEvent? = null
            var followEvent: TvEvent? = null

            var sdkEvent = serviceImpl.getUncachedCurrentEvent(toServiceChannel(tvChannel))
            if (sdkEvent != null) {
                presentEvent = fromServiceTvEvent(sdkEvent)
            }

            sdkEvent = serviceImpl.getUncachedNextEvent(toServiceChannel(tvChannel))
            if (sdkEvent != null) {
                followEvent = fromServiceTvEvent(sdkEvent)
            }

            var presentFound = false
            var followFound = false
            val finalList = mutableListOf<TvEvent>()
            for (i in 0 until channelEventList.size) {
                if (presentEvent != null  && channelEventList[i].tvEventId == presentEvent.tvEventId) {
                    finalList.add(presentEvent)
                    presentFound = true
                } else if (followEvent != null && channelEventList[i].tvEventId == followEvent.tvEventId) {
                    finalList.add(followEvent)
                    followFound = true
                } else {
                    finalList.add(channelEventList[i])
                }
            }

            if ((!presentFound) && (!followFound) &&
                (presentEvent != null) && (followEvent != null)
            ) {
                var nativeData = java.util.ArrayList<TvEvent>()

                if (presentEvent != null) {
                    nativeData.add(presentEvent)
                }
                if (followEvent != null) {
                    nativeData.add(followEvent)
                }

                channelEventList.forEach {
                    nativeData.add(it)
                }

                injectNoInfoState(nativeData, tvChannel)

                callback.onReceive(nativeData)
            } else {
                callback.onReceive(finalList as java.util.ArrayList)
            }

            return@runCoroutine

        })
    }

    override fun getEventListByChannelAndTime(
        tvChannel: TvChannel,
        startTime: Long,
        endTime: Long,
        callback: IAsyncDataCallback<ArrayList<TvEvent>>
    ) {
        getEventListByChannel(tvChannel, object : IAsyncDataCallback<ArrayList<TvEvent>> {
            override fun onFailed(error: Error) {
                callback.onReceive(arrayListOf())
            }

            override fun onReceive(data: ArrayList<TvEvent>) {
                var channelEventListByTime = arrayListOf<TvEvent>()
                run exitForEach@{
                    data.forEach { event ->
                        if ((startTime > event.startTime && startTime > event.endTime) //  Event completely left side of selected date
                            || (endTime < event.startTime && endTime < event.endTime) //  Event completely right side of selected date
                        ) {
                            return@forEach
                        } else {
                            channelEventListByTime.add(event)
                        }
                    }
                }
                callback.onReceive(channelEventListByTime)
            }
        })
    }

    override fun getAllCurrentEvent(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            var allEvents = epgDataProvider.getEventList()
            var currentEvents = arrayListOf<TvEvent>()
            var currentChannel = serviceImpl.getActiveChannel(ApplicationMode.DEFAULT.ordinal)
            allEvents.forEach { event ->
                val currentTime =
                    serviceImpl.getCurrentTimeByChannel(toServiceChannel(event.tvChannel))
                if (event.startTime <= currentTime && event.endTime >= currentTime) {
                    try {
                        if (event.tvChannel.isBroadcastChannel() && (currentChannel != null) && //optimize number of calls to service
                            (event.tvChannel.onId == currentChannel.onId) && (event.tvChannel.tsId == currentChannel.tsId)) {
                            var currentEvent = serviceImpl.getUncachedCurrentEvent(toServiceChannel(event.tvChannel))
                            if(currentEvent != null) {
                                currentEvents.add(fromServiceTvEvent(currentEvent))
                            } else {
                                currentEvents.first { it.name == event.name && it.tvChannel.channelId == event.tvChannel.channelId }
                            }
                        }
                        else {
                            currentEvents.first { it.name == event.name && it.tvChannel.channelId == event.tvChannel.channelId }
                        }

                    } catch (e: NoSuchElementException) {
                        currentEvents.add(event)
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

    private fun addSkippedSDKEvents(sameTSServices : ArrayList<TvChannel>, sameTSNextEvent : ArrayList<TvEvent>, nextEvents : ArrayList<TvEvent>) {
        for(service in sameTSServices) {
            if (service.isBroadcastChannel()
                && !(hideLockedServices && service.isLocked)
            ) {
                var serviceFound = false
                for (event in nextEvents) {
                    if(event.tvChannel.channelId == service.channelId) {
                        serviceFound = true
                    }
                }

                if(!serviceFound) {
                    for(event in sameTSNextEvent) {
                        if(event.tvChannel.channelId == service.channelId) {
                            nextEvents.add(event)
                        }
                    }
                }
            }
        }
    }

    override fun getAllNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            var allEvents = epgDataProvider.getEventList()
            var nextEvents = arrayListOf<TvEvent>()
            var sameTSServices = arrayListOf<TvChannel>()
            var sameTSNextEvent = arrayListOf<TvEvent>()
            var currentChannel = serviceImpl.getActiveChannel(ApplicationMode.DEFAULT.ordinal)

            serviceImpl.getChannelList(ApplicationMode.DEFAULT.ordinal).forEach {
                if((it.onId == currentChannel.onId) && (it.tsId == currentChannel.tsId)) {
                    sameTSServices.add(fromServiceChannel(it))
                }
            }

            for(service in sameTSServices) {
                var event = serviceImpl.getUncachedNextEvent(toServiceChannel(service))
                if(event!= null) {
                    sameTSNextEvent.add(fromServiceTvEvent(event))
                }
            }

            allEvents.sortBy { it.startTime }

            allEvents.forEach { event ->
                if(event.tvChannel.isBroadcastChannel()
                    && !(hideLockedServices && event.tvChannel.isLocked)) {
                    var currentTime = serviceImpl.getCurrentTimeByChannel(toServiceChannel(event.tvChannel))
                    if (event.startTime >= currentTime) {
                        try {
                            nextEvents.first { it.tvChannel.channelId == event.tvChannel.channelId && it.startTime == event.startTime }
                        } catch (e: NoSuchElementException) {
                            var addEvent = event
                            for (sdkEvent in sameTSNextEvent) {
                                if ((event.tvEventId == sdkEvent.tvEventId) &&
                                    (event.tvChannel.channelId == sdkEvent.tvChannel.channelId)
                                ) {
                                    addEvent = sdkEvent
                                }
                            }
                            nextEvents.add(addEvent)
                        }
                    }
                }
                //Limit epg next events collecting on 200 channels
                if (nextEvents.size > 200) {
                    addSkippedSDKEvents(sameTSServices,sameTSNextEvent,nextEvents)
                    nextEvents.sortBy { it.startTime }
                    callback.onReceive(nextEvents)
                    return@runCoroutine
                }
            }

            addSkippedSDKEvents(sameTSServices,sameTSNextEvent,nextEvents)

            if (nextEvents.size > 0) {
                nextEvents.sortBy { it.startTime }
                callback.onReceive(nextEvents)
                return@runCoroutine
            } else {
                callback.onFailed(Error("Events not found."))
            }
        })
    }

    override fun getNextEventByChannel(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>) {
        getEventListByChannel(tvChannel,object : IAsyncDataCallback<ArrayList<TvEvent>>{
            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }

            override fun onReceive(data: ArrayList<TvEvent>) {
                val currentTime = serviceImpl.getCurrentTimeByChannel(toServiceChannel(tvChannel))
                var isEventFound = false
                run breaking@{
                    data.forEach{
                        if (it.startTime>=currentTime){
                            isEventFound = true
                            callback.onReceive(it)
                            return@breaking
                        }
                    }
                }
                if (!isEventFound)callback.onFailed(Error("No event found"))
            }
        })
    }

    override fun setActiveWindow(tvChannelList: MutableList<TvChannel>, startTime: Long) {}

    override fun clearActiveWindow() {}

    override fun getStartTimeForActiveWindow(): Long {
        return 0
    }

    fun injectNoInfoState(eventsData : java.util.ArrayList<TvEvent>, tvChannel: TvChannel) {
        findCurrentEventPosition(eventsData, object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: Int) {
                if(data == -1) {
                    val currentDate  = serviceImpl.getCurrentTimeByChannel(toServiceChannel(tvChannel))
                    val futureEvents = java.util.ArrayList<TvEvent>()
                    val pastEvents = java.util.ArrayList<TvEvent>()

                    eventsData.forEach { event->
                        if(event.startTime > currentDate) {
                            futureEvents.add(event)
                        } else {
                            pastEvents.add(event)
                        }
                    }

                    val calendar = Calendar.getInstance()
                    calendar.time = Date(currentDate)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    val endCalendar = Calendar.getInstance()
                    endCalendar.time = Date(currentDate)
                    endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                    endCalendar.set(Calendar.MINUTE, 59)
                    endCalendar.set(Calendar.SECOND, 59)
                    endCalendar.set(Calendar.MILLISECOND, 999)

                    var eventStart: Long  = calendar.time.time
                    var endDate: Long = endCalendar.time.time

                    if(pastEvents.size >0) {
                        pastEvents.sortedBy { it.startTime }
                        pastEvents.forEach {
                            if (it.endTime < endCalendar.time.time) {
                                eventStart = pastEvents[pastEvents.size - 1].endTime
                            }else if((it.endTime > calendar.time.time) &&
                                (it.startTime < endCalendar.time.time)) {
                                eventStart =  calendar.time.time
                            }
                        }
                    }

                    if(futureEvents.size >0) {
                        futureEvents.sortedBy { it.startTime }
                        if(futureEvents[0].startTime < endCalendar.time.time) {
                            endDate = futureEvents[0].startTime - 1000
                        }
                    }
                    pastEvents.add(TvEvent.createNoInformationEvent(tvChannel, eventStart, endDate, serviceImpl.getCurrentTimeByChannel(
                        toServiceChannel(tvChannel))))

                    eventsData.clear()
                    pastEvents.forEach { eventsData.add(it)  }
                    futureEvents.forEach { eventsData.add(it) }
                }
            }
        })
    }

    private fun findCurrentEventPosition(
        events: List<TvEvent>,
        callback: IAsyncDataCallback<Int>
    ) {
        var currentPosition = -1
        for (i in events.indices) {
            val currentDate  = serviceImpl.getCurrentTimeByChannel(toServiceChannel(events[i].tvChannel))
            val startTime =
                Date(events[i].startTime)
            val endTime =
                Date(events[i].endTime)
            val isCurrentEvent =
                startTime.before(Date(currentDate)) && Date(currentDate).before(
                    endTime
                )

            if (isCurrentEvent) {
                currentPosition = i
            }
        }
        callback.onReceive(currentPosition)
    }
}