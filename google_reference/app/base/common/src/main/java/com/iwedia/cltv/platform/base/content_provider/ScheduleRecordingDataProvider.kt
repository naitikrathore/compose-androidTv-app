package com.iwedia.cltv.platform.base.content_provider

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.iwedia.cltv.platform.`interface`.ScheduledRecordingInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.recording.ScheduledRecording

class ScheduleRecordingDataProvider(var context: Context, var tvModule: TvInterface, var timeInterface: TimeInterface) :
    ScheduledRecordingInterface {

    private val TAG = javaClass.simpleName
    private var scheduledRecords = arrayListOf<ScheduledRecording>()
    private val UPDATE_TIMEOUT = 10000L
    private lateinit var databaseObserverScheduledRecordings: ContentObserver
    private var updateTimer: CountDownTimer? = null

    init {
        loadScheduledRecordings()

        databaseObserverScheduledRecordings = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " ScheduledRecordings data updated")
                startUpdateTimer()
            }
        }

        context.contentResolver.registerContentObserver(
            ContentProvider.SCHEDULED_RECORDINGS_URI,
            true,
            databaseObserverScheduledRecordings
        )

    }

    override fun loadScheduledRecordings() {
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            ContentProvider.SCHEDULED_RECORDINGS_URI,
            null,
            null,
            null,
            null
        )
        scheduledRecords.clear()
        if (cursor!!.count > 0) {
            cursor?.moveToFirst()
            do {
                val scheduledRecord = (createScheduledRecordingsFromCursor(cursor, tvModule, timeInterface))
                if (scheduledRecord != null) {
                    scheduledRecords.add(scheduledRecord)
                }
            }
            while (cursor.moveToNext())
        }
        cursor?.close()
    }

    override fun getScheduledRecordings(): ArrayList<ScheduledRecording> = scheduledRecords

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
                loadScheduledRecordings()
            }
        }
        updateTimer!!.start()
    }
}