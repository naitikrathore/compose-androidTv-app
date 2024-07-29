package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

interface ScheduledInterface {
    fun getScheduledRecordingsList(callback: IAsyncDataCallback<List<ScheduledRecording>>)
    fun getScheduledRemindersList(callback: IAsyncDataCallback<List<ScheduledReminder>>)
}