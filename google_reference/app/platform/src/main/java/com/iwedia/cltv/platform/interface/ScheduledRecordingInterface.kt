package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

interface ScheduledRecordingInterface {
    fun getScheduledRecordings(): ArrayList<ScheduledRecording>
    fun loadScheduledRecordings()
}