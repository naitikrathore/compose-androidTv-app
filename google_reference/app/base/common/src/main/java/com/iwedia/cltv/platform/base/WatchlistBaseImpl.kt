package com.iwedia.cltv.platform.base

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.content_provider.SchedulerDataProvider
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.util.*

open class WatchlistBaseImpl(
    private var epgInterfaceImpl: EpgInterface,
    private val dataProvider: ChannelDataProviderInterface,
    private var context: Context,
    private val timeInterface: TimeInterface
) : WatchlistInterface {

    private val TAG = javaClass.simpleName

    /**
     * List of scheduled objects added in watch list
     */
    var watchlist = mutableListOf<ScheduledReminder>()

    /**
     * Database scheduled reminders list
     */
    var dbWatchlist = mutableListOf<ScheduledReminder>()

    private var schedulerDataProvider: SchedulerDataProvider? = null
    private var eventsList = kotlin.collections.ArrayList<TvEvent>()
    private var timer:Timer? = Timer()
    init {
        schedulerDataProvider = SchedulerDataProvider(context)
    }

    override fun scheduleReminder(
        reminder: ScheduledReminder,
        callback: IAsyncCallback?
    ) {
        if (watchlist.isNotEmpty() && watchlist.contains(reminder)) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder: reminder already exists in watchlist!!!!!")
            callback!!.onFailed(Error("Cannot schedule recording"))
            return
        }

        //TODO The scene Scheduled reminder auto is supposed to pop out 30 seconds prior to start recording
        val timeBeforeProgrammeStarts: Long = 30000

        //Time before start of programme
        val df = SimpleDateFormat("E MMM dd HH:mm z yyyy", Locale("en"));
        val startTime: Date? = reminder.startTime?.let { Date(it) }
        val formattedStartTime = df.parse(df.format(startTime))
        val timeBeforeStartProgramme: Long =
            formattedStartTime.time - timeInterface.getCurrentTime(reminder.tvChannel!!) - timeBeforeProgrammeStarts

        if (timeBeforeStartProgramme > 0) {
            try {
                timer?.schedule(object : TimerTask() {
                    override fun run() {
                        if (watchlist.contains(reminder)) {
                            InformationBus.informationBusEventListener.submitEvent(
                                Events.SCHEDULED_REMINDER_NOTIFICATION,
                                arrayListOf(reminder)
                            )
                            removeScheduledReminder(reminder, null)
                        }
                    }
                }, timeBeforeStartProgramme)

                watchlist.add(reminder)
                storeScheduledReminder(reminder)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentTime onReceive: watchlist ${watchlist.size}")
                callback!!.onSuccess()
                //first add event to the watchlist, then submit event used for toast message in the Main activity
                InformationBus.informationBusEventListener.submitEvent(Events.WATCHLIST_SCHEDULED_TOAST)
                return
            } catch (e: Exception) {
                e.printStackTrace()
                callback!!.onFailed(Error("Cannot schedule recording"))
                return
            }
        } else {
            callback!!.onFailed(Error("Cannot schedule recording, time before start programme $timeBeforeStartProgramme"))
        }
    }

    override fun clearWatchList() {
        CoroutineHelper.runCoroutine({
            schedulerDataProvider!!.clearReminderList()
        })
        watchlist.clear()
        dbWatchlist.clear()
    }

    override fun removeScheduledReminder(
        reminder: ScheduledReminder,
        callback: IAsyncCallback?
    ) {
        CoroutineHelper.runCoroutine({
            var indexRemove = -1
            for (i in 0 until watchlist.size) {
                val scheduledReminder = watchlist[i]
                if (scheduledReminder.name == reminder.name) {
                    indexRemove = i
                    break
                }
            }

            if (indexRemove != -1) {
                watchlist.removeAt(indexRemove)
                removeScheduledReminder(reminder)
                callback?.onSuccess()
            } else {
                callback?.onFailed(Error("Cant remove a reminder that does not exist"))
            }
        })
    }

    override fun loadScheduledReminders() {
        epgInterfaceImpl.getEventList(
            object : IAsyncDataCallback<ArrayList<TvEvent>> {
                override fun onFailed(error: Error) {
                    //retry
                    //TODO Tanvi to check this and to find some better solution
                    //loadScheduledReminders()
                }

                override fun onReceive(data: ArrayList<TvEvent>) {
                    eventsList.clear()
                    eventsList.addAll(data)
                    watchlist.clear()
                    dbWatchlist.clear()
                    clearTimer()
                    val scheduledReminder: ArrayList<ScheduledReminder>? =
                        schedulerDataProvider?.getScheduledRemindersData()
                    if (scheduledReminder != null) {
                        if (scheduledReminder.size > 0) {
                            dbWatchlist.addAll(scheduledReminder)
                            scheduledReminder.forEach {
                                recreateStoredReminder(it)
                            }
                            InformationBus.informationBusEventListener.submitEvent(Events.WATCHLIST_RESTORED)
                        }
                    }
                }
            })
    }

    private fun clearTimer() {
        timer?.let {desObj->
            desObj.cancel()
            desObj.purge()
        }
        timer = null
    }

    override fun getWatchList(callback: IAsyncDataCallback<MutableList<ScheduledReminder>>) {
        callback.onReceive(watchlist)
    }

    override fun getWatchListCount(callback: IAsyncDataCallback<Int>) {
        callback.onReceive(watchlist.size)
    }

    override fun getWatchlistItem(
        position: Int,
        callback: IAsyncDataCallback<ScheduledReminder>
    ) {
        if (position >= 0 && position < watchlist.size) {
            callback.onReceive(watchlist[position])
        } else {
            callback.onFailed(Error("Watchlist not available in the given position"))
        }
    }

    override fun hasScheduledReminder(tvEvent: TvEvent?, callback: IAsyncDataCallback<Boolean>) {
        callback.onReceive(if (tvEvent != null) isInWatchlist(tvEvent) else false)
    }

    override fun isInWatchlist(data: TvEvent): Boolean {
        watchlist.forEach { item ->
            if (item.tvChannelId == data.tvChannel.id) {
                if (item.startTime == data.startTime) {
                    return true
                }
            }
        }
        return false
    }

    override fun removeWatchlistEventsForDeletedChannels() {
        CoroutineHelper.runCoroutine({
            var channelsIdList = mutableListOf<Int>()
            dataProvider.getChannelList().forEach { tvChannel ->
                channelsIdList.add(tvChannel.id)
            }
            //create a tempWatchList to avoid the concurrent exception while traversing through the list
            var tempWatchList = mutableListOf<ScheduledReminder>()
            watchlist.forEach { item -> tempWatchList.add(item) }
            tempWatchList.forEach {
                //removing scheduled reminder events for deleted channels
                if (!channelsIdList.contains(it.tvChannelId)) {
                    //todo no reason for callback here if its not used
                    removeScheduledReminder(it,
                        object : IAsyncCallback {
                            override fun onFailed(error: Error) {}
                            override fun onSuccess() {}
                        }
                    )
                }
            }
        })
    }

    private fun recreateStoredReminder(reminder: ScheduledReminder) {
        val tvChannel: TvChannel = getChannelById(reminder.tvChannelId!!) ?: return
        val currentTime = timeInterface.getCurrentTime(tvChannel)
        reminder.name.let { name ->
            reminder.startTime.let { startTime ->
                if(timer == null)
                    timer = Timer()
                eventsList.forEach { event ->
                    if (event.name == name && event.startTime == startTime) {
                        if (event.startTime > currentTime) {
                            val scheduledReminder = ScheduledReminder(
                                id = reminder.id,
                                name = reminder.name,
                                tvChannel = tvChannel,
                                tvEvent = reminder.tvEvent,
                                startTime = reminder.startTime,
                                tvChannelId = reminder.tvChannelId,
                                tvEventId = reminder.tvEventId
                            )
                            scheduleReminder(scheduledReminder, object : IAsyncCallback {
                                override fun onFailed(error: Error) {}
                                override fun onSuccess() {}
                            })
                        }
                    }
                }
            }
        }
    }

    override fun checkReminderConflict(channelId: Int, startTime: Long): Boolean {
        //Start time to minutes
        val recStartTime = startTime / 60000
        var remStartTime : Long

        dbWatchlist.forEach {
            remStartTime = (it.startTime!!) / 60000

            if (remStartTime == recStartTime) {
                return true
            }
        }
        return false
    }

    private fun removeScheduledReminder(reminder: ScheduledReminder) {
        val removeList = mutableListOf<ScheduledReminder>()
        dbWatchlist.forEach { item ->
            if (item.name == reminder.name) {
                if (item.tvEventId == reminder.tvEventId) {
                    removeList.add(item)
                }
            }
        }
        dbWatchlist.removeAll(removeList)
        val scheduledReminderData = ScheduledReminder(
            reminder.id,
            reminder.name,
            reminder.tvChannel,
            reminder.tvEvent,
            reminder.startTime,
            reminder.tvChannelId,
            reminder.tvEventId
        )

        CoroutineHelper.runCoroutine({
            schedulerDataProvider!!.removeScheduledReminder(
                scheduledReminderData,
                object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: $error")
                    }

                    override fun onSuccess() {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Removed Scheduled Reminder")
                    }

                })
        })

    }

    private fun storeScheduledReminder(reminder: ScheduledReminder) {
        val tvChannel: TvChannel? = getChannelById(reminder.tvChannelId!!)
        val dbSchedulerReminder = ScheduledReminder(
            reminder.id,
            reminder.name,
            tvChannel,
            reminder.tvEvent,
            reminder.startTime,
            reminder.tvChannelId,
            reminder.tvEventId
        )

        var isExisting = false
        dbWatchlist.forEach {
            if (it.tvEventId == findReminderId(reminder)) {
                isExisting = true
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isExisting) {

                dbWatchlist.add(dbSchedulerReminder)
                CoroutineHelper.runCoroutine({
                    schedulerDataProvider!!.storeScheduledReminder(
                        dbSchedulerReminder,
                        object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: $error")
                            }

                            override fun onSuccess() {
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "Schedule ${dbSchedulerReminder.name} reminder success"
                                )
                            }

                        })
                })
            }
        }, 1000)
    }

    private fun findReminderId(reminder: ScheduledReminder): Int? {
        dbWatchlist.forEach {
            if (it.startTime == reminder.startTime) {
                if (it.tvChannelId == reminder.tvChannelId) {
                    return it.tvEventId
                }
            }
        }
        return -1
    }

    private fun getChannelById(channelId: Int): TvChannel? {
        val channelList = dataProvider.getChannelList()
        for (i in 0 until channelList.size) {
            if (channelList[i].id == channelId) {
                return channelList[i]
            }
        }
        return null
    }
}
