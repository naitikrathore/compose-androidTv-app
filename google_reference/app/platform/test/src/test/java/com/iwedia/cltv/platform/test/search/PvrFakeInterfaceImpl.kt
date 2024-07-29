package com.iwedia.cltv.platform.test.search

import android.media.tv.TvView
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import java.util.ArrayList

class PvrFakeInterfaceImpl(override var showRecIndication: MutableLiveData<Boolean>) : PvrInterface {
    override var pvrPlaybackCallback: PvrInterface.PvrPlaybackCallback?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var isPvrPlaybackPaused: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun setPvrSpeed(speed: Int) {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun setup() {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun startRecordingByChannel(
        tvChannel: TvChannel,
        callback: IAsyncCallback,
        isInfiniteRec: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun startRecording(
        tvChannel: TvChannel,
        tvEvent: TvEvent?,
        duration: Long?,
        callback: IAsyncCallback
    ) {
        TODO("Not yet implemented")
    }

    override fun stopRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun stopRecordingByChannelAndEvent(
        tvChannel: TvChannel,
        tvEvent: TvEvent,
        callback: IAsyncCallback
    ) {
        TODO("Not yet implemented")
    }

    override fun getRecording(i: Int, callback: IAsyncDataCallback<Recording>) {
        TODO("Not yet implemented")
    }

    override fun removeRecording(recording: Recording, callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun renameRecording(recording: Recording, name: String, callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>) {
        TODO("Not yet implemented")
    }

    var recordingQueryList = ArrayList<Recording>()
    override fun getRecordingList(callback: IAsyncDataCallback<List<Recording>>) {
        callback.onReceive(recordingQueryList)
    }

    override fun getRecordingsCount(callback: IAsyncDataCallback<Int>) {
        TODO("Not yet implemented")
    }

    override fun updateRecording(duration: Long?) {
        TODO("Not yet implemented")
    }

    override fun setRecIndication(show: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setPlaybackPosition(recordingId: Int, position: Long) {
        TODO("Not yet implemented")
    }

    override fun setSignalAvailability(isAvailable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getPlaybackPosition(recordingId: Int): Long {
        TODO("Not yet implemented")
    }

    override fun getRecordingInProgressTvChannel(): TvChannel? {
        TODO("Not yet implemented")
    }

    override fun isRecordingInProgress(): Boolean {
        TODO("Not yet implemented")
    }

    override fun startPlayback(recording: Recording, callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun pausePlayPlayback(callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun stopPlayback(callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun resumePlayback(callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun seekPlayback(position: Long, callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun isChannelRecordable(channel: TvChannel): Boolean {
        TODO("Not yet implemented")
    }

    override fun setPvrTvView(liveTvView: TvView) {
        TODO("Not yet implemented")
    }

    override fun setPvrPositionCallback() {
        TODO("Not yet implemented")
    }

    override fun reset(callback: IAsyncCallback) {
        TODO("Not yet implemented")
    }

    override fun seek(positionMs: Long) {
        TODO("Not yet implemented")
    }

    override fun isUsbSpaceAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setPvrState(state: PvrInterface.PvrSeekType) {
        TODO("Not yet implemented")
    }

    override fun reloadRecordings() {
        TODO("Not yet implemented")
    }
}

