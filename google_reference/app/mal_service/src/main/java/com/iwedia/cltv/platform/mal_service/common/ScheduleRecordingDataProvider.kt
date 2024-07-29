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
import com.iwedia.cltv.platform.`interface`.ScheduledRecordingInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.mal_service.toServiceChannel
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording

class ScheduleRecordingDataProvider(
    var context: Context,
    var tvModule: TvInterface,
    var serviceImpl: IServiceAPI
) :
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
                val scheduledRecord = (createScheduledRecordingsFromCursor(cursor, tvModule, serviceImpl))
                if (scheduledRecord != null) {
                    scheduledRecords.add(scheduledRecord)
                }
            }
            while (cursor.moveToNext())
        }
        cursor?.close()
    }


    @SuppressLint("Range")
    fun createScheduledRecordingsFromCursor(cursor: Cursor, tvModule: TvInterface, serviceImpl: IServiceAPI) : ScheduledRecording? {
        var id = -1
        var name = ""
        var tvEvent : TvEvent
        var startTime : Long = 0
        var endTime : Long = 0

        if (cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)) != null) {
            id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
        }

        if (cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN)) != null){
            name = cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN))
        }

        if (cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.START_TIME_COLUMN)) != null){
            startTime = cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.START_TIME_COLUMN))
        }

        if (cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.END_TIME_COLUMN)) != null){
            endTime = cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.END_TIME_COLUMN))
        }

        var tvChannel : TvChannel? = tvModule.getChannelById(
            cursor.getLong(
                cursor.getColumnIndex(Contract.ScheduledRecordings.CHANNEL_ID_COLUMN)
            ).toInt()
        )

        if (tvChannel == null)
            return null

        tvEvent = TvEvent.createNoInformationEvent(
            tvChannel, serviceImpl.getCurrentTimeByChannel(
                toServiceChannel(tvChannel)
            )
        )

        return ScheduledRecording(
            id,
            name,
            startTime,
            endTime,
            tvChannel.id,
            tvEvent.id,
            RepeatFlag.NONE,
            tvChannel,
            tvEvent
        )
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