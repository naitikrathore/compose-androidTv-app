package com.iwedia.cltv.platform.base

import android.content.Context
import com.iwedia.cltv.platform.`interface`.ScheduledInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.base.content_provider.ScheduleRecordingDataProvider
import com.iwedia.cltv.platform.base.content_provider.ScheduleReminderDataProvider
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

open class ScheduledInterfaceBaseImpl (
    private var tvInterfaceImpl: TvInterface,
    private var context: Context,
    timeInterface: TimeInterface
) : ScheduledInterface {

    private val TAG = javaClass.simpleName

    private var scheduleRecordingsDataProvider : ScheduleRecordingDataProvider?= null
    private var scheduleReminderDataProvider : ScheduleReminderDataProvider? = null

    init {
        scheduleRecordingsDataProvider  = ScheduleRecordingDataProvider(context, tvInterfaceImpl, timeInterface)
        scheduleReminderDataProvider = ScheduleReminderDataProvider(context, tvInterfaceImpl, timeInterface)
    }

    override fun getScheduledRecordingsList(callback: IAsyncDataCallback<List<ScheduledRecording>>) {
        CoroutineHelper.runCoroutine({
            callback.onReceive(scheduleRecordingsDataProvider !!.getScheduledRecordings())
        })
    }

    override fun getScheduledRemindersList(callback: IAsyncDataCallback<List<ScheduledReminder>>) {
        CoroutineHelper.runCoroutine({
            callback.onReceive(scheduleReminderDataProvider !!.getScheduledReminders())
        })
    }
}