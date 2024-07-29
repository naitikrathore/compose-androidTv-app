package com.iwedia.cltv.platform.mal_service

import android.content.Context
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.ScheduledInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.mal_service.common.ScheduleRecordingDataProvider
import com.iwedia.cltv.platform.mal_service.common.ScheduleReminderDataProvider
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

class ScheduledInterfaceImpl(
    val context: Context,
    private val serviceImpl: IServiceAPI,
    val tvInterface: TvInterface
) : ScheduledInterface {


    private var scheduleRecordingsDataProvider : ScheduleRecordingDataProvider?= null
    private var scheduleReminderDataProvider : ScheduleReminderDataProvider? = null

    init {
        scheduleRecordingsDataProvider  = ScheduleRecordingDataProvider(context, tvInterface, serviceImpl)
        scheduleReminderDataProvider = ScheduleReminderDataProvider(context, tvInterface, serviceImpl)
    }

    override fun getScheduledRecordingsList(callback: IAsyncDataCallback<List<ScheduledRecording>>) {
        CoroutineHelper.runCoroutine({
            callback.onReceive(scheduleRecordingsDataProvider!!.getScheduledRecordings())
        })
    }

    override fun getScheduledRemindersList(callback: IAsyncDataCallback<List<ScheduledReminder>>) {
        CoroutineHelper.runCoroutine({
            callback.onReceive(scheduleReminderDataProvider!!.getScheduledReminders())
        })
    }
}