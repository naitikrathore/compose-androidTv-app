package com.iwedia.cltv.scene.recording_conflict

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import world.SceneListener

interface RecordingConflictListener: SceneListener, TTSSetterInterface,
    ToastInterface {
    /**
     * schedule recording
     */
    fun scheduleNew(toScheduleRec: ScheduledRecording, recordingsToRemove: MutableList<ScheduledRecording>)

    /**
     * get conflicted old recording
     */
    fun getNewRec() : ScheduledRecording?

    /**
     * get new recording
     */
    fun getOldRec(refScheduledRec: ScheduledRecording) : MutableList<ScheduledRecording>

    fun getOldRecByTvEvent(tvEvent: TvEvent?): MutableList<ScheduledRecording>

    fun getActiveRecording(): TvEvent?

    fun getRequestedRecording(): TvEvent?

    fun cancelScheduledStartActive(toStart: TvChannel, toDelete: MutableList<ScheduledRecording>)

    fun onFirstItemClicked()

    fun onSecondItemClicked()

    override fun onBackPressed(): Boolean

    override fun onSceneInitialized()

}