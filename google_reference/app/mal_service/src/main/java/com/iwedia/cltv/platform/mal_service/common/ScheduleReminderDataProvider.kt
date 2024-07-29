package com.iwedia.cltv.platform.mal_service.common

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.util.Log
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.ScheduledReminderInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.mal_service.toServiceChannel
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

class ScheduleReminderDataProvider(var context: Context, var tvModule: TvInterface, var serviceImpl: IServiceAPI) :
    ScheduledReminderInterface {
    private val TAG = javaClass.simpleName
    private val UPDATE_TIMEOUT = 10000L
    private var scheduledReminders = arrayListOf<ScheduledReminder>()
    private lateinit var databaseObserverScheduledReminders: ContentObserver
    private var updateTimer: CountDownTimer? = null

    init{
        loadScheduledReminders()

        databaseObserverScheduledReminders  = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " ScheduledReminders data updated")
                startUpdateTimer()
            }
        }

        context.contentResolver.registerContentObserver(
            ContentProvider.SCHEDULED_REMINDERS_URI,
            true,
            databaseObserverScheduledReminders
        )
    }

    override fun getScheduledReminders(): ArrayList<ScheduledReminder> = scheduledReminders

    override fun loadScheduledReminders() {
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            ContentProvider.SCHEDULED_REMINDERS_URI,
            null,
            null,
            null,
            null
        )
        scheduledReminders.clear()
        if (cursor!!.count > 0) {
            cursor?.moveToFirst()
            do {
                val scheduledReminder = (createScheduledRemindersFromCursor(cursor, tvModule, serviceImpl))
                if (scheduledReminder != null) {
                    scheduledReminders.add(scheduledReminder)
                }
            }
            while (cursor.moveToNext())
        }
        cursor?.close()
    }

    @SuppressLint("Range")
    fun createScheduledRemindersFromCursor(cursor: Cursor, tvModule: TvInterface, serviceImpl: IServiceAPI) : ScheduledReminder? {
        var id = -1
        var name = ""
        var tvEvent : TvEvent

        if (cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)) != null) {
            id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
        }

        if (cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN)) != null){
            name = cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN))
        }

        var tvChannel : TvChannel? = tvModule.getChannelById(
            cursor.getLong(
                cursor.getColumnIndex(Contract.ScheduledReminders.CHANNEL_ID_COLUMN)
            ).toInt()
        )

        if (tvChannel == null)
            return null

        tvEvent = TvEvent.createNoInformationEvent(
            tvChannel, serviceImpl.getCurrentTimeByChannel(
                toServiceChannel(tvChannel)
            )
        )

        return ScheduledReminder(
            id,
            name,
            tvChannel,
            tvEvent
        )
    }

//    private fun getChannelById(channelId: Int): TvChannel? {
//        var channels = channelDataProvider.getChannelList()
//        try {
//            channels.forEach { channel ->
//                Log.d(Constants.LogTag.CLTV_TAG +
//                    "VANDANA",
//                    "return any chahnneelll: list: " + (channel.channelId.toInt() == channelId)
//                )
//                if (channel.channelId.toInt() == channelId) {
//                    Log.d(Constants.LogTag.CLTV_TAG + "VANDANA", "return any chahnneelll: ")
//                    return channel
//                }
//            }
//        } catch (e: java.lang.Exception) {
//            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "getChannelById: ${e.message}")
//        }
//        return null
//    }

    /**
     * Stop data update timer if it is already started
     */
    private fun stopUpdateTimer() {
        if (updateTimer != null) {
            updateTimer!!.cancel()
            updateTimer = null
        }
    }

    /**
     * Start data update timer
     */
    private fun startUpdateTimer() {
        //Cancel timer if it's already started
        stopUpdateTimer()

        //Start new count down timer
        updateTimer = object :
            CountDownTimer(
                UPDATE_TIMEOUT,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                // Wait for event loading
                loadScheduledReminders()
            }
        }
        updateTimer!!.start()
    }
}