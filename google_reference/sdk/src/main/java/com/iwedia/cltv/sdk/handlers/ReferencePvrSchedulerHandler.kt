package com.iwedia.cltv.sdk.handlers

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.entities.*
import core_entities.Error
import core_entities.ScheduledRecording
import core_entities.ScheduledReminder
import data_type.GList
import data_type.GLong
import handlers.DataProvider
import handlers.SchedulerHandler
import handlers.TimeHandler
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import utils.information_bus.events.Events
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class ReferencePvrSchedulerHandler(dataProvider: DataProvider<*>) :
    SchedulerHandler<ReferenceTvChannel, ReferenceTvEvent, ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>, ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>>(
        dataProvider
    ) {

    enum class ScheduleRecordingResult {
        SCHEDULE_RECORDING_SUCCESS,
        SCHEDULE_RECORDING_ERROR,
        SCHEDULE_RECORDING_CONFLICTS,
        SCHEDULE_RECORDING_ALREADY_PRESENT
    }

    private val conflictOffset = 1 * 60 * 1000 // 1 minute

    private val TAG = javaClass.simpleName + ": "

    /**
     * List of scheduled objects added in recording list
     */
    var recordings = mutableListOf<ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>>()

    /**
     * Database scheduled recordings list
     */
    var dbRecList = mutableListOf<ScheduledRecordingData>()

    var conflictedRecList = mutableListOf<ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>>()

    /**
     * Is scheduled events loaded from prefs
     */
    private var isScheduledLoaded = false

    lateinit var schedulerHelper: SchedulerHelper

    private var isSheduleWithinMinute = false

    fun scheduleRecording(
        scheduledRecording: ReferenceScheduledRecording,
        callback: AsyncDataReceiver<ScheduleRecordingResult>
    ) {
        if(!isUsbConnected()){
            Toast.makeText(ReferenceSdk.context,"USB NOT CONNECTED\nConnect USB to schedule record", Toast.LENGTH_LONG).show()
            return
        }
        var startTime: GLong?

        //Time before start of programme
        var timeBeforeStartProgramme: Long = 0

        var repeatFreq: Int? = (scheduledRecording.repeatFreq)!!.toInt()
        startTime = (scheduledRecording.scheduledDateStart)

        val eventStartTime = startTime!!.value.toLong()

        //Current time
        TimeHandler.getCurrentTime(object : AsyncDataReceiver<GLong> {
            override fun onFailed(error: Error?) {
                Log.i("TAG", "onFailed: getCurrentTime failed")
            }

            override fun onReceive(currentTime: GLong) {

                Log.d(Constants.LogTag.CLTV_TAG + "SCHEDULE_", "START TIME "+Date(eventStartTime))
                Log.d(Constants.LogTag.CLTV_TAG + "SCHEDULE_", "CURRENT TIME "+Date(currentTime.value.toLong()))

                timeBeforeStartProgramme = (eventStartTime - currentTime.value.toLong())

                //check if recording of same start time already exists
                if (findConflictedRecordings(scheduledRecording).isNotEmpty()) {
                    callback.onReceive(ScheduleRecordingResult.SCHEDULE_RECORDING_ALREADY_PRESENT)
                    return
                }

                /*//check if reminder is scheduled at same start time
                result =
                    (ReferenceSdk.watchlistHandler as ReferenceWatchlistHandler).checkReminderConflict(scheduledRecording.tvChannel.id,
                        scheduledRecording.scheduledDateStart.value.toLong()
                    )
                if (result) {
                    callback.onReceive(ScheduleRecordingResult.SCHEDULE_RECORDING_CONFLICTS)
                }*/

                if (timeBeforeStartProgramme > 0) {

                    try {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "repeatFreq :$repeatFreq ")
                        schedule(timeBeforeStartProgramme, scheduledRecording)
                        callback.onReceive(ScheduleRecordingResult.SCHEDULE_RECORDING_SUCCESS)
                        return
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: Cannot schedule recording")
                        callback.onReceive(ScheduleRecordingResult.SCHEDULE_RECORDING_ERROR)
                        return
                    }
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: Cannot schedule $timeBeforeStartProgramme $eventStartTime")
                    callback.onReceive(ScheduleRecordingResult.SCHEDULE_RECORDING_ERROR)
                }
            }
        })
    }

    private fun updateTimerForScheduledRecording(recording: ReferenceScheduledRecording) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateTimerForScheduledRecording: ")
        if (recording.repeatFreq == null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateTimerForScheduledRecording: recording data is null")
            return
        }
        TimeHandler.getCurrentTime(object : AsyncDataReceiver<GLong> {
            override fun onFailed(error: Error?) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to get current time")
            }

            override fun onReceive(data: GLong) {

                var currentTime = data.value.toLong()
                val durationToStartRecording =
                    (recording.scheduledDateStart.value.toLong() - currentTime)
                if (recording.repeatFreq == ReferenceScheduledRecording.REPEAT_FLAG.NONE) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "repeat flag is NONE")
                    if (recording.scheduledDateStart.value.toLong() >= currentTime) {
                        recordings.add(
                            recording
                        )
                        // scheduleRecording
                        schedule(durationToStartRecording, recording)
                    } else {
                        // remove recording
                        removeScheduledRecording(recording, object : AsyncReceiver {
                            override fun onFailed(error: Error?) {

                            }

                            override fun onSuccess() {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledRecording Success")
                            }
                        })
                    }
                } else {

                    var recordingStart = recording.scheduledDateStart.value.toLong()
                    var recordingEnd = recording.scheduledDateEnd.value.toLong()
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "recordingStart = ${Date(recordingStart)}")
                    while (currentTime > recordingStart) {
                        val oneDayDuration = (24 * 60 * 60 * 1000)
                        if (recording.repeatFreq == ReferenceScheduledRecording.REPEAT_FLAG.DAILY) {
                            recordingStart += oneDayDuration
                            recordingEnd += oneDayDuration
                        } else if (recording.repeatFreq == ReferenceScheduledRecording.REPEAT_FLAG.WEEKLY) {
                            recordingStart += (7 * oneDayDuration)
                            recordingEnd += (7 * oneDayDuration)
                        }
                    }

                    val scheduledRecording = ReferenceScheduledRecording(
                        recording.id,
                        recording.name,
                        GLong(recordingStart.toString()),
                        GLong(recordingEnd.toString()),
                        recording.tvChannel,
                        recording.tvEvent,
                        recording.repeatFreq,
                    )
                    recordings.add(
                        scheduledRecording
                    )

                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "repeat timeBeforeStartProgramme = ${durationToStartRecording / 60000} minutes"
                    )
                    schedule(durationToStartRecording, scheduledRecording)

                }
            }
        })
    }

    private fun isUsbConnected(): Boolean {
        var usbManager = ReferenceSdk.context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.size > 0
    }

    fun checkRecordingConflict(startTime: Long): Boolean {
        dbRecList.forEach {
            if (it.scheduledDateStart == startTime) {
                return true
            }
        }
        return false
    }

    fun schedule(durationToStartRecording: Long, scheduledRecording: ReferenceScheduledRecording) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "schedule : $scheduledRecording")
        val durationToNotify = durationToStartRecording - 60000

        isSheduleWithinMinute = (durationToNotify < 0)

        storeScheduledRecording(scheduledRecording)

        Log.d(Constants.LogTag.CLTV_TAG + "SCHEDULE_", "DURATION BEFORE START (IN MILLIS) "+durationToNotify)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (recordings.contains(scheduledRecording)) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "run: EVENT SUBMITTED")

                    // DIRECT START RECORDING WHEN USER IN SAME CHANNEL (NO NEED TO SHOW CHANNEL CHANGE DIALOG 1 MINUTE BEFORE)
                    if((ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel!!.id == scheduledRecording.tvChannel.id){

                        val timeRemainingToStartRecord = durationToStartRecording - max(durationToNotify,0)

                        Log.d(Constants.LogTag.CLTV_TAG + "SCHEDULE_", "DIRECT DURATION BEFORE START (IN MILLIS) "+timeRemainingToStartRecord)

                         Timer().schedule(object : TimerTask() {
                            override fun run() {

                                if (recordings.contains(scheduledRecording)) {

                                    InformationBus.submitEvent(
                                        Event(
                                            ReferenceEvents.SCHEDULED_RECORDING_NOTIFICATION_START,
                                            scheduledRecording
                                        )
                                    )
                                    removeAndReschedule()
                                }
                            }
                        },max(0, timeRemainingToStartRecord))
                    }else{
                        //SHOW CHANNEL CHANGE DIALOG 1 MINUTE BEFORE
                        InformationBus.submitEvent(
                            Event(
                                ReferenceEvents.SCHEDULED_RECORDING_NOTIFICATION,
                                scheduledRecording
                            )
                        )
                        removeAndReschedule()
                    }
                }
            }

            fun removeAndReschedule(){

                removeScheduledRecording(scheduledRecording,
                    object : AsyncReceiver {
                        override fun onFailed(error: Error?) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to remove and reschedule recordings!! ")
                        }

                        override fun onSuccess() {

                            //Current time
                            TimeHandler.getCurrentTime(object : AsyncDataReceiver<GLong> {
                                override fun onFailed(error: Error?) {
                                    Log.i("TAG", "onFailed: getCurrentTime failed")
                                }

                                override fun onReceive(currentTime: GLong) {

                                    Log.d(Constants.LogTag.CLTV_TAG + "SCHEDULE_", "START TIME "+Date(scheduledRecording.scheduledDateStart.value.toLong()))
                                    Log.d(Constants.LogTag.CLTV_TAG + "SCHEDULE_", "CURRENT TIME "+Date(currentTime.value.toLong()))

                                    val timeBeforeStartProgramme = (scheduledRecording.scheduledDateStart.value.toLong() - currentTime.value.toLong())- 60000

                                    when (scheduledRecording.repeatFreq) {
                                        ReferenceScheduledRecording.REPEAT_FLAG.DAILY -> {
                                            scheduleWithDailyRepeat(
                                                timeBeforeStartProgramme,
                                                scheduledRecording
                                            )
                                            Log.d(Constants.LogTag.CLTV_TAG +
                                                "SCHEDULED RECORDING",
                                                "Recording scheduled for ${(timeBeforeStartProgramme) / 60000} minutes"
                                            )
                                        }
                                        ReferenceScheduledRecording.REPEAT_FLAG.WEEKLY -> {
                                            scheduleWithWeeklyRepeat(
                                                timeBeforeStartProgramme,
                                                scheduledRecording
                                            )
                                            Log.d(Constants.LogTag.CLTV_TAG +
                                                "SCHEDULED RECORDING",
                                                "Recording scheduled for ${(timeBeforeStartProgramme) / 3600000} hours"
                                            )
                                        }
                                        ReferenceScheduledRecording.REPEAT_FLAG.NONE -> {
                                            // no need to repeat
                                        }
                                    }
                                }
                            })
                        }
                    })
                }
        }, if(durationToNotify<0) 0 else durationToNotify)
        InformationBus.submitEvent(
            Event(ReferenceEvents.RECORDING_SCHEDULED_TOAST,
                isSheduleWithinMinute
            )
        )
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "schedule: displayed toast")
    }

    fun scheduleWithDailyRepeat(
        durationToStartRecording: Long,
        scheduledRecordingPrevious: ReferenceScheduledRecording
    ) {
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "Recording will be start in ${durationToStartRecording/60000} minutes"
        )

        val recordingStart = (scheduledRecordingPrevious.scheduledDateStart.value.toLong() + (24 * 60 * 60 * 1000))
        val recordingEnd = (scheduledRecordingPrevious.scheduledDateEnd.value.toLong() + (24 * 60 * 60 * 1000))

        val scheduledRecording = ReferenceScheduledRecording(
            scheduledRecordingPrevious.id,
            scheduledRecordingPrevious.name,
            GLong(recordingStart.toString()),
            GLong(recordingEnd.toString()),
            scheduledRecordingPrevious.tvChannel,
            scheduledRecordingPrevious.tvEvent,
            scheduledRecordingPrevious.repeatFreq,
        )
        var timeBeforeStartProgramme = (durationToStartRecording + (24 * 60 * 60 * 1000))
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "Recording will be Scheduled for ${Date(timeBeforeStartProgramme)}"
        )
        Timer().schedule(object : TimerTask() {

            override fun run() {
                InformationBus.submitEvent(
                    Event(
                        ReferenceEvents.SCHEDULED_RECORDING_NOTIFICATION,
                        scheduledRecording
                    )
                )
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "run: EVENT SUBMITTED")
                //Recursive call for scheduling daily
                scheduleWithDailyRepeat(timeBeforeStartProgramme, scheduledRecording)
            }
        }, timeBeforeStartProgramme)
        storeScheduledRecording(scheduledRecording)
    }

    fun scheduleWithWeeklyRepeat(
        durationToStartRecording: Long,
        scheduledRecordingPrevious: ReferenceScheduledRecording
    ) {
        val recordingStart = (scheduledRecordingPrevious.scheduledDateStart.value.toLong() +  (7 * (24 * 60 * 60 * 1000)))
        val recordingEnd = (scheduledRecordingPrevious.scheduledDateEnd.value.toLong() +  (7 * (24 * 60 * 60 * 1000)))

        val scheduledRecording = ReferenceScheduledRecording(
            scheduledRecordingPrevious.id,
            scheduledRecordingPrevious.name,
            GLong(recordingStart.toString()),
            GLong(recordingEnd.toString()),
            scheduledRecordingPrevious.tvChannel,
            scheduledRecordingPrevious.tvEvent,
            scheduledRecordingPrevious.repeatFreq,
        )

        var timeBeforeStartProgramme = (durationToStartRecording + (7 * (24 * 60 * 60 * 1000)))
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "Recording will be Scheduled for ${Date(timeBeforeStartProgramme)}"
        )
        Timer().schedule(object : TimerTask() {

            override fun run() {
                InformationBus.submitEvent(
                    Event(
                        ReferenceEvents.SCHEDULED_RECORDING_NOTIFICATION,
                        scheduledRecording
                    )
                )
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "run: EVENT SUBMITTED")
                //Recursive call for scheduling weekly
                scheduleWithWeeklyRepeat(timeBeforeStartProgramme, scheduledRecording)
            }
        }, timeBeforeStartProgramme)
        storeScheduledRecording(scheduledRecording)
        return
    }

    fun getId(scheduledRecording: ScheduledRecordingData): Int {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getId: ############## GET ID IN PVR SCHEDULER HANDLER ${recordings.size}")
        dbRecList.forEach { item ->
            if (scheduledRecording.tvChannelId == item.tvChannelId &&
                scheduledRecording.scheduledDateStart == item.scheduledDateStart
            ) {
                return item.id
            }
        }
        return -1
    }

    override fun removeScheduledRecording(
        recording: ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>,
        callback: AsyncReceiver
    ) {
        val removeList = mutableListOf<ScheduledRecordingData>()
        val removeList2 = mutableListOf<ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>>()

        dbRecList.forEach { item ->
            if (item.name == recording.name) {
                if (item.scheduledDateStart == recording.scheduledDateStart.value.toLong()) {
                    removeList.add(item)
                }
            }
        }

        removeList.forEach { it ->
            schedulerHelper.removeScheduledRecording(it)
            dbRecList.remove(it)
        }

        recordings.forEach { item ->
            if (item.name == recording.name) {
                if (item.scheduledDateStart.value.toLong() == recording.scheduledDateStart.value.toLong()) {
                    removeList2.add(item)
                }
            }
        }

        removeList2.forEach { item ->
            recordings.remove(item)
        }

        InformationBus.submitEvent(Event(Events.SCHEDULED_RECORDING_REMOVED))
        callback.onSuccess()
    }

    fun clearRecordingList() {
        schedulerHelper.clearRecordingList()
        recordings.clear()
        dbRecList.clear()
    }

    private fun loadFromDatabase(callback: AsyncReceiver) {
        CoroutineHelper.runCoroutine( {
            recordings.clear()
            dbRecList.clear()

            dbRecList = schedulerHelper.getScheduledRecordingData()

            var data = GLong(System.currentTimeMillis().toString())

            var currentTime = Date(data.value.toLong())
            var listToRemove = mutableListOf<ScheduledRecordingData>()
            dbRecList.forEach { scheduledRecordingData ->
                if (scheduledRecordingData.scheduledDateStart < currentTime!!.time) {
                    listToRemove.add(scheduledRecordingData)
                }
            }
            //Remove expired recording data from db
            listToRemove.forEach {
                schedulerHelper.removeScheduledRecording(it)
            }

            //Recreate stored recordings
            recreateStoredRecording(object : AsyncReceiver {
                override fun onFailed(error: Error?) {
                    callback.onFailed(error)
                }

                override fun onSuccess() {
                    callback.onSuccess()
                }
            })
        })
    }

    private fun recreateStoredRecording(callback: AsyncReceiver) {
        if (dbRecList != null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " recreateStoredRecording")
            Log.i(TAG, " List size: ${dbRecList.size}")
            var index = AtomicInteger(0)
            if (dbRecList.size > 0) {
                dbRecList.forEach {
                    if (getChannelById(it.tvChannelId) != null) {
                        var tvChannel: ReferenceTvChannel =
                            getChannelById(it.tvChannelId) as ReferenceTvChannel
                        var tvEvent: ReferenceTvEvent? = null
                        ReferenceSdk.epgHandler!!.getEventListByChannelAndTime(tvChannel!!,
                            GLong(it.scheduledDateStart.toString()),
                            GLong(it.scheduledDateEnd.toString()),
                            object : AsyncDataReceiver<GList<ReferenceTvEvent>> {
                                override fun onFailed(error: Error?) {
                                    index.incrementAndGet()

                                    tvEvent = ReferenceTvEvent.createNoInformationEvent(
                                        tvChannel,
                                        Date(it.scheduledDateStart),
                                        Date(it.scheduledDateEnd)
                                    )
                                    addRecording(
                                        ReferenceScheduledRecording(
                                            it.id,
                                            it.name,
                                            GLong(it.scheduledDateStart.toString()),
                                            GLong(it.scheduledDateEnd.toString()),
                                            tvChannel,
                                            tvEvent,
                                            it.data.toInt()
                                        )
                                    )
                                }

                                fun addRecording(recording: ReferenceScheduledRecording) {
                                    updateTimerForScheduledRecording(recording)

                                    if (index.get() == dbRecList.size)
                                        callback.onSuccess()
                                }

                                override fun onReceive(events: GList<ReferenceTvEvent>) {
                                    index.incrementAndGet()
                                    tvEvent = events.get(0)

                                    if (tvEvent == null) {
                                        tvEvent = ReferenceTvEvent.createNoInformationEvent(
                                            tvChannel,
                                            Date(it.scheduledDateStart),
                                            Date(it.scheduledDateEnd)
                                        )
                                    }

                                    addRecording(
                                        ReferenceScheduledRecording(
                                            it.id,
                                            it.name,
                                            GLong(it.scheduledDateStart.toString()),
                                            GLong(it.scheduledDateEnd.toString()),
                                            tvChannel,
                                            tvEvent,
                                            it.data.toInt()
                                        )
                                    )
                                }//onReceive
                            })//AsyncCallEnd
                    } else if (index.get() == dbRecList.size) {
                        callback.onSuccess()
                    } else {
                        index.incrementAndGet()
                        if (index.get() == dbRecList.size)
                            callback.onSuccess()
                    }
                }//forloop
            } else {
                Log.i(TAG, " getScheduledRecordingsList: Size is 0 ")
                callback.onFailed(Error(404, "No scheduled recordings found"))
            }
        } else {
            Log.i(TAG, " getScheduledRecordingsList: templist is null")
            callback.onFailed(Error(404, "No scheduled recordings found"))
        }
    }


    fun storeScheduledRecording(recording: ReferenceScheduledRecording) {
        var eventId = -1
        if (recording.tvEvent != null) {
            //eventId = recording.tvEvent.id
        }

        val scheduledRecordingData = ScheduledRecordingData(
            recording.id,
            recording.name,
            recording.scheduledDateStart.value.toLong(),
            recording.scheduledDateEnd.value.toLong(),
            recording.tvChannel.id,
            eventId,
            recording.repeatFreq.toString()
        )
        var isExisting = false
        dbRecList.forEach {
            if (it.id == recording.id) {
                if (it.name == recording.name) {
                    isExisting = true
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        " storeScheduledRecording: Recording exists in database"
                    )
                    return
                }
            }
        }

        if (!isExisting) {
            schedulerHelper.storeScheduledRecording(scheduledRecordingData)
            var id = schedulerHelper.getRecodingId(scheduledRecordingData)
            scheduledRecordingData.id = id
            dbRecList.add(scheduledRecordingData)
            recording.updateId(id)
            recordings.add(recording)
            InformationBus.submitEvent(Event(Events.SCHEDULED_RECORDING_ADD))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " storeScheduledRecording: Recording added to database")
        }
    }

    fun updateConflictRecordings(recording: ReferenceScheduledRecording, addRemove: Boolean){
        if (addRemove){
            if (conflictedRecList.contains(recording)){
            } else {
                conflictedRecList.add(recording)
            }
        } else {
            if (conflictedRecList.contains(recording)){
                conflictedRecList.remove(recording)
            } else {
            }
        }
    }

    fun getRecList(callback: AsyncDataReceiver<MutableList<ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>>>) {
        callback.onReceive(recordings)
    }

    fun hasScheduledRec(tvEvent: ReferenceTvEvent?, callback: AsyncDataReceiver<Boolean>) {
        recordings.forEach {
            if (it.tvEvent?.id != -1 && it.tvEvent?.id == tvEvent!!.id) {
                callback.onReceive(true)
            }
        }
        callback.onReceive(false)
    }

    fun getEventId(
        tvChannel: ReferenceTvChannel,
        eventId: Int,
        callback: AsyncDataReceiver<ReferenceTvEvent>
    ) {

        //Custom scheduled recording
        if (eventId == -1) {
            callback.onReceive(ReferenceTvEvent.createNoInformationEvent(tvChannel))
            return
        }


        val startTime: Long = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(0)
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

    fun getChannelById(channelId: Int): ReferenceTvChannel? {
        var channelList = ReferenceSdk.tvHandler!!.getChannelList()
        for (i in 0 until channelList.size()) {

            if (channelList.get(i)!!.id == channelId) {
                return channelList.get(i)
            }
        }

        return null
    }

    override fun setup() {
        schedulerHelper = SchedulerHelper()

        loadFromDatabase(object : AsyncReceiver {
            override fun onFailed(error: Error?) {

            }

            override fun onSuccess() {
                isScheduledLoaded = true
            }
        })
    }

    override fun dispose() {}

    fun isInReclist(referenceTvEvent: ReferenceTvEvent): Boolean {
        recordings.forEach { item ->
            if (item.tvChannel.name == referenceTvEvent.tvChannel.name &&
                item.tvEvent?.name == referenceTvEvent.name &&
                    item.tvEvent?.startDate?.value == referenceTvEvent.startDate.value) {
                return true
            }
        }
        return false
    }

    fun isInConflictedList(recording: ReferenceScheduledRecording): Boolean{
        conflictedRecList.forEach { item ->
            if (recording.name == item.name &&
                recording.scheduledDateStart.value == item.scheduledDateStart.value &&
                    recording.tvChannel.name == item.tvChannel.name){
                return true
            }
        }
        return false
    }

    fun findConflictedRecordings(referenceScheduledRecording: ReferenceScheduledRecording) : MutableList<ReferenceScheduledRecording> {

        val startTime = referenceScheduledRecording.tvEvent!!.startDate.value.toLong()
        val endTime = referenceScheduledRecording.tvEvent!!.endDate.value.toLong()

        return findConflictedRecordings(startTime, endTime)
    }


    fun findConflictedRecordings(recordingStartTime : Long, recordingEndTime : Long) : MutableList<ReferenceScheduledRecording> {

        val startTime = recordingStartTime - conflictOffset
        val endTime = recordingEndTime + conflictOffset

        val conflictedRecordings = mutableListOf<ReferenceScheduledRecording>()

        run exitForEach@{
            recordings.forEach { item ->

                if ((startTime > item.scheduledDateStart.value.toLong() && startTime > item.scheduledDateEnd.value.toLong()) //  Event completely left side of selected event
                    || (endTime < item.scheduledDateStart.value.toLong() && endTime < item.scheduledDateEnd.value.toLong()) //  Event completely right side of selected event
                ) {
                    return@forEach// no conflicts in this event, so check next event
                } else {
                    // having conflicts
                    conflictedRecordings.add(
                        ReferenceScheduledRecording(
                            item.id,
                            item.name,
                            item.scheduledDateStart,
                            item.scheduledDateEnd,
                            item.tvChannel,
                            item.tvEvent!!,
                            ReferenceScheduledRecording.REPEAT_FLAG.NONE
                        )
                    )

                }
            }
        }

        return conflictedRecordings
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun getScheduledRecordingsList(callback: AsyncDataReceiver<GList<ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>>>) {
        Log.i("TAG", "getScheduledRecordingsList: RecordingList size = ${recordings.size}")

        var retVal = GList<ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>>()
        recordings.forEach { item -> retVal.add(item) }
        callback.onReceive(retVal)
    }

    fun removeScheduledRecordingForDeletedChannels() {
        CoroutineHelper.runCoroutine({
            var channelsIdList = mutableListOf<Int>()
            ReferenceSdk.tvHandler!!.getChannelList().value.forEach { tvChannel->
                channelsIdList.add(tvChannel.id)
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledRecordingsForDeletedChannels recordings.size is ${recordings.size}")
            //create a tempRecordingList to avoid the concurrent exception while traversing through the list
            var tempRecordingList = mutableListOf<ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>>()
            recordings.forEach { item -> tempRecordingList.add(item) }
            tempRecordingList.forEach {
                //removing scheduled recording events for deleted channels
                if (!channelsIdList.contains(it.tvChannel.id)) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG,"removeScheduledRecordingsForDeletedChannels - removing for ${it.tvChannel.id}")
                    removeScheduledRecording(it,
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