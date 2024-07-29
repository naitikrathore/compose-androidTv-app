package com.iwedia.cltv.manager

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.recording_conflict.RecordingConflictListener
import com.iwedia.cltv.scene.recording_conflict.RecordingConflictScene
import com.iwedia.cltv.scene.recording_conflict.RecordingConflictSceneData
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus

class RecordingConflictSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val schedulerModule: SchedulerInterface,
    val epgModule: EpgInterface,
    val tvModule: TvInterface,
    val pvrModule: PvrInterface,
    val utilsModule: UtilsInterface,
    val timeInterface: TimeInterface,
    private val textToSpeechModule: TTSInterface
) : GAndroidSceneManager(
    context,
    worldHandler,
    ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE),
    RecordingConflictListener
{

    private val TAG = "RecordingConflictSceneManager: "
    private val CONFLICT_OFFSET = 1 * 60 * 1000
    private var activeRecordingEvent: TvEvent? = null
    private var requestedRecordingEvent: TvEvent? = null
    private var sceneData: RecordingConflictSceneData? = null

    override fun onSceneInitialized() {
        sceneData = data as RecordingConflictSceneData
        scene!!.refresh(sceneData)
    }
    override fun createScene() {
        scene = RecordingConflictScene(context!!, this)
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun scheduleNew(
        toScheduleRec: ScheduledRecording,
        recordingsToRemove: MutableList<ScheduledRecording>) {

        recordingsToRemove.forEach {
            cancelScheduledRecording(it)
        }
        utilsModule.runCoroutineWithDelay({
            //schedule new
            checkConflictAndScheduleRecording(toScheduleRec)
        },500)
    }

    override fun getNewRec(): ScheduledRecording? {
         return schedulerModule.getNewRec()
    }

    override fun getOldRec(
        refScheduledRec: ScheduledRecording,
    ): MutableList<ScheduledRecording> {

        val scheduleConflictRec = ScheduledRecording(
            refScheduledRec.id,
            refScheduledRec.name,
            refScheduledRec.scheduledDateStart,
            refScheduledRec.scheduledDateEnd,
            refScheduledRec.tvChannelId,
            refScheduledRec.tvEventId,
            refScheduledRec.repeatFreq,
            refScheduledRec.tvChannel,
            refScheduledRec.tvEvent
        )
        return schedulerModule.findConflictedRecordings(scheduleConflictRec)!!
    }

    override fun getOldRecByTvEvent(tvEvent: TvEvent?): MutableList<ScheduledRecording> {
        return schedulerModule.findConflictedRecordings(tvEvent!!.startTime,  tvEvent.endTime)
    }

    @SuppressLint("NewApi")
    override fun onBackPressed(): Boolean {
        ReferenceApplication.worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }

    override fun getActiveRecording(): TvEvent? {
        return activeRecordingEvent
    }

    override fun getRequestedRecording(): TvEvent? {
        return requestedRecordingEvent
    }

    override fun onFirstItemClicked() {
        sceneData?.onItemClicked?.onFirstItemClicked()
    }

    override fun onSecondItemClicked() {
        sceneData?.onItemClicked?.onSecondItemClicked()
    }

    override fun cancelScheduledStartActive(tvChannel: TvChannel,
        scheduleRecDeleteList: MutableList<ScheduledRecording>) {

        scheduleRecDeleteList.forEach {
           cancelScheduledRecording(it)
        }
        utilsModule.runCoroutineWithDelay({
            //schedule new
                  pvrModule.startRecordingByChannel(tvChannel, object : IAsyncCallback{
                      override fun onFailed(error: Error) { }

                      override fun onSuccess() {}
                  })
        },500)
    }

    private fun cancelScheduledRecording(record : ScheduledRecording) {
        if (schedulerModule.isInReclist(record.tvChannelId, record.scheduledDateStart)) {
            schedulerModule.removeScheduledRecording(record, object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "remove recording failed")
                }

                override fun onSuccess() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "remove recording success: ")
                }
            })
        } else {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Unable to cancelScheduledRecording: recording doesn't exist in list ")
        }
    }

    @SuppressLint("NewApi")
    private fun checkConflictAndScheduleRecording(recording: ScheduledRecording) {

        tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel>{
            override fun onFailed(error: Error) {}

            override fun onReceive(tvChannel: TvChannel) {
                epgModule.getCurrentEvent(tvChannel, object : IAsyncDataCallback<TvEvent> {
                    override fun onFailed(error: Error) {
                        onReceive(TvEvent.createNoInformationEvent( tvChannel, timeInterface.getCurrentTime(tvChannel)))
                    }
                    override fun onReceive(tvEvent: TvEvent) {
                        if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING){
                            if (recording.scheduledDateStart < (activeRecordingEvent!!.endTime) + CONFLICT_OFFSET){
                                updateScheduleRecordingConflict(recording, tvEvent )
                            } 
                        }
                        else {
                            if (!schedulerModule.isInReclist(recording.tvChannelId, recording.scheduledDateStart)) {
                                    val conflictedRecordings =
                                        schedulerModule.findConflictedRecordings(recording)
                                if (conflictedRecordings?.isNotEmpty() == true) {
                                    val currentActiveScene =
                                            ReferenceApplication.worldHandler!!.active
                                        val sceneData = RecordingConflictSceneData(
                                            currentActiveScene!!.id,
                                            currentActiveScene.instanceId
                                        )
                                        sceneData.type =
                                            RecordingConflictSceneData.RecordingConflictType.SCHEDULE_SCHEDULE
                                        sceneData.secondSchedule = recording
                                        sceneData.firstSchedule = conflictedRecordings

                                        return
                                    }

                                    if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
                                        checkAndLoadScheduleRecording(recording, tvChannel, tvEvent)
                                    }

                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "startRecordingScheduleEvent")
                                    schedulerModule.scheduleRecording(recording,
                                        object :
                                            IAsyncDataCallback<SchedulerInterface.ScheduleRecordingResult> {
                                            override fun onFailed(error: Error) {
                                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleRecording onFailed")
                                            }

                                            @RequiresApi(Build.VERSION_CODES.R)
                                            override fun onReceive(data: SchedulerInterface.ScheduleRecordingResult) {
                                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: ${recording.name}")
                                                InformationBus.submitEvent(Event(Events.REFRESH_FOR_YOU, data))
                                                InformationBus.submitEvent(Event(Events.UPDATE_REC_LIST_INDICATOR))
                                            }
                                        })
                            } else  Log.d(Constants.LogTag.CLTV_TAG + TAG, "Recording already scheduled")
                        }
                    }
                })
            }
        })

    }

    @SuppressLint("NewApi")
    private fun checkAndLoadScheduleRecording(recording: ScheduledRecording, activeTvChannel: TvChannel, tvEvent: TvEvent) {
        
        if (recording.scheduledDateStart < tvEvent.endTime) {
            val currentActiveScene =
                ReferenceApplication.worldHandler!!.active
            val sceneData = RecordingConflictSceneData(
                currentActiveScene!!.id,
                currentActiveScene!!.instanceId
            )
            sceneData.type =
                RecordingConflictSceneData.RecordingConflictType.RECORDING_SCHEDULE
            sceneData.firstRecording = tvEvent
            sceneData.secondSchedule = recording
            sceneData.onItemClicked =
                object : RecordingConflictSceneData.ItemClicked {
                    override fun onFirstItemClicked() {
                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,
                            Action.DESTROY
                        )
                        pvrModule.stopRecordingByChannel(
                            activeTvChannel,
                            object : IAsyncCallback {
                                override fun onFailed(error: Error) {}

                                override fun onSuccess() {
                                    checkConflictAndScheduleRecording(recording)
                                }

                            })
                        return
                    }

                    override fun onSecondItemClicked() {
                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,
                            Action.DESTROY
                        )
                    }
                }
            ReferenceApplication.worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,
                Action.SHOW, sceneData
            )
        }
    }

    @SuppressLint("NewApi")
    private fun updateScheduleRecordingConflict(recording: ScheduledRecording, activeRecordingEvent: TvEvent) {
        val currentActiveScene = ReferenceApplication.worldHandler!!.active
        val sceneData = RecordingConflictSceneData(currentActiveScene!!.id, currentActiveScene!!.instanceId)
        sceneData.type = RecordingConflictSceneData.RecordingConflictType.RECORDING_SCHEDULE
        sceneData.title = ConfigStringsManager.getStringById("description_text")
        sceneData.firstRecording = activeRecordingEvent
        sceneData.secondSchedule = recording
        sceneData.firstItemText = getEventName(activeRecordingEvent)
        sceneData.secondItemText = getEventName(recording.tvEvent)
        sceneData.onItemClicked = object : RecordingConflictSceneData.ItemClicked {
            override fun onFirstItemClicked() {

                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,Action.DESTROY
                )
                ReferenceApplication.runOnUiThread {
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                        Action.SHOW_OVERLAY
                    )
                }
                return
            }

            override fun onSecondItemClicked() {
                checkConflictAndScheduleRecording(recording)
                schedulerModule.updateConflictRecordings(recording, true)

                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,Action.DESTROY)

                ReferenceApplication.runOnUiThread {
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                        Action.SHOW_OVERLAY
                    )
                }
            }
        }

        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
        ReferenceApplication.worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,Action.SHOW, sceneData)
    }

    private fun getEventName(data: Any?):String{
        if (data is TvEvent?){
            return if (data!!.name == "No Information"){
                data!!.tvChannel.name
            } else {
                data!!.tvChannel.name + " - " + data.name
            }
        } else if (data is MutableList<*>){
            var channelNames = ""
            data.forEach {
                if (it is ScheduledRecording){
                    val tvEvent = it.tvEvent
                    if (channelNames.isNotEmpty()) channelNames += ", "
                    channelNames += it.tvChannel?.name
                    if (tvEvent!!.name != "No Information") channelNames += (" - " + tvEvent.name)
                }
            }
            return channelNames
        }
        return ""
    }

}
