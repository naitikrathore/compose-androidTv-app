package com.iwedia.cltv.platform.base.content_provider

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.iwedia.cltv.platform.`interface`.ScheduledReminderInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

class ScheduleReminderDataProvider(var context: Context, var tvModule: TvInterface, var timeInterface: TimeInterface) :
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
                val scheduledReminder = (createScheduledRemindersFromCursor(cursor, tvModule, timeInterface))
                if (scheduledReminder != null) {
                    scheduledReminders.add(scheduledReminder)
                }
            }
            while (cursor.moveToNext())
        }
        cursor?.close()
    }

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