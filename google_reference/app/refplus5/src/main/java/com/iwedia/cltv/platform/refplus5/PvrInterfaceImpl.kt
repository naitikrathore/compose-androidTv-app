package com.iwedia.cltv.platform.refplus5

import android.R.id
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.base.CiPlusInterfaceBaseImpl
import com.iwedia.cltv.platform.base.PvrInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.*
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
    private var timeInterface : TimeInterface
) : PvrInterfaceBaseImpl(
    epgInterfaceImpl,
    playerInterface,
    tvInterfaceImpl,
    utilsInterface,
    context,
    timeInterface
) {

    private val CACHED_CAM_PIN = "com.mediatek.dtv.tvinput.dvbtuner.CACHE_PIN"
    private val RECORDING_CONTENT_URI: Uri = TvContract.RecordedPrograms.CONTENT_URI
    private val CHANNEL_CONTENT_URI: Uri = TvContract.Channels.CONTENT_URI
    private val RECORD_START_EVENT = "RecordingStarted"
    private val DVR_PRV_CMD_RECORD_PROGRAM_URI: String = "RecordedProgramUri"

    @SuppressLint("Range")
    override fun handleRecordingEvent(inputId : String, eventType : String, args : Bundle) {
        if(eventType == RECORD_START_EVENT) {
            try {
                var programuri: String = args.getString(DVR_PRV_CMD_RECORD_PROGRAM_URI, "")
                if(programuri != "") {
                    val recordingID = programuri.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    for (id in recordingID) {
                        programuri = id
                    }
                    var selection = "_id=$programuri"
                    val mContentResolver = context.contentResolver
                    var cursor =
                        mContentResolver.query(RECORDING_CONTENT_URI, null, selection, null, null)
                    if ((cursor != null) && (cursor.moveToFirst())) {
                        if (cursor.getColumnIndex("channel_id") != -1) {
                            var channelid = cursor.getInt(cursor.getColumnIndex("channel_id"));
                            cursor.close()
                            var channelName = ""
                            var channelNumber = ""
                            selection = TvContract.Channels._ID + "=" + channelid;
                            cursor = mContentResolver.query(
                                CHANNEL_CONTENT_URI,
                                null,
                                selection,
                                null,
                                null
                            );
                            if ((cursor != null) && (cursor.moveToFirst())) {
                                channelName =
                                    cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME));
                                channelNumber =
                                    cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER));
                                cursor.close()
                                val values = ContentValues()
                                values.put("poster_art_uri", channelName)
                                values.put("thumbnail_uri", channelNumber)

                                context.contentResolver.update(
                                    ContentUris.withAppendedId(
                                        RECORDING_CONTENT_URI,
                                        programuri.toLong()
                                    ),
                                    values, null, null
                                )
                                Log.d(TAG,"handleRecordingEvent set recording $programuri service to $channelNumber $channelName")
                            }
                        }
                    }
                } else {
                    Log.e(TAG,"handleRecordingEvent failed, bad uri $programuri")
                }
            } catch(e: Exception) {
                Log.e(TAG,"handleRecordingEvent failed with exception ${e.message}")
            }
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

            @RequiresApi(Build.VERSION_CODES.R)
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

                            val path = utilsInterface.getPvrStoragePath()
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Pvr storage path: $path")
                            val bundle = Bundle()
                            bundle.putString(UtilsInterface.STORAGE_PATH, path)
                            try {
                                var cachedCamPin = utilsInterface.getPrefsValue(
                                    CiPlusInterfaceBaseImpl.CASHED_CAM_PIN,
                                    ""
                                ) as String
                                if (cachedCamPin != "") {
                                    val pinInt = intArrayOf(0, 0, 0, 0)
                                    if (!(cachedCamPin.isEmpty()) && (cachedCamPin.length == 4)) {
                                        var index = 0
                                        for(number in cachedCamPin) {
                                            pinInt[index] = number.toString().toInt()
                                            index++
                                        }
                                    }
                                    bundle.putIntArray(CACHED_CAM_PIN, pinInt);
                                }
                            } catch(e: Exception) {
                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"Failed to set cached CAM pin ${e.message}")
                            }
                            utilsInterface.setPvrStoragePath(path)
                            tvRecordingClient?.startRecording(uri, bundle)
                            InformationBus.informationBusEventListener?.submitEvent(Events.PVR_RECORDING_STARTED)
                            InformationBus.informationBusEventListener?.submitEvent(Events.SHOW_PVR_BANNER)
                            //todo setIsRecordingStarted should be moved here
                            //Not implemented  - ReferenceSdk.setIsRecordingStarted(true)
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

    override fun seek(positionMs: Long) {
        pvrPlaybackPositionMs = pvrPlaybackStartPosition + positionMs
        Log.d(Constants.LogTag.CLTV_TAG + "PVR TEST","pvrPlaybackPositionMs $pvrPlaybackPositionMs")
        pvrPlaybackView!!.timeShiftSeekTo(pvrPlaybackPositionMs)

//        val playbackParams = PlaybackParams()
//        playbackParams.pitch = 1.0f
//        playbackParams.speed = 1.0f
//        pvrPlaybackView!!.timeShiftSetPlaybackParams(playbackParams)
    }
}