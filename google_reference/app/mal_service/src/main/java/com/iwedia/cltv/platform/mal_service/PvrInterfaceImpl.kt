package com.iwedia.cltv.platform.mal_service

import android.content.Context
import android.media.PlaybackParams
import android.media.tv.TvContract
import android.media.tv.TvRecordingClient
import android.media.tv.TvView
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncRecordingListener
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.mal_service.common.RecordingsDataProvider
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RecordingInProgress

/**
 * MAL service pvr interface implementation
 *
 * @author Dejan Nadj
 */
class PvrInterfaceImpl(
    private val context: Context,
    private val serviceImpl: IServiceAPI,
    private var playerInterface: PlayerInterface,
    private var epgInterfaceImpl: EpgInterface,
    private var tvInterfaceImpl: TvInterface) : PvrInterface {

    private val TAG = "MalPvrInterfaceImpl"

    /**
     * Pvr playback tv view
     */
    private var pvrPlaybackView: TvView? = null

    /**
     * Tv recording client
     */
    private var tvRecordingClient: TvRecordingClient? = null
    private var tuneCallback: TuneCallback? = null
    private var stopRecordingCallback: StopRecordingCallback? = null

    /**
     * Recording in progress object
     */
    private var recordingInProgress: RecordingInProgress? = null

    /**
     * Is pvr playback paused
     */
    override var isPvrPlaybackPaused = false

    /**
     * Is signal available
     */
    protected var isSignalAvailable = true

    /**
     * Show recording indication mutable live data
     */
    override var showRecIndication: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    /**
     * Pvr playback positions hash map
     */
    private var playbackPosition = HashMap<Int, Long>()

    /**
     * To Stop recording automatically
     */
    open var pvrStopTimer: CountDownTimer? = null

    /**
     * Recording data provider
     */
    private var recordingsDataProvider: RecordingsDataProvider? = null

    /**
     * Pvr playback position constants
     */
    var pvrPlaybackStartPosition: Long = 0
    private var initialPlaybackPosition = 0L
    override var pvrPlaybackCallback: PvrInterface.PvrPlaybackCallback? = null
    private var pvrPositionCallback: ReferencePvrPositionCallback? = null

    private var pvrState =
        PvrInterface.PvrSeekType.REGULAR_SEEK //todo this is probably not needed any more
    var pvrPlaybackPositionMs = 0L

    init {
        recordingsDataProvider = RecordingsDataProvider(context, tvInterfaceImpl, serviceImpl)
    }

    override fun setPvrSpeed(speed: Int) {
        val playbackParams = PlaybackParams()
        playbackParams.pitch = 1.0f
        playbackParams.speed = speed.toFloat()
        pvrPlaybackView?.timeShiftSetPlaybackParams(playbackParams)
    }

    override fun reset() {
        pvrPlaybackCallback = null
        pvrPlaybackView!!.setTimeShiftPositionCallback(null)
        pvrPlaybackStartPosition = 0
        initialPlaybackPosition = 0L
    }

    override fun reset(callback: IAsyncCallback) {
        if (pvrPlaybackView == null) {
            callback.onFailed(Error("Pvr TvView is null"))
        } else {
            pvrPlaybackView?.reset()
            pvrPlaybackCallback = null
            pvrPlaybackStartPosition = 0
            callback.onSuccess()
        }
    }

    override fun seek(positionMs: Long) {
        pvrPlaybackPositionMs = pvrPlaybackStartPosition + positionMs
        Log.d(Constants.LogTag.CLTV_TAG + "PVR seek", "pvrPlaybackPositionMs $pvrPlaybackPositionMs")
        pvrPlaybackView!!.timeShiftSeekTo(pvrPlaybackPositionMs)
    }

    override fun setup() {
        createRecordingClient()
    }

    override fun dispose() {
        destroyRecordingClient()
    }

    override fun startRecordingByChannel(
        tvChannel: TvChannel,
        callback: IAsyncCallback,
        isInfiniteRec: Boolean
    ) {
        val currentTime = serviceImpl.getCurrentTimeByChannel(toServiceChannel(tvChannel))
        var eventFound = false
        InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_STARTING)
        if (!serviceImpl.isUsbConnected()) {
            callback.onFailed(Error("USB NOT CONNECTED\nConnect USB to record"))
            return
        }

        if (!isRecordingInProgress()) {
            tvInterfaceImpl.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                    return
                }

                override fun onReceive(data: TvChannel) {
                    if (!TvChannel.compare(data, tvChannel)) {
                        tvInterfaceImpl.changeChannel(tvChannel, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                callback.onFailed(error)
                                return
                            }

                            override fun onSuccess() {
                                if (isInfiniteRec) {
                                    startRecording(tvChannel, null, null, callback)
                                } else {
                                    // Fetch current event to start recording
                                    epgInterfaceImpl.getEventListByChannel(tvChannel, object :
                                        IAsyncDataCallback<ArrayList<TvEvent>> {
                                        override fun onFailed(error: Error) {
                                            Log.d(
                                                Constants.LogTag.CLTV_TAG +
                                                TAG,
                                                "Start recording failed unable to get current event!"
                                            )
                                            startRecording(tvChannel, null, null, callback)
                                        }

                                        override fun onReceive(data: ArrayList<TvEvent>) {
                                            data.forEach { tvEvent ->
                                                if (tvEvent.startTime <= currentTime && tvEvent.endTime >= currentTime) {
                                                    Log.d(Constants.LogTag.CLTV_TAG +
                                                        TAG,
                                                        "Start recording called after getting current event!"
                                                    )
                                                    eventFound = true
                                                    startRecording(
                                                        tvChannel,
                                                        tvEvent,
                                                        null,
                                                        callback
                                                    )
                                                }
                                            }
                                            if (!eventFound) {
                                                startRecording(tvChannel, null, null, callback)
                                            }
                                        }
                                    })
                                }
                            }
                        })
                    } else {
                        if (isInfiniteRec) {
                            startRecording(tvChannel, null, null, callback)
                        } else {
                            // Fetch current event to start recording
                            epgInterfaceImpl.getEventListByChannel(tvChannel, object :
                                IAsyncDataCallback<ArrayList<TvEvent>> {
                                override fun onFailed(error: Error) {
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "Start recording failed unable to get current event!"
                                    )
                                    startRecording(tvChannel, null, null, callback)
                                }

                                override fun onReceive(data: ArrayList<TvEvent>) {
                                    data.forEach { tvEvent ->
                                        if (tvEvent.startTime <= currentTime && tvEvent.endTime >= currentTime) {
                                            Log.d(Constants.LogTag.CLTV_TAG +
                                                TAG,
                                                "Start recording called after getting current event!"
                                            )
                                            eventFound = true
                                            startRecording(tvChannel, tvEvent, null, callback)
                                        }
                                    }
                                    if (!eventFound) {
                                        startRecording(tvChannel, null, null, callback)

                                    }
                                }
                            })
                        }
                    }
                }
            })
        } else {
            tvInterfaceImpl.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                    return
                }

                override fun onReceive(data: TvChannel) {
                    if (!TvChannel.compare(data, tvChannel)) {
                        stopRecordingByChannel(data, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                callback.onFailed(error)
                                return
                            }

                            override fun onSuccess() {
                                tvInterfaceImpl.changeChannel(tvChannel, object : IAsyncCallback {
                                    override fun onFailed(error: Error) {
                                        callback.onFailed(error)
                                        return
                                    }

                                    override fun onSuccess() {
                                        if (isInfiniteRec) {
                                            startRecording(tvChannel, null, null, callback)
                                        } else {
                                            // Fetch current event to start recording
                                            epgInterfaceImpl.getEventListByChannel(
                                                tvChannel,
                                                object :
                                                    IAsyncDataCallback<ArrayList<TvEvent>> {
                                                    override fun onFailed(error: Error) {
                                                        Log.d(Constants.LogTag.CLTV_TAG +
                                                            TAG,
                                                            "Start recording failed unable to get current event!"
                                                        )
                                                        startRecording(
                                                            tvChannel,
                                                            null,
                                                            null,
                                                            callback
                                                        )
                                                    }

                                                    override fun onReceive(data: ArrayList<TvEvent>) {
                                                        data.forEach { tvEvent ->
                                                            if (tvEvent.startTime <= currentTime && tvEvent.endTime >= currentTime) {
                                                                Log.d(Constants.LogTag.CLTV_TAG +
                                                                    TAG,
                                                                    "Start recording called after getting current event!"
                                                                )
                                                                eventFound = true
                                                                startRecording(
                                                                    tvChannel,
                                                                    tvEvent,
                                                                    null,
                                                                    callback
                                                                )
                                                            }
                                                        }
                                                        if (!eventFound) {
                                                            startRecording(
                                                                tvChannel,
                                                                null,
                                                                null,
                                                                callback
                                                            )

                                                        }
                                                    }
                                                })
                                        }
                                    }
                                })
                            }
                        })
                    } else {
                        stopRecordingByChannel(tvChannel, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                callback.onFailed(error)
                                return
                            }

                            override fun onSuccess() {
                                callback.onSuccess()
                                return
                            }
                        })
                    }
                }
            })
        }
    }

    override fun startRecording(
        tvChannel: TvChannel,
        tvEvent: TvEvent?,
        duration: Long?,
        callback: IAsyncCallback
    ) {
        if (!isSignalAvailable) {
            callback.onFailed(Error("Start recording failed!"))
            return
        }

        createRecordingClient()
        var channelUri = TvContract.buildChannelUri(tvChannel.channelId)
        tuneCallback = object : TuneCallback {
            override fun onError() {
                callback.onFailed(Error("Start recording tune error!"))
            }

            override fun onTuned(uri: Uri?) {
                if (channelUri == uri) {
                    if (getActiveChannelScrambled()) {
                        destroyRecordingClient()
                        callback.onFailed(Error("Start recording failed active channel is scrambled!"))
                    } else {
                        var uri =
                            if (tvEvent != null && tvEvent.id != -1) TvContract.buildProgramUri(
                                tvEvent!!.id.toLong()
                            ) else null
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, " program uri $uri")
                        try {
                            tvRecordingClient?.startRecording(uri)
                            InformationBus.informationBusEventListener?.submitEvent(Events.PVR_RECORDING_STARTED)
                            InformationBus.informationBusEventListener?.submitEvent(Events.SHOW_PVR_BANNER)
                            var currentTime =
                                serviceImpl.getCurrentTimeByChannel(toServiceChannel(tvChannel))
                            var durationMs = duration
                            var noInfoEvent = TvEvent.createNoInformationEvent(
                                tvChannel,
                                serviceImpl.getCurrentTimeByChannel(toServiceChannel(tvChannel))
                            )
                            if (tvEvent == null) {
                                if (duration == null) {
                                    //todo check the differences in RecordingInProgress
                                    recordingInProgress = RecordingInProgress(
                                        0,
                                        currentTime,
                                        null,
                                        tvChannel,
                                        noInfoEvent
                                    )
                                } else {
                                    //Schedule Recording from current time to till duration
                                    val endTime = duration + currentTime
                                    noInfoEvent.startTime = currentTime
                                    noInfoEvent.endTime = endTime
                                    triggerRecordingTimer(duration, tvChannel)
                                    recordingInProgress = RecordingInProgress(
                                        0,
                                        currentTime,
                                        endTime,
                                        tvChannel,
                                        noInfoEvent
                                    )
                                }
                            } else {
                                if ((tvEvent.startTime < currentTime)
                                    && (currentTime < tvEvent.endTime)
                                ) {
                                    // Selected Current event recording will perform from current time to end time
                                    tvEvent.startTime = currentTime
                                    durationMs = tvEvent.endTime - tvEvent.startTime
                                } else {
                                    if (currentTime > tvEvent.endTime) {
                                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                                            TAG,
                                            "Selected event is past event, current time will not be able to map"
                                        )
                                    }
                                }
                                if (durationMs != null && durationMs > 0) {
                                    recordingInProgress = RecordingInProgress(
                                        0,
                                        currentTime,
                                        tvEvent.endTime,
                                        tvChannel,
                                        tvEvent
                                    )
                                } else {
                                    recordingInProgress = RecordingInProgress(
                                        0,
                                        currentTime,
                                        null,
                                        tvChannel,
                                        noInfoEvent
                                    )
                                }
                            }

                            if (durationMs == null || durationMs <= 0) {
                                durationMs = Long.MAX_VALUE
                            }
                            triggerRecordingTimer(durationMs, tvChannel)
                            setRecIndication(true)
                            callback.onSuccess()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            callback.onFailed(Error("Start recording failed!"))
                        }
                    }
                }
            }
        }

        Handler(Looper.getMainLooper()).post {
            var bundle = Bundle()
            bundle.putBoolean("session_event_dvr_record_is_onetouch", true)
            try {
                // Switch to the recording channel
                InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_INIT)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "tune() :  tvRecordingClient =$tvRecordingClient ")
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, " tune ${tvChannel.inputId} ${channelUri.toString()}")
                    try {
                        tvRecordingClient?.tune(tvChannel.inputId, channelUri, bundle)
                    } catch (e: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "tune() :  tvRecordingClient already start, hence attempt stopRecording here"
                        )
                        tvRecordingClient?.stopRecording()
                    }
                }, 2000)
                //TODO DEJAN
                /*tvInterfaceImpl.getActiveChannel(object : IAsyncDataCallback<TvChannel>{
                    override fun onFailed(error: Error) {
                        callback.onFailed(Error("Get active channel failed"))
                    }

                    override fun onReceive(data: TvChannel) {
                        if (TvChannel.compare(tvChannel, data)) {//getActiveCHannel
                            tvRecordingClient?.tune(tvChannel.inputId, channelUri, bundle)
                        }
                        else {
                            tvInterfaceImpl!!.changeChannel(tvChannel, object : IAsyncCallback{
                                override fun onFailed(error: Error) {
                                    callback.onFailed(Error("Change channel failed"))
                                }

                                override fun onSuccess() {
                                    //todo changeChannel should set if onSuccess activeChannel
//                                    tvInterfaceImpl.activeChannel = tvChannel
                                    Handler(Looper.getMainLooper()).postDelayed ({
                                        tvRecordingClient?.tune(tvChannel.inputId, channelUri, bundle)
                                    },2000)
                                }
                            })
                        }
                    }
                })*/
            } catch (e: Exception) {
                callback.onFailed(Error("Recording already started!"))
            }
        }
    }

    override fun stopRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback) {
        CoroutineHelper.runCoroutine({
            stopRecordingCallback = object : StopRecordingCallback {
                override fun onRecordingStopError() {
                    recordingInProgress = null
                    destroyRecordingClient()
                    callback.onFailed(Error("Stop recording failed!"))
                    // Go back to the active channel
                    /*vInterfaceImpl.changeChannel(tvChannel, object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            //callback.onFailed(Error("Stop recording failed!"))
                        }

                        override fun onSuccess() {
                            //callback.onSuccess()
                        }
                    })*/
                }

                override fun onRecordingStopped() {
                    recordingInProgress = null
                    destroyRecordingClient()
                    setRecIndication(false)
                    callback.onSuccess()
                }
            }
            if (tvRecordingClient == null) {
                callback.onFailed(java.lang.Error("TvRecordingClient is null"))
            } else {
                Handler(Looper.getMainLooper()).post {
                    try {
                        tvRecordingClient?.stopRecording()
                        if (!serviceImpl.isUsbConnected()) {
                            //stop recording immediately when usb is removed
                            //done in this way because when usb is removed during recording we do not get any callbacks
                            stopRecordingCallback?.onRecordingStopped()
                            //TODO possible issues with USB when it's removed during recording
                            InformationBus.informationBusEventListener?.submitEvent(Events.PVR_RECORDING_INTERRUPTED)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        stopRecordingCallback?.onRecordingStopError()
                    }
                }
            }
        })
    }

    override fun stopRecordingByChannelAndEvent(
        tvChannel: TvChannel,
        tvEvent: TvEvent,
        callback: IAsyncCallback
    ) {
        stopRecordingByChannel(tvChannel, callback)
    }

    override fun getRecording(i: Int, callback: IAsyncDataCallback<Recording>) {
        getRecordingList(object : IAsyncDataCallback<List<Recording>> {
            override fun onReceive(data: List<Recording>) {
                if (i in data.indices)
                    callback.onFailed(Error("Requested recording is out of bounds"))
                else
                    callback.onReceive(data[i])
            }

            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }
        })
    }

    override fun removeRecording(recording: Recording, callback: IAsyncCallback) {
        if (recordingsDataProvider == null) {
            callback.onFailed(Error("RecordingsDataProvider is null"))
        } else {
            CoroutineHelper.runCoroutine({
                recordingsDataProvider?.deleteRecording(recording, callback)
                //utilsInterface.deleteRecordingFromUsb(recording, callback)
            })
        }
    }

    override fun removeAllRecordings(callback: IAsyncCallback) {
        if (recordingsDataProvider == null) {
            callback.onFailed(Error("RecordingsDataProvider is null"))
        } else {
            CoroutineHelper.runCoroutine({
                recordingsDataProvider?.deleteAllRecordings(callback)
            })
        }
    }

    override fun renameRecording(recording: Recording, name: String, callback: IAsyncCallback) {
        if (recordingsDataProvider == null) {
            callback.onFailed(Error("RecordingsDataProvider is null"))
        } else {
            CoroutineHelper.runCoroutine({
                recordingsDataProvider?.renameRecording(recording, name, callback)
            })
        }
    }

    override fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>) {
        if (recordingInProgress == null) {
            callback.onFailed(Error("RecordingInProgress is null"))
        } else {
            recordingInProgress?.let { callback.onReceive(it) }
        }
    }

    override fun getRecordingList(callback: IAsyncDataCallback<List<Recording>>) {
        serviceImpl.getRecordingList(object : IAsyncRecordingListener.Stub() {
            override fun onResponse(response: Array<out com.cltv.mal.model.pvr.Recording>?) {
                var result = arrayListOf<Recording>()
                response?.forEach { recording ->
                    result.add(fromServiceRecording(recording))
                }
                callback.onReceive(result)
            }
        })
    }

    override fun getRecordingsCount(callback: IAsyncDataCallback<Int>) {
        getRecordingList(object : IAsyncDataCallback<List<Recording>> {
            override fun onReceive(data: List<Recording>) {
                callback.onReceive(data.size)
            }

            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }
        })
    }

    override fun updateRecording(duration: Long?) {
        if (!isRecordingInProgress()) return

        val name: String?
        val shortDescription: String?
        val longDescription: String?
        val parentalRating: String?
        val genre: String?
        val subGenre: String?

        if (recordingInProgress?.tvEvent?.id != null && recordingInProgress?.tvEvent?.id != -1) {
            name = recordingInProgress?.tvEvent?.name
            shortDescription = recordingInProgress?.tvEvent?.shortDescription
            longDescription = recordingInProgress?.tvEvent?.longDescription
            parentalRating = recordingInProgress?.tvEvent!!.parentalRating
            genre = recordingInProgress?.tvEvent!!.genre
            subGenre = recordingInProgress?.tvEvent!!.subGenre
        } else {
            name = ""
            shortDescription = ""
            longDescription = ""
            parentalRating = ""
            genre = ""
            subGenre = ""
        }


        CoroutineHelper.runCoroutine({
            recordingsDataProvider!!.updateRecording(
                duration,
                name,
                shortDescription,
                longDescription,
                parentalRating,
                genre,
                subGenre,
                playerInterface.getVideoResolution()
            )
        })
    }

    override fun setRecIndication(show: Boolean) {
        showRecIndication.value = show
    }

    override fun setPlaybackPosition(recordingId: Int, position: Long) {
        playbackPosition[recordingId] = position
    }

    override fun setSignalAvailability(isAvailable: Boolean) {
        isSignalAvailable = isAvailable
    }

    override fun getPlaybackPosition(recordingId: Int): Long {
        return if (playbackPosition.contains(recordingId)) playbackPosition[recordingId]!! else 0L
    }

    override fun getPlaybackPositionPercent(recording: Recording): Double {
        var percent = 0.0
        if (playbackPosition.contains(recording.id)) {
            percent =
                (playbackPosition[recording.id]!!.toDouble() * 100) / recording.duration.toDouble()
        }
        return percent
    }

    override fun getRecordingInProgressTvChannel(): TvChannel? {
        return recordingInProgress?.tvChannel
    }

    override fun isRecordingInProgress(): Boolean {
        return recordingInProgress != null
    }

    override fun startPlayback(recording: Recording, callback: IAsyncCallback) {
        playerInterface.play(recording)
        callback.onSuccess()
    }

    override fun pausePlayPlayback(callback: IAsyncCallback) {
        isPvrPlaybackPaused = !isPvrPlaybackPaused
        if (isPvrPlaybackPaused) {
            pvrPlaybackView?.timeShiftPause()
        } else {
            pvrPlaybackView?.timeShiftResume()
        }
        callback.onSuccess()
    }

    override fun pausePlayback() {
        isPvrPlaybackPaused = true
        pvrPlaybackView?.timeShiftPause()
    }

    override fun stopPlayback(callback: IAsyncCallback) {
        playerInterface.stop()
        callback.onSuccess()
    }

    override fun resumePlayback() {
        isPvrPlaybackPaused = false
        pvrPlaybackView?.timeShiftResume()
    }

    override fun seekPlayback(position: Long, callback: IAsyncCallback) {
        seek(position)
        callback.onSuccess()
    }

    override fun isChannelRecordable(channel: TvChannel): Boolean {
        return true
    }

    override fun setPvrTvView(tvView: TvView) {
        pvrPlaybackView = tvView
    }

    override fun setPvrPositionCallback() {
        pvrPositionCallback = ReferencePvrPositionCallback()
        pvrPlaybackView?.setTimeShiftPositionCallback(pvrPositionCallback)
    }

    override fun setPvrState(state: PvrInterface.PvrSeekType) {
        pvrState = state
    }

    override fun reloadRecordings() {
        if (serviceImpl.isUsbConnected()) {
            recordingsDataProvider!!.loadRecordings()
        }
    }

    /**
     * Create tv recording client instance
     */
    private fun createRecordingClient() {
        if (tvRecordingClient == null) {
            tvRecordingClient = TvRecordingClient(
                context,
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
        }
    }

    /**
     * Start recording tune callback
     */
    interface TuneCallback {
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

    inner class ReferencePvrPositionCallback : TvView.TimeShiftPositionCallback() {
        //Duration in seconds time shift position - start position

        override fun onTimeShiftCurrentPositionChanged(inputId: String?, timeMs: Long) {
            super.onTimeShiftCurrentPositionChanged(inputId, timeMs)
            if (timeMs < 0) return
            Log.i(TAG, "onTimeShiftCurrentPositionChanged: timeMs $timeMs")
            if (initialPlaybackPosition == 0L) {
                initialPlaybackPosition = timeMs
            }
            if (isRichTvInput(inputId) && pvrPlaybackStartPosition == 0L && timeMs > 0) {
                if (timeMs - initialPlaybackPosition > 1500) {
                    pvrPlaybackStartPosition = timeMs
                } else {
                    initialPlaybackPosition = timeMs
                }
            }
            if (isRichTvInput(inputId)) {
                pvrPlaybackCallback?.onPlaybackPositionChanged(timeMs - pvrPlaybackStartPosition - 1500)
            } else {
                val currSeekPosition = timeMs - pvrPlaybackStartPosition
                pvrPlaybackCallback?.onPlaybackPositionChanged(currSeekPosition)
            }
        }

        override fun onTimeShiftStartPositionChanged(inputId: String?, timeMs: Long) {
            super.onTimeShiftStartPositionChanged(inputId, timeMs)
            if (timeMs < 0) return
            Log.i(TAG, "onTimeShiftStartPositionChanged: timeMs $timeMs")
            if (!isRichTvInput(inputId)) {
                if (pvrPlaybackStartPosition == 0L) {
                    pvrPlaybackStartPosition = timeMs
                }
            }
        }
    }

    private var recordingCallback = object : TvRecordingClient.RecordingCallback() {
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
            //todo this is the difference
            recordedProgramUri?.let { updateRecording(null) }
            //or if (recordedProgramUri != null) updateRecording(recordedProgramUri)
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

    private fun isRichTvInput(inputId: String?): Boolean {
        return inputId == "com.example.android.sampletvinput/.rich.RichTvInputService"
    }

    /**
     * Ongoing recording specific constraints included
     */
    private fun triggerRecordingTimer(duration: Long, tvChannel: TvChannel) {
        pvrStopTimer = object : CountDownTimer(duration, 1000) {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTick(millisUntilFinished: Long) {
                if (!serviceImpl.isUsbConnected()) {
                    cancel()
                    stopRecordingWhenInterrupted(tvChannel)
                } else if (serviceImpl.isUsbConnected() && !serviceImpl.isUsbFreeSpaceAvailable()) {
                    cancel()
                    stopRecordingWhenInterrupted(tvChannel)
                }

                val durationInMillis = duration - millisUntilFinished
                val durationInSeconds = durationInMillis / 1000

                if (durationInSeconds != 0L
                    && ((durationInSeconds % 10 == 0L && durationInSeconds <= 60) // updating every 10 seconds up to 1 minute
                            || durationInSeconds % 60 == 0L)
                ) {  // after 1 minute updating every 1 minute
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, " Update Recording $durationInSeconds")
                    updateRecording(durationInMillis)
                }
                recordingInProgress?.currentRecordedDuration = durationInMillis
            }

            override fun onFinish() {
                stopRecordingByChannel(tvChannel, object : IAsyncCallback {
                    override fun onFailed(error: Error) {}

                    override fun onSuccess() {
                        InformationBus.informationBusEventListener?.submitEvent(Events.PVR_RECORDING_FINISHED)
                    }
                })
            }
        }.start()
    }

    private fun stopRecordingWhenInterrupted(tvChannel: TvChannel) {
        stopRecordingByChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {}

            override fun onSuccess() {
                if (!serviceImpl.isUsbConnected()) {
                    InformationBus.informationBusEventListener?.submitEvent(Events.PVR_RECORDING_FINISHED)
                } else if (!serviceImpl.isUsbFreeSpaceAvailable()) {
                    InformationBus.informationBusEventListener?.submitEvent(Events.SHOW_USB_FULL_DIALOG)
                }
            }
        })


    }

    /**
     * Retrieve active scramble channel from playerInterface
     */
    //todo missing implementacion
    private fun getActiveChannelScrambled(): Boolean {
        return false  //return playerInterface?.getActiveChannelScrambled() or playerInterface.isActiveChannelScrambled()
    }
}