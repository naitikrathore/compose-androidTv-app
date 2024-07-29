package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

interface SchedulerInterface {
    enum class ScheduleRecordingResult {
        SCHEDULE_RECORDING_SUCCESS,
        SCHEDULE_RECORDING_ERROR,
        SCHEDULE_RECORDING_CONFLICTS,
        SCHEDULE_RECORDING_ALREADY_PRESENT
    }
    fun getRecodingId(recordedData: ScheduledRecording,callback: IAsyncDataCallback<Int>)
    fun removeScheduledRecording(scheduledRecording: ScheduledRecording, callback: IAsyncCallback?)
    fun clearRecordingList()
    fun scheduleRecording(
        scheduledRecording: ScheduledRecording,
        callback: IAsyncDataCallback<ScheduleRecordingResult>
    )
    fun findConflictedRecordings(referenceScheduledRecording: ScheduledRecording) : MutableList<ScheduledRecording>?
    fun findConflictedRecordings(recordingStartTime : Long, recordingEndTime : Long) : MutableList<ScheduledRecording>
    fun schedule(durationToStartRecording: Long, scheduledRecording: ScheduledRecording)
    fun removeAllScheduledRecording(
        recording: ScheduledRecording,
        callback: IAsyncCallback
    )
    fun scheduleWithDailyRepeat(
        durationToStartRecording: Long,
        scheduledRecordingPrevious: ScheduledRecording
    )
    fun scheduleWithWeeklyRepeat(
        durationToStartRecording: Long,
        scheduledRecordingPrevious: ScheduledRecording
    )
    fun checkRecordingConflict(startTime: Long): Boolean
    fun getId(scheduledRecording: ScheduledRecording): Int
    fun clearRecordingListPvr()
    fun getChannelById(channelId: Int): TvChannel?
    fun updateConflictRecordings(recording: ScheduledRecording, addRemove: Boolean)
    fun getRecList(callback: IAsyncDataCallback<MutableList<ScheduledRecording>>)
    fun hasScheduledRec(tvEvent: TvEvent?, callback: IAsyncDataCallback<Boolean>)
    fun getEventId(
        tvChannel: TvChannel,
        eventId: Int,
        callback: IAsyncDataCallback<TvEvent>
    )
    fun isInReclist(channelId: Int, startTime: Long): Boolean
    fun isInConflictedList(recording: ScheduledRecording): Boolean
    fun getScheduledRecordingsList(callback: IAsyncDataCallback<ArrayList<ScheduledRecording>>)
    fun removeScheduledRecordingForDeletedChannels()
    fun loadScheduledRecording()
    fun getScheduledRecListCount(callback: IAsyncDataCallback<Int>)
    fun getNewRec() : ScheduledRecording?
}