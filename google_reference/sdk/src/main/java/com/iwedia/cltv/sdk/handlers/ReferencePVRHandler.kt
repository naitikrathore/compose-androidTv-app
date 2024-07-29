package com.iwedia.cltv.sdk.handlers

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.hardware.usb.UsbManager
import android.media.tv.TvContract
import android.media.tv.TvRecordingClient
import android.media.tv.TvRecordingClient.RecordingCallback
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.ReferenceSdk.context
import com.iwedia.cltv.sdk.entities.ReferenceRecording
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.entities.ReferenceTvEvent
import core_entities.Error
import core_entities.PlayableItem
import core_entities.PlayableItemType
import core_entities.RecordingInProgress
import data_type.GList
import data_type.GLong
import handlers.DataProvider
import handlers.PvrHandler
import kotlinx.coroutines.Dispatchers
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import utils.information_bus.events.CurrentTimeEvent
import utils.information_bus.events.Events
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reference PVR Handler
 *
 * @author Dejan Nadj
 */
@RequiresApi(Build.VERSION_CODES.N)
class ReferencePVRHandler(dataProvider: DataProvider<*>):
    PvrHandler<ReferenceTvChannel, ReferenceTvEvent, ReferenceRecording>(dataProvider) {

    private val TAG = "ReferencePVRHandler"

    /**
     * To Stop recording automatically
     */
    var pvrStopTimer: CountDownTimer? = null

    /**
     * Show recording indication mutable live data
     */
    var showRecIndication: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    /**
     * Is signal available
     */
    private var isSignalAvailable = true

    /**
     * Pvr playback positions hash map
     */
    var playbackPosition = HashMap<Int, Long>()

    /**
     * Is pvr playback paused
     */
     var isPvrPlaybackPaused = false

    /**
     * Set recording indication flag
     */
    fun setRecIndication(show: Boolean) {
        showRecIndication.value = show
    }

    /**
     * Set playback position for recording item
     */
    fun setPlaybackPosition(recordingId: Int, position : Long) {
        playbackPosition.put(recordingId, position)
    }

    /**
     * Set signal availability
     */
    fun setSignalAvailability(isAvailable: Boolean) {
        this.isSignalAvailable = isAvailable
    }

    /**
     * Get playback position for recording item
     */
    fun getPlaybackPosition(recordingId: Int): Long {
        return if (playbackPosition.contains(recordingId)) playbackPosition[recordingId]!! else 0L
    }

    /**
     * Start recording tune callback
     */
    private interface TuneCallback{
        /**
         * On recording channel tuned
         * @param channelUri    uri of the tuned channel
         */
        fun onTuned(channelUri: Uri?)

        /**
         * Error occurred on recording channel tune
         */
        fun onError()
    }

    /**
     * Stop recording callback
     */
    private interface StopRecordingCallback {
        /**
         * On recording successfully stopped
         */
        fun onRecordingStopped()

        /**
         * On stop recording error happened
         */
        fun onRecordingStopError()
    }

    private var tuneCallback: TuneCallback ?= null
    private var stopRecordingCallback: StopRecordingCallback ?= null
    private var tvRecordingClient: TvRecordingClient ?= null
    private var recordingInProgress: RecordingInProgress<ReferenceTvChannel, ReferenceTvEvent> ?= null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun setup() {
        createRecordingClient()
    }

    override fun dispose() {
        super.dispose()
        destroyRecordingClient()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getRecordingList(callback: AsyncDataReceiver<GList<ReferenceRecording>>) {
        CoroutineHelper.runCoroutine({
            val appContext: Context = ReferenceSdk.context
            val contentResolver = appContext.contentResolver
            var recordings: GList<ReferenceRecording> = GList()
            var cursor: Cursor

            try {
                cursor = contentResolver.query(
                    TvContract.RecordedPrograms.CONTENT_URI,
                    null,
                    null,
                    null
                )!!
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " getRecordingList ${cursor?.count}")
                var counter = AtomicInteger(0)
                cursor.moveToFirst()
                while (!cursor.isClosed && !cursor.isAfterLast) {
                    var recording = fromCursor(cursor)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, " ${counter.get()} getRecordingList $recording")
                    if (recording != null && !recording.isInProgress())
                        recordings.add(recording)
                    cursor.moveToNext()
                }
                callback.onReceive(recordings)
            } catch (e: SecurityException) {
                callback.onFailed(
                    Error(
                        100,
                        "getRecordingList: Failed to acquire recordings from the database!"
                    )
                )
                return@runCoroutine
            } catch (e: NullPointerException) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, e.printStackTrace().toString())
                e.printStackTrace()
                callback.onFailed(
                    Error(
                        100,
                        "getRecordingList: Failed to acquire recordings from the database!"
                    )
                )
                return@runCoroutine
            } catch (e: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, e.printStackTrace().toString())
                callback.onFailed(
                    Error(
                        100,
                        "getRecordingList: Failed to acquire recordings from the database!"
                    )
                )
                return@runCoroutine
            }
        })
    }

    @SuppressLint("Range")
    private fun fromCursor(cursor: Cursor): ReferenceRecording? {
        var recordingId = -1
        var recordingName = ""
        var recordingDuration = 0L
        var recordingStartTime = 0L
        var recordingEndTime = 0L
        var recordingThumbnail = ""
        var recordingUri = ""
        var recordingTvChannel: ReferenceTvChannel ?= null
        var recordedTvEvent: ReferenceTvEvent ? = null
        var recordingShortDescription = ""
        var contentRating =""

        if (cursor.getInt(cursor.getColumnIndex(TvContract.RecordedPrograms._ID)) != null) {
            recordingId = cursor.getInt(cursor.getColumnIndex(TvContract.RecordedPrograms._ID))
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_CONTENT_RATING)) != null){
            contentRating = cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_CONTENT_RATING))
        }

        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_TITLE)) != null) {
            recordingName = cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_TITLE))
        }
        if (cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_RECORDING_DURATION_MILLIS)) != null) {
            recordingDuration = cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_RECORDING_DURATION_MILLIS))
        }
        if (cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_END_TIME_UTC_MILLIS)) != null) {
            recordingEndTime = cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_END_TIME_UTC_MILLIS))
        }
        if (cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_START_TIME_UTC_MILLIS)) != null) {
            recordingStartTime = cursor.getLong(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_START_TIME_UTC_MILLIS))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_THUMBNAIL_URI)) != null) {
            recordingThumbnail = cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_THUMBNAIL_URI))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_RECORDING_DATA_URI)) != null) {
            recordingUri = cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_RECORDING_DATA_URI))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_SHORT_DESCRIPTION)) != null) {
            recordingShortDescription =
                cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_SHORT_DESCRIPTION))
        }
        recordingTvChannel = (ReferenceSdk.tvHandler as ReferenceTvHandler)?.getChannelById(
            cursor.getLong(
                cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_CHANNEL_ID)
            ).toInt()
        )
        if (recordingTvChannel == null) {
            return null
        }
        if (recordingTvChannel.inputId.contains("mediatek")) {
            recordingDuration *= 1000L
            recordingStartTime *= 1000L
        }
        recordingEndTime = recordingStartTime + recordingDuration

        recordedTvEvent = ReferenceTvEvent.createNoInformationEvent(recordingTvChannel,Date(recordingStartTime),Date(recordingEndTime))

        var recording = ReferenceRecording(
            recordingId,
            recordingName,
            GLong(recordingDuration.toString()),
            GLong(recordingStartTime.toString()),
            recordingThumbnail,
            recordingUri,
            recordingTvChannel,
            recordedTvEvent)
        recording.recordedEvent = recordedTvEvent
        recording.shortDescription = recordingShortDescription
        recording.recordingEndTime = GLong(recordingEndTime.toString())
        recording.contentRating = contentRating

        ReferenceSdk.epgHandler?.getEventListByChannel(
            recordingTvChannel,
            object: AsyncDataReceiver<GList<ReferenceTvEvent>> {
                override fun onFailed(error: Error?) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "failed to get event for the recorded content")
                }

                override fun onReceive(data: GList<ReferenceTvEvent>) {
                    run exitForEach@{
                        data.value.forEach { event ->
                            if (event.startDate!!.value.toLong() <= recordingStartTime &&
                                event.endDate!!.value.toLong() >= recordingStartTime
                            ) {
                                recording.recordedEvent = event
                                return@exitForEach
                            }
                        }
                    }
                }
            }
        )
        return recording
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getRecordingsCount(callback: AsyncDataReceiver<Int>){
        getRecordingList(object : AsyncDataReceiver<GList<ReferenceRecording>>{
            override fun onReceive(refRecording: GList<ReferenceRecording>) {
                callback.onReceive(refRecording.size())
            }

            override fun onFailed(error: Error?) {
                callback.onFailed(error)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getRecording(i: Int, callback: AsyncDataReceiver<ReferenceRecording>) {
        getRecordingList(object : AsyncDataReceiver<GList<ReferenceRecording>> {
            override fun onReceive(refRecordings: GList<ReferenceRecording>) {
                callback.onReceive(refRecordings.get(i)!!)
            }

            override fun onFailed(error: Error?) {
                callback.onFailed(error)
            }
        })
    }

    fun isUsbConnected(): Boolean {
        var usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.size > 0
    }

    override fun startRecordingByChannel(tvChannel: ReferenceTvChannel, callback: AsyncReceiver) {
        if(!isUsbConnected()){
            callback.onFailed(Error(-1, "USB NOT CONNECTED\nConnect USB to record"))
            return
        }
        // Fetch current event to start recording
        (ReferenceSdk.epgHandler as ReferenceEpgHandler).getCurrentEvent(tvChannel!!, object :
            AsyncDataReceiver<ReferenceTvEvent> {
            override fun onFailed(error: Error?) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Start recording failed unable to get current event!")
                startRecording(tvChannel, null, null, callback)
            }

            override fun onReceive(tvEvent: ReferenceTvEvent) {
                startRecording(tvChannel, tvEvent, null, callback)
            }
        })
    }

    override fun startRecordingByChannelAndEvent(
        tvChannel: ReferenceTvChannel,
        tvEvent: ReferenceTvEvent,
        callback: AsyncReceiver
    ) {
        if(!isUsbConnected()){
            callback.onFailed(Error(-1, "USB NOT CONNECTED\nConnect USB to record"))
            return
        }
        startRecording(tvChannel, tvEvent, null, callback)
    }

    override fun startRecordingByChannelAndDuration(
        tvChannel: ReferenceTvChannel,
        recordingDuration: GLong,
        callback: AsyncReceiver
    ) {
        if(!isUsbConnected()){
            callback.onFailed(Error(-1, "USB NOT CONNECTED\nConnect USB to record"))
            return
        }
        startRecording(tvChannel, null, recordingDuration.value.toLong(), callback)
    }

    fun startIndefiniteRecording(
        tvChannel: ReferenceTvChannel,
        callback: AsyncReceiver
    ){
        startRecording(tvChannel, null, null, callback)
    }

    private fun startRecording(
        tvChannel: ReferenceTvChannel,
        tvEvent: ReferenceTvEvent?,
        duration: Long?,
        callback: AsyncReceiver
    ) {
        if (tvChannel == null || !isSignalAvailable) {
            callback.onFailed(Error(100, "Start recording failed!"))
            return
        }

        createRecordingClient()
        var channelUri = TvContract.buildChannelUri(tvChannel.channelId)
        tuneCallback = object: TuneCallback {
            override fun onError() {
                callback.onFailed(Error(100, "Start recording tune error!"))
            }

            override fun onTuned(uri: Uri?) {
                if (channelUri == uri) {
                    CoroutineHelper.runCoroutineWithDelay({
                        if ((ReferenceSdk.playerHandler as ReferencePlayerHandler).isActiveChannelScrambled()) {
                            destroyRecordingClient()
                            callback.onFailed(Error(100, "Start recording failed active channel is scrambled!"))
                        } else {
                            var uri = if (tvEvent != null) TvContract.buildProgramUri(tvEvent!!.id.toLong()) else null
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, " program uri $uri")
                            try {
                                tvRecordingClient?.startRecording(uri)
                                ReferenceSdk.setIsRecordingStarted(true)

                                InformationBus.submitEvent(CurrentTimeEvent(object : AsyncDataReceiver<GLong> {
                                    override fun onReceive(currentTime: GLong) {

                                            var duration = duration
                                            var currentUpdatedTime = currentTime

                                            if(tvEvent!=null && duration == null) {
                                                //event recording
                                                var startTime: Long = 0

                                                if(tvEvent.startDate.value.toLong() > currentTime.value.toLong()) {  ////////DID SWITCH
                                                    startTime = tvEvent.startDate.value.toLong()
                                                }else{
                                                    startTime = currentTime.value.toLong()
                                                }

                                                currentUpdatedTime = GLong(startTime.toString())

                                                duration = tvEvent.endDate.value.toLong() - startTime
                                            }

                                        // stop timer
                                        if(duration!=null){
                                            pvrStopTimer = object : CountDownTimer(duration, 1000){
                                                override fun onTick(millisUntilFinished: Long) {

                                                }

                                                override fun onFinish() {
                                                    stopRecordingByChannel(tvChannel,  object :AsyncReceiver{
                                                        override fun onFailed(error: Error?) {

                                                        }

                                                        override fun onSuccess() {
                                                            InformationBus.submitEvent(
                                                                Event(
                                                                    ReferenceEvents.PVR_RECORDING_FINISHED,
                                                                    tvChannel
                                                                )
                                                            )
                                                        }
                                                    })
                                                } }.start()
                                            }

                                        recordingInProgress =
                                            if (tvEvent == null) {
                                                var noInfoEvent = ReferenceTvEvent.createNoInformationEvent(tvChannel)
                                                RecordingInProgress(0, currentUpdatedTime, null, tvChannel, noInfoEvent)
                                            } else {
                                                RecordingInProgress(0, currentUpdatedTime, tvEvent.endDate, tvChannel, tvEvent)
                                            }

                                        callback.onSuccess()
                                    }

                                    override fun onFailed(error: Error?) {
                                        callback.onFailed(error)
                                    }
                                }))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback.onFailed(Error(100, "Start recording failed!"))
                            }
                        }
                    },1000, Dispatchers.Main)
                }
            }
        }

        Handler(Looper.getMainLooper()).post {
            // Bundle mtk specific
            var bundle = Bundle()
            bundle.putBoolean("session_event_dvr_record_is_onetouch", true)
            try {
                // Switch to the recording channel
                (ReferenceSdk.playerHandler as ReferencePlayerHandler).resetVideoUnavailable()
                InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_INIT))
                if (ReferenceTvChannel.compare(tvChannel, (ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel!!)) {
                    tvRecordingClient?.tune(tvChannel.inputId, channelUri, bundle)
                } else {
                    ReferenceSdk.tvHandler!!.changeChannel(tvChannel, object : AsyncReceiver{
                        override fun onFailed(error: Error?) {
                        }

                        override fun onSuccess() {
                            (ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel = tvChannel
                            CoroutineHelper.runCoroutineWithDelay({
                                tvRecordingClient?.tune(tvChannel.inputId, channelUri, bundle)
                            },2000, Dispatchers.Main)

                        }
                    })
                }
            } catch (e: Exception) {
                callback.onFailed(Error(100, "Recording already started!"))
            }
        }

    }

    override fun stopRecordingByChannel(tvChannel: ReferenceTvChannel, callback: AsyncReceiver) {
        CoroutineHelper.runCoroutine({
            stopRecordingCallback = object : StopRecordingCallback {
                override fun onRecordingStopError() {
                    recordingInProgress = null
                    destroyRecordingClient()
                    callback.onFailed(Error(100, "Stop recording failed!"))
                    // Go back to the active channel
                    ReferenceSdk.tvHandler!!.changeChannel(tvChannel, object : AsyncReceiver {
                        override fun onFailed(error: Error?) {
                        }

                        override fun onSuccess() {
                        }
                    })
                }

                override fun onRecordingStopped() {
                    recordingInProgress = null
                    destroyRecordingClient()
                    callback.onSuccess()
                }
            }
            Handler(Looper.getMainLooper()).post {
                try {
                    tvRecordingClient?.stopRecording()
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopRecordingCallback?.onRecordingStopError()
                }
            }
        })
    }

    override fun stopRecordingByChannelAndEvent(
        tvChannel: ReferenceTvChannel,
        tvEvent: ReferenceTvEvent,
        callback: AsyncReceiver
    ) {
        stopRecordingByChannel(tvChannel, callback)
    }

    override fun getRecordingInProgress(callback: AsyncDataReceiver<RecordingInProgress<*, *>?>) {
        callback.onReceive(recordingInProgress)
    }

    fun getRecordingInProgressTvChannel(): ReferenceTvChannel? {
        if (recordingInProgress != null) {
            return recordingInProgress!!.tvChannel
        }
        return null
    }

    fun isRecordingInProgress(): Boolean {
        return recordingInProgress != null
    }

    override fun removeRecording(recording: ReferenceRecording, callback: AsyncReceiver) {
        CoroutineHelper.runCoroutine( {
        val appContext: Context = ReferenceSdk.context
        val contentResolver = appContext.contentResolver
        var selection = TvContract.RecordedPrograms._ID + " = ?"

            var res = contentResolver.delete(TvContract.RecordedPrograms.CONTENT_URI, selection,  arrayOf(recording.id.toString()))!!
            Log.d (TAG, " remove recording $res")
            if (res == 1) {
                InformationBus.submitEvent(Event(Events.PVR_RECORDING_REMOVED))
                callback.onSuccess()
            } else {
                callback.onFailed(
                    Error(
                        100,
                        "removeRecording: Failed to delete recording from the database!"
                    )
                )
            }
        })
    }

    override fun renameRecording(
        recording: ReferenceRecording,
        name: String,
        callback: AsyncReceiver
    ) {
        CoroutineHelper.runCoroutine( {

        val appContext: Context = context
        val contentResolver = appContext.contentResolver
        val contentValues = ContentValues()
        contentValues.put(TvContract.RecordedPrograms.COLUMN_TITLE, name)

            var uri = TvContract.buildRecordedProgramUri(recording.id.toLong())
            contentResolver.update(
                uri,
                contentValues,
                null,null
            )
            callback.onSuccess()

        })
    }

    fun updateRecording(uri: Uri) {
        var name: String?
        var shortDescription :String?

        if(recordingInProgress!!.tvEvent.id!=-1){
            name = recordingInProgress!!.tvEvent.name
            shortDescription = recordingInProgress!!.tvEvent.shortDescription
        }else{
            name = ""
            shortDescription = ""
        }

        val contentValues = ContentValues()
        contentValues.put(TvContract.RecordedPrograms.COLUMN_TITLE, name)
        contentValues.put(TvContract.RecordedPrograms.COLUMN_SHORT_DESCRIPTION, shortDescription)

        CoroutineHelper.runCoroutine( {
            try {
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,null
                )
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        })
    }

    /**
     * Start pvr playback
     *
     * @param recording recording to play
     * @param callback callback instance
     */
    fun startPlayback(recording: ReferenceRecording, callback: AsyncReceiver) {
        var playableItem = PlayableItem(PlayableItemType.TV_PVR, recording)
        (ReferenceSdk.playerHandler as ReferencePlayerHandler).play(playableItem, callback)
    }

    /**
     * Pause pvr playback
     * @param callback callback instance
     */
    fun pausePlayback(callback: AsyncReceiver) {
        isPvrPlaybackPaused = !isPvrPlaybackPaused
        if (isPvrPlaybackPaused) {
            (ReferenceSdk.playerHandler as ReferencePlayerHandler).pause(callback)
        } else {
            (ReferenceSdk.playerHandler as ReferencePlayerHandler).resume(callback)
        }
    }

    /**
     * Stop pvr playback
     * @param callback callback instance
     */
    fun stopPlayback(callback: AsyncReceiver) {
        (ReferenceSdk.playerHandler as ReferencePlayerHandler).stop(callback)
    }

    /**
     * Seek pvr playback
     *
     * @param position position to seek
     * @param callback callback instance
     */
    fun seekPlayback(position: Long, callback: AsyncReceiver) {
        (ReferenceSdk.playerHandler as ReferencePlayerHandler).seek(GLong(position.toString()),true, callback)
    }

    /**
     * Is channel recordable check
     *
     * @return true if the channel is recordable
     */
    fun isChannelRecordable(channel: ReferenceTvChannel): Boolean {
        //TODO check this
        //return ReferenceSdk.tvInputHandler!!.isChannelRecordable(channel.inputId)
        return true
    }

    /**
     * Create tv recording client instance
     */
    private fun createRecordingClient() {
        if (tvRecordingClient == null) {
            tvRecordingClient = TvRecordingClient(
                ReferenceSdk.context,
                "$TAG TvRecordingClient",
                recordingCallback,
                Handler(Looper.getMainLooper())
            )
        }
    }

    /**
     * Destroy tv recording client instance
     */
    private fun destroyRecordingClient() {
        if (pvrStopTimer != null) {
            pvrStopTimer!!.cancel()
            pvrStopTimer = null
        }
        if (tvRecordingClient != null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " destroyRecordingClient ")
            if (recordingInProgress != null)
                tvRecordingClient?.stopRecording()
            tvRecordingClient?.release()
            tvRecordingClient = null
            stopRecordingCallback = null
            tuneCallback = null
            recordingInProgress = null
            setRecIndication(false)
            ReferenceSdk.setIsRecordingStarted(false)
        }
    }

    private var recordingCallback = object : RecordingCallback() {
        override fun onConnectionFailed(inputId: String?) {
            super.onConnectionFailed(inputId)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " onConnectionFailed")
            destroyRecordingClient()
        }

        override fun onDisconnected(inputId: String?) {
            super.onDisconnected(inputId)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " onDisconnected")
            destroyRecordingClient()
        }

        override fun onError(error: Int) {
            super.onError(error)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " onError $error")
            tuneCallback?.onError()
            stopRecordingCallback?.onRecordingStopError()
        }

        override fun onRecordingStopped(recordedProgramUri: Uri?) {
            super.onRecordingStopped(recordedProgramUri)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " onRecordingStopped")
            if(recordedProgramUri!=null) updateRecording(recordedProgramUri)
            stopRecordingCallback?.onRecordingStopped()
            stopRecordingCallback = null
        }

        override fun onTuned(channelUri: Uri?) {
            super.onTuned(channelUri)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " onTuned")
            tuneCallback?.onTuned(channelUri)
            tuneCallback = null
        }
    }
}