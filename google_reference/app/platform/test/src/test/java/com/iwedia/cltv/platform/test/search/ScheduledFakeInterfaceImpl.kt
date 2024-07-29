package com.iwedia.cltv.platform.test.search

import com.iwedia.cltv.platform.`interface`.ScheduledInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.util.ArrayList

class ScheduledFakeInterfaceImpl : ScheduledInterface {
    var scheduledRecordingsQueryList = ArrayList<ScheduledRecording>()
    override fun getScheduledRecordingsList(callback: IAsyncDataCallback<List<ScheduledRecording>>) {
        callback.onReceive(scheduledRecordingsQueryList)
    }

    var scheduledReminderQueryList = ArrayList<ScheduledReminder>()
    override fun getScheduledRemindersList(callback: IAsyncDataCallback<List<ScheduledReminder>>) {
        callback.onReceive(scheduledReminderQueryList)
    }
}