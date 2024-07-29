package com.iwedia.cltv.platform.rtk

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.TvContract
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.iwedia.cltv.platform.base.PvrInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.recording.RecordingInProgress

internal class PvrInterfaceImpl(
    private var epgInterfaceImpl: EpgInterface,
    private var playerInterface: PlayerInterface,
    private var tvInterfaceImpl: TvInterface,
    private var utilsInterface: UtilsInterface,
    private var context: Context,
    private var timeInterface: TimeInterface
) : PvrInterfaceBaseImpl(
    epgInterfaceImpl,
    playerInterface,
    tvInterfaceImpl,
    utilsInterface,
    context,
    timeInterface
) {
    @SuppressLint("InvalidWakeLockTag")
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
                InformationBus.informationBusEventListener?.submitEvent(Events.PVR_RECORDING_STARTED)
                callback.onFailed(Error("Start recording tune error!"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "startRecording: tune onError: ")
            }

            override fun onTuned(uri: Uri?) {
                if (channelUri == uri) {
                    if (getActiveChannelScrambled()) {
                        destroyRecordingClient()
                        callback.onFailed(Error("Start recording failed active channel is scrambled!"))
                    } else {
                        var uri =
                            if (tvEvent != null) TvContract.buildProgramUri(tvEvent!!.id.toLong()) else null
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, " program uri $uri")
                        try {
                            tvRecordingClient?.startRecording(uri)
                            InformationBus.informationBusEventListener?.submitEvent(Events.PVR_RECORDING_STARTED)
                            InformationBus.informationBusEventListener?.submitEvent(Events.SHOW_PVR_BANNER)
                            var currentTime = timeInterface.getCurrentTime(tvChannel)
                            var durationMs = duration
                            var noInfoEvent = TvEvent.createNoInformationEvent(tvChannel, timeInterface.getCurrentTime(tvChannel))
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
                                        Log.e(
                                            Constants.LogTag.CLTV_TAG + TAG,
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
                        val RECORDING_FOLDER = "/recording/"
                        val recordPath: String = utilsInterface.getPvrStoragePath() + RECORDING_FOLDER
                        bundle.putBoolean("backgroundRecord", false)
                        bundle.putBoolean("channelChange", false)
                        bundle.putString("recordPath", recordPath)
                        bundle.putInt("scheduleType", 0)
                        bundle.putString("programTitle", tvEvent?.name)
                        bundle.putLong("startTimeMs", tvEvent?.startTime!!)
                        Log.d(
                            Constants.LogTag.CLTV_TAG + TAG,
                            "start tune: backgroundRecord = " + bundle.getBoolean("backgroundRecord")
                                    + ", channelChange = " + bundle.getBoolean("channelChange")
                                    + ", recordPath = " + bundle.getString("recordPath")
                                    + ", scheduleType = " + bundle.getInt("scheduleType")
                                    + ", programTitle = " + bundle.getString("programTitle")
                                    + ", startTimeMs = " + bundle.getLong("startTimeMs")
                        )

                        tvRecordingClient?.tune(tvChannel.inputId, channelUri, bundle)
                    }catch (e: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "tune() :  tvRecordingClient already start, hence attempt stopRecording here")
                        tvRecordingClient?.stopRecording()
                    }
                }, 2000)
            } catch (e: Exception) {
                callback.onFailed(Error("Recording already started!"))
            }
        }
    }
}