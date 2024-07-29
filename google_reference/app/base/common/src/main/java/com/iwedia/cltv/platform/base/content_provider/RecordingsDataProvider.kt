package com.iwedia.cltv.platform.base.content_provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.media.tv.TvContract
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.iwedia.cltv.platform.`interface`.RecordingsDataProviderInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.recording.Recording

class RecordingsDataProvider(var context: Context, var tvModule: TvInterface, var timeInterface: TimeInterface, var utilsInterface: UtilsInterface) : RecordingsDataProviderInterface {
    private val TAG = javaClass.simpleName
    var recordings = ArrayList<Recording>()
    //todo need to check timeout
    private val UPDATE_TIMEOUT = 5000L
    private var updateTimer: CountDownTimer? = null
    private lateinit var databaseObserver: ContentObserver
    private var listener: Any? = null

    init {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "CHANNELS_LOADED: init")
        InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.CHANNELS_LOADED), {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "CHANNELS_LOADED: listener")
            listener = it
        }, {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "CHANNELS_LOADED: loadRecordings()")
            loadRecordings()
            listener?.let { InformationBus.informationBusEventListener.unregisterEventListener(it) }
            listener = null
        })

        databaseObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Recorded data updated")
                startUpdateTimer()
            }
        }

        context.contentResolver.registerContentObserver(
            TvContract.RecordedPrograms.CONTENT_URI,
            true,
            databaseObserver
        )
    }

    @SuppressLint("Recycle")
    override fun loadRecordings() {
        recordings = ArrayList<Recording>()
        val contentResolver: ContentResolver = context.contentResolver
        val cursor = contentResolver.query(
            TvContract.RecordedPrograms.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        Log.d(Constants.LogTag.CLTV_TAG + TAG, " getRecordingList ${cursor?.count}")
        if (cursor != null && cursor.count > 0) {
            recordings.clear()
            cursor.moveToFirst()
            do {
                var recording = createRecordingsFromCursor(cursor, tvModule, timeInterface, utilsInterface)
                if (recording != null && !recording.isInProgress())
                    recordings.add(recording)
            } while (cursor.moveToNext())
        }
        cursor?.close()

        //get mounted devices
        var devices = mutableListOf<String>()
        utilsInterface.getUsbDevices().entries.forEach {
            devices.add(it.key) }

        //check if recordings saved in database are on device
        if(devices.isNotEmpty()) {
            var iterator = recordings.iterator()
            while (iterator.hasNext()) {
                var recording = iterator.next()
                try {
                    var path = ""
                    if (recording.videoUrl.contains("/com")) {
                        var index = recording.videoUrl.indexOf("/com")
                        path = recording.videoUrl.substring(0..index - 1)
                    }
                    else if (recording.videoUrl.contains("/recording")) {
                        var startIndex = recording.videoUrl.indexOf("/storage")
                        var endIndex = recording.videoUrl.indexOf("/recording")
                        path = recording.videoUrl.substring(startIndex..endIndex - 1)
                    }

                    devices.forEach { device ->
                        if (path != device) {
                            iterator.remove()
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        else {
            recordings.removeAll(recordings.toSet())
        }

        recordings.reverse()
    }

    override fun deleteRecording(recording: Recording, callback: IAsyncCallback) {
        val appContext: Context = context
        val contentResolver = appContext.contentResolver
        val selection = TvContract.RecordedPrograms._ID + " = ?"

        val res = contentResolver.delete(
            TvContract.RecordedPrograms.CONTENT_URI,
            selection,
            arrayOf(recording.id.toString())
        )
        Log.d(Constants.LogTag.CLTV_TAG + TAG, " remove recording $res")

        if (res == 1) {
            callback.onSuccess()
        } else {
            callback.onFailed(Error("removeRecording: Failed to delete recording from the database!"))
        }
    }

    override fun renameRecording(recording: Recording, name: String, callback: IAsyncCallback) {
        val appContext: Context = context
        val contentResolver = appContext.contentResolver
        val contentValues = ContentValues()
        contentValues.put(TvContract.RecordedPrograms.COLUMN_TITLE, name)

        val uri = TvContract.buildRecordedProgramUri(recording.id.toLong())
        val res = contentResolver.update(
            uri,
            contentValues,
            null, null
        )
        if (res == 1) {
            callback.onSuccess()
        } else {
            callback.onFailed(Error("renameRecording: Failed to rename recording from the database!"))
        }
    }

    override fun deleteAllRecordings(callback: IAsyncCallback) {
        val appContext: Context = context
        val contentResolver = appContext.contentResolver
        val selection = null
        val selectionArgs: Array<String>? = null

        val res = contentResolver.delete(
            TvContract.RecordedPrograms.CONTENT_URI,
            selection,
            selectionArgs
        )
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Remove all recordings, deleted count: $res")

        if (res >= 0) {
            callback.onSuccess()
        } else {
            callback.onFailed(Error("removeAllRecordings: Failed to delete recordings from the database!"))
        }
    }

    @SuppressLint("Range")
    override fun updateRecording(
        duration: Long?,
        name: String?,
        shortDescription: String?,
        longDescription: String?,
        parentalRating: String?,
        genre: String?,
        subGenre: String?,
        resolution: String?
    ) {
        val cursor = context.contentResolver.query(
            TvContract.RecordedPrograms.CONTENT_URI,
            null,
            null,
            null
        )!!
        cursor.moveToLast()

        val id = cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms._ID))
        val startTime =
            cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_START_TIME_UTC_MILLIS))

        val uri = ContentUris.withAppendedId(TvContract.RecordedPrograms.CONTENT_URI, id)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, " Update Recording $uri")

        val contentValues = ContentValues()
        contentValues.put(TvContract.RecordedPrograms.COLUMN_TITLE, name)
        contentValues.put(TvContract.RecordedPrograms.COLUMN_SHORT_DESCRIPTION, shortDescription)
        contentValues.put(TvContract.RecordedPrograms.COLUMN_LONG_DESCRIPTION, longDescription)
        contentValues.put(TvContract.RecordedPrograms.COLUMN_CONTENT_RATING, parentalRating)
        contentValues.put(TvContract.RecordedPrograms.COLUMN_BROADCAST_GENRE, genre)
        contentValues.put(TvContract.RecordedPrograms.COLUMN_CANONICAL_GENRE, subGenre)
        if (duration != null) {
            contentValues.put(
                TvContract.RecordedPrograms.COLUMN_END_TIME_UTC_MILLIS,
                startTime + duration
            )
            contentValues.put(
                TvContract.RecordedPrograms.COLUMN_RECORDING_DURATION_MILLIS,
                duration
            )
        }

        try {
            var res = context.contentResolver.update(
                uri,
                contentValues,
                null, null
            )
            if (res == 1) {
                utilsInterface.setVideoResolutionForRecording(id, resolution!!)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Update Recording Success")
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Update Recording Failed")
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " Update Recording Failed")
        }
    }

    override fun getRecordings(): List<Recording> {
        //reload recordings before getting them because usb device could be unplugged in the meantime
        loadRecordings()
        return recordings
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
                loadRecordings()
            }
        }
        updateTimer!!.start()
    }
}