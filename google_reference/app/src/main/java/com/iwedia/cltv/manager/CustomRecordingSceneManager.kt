package com.iwedia.cltv.manager


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.SchedulerInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.scene.custom_recording.CustomRecordingScene
import com.iwedia.cltv.scene.custom_recording.CustomRecordingSceneListener
import com.iwedia.cltv.scene.recording_conflict.RecordingConflictSceneData
import com.iwedia.guide.android.tools.GAndroidSceneManager
import world.SceneManager
import java.util.*
import kotlin.Error

/**
 * Custom Recording Scene
 *
 * @author Kaustubh Kadam
 */
class CustomRecordingSceneManager : GAndroidSceneManager, CustomRecordingSceneListener {
    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        epgModule: EpgInterface,
        schedulerModule: SchedulerInterface?,
        tvModule: TvInterface,
        timeModule: TimeInterface,
        textToSpeechModule: TTSInterface,
        utilsModule: UtilsInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.CUSTOM_RECORDING
    ) {
        isScreenFlowSecured = false
        this.epgModule = epgModule
        this.schedulerModule = schedulerModule
        this.tvModule = tvModule
        this.timeModule = timeModule
        this.textToSpeechModule = textToSpeechModule
        this.utilsModule = utilsModule
    }
    private val TAG = javaClass.simpleName + " :"

    private var epgModule: EpgInterface? = null
    private var schedulerModule: SchedulerInterface? = null
    private var tvModule: TvInterface? = null
    private var timeModule: TimeInterface? = null
    private var textToSpeechModule: TTSInterface
    private var utilsModule: UtilsInterface

    override fun createScene() {
        scene = CustomRecordingScene(context!!, this)
    }

    override fun onSceneInitialized() {
        if (ReferenceApplication.isInitalized) {
            scene!!.refresh(true)
        }
    }

    override fun refresh(position: Int) {
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun scheduleCustomRecording(
        tvChannel: TvChannel,
        startTime: Long?,
        endTime: Long?,
        repeat: Int) {
        
        if (startTime != null) {
            Log.i(
                TAG,
                "callHandler: ${System.currentTimeMillis()} -  ${startTime} = ${System.currentTimeMillis() - startTime}"
            )
        }

        if (startTime != null && endTime != null) {
            var tvEvent: com.iwedia.cltv.platform.model.TvEvent? = null
            var repeatFreq : RepeatFlag = RepeatFlag.NONE
            if (repeat == 1){
                repeatFreq = RepeatFlag.DAILY
            }else if (repeat == 2){
                repeatFreq = RepeatFlag.WEEKLY
            }

                epgModule?.getEventListByChannelAndTime(tvChannel,
                    startTime,
                    endTime,
                    object : IAsyncDataCallback<ArrayList<TvEvent>>{
                        override fun onFailed(error: Error) {
                            tvEvent = TvEvent.createNoInformationEvent(tvChannel, startTime, endTime, timeModule?.getCurrentTime(tvChannel)!!)
                            Log.i(TAG, "TV Event created for: $startTime")
                            val obj = ScheduledRecording(
                                id,
                                tvChannel!!.name,
                                startTime,
                                endTime,
                                tvChannel.id,
                                tvEvent?.id,
                                repeatFreq,
                                tvChannel,
                                tvEvent
                            )
                            checkRecordingScheduleConflict(obj)
                        }

                        override fun onReceive(data: ArrayList<TvEvent>) {
                            Log.i(TAG, " epgModule : getEventListByChannelAndTime  & event size = ${data.size} ")
                            if(data.size > 0 &&  data.get(0) != null){
                                tvEvent = data.get(0)
                                    val obj = ScheduledRecording(
                                        id,
                                        tvChannel!!.name,
                                        startTime,
                                        endTime,
                                        tvChannel.id,
                                        tvEvent?.id,
                                        repeatFreq,
                                        tvChannel,
                                        tvEvent
                                    )
                                    checkRecordingScheduleConflict(obj)
                            }else{
                                tvEvent = TvEvent.createNoInformationEvent(tvChannel, startTime, endTime, timeModule?.getCurrentTime(tvChannel)!!)
                                val obj = ScheduledRecording(
                                    id,
                                    tvChannel!!.name,
                                    startTime,
                                    endTime,
                                    tvChannel.id,
                                    tvEvent?.id,
                                    repeatFreq,
                                    tvChannel,
                                    tvEvent
                                )
                                checkRecordingScheduleConflict(obj)
                            }
                        }
                    })
              }

    }


    override fun getChannelList(): ArrayList<TvChannel>{
      return tvModule?.getChannelList()!!
    }

    override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>) {
        tvModule?.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
            override fun onReceive(tvChannel: TvChannel) {
                Log.w(ReferenceApplication.TAG, "getActiveChannel() -> onReceive() ->  tvChannel = $tvChannel  ")
                callback.onReceive(tvChannel)
            }

            override fun onFailed(error: Error) {
                Log.w(ReferenceApplication.TAG, "getActiveChannel() -> onFailed()  ")
                callback.onFailed(error)
            }
        })
    }

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return timeModule!!.getCurrentTime(tvChannel)
    }

    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        if (data != null && data!!.previousSceneId == ReferenceWorldHandler.SceneId.HOME_SCENE) {
            worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.HOME_SCENE,
                Action.SHOW
            )
        } else {
            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.LIVE,
                Action.SHOW
            )
        }
        return true
    }

    fun checkRecordingScheduleConflict( recording : ScheduledRecording){
        recording.let {
            schedulerModule?.scheduleRecording( it,
                object : IAsyncDataCallback<SchedulerInterface.ScheduleRecordingResult>{
                    override fun onFailed(error: Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleRecording - onFailed()")
                    }

                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun onReceive(result: SchedulerInterface.ScheduleRecordingResult) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleRecording - onReceive()")
                        when (result){
                            SchedulerInterface.ScheduleRecordingResult.SCHEDULE_RECORDING_ALREADY_PRESENT ->{

                                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                                    ReferenceWorldHandler.SceneId.LIVE
                                )
                                val currentActiveScene = ReferenceApplication.worldHandler!!.active
                                val sceneData = RecordingConflictSceneData(currentActiveScene!!.id, currentActiveScene.instanceId)
                                sceneData.type = RecordingConflictSceneData.RecordingConflictType.SCHEDULE_SCHEDULE
                                sceneData.title = ConfigStringsManager.getStringById("description_text")
                                sceneData.secondSchedule = recording

                                var conflictSchedule = schedulerModule?.findConflictedRecordings(recording)

                                sceneData.firstSchedule = conflictSchedule
                                sceneData.firstItemText = getEventName(conflictSchedule)
                                sceneData.secondItemText = getEventName(recording.tvEvent)
                                ReferenceApplication.runOnUiThread {
                                    ReferenceApplication.worldHandler!!.triggerActionWithData(
                                        ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,
                                        SceneManager.Action.SHOW,
                                        sceneData
                                    )
                                }
                            }
                            SchedulerInterface.ScheduleRecordingResult.SCHEDULE_RECORDING_CONFLICTS ->{
                                showToast(ConfigStringsManager.getStringById("recording_watchlist_conflict"))
                            }
                            SchedulerInterface.ScheduleRecordingResult.SCHEDULE_RECORDING_SUCCESS ->{
                                Log.d(Constants.LogTag.CLTV_TAG + TAG,  "onSuccess: ${recording.name}")
                            }
                            SchedulerInterface.ScheduleRecordingResult.SCHEDULE_RECORDING_ERROR ->{
                                Log.d(Constants.LogTag.CLTV_TAG + TAG,  "onFailed")
                            }
                        }
                    }
                })
        }
    }

    private fun getEventName(data: Any?):String{
         if (data is MutableList<*>){
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