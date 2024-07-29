package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

interface SchedulerDataProviderInterface {
    fun storeScheduledReminder(scheduledReminder: ScheduledReminder, callback: IAsyncCallback)
    fun removeScheduledReminder(scheduledReminder: ScheduledReminder, callback: IAsyncCallback)
    fun getScheduledRemindersData(): ArrayList<ScheduledReminder>

    fun clearReminderList()

    fun storeScheduledRecording(scheduledRecording: ScheduledRecording, callback: IAsyncCallback)

    fun getRecodingId(recordedData: ScheduledRecording, callback: IAsyncDataCallback<Int>)

    fun removeScheduledRecording(scheduledRecording: ScheduledRecording, callback: IAsyncCallback)

    fun getScheduledRecordingData(): ArrayList<ScheduledRecording>

    fun clearRecordingList()
}