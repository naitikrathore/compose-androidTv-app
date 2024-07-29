package com.iwedia.cltv.platform.base

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.base.content_provider.SchedulerDataProvider
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.*
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

class SchedulerInterfaceBaseImpl(
    private var utilsInterface: UtilsInterface,
    private val dataProvider: ChannelDataProviderInterface,
    private var epgInterface: EpgInterface,
    private var watchlistInterface: WatchlistInterface,
    private var context: Context,
    private var timeInterface: TimeInterface
) : SchedulerInterface {

    private val TAG = javaClass.simpleName

    /**
     * List of scheduled objects added in recording list
     */
    var recordings = mutableListOf<ScheduledRecording>()

    /**
     * Database scheduled recordings list
     */
    var dbRecList = mutableListOf<ScheduledRecording>()

    /**
     * Is scheduled events loaded from prefs
     */
    private var isScheduledLoaded = false
    private var schedulerDataProvider: SchedulerDataProvider? = null
    private var eventsList = kotlin.collections.ArrayList<TvEvent>()
    private var conflictedRecList = mutableListOf<ScheduledRecording>()
    private var refScheduledRecordingData: ScheduledRecording? = null

    init {
        isScheduledLoaded = true
        schedulerDataProvider = SchedulerDataProvider(context)
    }

    override fun scheduleRecording(
        scheduledRecording: ScheduledRecording,
        callback: IAsyncDataCallback<SchedulerInterface.ScheduleRecordingResult>
    ) {
        if (recordings.isNotEmpty() && recordings.contains(scheduledRecording)) {
            callback.onFailed(Error("Cannot schedule recording"))
            return
        }
        val timeBeforeStartProgramme: Long
        val repeatFreq: Int = scheduledRecording.repeatFreq.ordinal
        val tvChannel: TvChannel? = getChannelById(scheduledRecording.tvChannelId)
        val currentTime = timeInterface.getCurrentTime(tvChannel!!)

        timeBeforeStartProgramme = (scheduledRecording.scheduledDateStart - currentTime)

        //check if recording of same start time already exists
        if (findConflictedRecordings(scheduledRecording).isNotEmpty()) {
            callback.onReceive(SchedulerInterface.ScheduleRecordingResult.SCHEDULE_RECORDING_ALREADY_PRESENT)
            refScheduledRecordingData = scheduledRecording
            InformationBus.informationBusEventListener.submitEvent(Events.SCHEDULED_RECORDING_CONFLICT,
                arrayListOf(scheduledRecording))
            return
        }

        if (!utilsInterface.isUsbConnected()) {
            callback.onFailed(Error("USB NOT CONNECTED\nConnect USB to record"))
            return
        }
        if (timeBeforeStartProgramme > 0) {
            try {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "repeatFreq :$repeatFreq ")
                recordings.add(scheduledRecording)
                schedule(timeBeforeStartProgramme, scheduledRecording)
                callback.onReceive(SchedulerInterface.ScheduleRecordingResult.SCHEDULE_RECORDING_SUCCESS)
                if (isScheduledLoaded) {
                    InformationBus.informationBusEventListener.submitEvent(Events.RECORDING_SCHEDULED_TOAST)
                }
                return
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: Cannot schedule recording")
                callback.onReceive(SchedulerInterface.ScheduleRecordingResult.SCHEDULE_RECORDING_ERROR)
                return
            }
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: Cannot schedule $timeBeforeStartProgramme ${scheduledRecording.scheduledDateStart}")
            callback.onReceive(SchedulerInterface.ScheduleRecordingResult.SCHEDULE_RECORDING_ERROR)
        }
    }

    override fun getNewRec(): ScheduledRecording? {
       return refScheduledRecordingData
    }

    private fun findRecordingId(recording: ScheduledRecording): Int? {
        dbRecList.forEach {
            if (it.scheduledDateStart == recording.scheduledDateStart) {
                if (it.tvChannelId == recording.tvChannelId) {
                    return it.tvEventId
                }
            }
        }
        return -1
    }

    @SuppressLint("Range")
    override fun getRecodingId(
        recordedData: ScheduledRecording,
        callback: IAsyncDataCallback<Int>
    ) {
        CoroutineHelper.runCoroutine({
            schedulerDataProvider!!.getRecodingId(recordedData, callback)
        })
    }

    override fun removeScheduledRecording(
        scheduledRecording: ScheduledRecording, callback: IAsyncCallback?
    ) {
        CoroutineHelper.runCoroutine({
            var indexRemove = -1
            for (i in 0 until recordings.size) {
                val recording = recordings[i]
                if (recording.name == scheduledRecording.name) {
                    indexRemove = i
                    break
                }
            }

            if (indexRemove != -1) {
                recordings.removeAt(indexRemove)
                removeScheduledRecording(scheduledRecording)
                callback?.onSuccess()
            } else {
                callback?.onFailed(Error("Cant remove a Recording that does not exist"))
            }
        })
    }

    private fun removeScheduledRecording(recording: ScheduledRecording) {
        val removeList = mutableListOf<ScheduledRecording>()
        dbRecList.forEach { item ->
            if (item.name == recording.name) {
                if (item.id == recording.id) {
                    removeList.add(item)
                }
            }
        }

        dbRecList.removeAll(removeList)
        val scheduledRecordingData = ScheduledRecording(
            recording.id,
            recording.name,
            recording.scheduledDateStart,
            recording.scheduledDateEnd,
            recording.tvChannelId,
            recording.tvEventId,
            RepeatFlag.NONE,
            recording.tvChannel,
            recording.tvEvent
        )

        CoroutineHelper.runCoroutine({
            schedulerDataProvider!!.removeScheduledRecording(
                scheduledRecordingData,
                object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: $error")
                    }

                    override fun onSuccess() {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Removed Scheduled Recording")
                    }

                })
        })

    }

    override fun clearRecordingList() {
        CoroutineHelper.runCoroutine({
            schedulerDataProvider!!.clearRecordingList()
        })
        recordings.clear()
        dbRecList.clear()
    }

    override fun findConflictedRecordings(referenceScheduledRecording: ScheduledRecording): MutableList<ScheduledRecording> {
        return findConflictedRecordings(
            referenceScheduledRecording.scheduledDateStart,
            referenceScheduledRecording.scheduledDateEnd
        )
    }

    override fun findConflictedRecordings(
        recordingStartTime: Long,
        recordingEndTime: Long
    ): MutableList<ScheduledRecording> {
        val conflictedRecordings = mutableListOf<ScheduledRecording>()

        recordings.forEach { item ->

            if ((recordingStartTime >= item.scheduledDateStart && recordingStartTime >= item.scheduledDateEnd) //  Event completely left side of selected event
                || (recordingEndTime <= item.scheduledDateStart && recordingEndTime <= item.scheduledDateEnd) //  Event completely right side of selected event
            ) {
                return@forEach// no conflicts in this event, so check next event
            } else {
                // having conflicts
                conflictedRecordings.add(
                    ScheduledRecording(
                        item.id,
                        item.name,
                        item.scheduledDateStart,
                        item.scheduledDateEnd,
                        item.tvChannelId,
                        item.tvEventId,
                        RepeatFlag.NONE,
                        item.tvChannel,
                        item.tvEvent
                    )
                )
            }
        }
        return conflictedRecordings
    }

    override fun schedule(durationToStartRecording: Long, scheduledRecording: ScheduledRecording) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "schedule : $scheduledRecording")
        val durationToNotify = durationToStartRecording - 60000

        addAndStoreScheduledRecording(scheduledRecording)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (recordings.contains(scheduledRecording)) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "run: EVENT SUBMITTED")

                    // CHECK FOR WATCHLIST & SCHEDULE RECORDING CONFLICTS
                    if (watchlistInterface.checkReminderConflict(
                            scheduledRecording.tvChannelId,
                            scheduledRecording.scheduledDateStart
                        )
                    ) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "run: WATCHLIST & SCHEDULE RECORDING CONFLICTS")
                        //SHOW WATCHLIST & SCHEDULE RECORDING CONFLICTS
                        InformationBus.informationBusEventListener.submitEvent(
                            Events.SCHEDULED_RECORDING_REMINDER_CONFLICTS_NOTIFICATION,
                            arrayListOf(scheduledRecording)
                        )
                    } else {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "run: EVENT SUBMITTED -> SCHEDULED_RECORDING_NOTIFICATION/scheduledRecording == $scheduledRecording")
                        //SHOW CHANNEL CHANGE DIALOG 1 MINUTE BEFORE
                        InformationBus.informationBusEventListener.submitEvent(
                            Events.SCHEDULED_RECORDING_NOTIFICATION,
                            arrayListOf(scheduledRecording)
                        )
                    }
                    removeAndReschedule()
                }
            }

            fun removeAndReschedule() {
                removeAllScheduledRecording(scheduledRecording,
                    object : IAsyncCallback {
                        override fun onFailed(error: Error) {}

                        override fun onSuccess() {
                            //Current time
                            val currentTime = timeInterface.getCurrentTime()
                            val timeBeforeStartProgramme =
                                (scheduledRecording.scheduledDateStart - currentTime) - 60000

                            when (scheduledRecording.repeatFreq) {
                                RepeatFlag.DAILY -> {
                                    scheduleWithDailyRepeat(
                                        timeBeforeStartProgramme,
                                        scheduledRecording
                                    )
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "Recording scheduled for ${(timeBeforeStartProgramme) / 60000} minutes"
                                    )
                                }
                                RepeatFlag.WEEKLY -> {
                                    scheduleWithWeeklyRepeat(
                                        timeBeforeStartProgramme,
                                        scheduledRecording
                                    )
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "Recording scheduled for ${(timeBeforeStartProgramme) / 3600000} hours"
                                    )
                                }
                                RepeatFlag.NONE -> {
                                    // no need to repeat
                                }
                            }
                        }
                    })
            }
        }, if (durationToNotify < 0) 0 else durationToNotify)
    }

    override fun removeAllScheduledRecording(
        recording: ScheduledRecording,
        callback: IAsyncCallback
    ) {
        val removeList = mutableListOf<ScheduledRecording>()
        val removeList2 = mutableListOf<ScheduledRecording>()

        dbRecList.forEach { item ->
            /*if (item.name == recording.name) {
                if (item.tvEventId == recording.tvEvent!!.id) {
                    removeList.add(item)
                }
            }*/
            //TODO: temporarily fixing with starttime as recording id could be 0/-1 sometime

            if (item.scheduledDateStart == recording.scheduledDateStart) {
                removeList.add(item)
            }
        }

        removeList.forEach { it ->
            removeScheduledRecording(it, object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: $error")
                }

                override fun onSuccess() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: Removed ${it.name} from Recording")
                }
            })
        }

        InformationBus.informationBusEventListener?.submitEvent(Events.SCHEDULED_RECORDING_REMOVED)
        callback.onSuccess()
    }

    override fun scheduleWithDailyRepeat(
        durationToStartRecording: Long,
        scheduledRecordingPrevious: ScheduledRecording
    ) {
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "Recording will be start in ${durationToStartRecording / 60000} minutes"
        )

        val recordingStart = (scheduledRecordingPrevious.scheduledDateStart + (24 * 60 * 60 * 1000))
        val recordingEnd = (scheduledRecordingPrevious.scheduledDateEnd + (24 * 60 * 60 * 1000))

        val tvEvent = TvEvent.createNoInformationEvent(
            scheduledRecordingPrevious.tvChannel!!,
            timeInterface.getCurrentTime(scheduledRecordingPrevious.tvChannel!!)
        )

        val scheduledRecording = ScheduledRecording(
            scheduledRecordingPrevious.id,
            scheduledRecordingPrevious.name,
            recordingStart,
            recordingEnd,
            scheduledRecordingPrevious.tvChannelId,
            scheduledRecordingPrevious.tvEventId,
            scheduledRecordingPrevious.repeatFreq,
            scheduledRecordingPrevious.tvChannel,
            tvEvent
        )

        val timeBeforeStartProgramme = (durationToStartRecording + (24 * 60 * 60 * 1000))
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "Recording will be Scheduled for ${Date(timeBeforeStartProgramme)}"
        )
        Timer().schedule(object : TimerTask() {

            override fun run() {
                if (recordings.contains(scheduledRecording)) {
                    //Recursive call for scheduling daily
                    scheduleWithDailyRepeat(timeBeforeStartProgramme, scheduledRecording)

                    // CHECK FOR WATCHLIST & SCHEDULE RECORDING CONFLICTS
                    if (watchlistInterface.checkReminderConflict(
                            scheduledRecording.tvChannelId,
                            scheduledRecording.scheduledDateStart
                        )
                    ) {
                        //SHOW WATCHLIST & SCHEDULE RECORDING CONFLICTS
                        InformationBus.informationBusEventListener.submitEvent(
                            Events.SCHEDULED_RECORDING_REMINDER_CONFLICTS_NOTIFICATION,
                            arrayListOf(scheduledRecording)
                        )
                    } else {
                        //SHOW CHANNEL CHANGE DIALOG 1 MINUTE BEFORE
                        InformationBus.informationBusEventListener.submitEvent(
                            Events.SCHEDULED_RECORDING_NOTIFICATION,
                            arrayListOf(scheduledRecording)
                        )
                    }
                }
            }
        }, timeBeforeStartProgramme)

        addAndStoreScheduledRecording(scheduledRecording)
    }

    override fun scheduleWithWeeklyRepeat(
        durationToStartRecording: Long,
        scheduledRecordingPrevious: ScheduledRecording
    ) {
        val recordingStart =
            (scheduledRecordingPrevious.scheduledDateStart + (7 * (24 * 60 * 60 * 1000)))
        val recordingEnd =
            (scheduledRecordingPrevious.scheduledDateEnd + (7 * (24 * 60 * 60 * 1000)))

        val tvEvent = TvEvent.createNoInformationEvent(
            scheduledRecordingPrevious.tvChannel!!,
            timeInterface.getCurrentTime(scheduledRecordingPrevious.tvChannel!!)
        )

        val scheduledRecording = ScheduledRecording(
            scheduledRecordingPrevious.id,
            scheduledRecordingPrevious.name,
            recordingStart,
            recordingEnd,
            scheduledRecordingPrevious.tvChannelId,
            scheduledRecordingPrevious.tvEventId,
            scheduledRecordingPrevious.repeatFreq,
            scheduledRecordingPrevious.tvChannel,
            tvEvent
        )

        val timeBeforeStartProgramme = (durationToStartRecording + (7 * (24 * 60 * 60 * 1000)))
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "Recording will be Scheduled for ${Date(timeBeforeStartProgramme)}"
        )
        Timer().schedule(object : TimerTask() {

            override fun run() {
                if (recordings.contains(scheduledRecording)) {
                    //Recursive call for scheduling weekly
                    scheduleWithWeeklyRepeat(timeBeforeStartProgramme, scheduledRecording)

                    // CHECK FOR WATCHLIST & SCHEDULE RECORDING CONFLICTS
                    if (watchlistInterface.checkReminderConflict(
                            scheduledRecording.tvChannelId,
                            scheduledRecording.scheduledDateStart
                        )
                    ) {
                        //SHOW WATCHLIST & SCHEDULE RECORDING CONFLICTS
                        InformationBus.informationBusEventListener.submitEvent(
                            Events.SCHEDULED_RECORDING_REMINDER_CONFLICTS_NOTIFICATION,
                            arrayListOf(scheduledRecording)
                        )
                    } else {
                        //SHOW CHANNEL CHANGE DIALOG 1 MINUTE BEFORE
                        InformationBus.informationBusEventListener.submitEvent(
                            Events.SCHEDULED_RECORDING_NOTIFICATION,
                            arrayListOf(scheduledRecording)
                        )
                    }
                }
            }
        }, timeBeforeStartProgramme)

        addAndStoreScheduledRecording(scheduledRecording)
    }

    override fun checkRecordingConflict(startTime: Long): Boolean {
        dbRecList.forEach {
            if (it.scheduledDateStart == startTime) {
                return true
            }
        }
        return false
    }

    override fun getId(scheduledRecording: ScheduledRecording): Int {

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getId: GET ID IN PVR SCHEDULER HANDLER ${recordings.size}")

        dbRecList.forEach { item ->
            if (scheduledRecording.tvChannelId == item.tvChannelId &&
                scheduledRecording.scheduledDateStart == item.scheduledDateStart
            ) {
                return item.id
            }
        }
        return -1
    }

    override fun clearRecordingListPvr() {
        clearRecordingList()
        recordings.clear()
        dbRecList.clear()
    }

    private fun recreateStoredRecording(recording: ScheduledRecording) {
        val tvChannel: TvChannel? = getChannelById(recording.tvChannelId)
        if (tvChannel != null) {
            val currentTime = timeInterface.getCurrentTime(tvChannel!!)
            recording.name.let { name ->
                recording.scheduledDateStart.let { startTime ->
                    eventsList.forEach { event ->
                        if (event.name == name && event.startTime == startTime) {
                            if (event.startTime > currentTime) {
                                val scheduleRecording = ScheduledRecording(
                                    id = recording.id,
                                    name = recording.name,
                                    scheduledDateStart = recording.scheduledDateStart,
                                    scheduledDateEnd = recording.scheduledDateEnd,
                                    tvChannelId = recording.tvChannelId,
                                    tvEventId = recording.tvEventId,
                                    repeatFreq = recording.repeatFreq,
                                    tvChannel = tvChannel,
                                    tvEvent = recording.tvEvent
                                )
                                scheduleRecording(
                                    scheduleRecording,
                                    object :
                                        IAsyncDataCallback<SchedulerInterface.ScheduleRecordingResult> {
                                        override fun onFailed(error: Error) {}
                                        override fun onReceive(data: SchedulerInterface.ScheduleRecordingResult) {}
                                    })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getChannelById(channelId: Int): TvChannel? {
        val channelList = dataProvider.getChannelList()
        for (i in 0 until channelList.size) {
            if (channelList[i].id == channelId) {
                return channelList[i]
            }
        }
        return null
    }

    override fun updateConflictRecordings(recording: ScheduledRecording, addRemove: Boolean) {
        if (addRemove) {
            if (conflictedRecList.contains(recording)) {
            } else {
                conflictedRecList.add(recording)
            }
        } else {
            if (conflictedRecList.contains(recording)) {
                conflictedRecList.remove(recording)
            }
        }
    }

    override fun getRecList(callback: IAsyncDataCallback<MutableList<ScheduledRecording>>) {
        callback.onReceive(recordings)
    }

    override fun getScheduledRecListCount(callback: IAsyncDataCallback<Int>) {
        callback.onReceive(recordings.size)
    }

    override fun hasScheduledRec(tvEvent: TvEvent?, callback: IAsyncDataCallback<Boolean>) {
        callback.onReceive(if (tvEvent != null) isInReclist(tvEvent.tvChannel.id, tvEvent.startTime) else false)
    }

    override fun getEventId(tvChannel: TvChannel, eventId: Int, callback: IAsyncDataCallback<TvEvent>
    ) {

        //Custom scheduled recording
        if (eventId == -1) {
            callback.onReceive(TvEvent.createNoInformationEvent(tvChannel, timeInterface.getCurrentTime(tvChannel)))
            return
        }

        val startTime: Long = timeInterface.getCurrentTime(tvChannel) - TimeUnit.HOURS.toMillis(0)
        val endTime: Long = timeInterface.getCurrentTime(tvChannel) + TimeUnit.HOURS.toMillis((24 * 8).toLong())

        epgInterface.getEventListByChannelAndTime(tvChannel,
            startTime,
            endTime,
            object : IAsyncDataCallback<ArrayList<TvEvent>> {
                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }

                override fun onReceive(data: ArrayList<TvEvent>) {

                    data.forEach {
                        if (it.id == eventId) {
                            callback.onReceive(it)
                            return
                        }
                    }

                    callback.onFailed(Error("Empty list"))
                }
            })
    }

    override fun isInReclist(channelId: Int, startTime: Long): Boolean {
        recordings.forEach { item ->
            if (item.tvChannelId == channelId) {
                if (item.scheduledDateStart == startTime) {
                    return true
                }
            }
        }
        return false
    }

    override fun isInConflictedList(recording: ScheduledRecording): Boolean {
        conflictedRecList.forEach { item ->
            if (recording.name == item.name &&
                recording.scheduledDateStart == item.scheduledDateStart &&
                recording.tvChannel?.name == item.tvChannel?.name
            ) {
                return true
            }
        }
        return false
    }

    override fun getScheduledRecordingsList(callback: IAsyncDataCallback<ArrayList<ScheduledRecording>>) {
        var retVal = ArrayList<ScheduledRecording>()
        recordings.forEach { item ->    //  recordings.forEach { item -> retVal.add(item) }
            if (retVal.size > 0) {
                retVal.forEach {
                    if (it.id != item.id && item.scheduledDateStart != it.scheduledDateStart)
                        retVal.add(item)
                }
            } else retVal.add(item)
        }
        callback.onReceive(retVal)
    }

    override fun removeScheduledRecordingForDeletedChannels() {
        CoroutineHelper.runCoroutine({
            val channelsIdList = mutableListOf<Int>()
            dataProvider.getChannelList().forEach { tvChannel ->
                channelsIdList.add(tvChannel.id)
            }
            //create a tempRecList to avoid the concurrent exception while traversing through the list
            val tempRecList = mutableListOf<ScheduledRecording>()
            recordings.forEach { item -> tempRecList.add(item) }
            tempRecList.forEach {
                //removing scheduled recording events for deleted channels
                if (!channelsIdList.contains(it.tvChannelId)) {
                    removeScheduledRecording(it,
                        object : IAsyncCallback {
                            override fun onFailed(error: Error) {}
                            override fun onSuccess() {}
                        }
                    )
                }
            }
        })
    }

    override fun loadScheduledRecording() {
        epgInterface.getEventList(
            object : IAsyncDataCallback<java.util.ArrayList<TvEvent>> {
                override fun onFailed(error: Error) {}

                override fun onReceive(data: java.util.ArrayList<TvEvent>) {
                    eventsList.clear()
                    eventsList.addAll(data)
                    recordings.clear()
                    dbRecList.clear()
                    val scheduledRecording: java.util.ArrayList<ScheduledRecording>? =
                        schedulerDataProvider?.getScheduledRecordingData()
                    if (scheduledRecording != null) {
                        if (scheduledRecording.size > 0) {
                            dbRecList.addAll(scheduledRecording)
                            scheduledRecording.forEach {
                                recreateStoredRecording(it)
                            }
                            //TODO - Tanvi - add similar event for recording
//                            InformationBus.informationBusEventListener.submitEvent(Events.WATCHLIST_RESTORED)
                        }
                    }
                }
            })
    }

    private fun addAndStoreScheduledRecording(recording: ScheduledRecording) {
        val tvChannel: TvChannel? = getChannelById(recording.tvChannelId)
        val dbSchedulerRecording = ScheduledRecording(
            recording.id,
            recording.name,
            recording.scheduledDateStart,
            recording.scheduledDateEnd,
            recording.tvChannelId,
            recording.tvEventId,
            recording.repeatFreq,
            tvChannel,
            recording.tvEvent
        )

        var isExisting = false
        dbRecList.forEach {
            if (it.tvEventId == findRecordingId(recording)) {
                isExisting = true
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isExisting) {

                dbRecList.add(dbSchedulerRecording)
                if (isScheduledLoaded) {
                    CoroutineHelper.runCoroutine({
                        schedulerDataProvider!!.storeScheduledRecording(
                            dbSchedulerRecording,
                            object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: $error")
                                }

                                override fun onSuccess() {
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "Schedule ${dbSchedulerRecording.name} recording success"
                                    )
                                }

                            })
                    })
                }
            }

        }, 1000)
    }
}
