package com.iwedia.cltv.sdk.handlers

import android.util.Log
import api.HandlerAPI
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.entities.ReferenceTvEvent
import com.iwedia.cltv.sdk.entities.ScheduledReminderData
import core_entities.Error
import core_entities.ScheduledReminder
import data_type.GList
import data_type.GLong
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Watchlist handler
 *
 * @author Aleksandar Lazic
 */
class ReferenceWatchlistHandler : HandlerAPI {

    private val TAG = javaClass.simpleName
    /**
     * List of scheduled objects added in watch list
     */
    var watchlist = mutableListOf<ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>>()

    /**
     * Database scheduled reminders list
     */
    var dbWatchlist = mutableListOf<ScheduledReminderData>()

    /**
     * Is scheduled events loaded from prefs
     */
    private var isScheduledLoaded = false

    /**
    * List for start time of each reminder
    **/
    var startTimeList = mutableListOf<Date>()

    /**
     * Scheduler handler
     */
    lateinit var schedulerHandler: SchedulerHelper

    fun scheduleReminder(
        reminder: ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>,
        callback: AsyncReceiver?
    ) {
        var startTime: Date?
        var endTime: Date?

        //TODO The scene Scheduled reminder auto is supposed to pop out 30 seconds prior of event start time
        val timeBeforeProgrammeStarts: Long = 30000

        //Time before start of programme
        var timeBeforeStartProgramme: Long = 0

        //Current time
        var data = GLong(System.currentTimeMillis().toString())

        startTime = Date(data.value.toLong())
        endTime = Date(reminder.tvEvent.startDate.value.toLong())

        timeBeforeStartProgramme =
            endTime!!.time - startTime!!.time - timeBeforeProgrammeStarts

        Log.d(Constants.LogTag.CLTV_TAG + "ALLVALUES", "************************************************")
        Log.d(Constants.LogTag.CLTV_TAG + "ALLVALUES", "Watch lISt Size : ${dbWatchlist.size} ")
        dbWatchlist.forEach {
            Log.d(Constants.LogTag.CLTV_TAG + "ALLVALUES", "inside Watch List: ${it.startTime}")
        }
        Log.d(Constants.LogTag.CLTV_TAG + "ALLVALUES", "reminder time : ${reminder.tvEvent.startDate.value.toLong()} ")
        Log.d(Constants.LogTag.CLTV_TAG + "ALLVALUES", "************************************************")

        //check if reminder of same start time already exists
        var result = checkReminderConflict(reminder.tvChannel.id, reminder.tvEvent.startDate.value.toLong())
        if(result){
            callback!!.onFailed(Error(200, "alreadyPresent"))
            return
        }

        //check if recording is scheduled at same start time
        result = (ReferenceSdk.pvrSchedulerHandler as ReferencePvrSchedulerHandler).checkRecordingConflict(reminder.tvEvent.startDate.value.toLong())
        if(result){
            callback!!.onFailed(Error(200, "conflict"))
        }

        if (timeBeforeStartProgramme > 0) {
            try {

                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (watchlist.contains(reminder)) {
                            InformationBus.submitEvent(
                                Event(
                                    ReferenceEvents.SCHEDULED_REMINDER_NOTIFICATION,
                                    reminder
                                )
                            )
                            removeScheduledReminder(reminder, null)
                        }
                    }
                }, timeBeforeStartProgramme)

                watchlist.add(reminder)
                storeScheduledReminder(reminder)
                startTimeList.add(startTime!!)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentTime onReceive: watchlist ${watchlist.size}")
                callback!!.onSuccess()
                InformationBus.submitEvent(
                    Event(
                        ReferenceEvents.PROCEED_CLICKED
                    )
                )
                return
            } catch (e: Exception) {
                e.printStackTrace()
                callback!!.onFailed(Error(200, "Cannot schedule recording"))
                return
            }
        } else {
            callback!!.onFailed(
                Error(
                    200,
                    "Cannot schedule recording, time before start programme $timeBeforeStartProgramme"
                )
            )
        }
    }

    fun clearWatchList(){
        schedulerHandler.clearReminderList()
        watchlist.clear()
        dbWatchlist.clear()
    }

    fun removeScheduledReminder(
        reminder: ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>,
        callback: AsyncReceiver?
    ) {
        CoroutineHelper.runCoroutine({
            var indexRemove = -1
            for (i in 0 until watchlist.size) {
                var scheduledReminder = watchlist[i]
                if (scheduledReminder.name == reminder.name) {
                    if (scheduledReminder.tvEvent.name == reminder.tvEvent.name) {
                        if (scheduledReminder.tvChannel.name == reminder.tvChannel.name) {
                            indexRemove = i
                            break
                        }
                    }
                }
            }

            if (indexRemove != -1) {
                watchlist.removeAt(indexRemove)
                removeScheduledReminder(reminder)
                callback?.onSuccess()
                InformationBus.submitEvent(
                    Event(
                        ReferenceEvents.PROCEED_CLICKED
                    )
                )
            } else {
                callback?.onFailed(Error(404, ""))
            }
        })
    }

    private fun loadScheduledReminders() {

        CoroutineHelper.runCoroutine({
            watchlist.clear()
            dbWatchlist.clear()

            val tempList = schedulerHandler.getScheduledRemindersData()

            if (tempList != null) {
                if (tempList.size > 0) {
                    tempList.forEach {
                        recreateStoredReminder(it)
                    }
                }
            }
        })
    }

    private fun recreateStoredReminder(reminder: ScheduledReminderData) {
        var currentTime: Date?
        var tvChannel: ReferenceTvChannel? = getChannelById(reminder.channelId) ?: return

        var data = GLong(System.currentTimeMillis().toString())
        currentTime = Date(data.value.toLong())

        getEventId(
            tvChannel!!,
            reminder.eventId,
            object : AsyncDataReceiver<ReferenceTvEvent> {
                override fun onFailed(error: Error?) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, " WATCH LIST RECREATE FAILED " + error?.message)
                }

                override fun onReceive(tvEvent: ReferenceTvEvent) {
                    if (tvEvent.startDate.value.toLong() > currentTime!!.time) {
                        var reminder = ScheduledReminder(
                            reminder.id,
                            reminder.name!!,
                            tvChannel,
                            tvEvent
                        )
                        scheduleReminder(reminder, object : AsyncReceiver {
                            override fun onFailed(error: Error?) {
                            }

                            override fun onSuccess() {
                            }
                        })
                    }
                }
            }
        )
    }

    fun checkReminderConflict(channelId: Int, startTime: Long): Boolean {
        dbWatchlist.forEach {
            if (it.channelId == channelId &&
                it.startTime == startTime) {
                return true
            }
        }
        return false
    }

    private fun storeScheduledReminder(reminder: ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>) {
        val dbSchedulerReminder = ScheduledReminderData(
            reminder.id,
            reminder.name,
            reminder.tvChannel.id,
            reminder.tvEvent.id,
            reminder.tvEvent.startDate.value.toLong()
        )

        var isExisting = false
        dbWatchlist.forEach {
            if (it.id == reminder.id) {
                if (it.name == reminder.name) {
                    isExisting = true
                    return
                }
            }
        }

        if (!isExisting) {
            dbWatchlist.add(dbSchedulerReminder)
            schedulerHandler.storeScheduledReminder(dbSchedulerReminder)
        }
    }

    private fun removeScheduledReminder(reminder: ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>) {
        val removeList = mutableListOf<ScheduledReminderData>()
        dbWatchlist.forEach { item ->
            if (item.name == reminder.name) {
                if (item.id == reminder.id) {
                    removeList.add(item)
                }
            }
        }

        dbWatchlist.removeAll(removeList)
        var scheduledReminderData= ScheduledReminderData(reminder.id, reminder.name, reminder.tvChannel.id, reminder.tvEvent.id, reminder.tvEvent.startDate.value.toLong())
        schedulerHandler.removeScheduledReminder(scheduledReminderData)
    }

    fun getWatchList(callback: AsyncDataReceiver<MutableList<ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>>>) {
        callback.onReceive(watchlist)
    }

    fun getWatchListCount(callback: AsyncDataReceiver<Int>) {
        callback.onReceive(watchlist.size)
    }

    fun getWatchlistItem(
        position: Int,
        callback: AsyncDataReceiver<ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>>
    ) {
        callback.onReceive(watchlist[position])
    }

    fun hasScheduledReminder(tvEvent: ReferenceTvEvent?, callback: AsyncDataReceiver<Boolean>) {
        watchlist.forEach {
            if (it.tvEvent.id == tvEvent!!.id) {
                callback.onReceive(true)
                return
            }
        }
        callback.onReceive(false)
    }

    private fun getChannelById(channelId: Int): ReferenceTvChannel? {
        var channelList = ReferenceSdk.tvHandler!!.getChannelList()
        for (i in 0 until channelList.size()) {
            if (channelList.get(i)!!.id == channelId) {
                return channelList.get(i)
            }
        }

        return null
    }

    fun getEventId(
        tvChannel: ReferenceTvChannel,
        eventId: Int,
        callback: AsyncDataReceiver<ReferenceTvEvent>
    ) {
        val startTime: Long = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(6)
        val endTime: Long = System.currentTimeMillis() + TimeUnit.HOURS.toMillis((24 * 8).toLong())

        ReferenceSdk.epgHandler!!.getEventListByChannelAndTime(tvChannel,
            GLong(startTime.toString()),
            GLong(endTime.toString()),
            object : AsyncDataReceiver<GList<ReferenceTvEvent>> {
                override fun onFailed(error: Error?) {
                    callback.onFailed(error)
                }

                override fun onReceive(data: GList<ReferenceTvEvent>) {
                    data.value.forEach {
                        if (it.id == eventId) {
                            callback.onReceive(it)
                            return
                        }
                    }
                    callback.onFailed(Error(-888, "Empty list"))
                }
            })
    }


    override fun setup() {
        schedulerHandler = SchedulerHelper()
        loadScheduledReminders()
        isScheduledLoaded = true
    }

    override fun dispose() {
    }

    fun isInWatchlist(referenceTvEvent: ReferenceTvEvent): Boolean {
        watchlist.forEach { item ->
            if (item.tvEvent.id == referenceTvEvent.id) {
                return true
            }
        }
        return false
    }

    fun removeWatchlistEventsForDeletedChannels() {
        CoroutineHelper.runCoroutine({
            var channelsIdList = mutableListOf<Int>()
            ReferenceSdk.tvHandler!!.getChannelList().value.forEach { tvChannel->
                channelsIdList.add(tvChannel.id)
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledRemindersForDeletedChannels watchlist.size is ${watchlist.size}")
            //create a tempWatchList to avoid the concurrent exception while traversing through the list
            var tempWatchList = mutableListOf<ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>>()
            watchlist.forEach { item -> tempWatchList.add(item) }
            tempWatchList.forEach {
                //removing scheduled reminder events for deleted channels
                if (!channelsIdList.contains(it.tvChannel.id)) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG,"removeScheduledRemindersForDeletedChannels - removing for ${it.tvChannel.id}")
                    removeScheduledReminder(it,
                        object : AsyncReceiver {
                            override fun onFailed(error: Error?) {}
                            override fun onSuccess() {}
                        }
                    )
                }
            }
        })
    }
}