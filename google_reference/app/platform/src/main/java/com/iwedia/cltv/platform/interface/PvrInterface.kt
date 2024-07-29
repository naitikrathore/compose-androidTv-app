package com.iwedia.cltv.platform.`interface`

import android.media.tv.TvView
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RecordingInProgress

interface PvrInterface {

    enum class PvrSeekType {
        FF_SEEK, FR_SEEK, REGULAR_SEEK }

    var showRecIndication: MutableLiveData<Boolean>
    var pvrPlaybackCallback: PvrPlaybackCallback?
    var isPvrPlaybackPaused: Boolean

    interface PvrPlaybackCallback {
        fun onPlaybackPositionChanged(position: Long)
    }

    fun setPvrSpeed(speed: Int)
    fun reset()
    fun seek(positionMs: Long)

    fun setup()

    fun dispose()

    fun startRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback, isInfiniteRec: Boolean = false)

    fun startRecording(tvChannel: TvChannel, tvEvent: TvEvent?, duration: Long?, callback: IAsyncCallback)

    fun stopRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback)
    fun stopRecordingByChannelAndEvent(
        tvChannel: TvChannel, tvEvent: TvEvent, callback: IAsyncCallback)

    fun getRecording(i: Int, callback: IAsyncDataCallback<Recording>)
    fun removeRecording(recording: Recording, callback: IAsyncCallback)

    fun removeAllRecordings(callback: IAsyncCallback)

    fun renameRecording(recording: Recording, name: String, callback: IAsyncCallback)
    fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>)

    fun getRecordingList(callback: IAsyncDataCallback<List<Recording>>)
    fun getRecordingsCount(callback: IAsyncDataCallback<Int>)

    fun updateRecording(duration: Long?)

    // TODO: Resolve: ReferenceRecordingHandler has fun that returns ReferenceRecordings,
    //       probably not important.

    // TODO: Resolve ReferencePVRHandler methods, should we keep it
    fun setRecIndication(show: Boolean)
    fun setPlaybackPosition(recordingId: Int, position : Long)
    fun setSignalAvailability(isAvailable: Boolean)
    fun getPlaybackPosition(recordingId: Int): Long
    fun getPlaybackPositionPercent(recording: Recording): Double

    // TODO: Synchronous getRecordingInProgress function should keep one of them
    fun getRecordingInProgressTvChannel(): TvChannel?

    // TODO: If recording in progress should probably be moved to PlayerInterface?
    fun isRecordingInProgress(): Boolean

    // TODO: Maybe add function to PlayerInterface to start Recorded material (Get Recording from Pvr Interface)
    fun startPlayback(recording: Recording, callback: IAsyncCallback)
    fun pausePlayPlayback(callback: IAsyncCallback)
    fun pausePlayback()
    fun stopPlayback(callback: IAsyncCallback)
    fun resumePlayback()
    fun seekPlayback(position: Long, callback: IAsyncCallback)
    fun isChannelRecordable(channel: TvChannel): Boolean

    //Added functions
    fun setPvrTvView(liveTvView: TvView)
    fun setPvrPositionCallback()
    fun reset(callback: IAsyncCallback)

    fun setPvrState(state: PvrSeekType)

    fun reloadRecordings()

}