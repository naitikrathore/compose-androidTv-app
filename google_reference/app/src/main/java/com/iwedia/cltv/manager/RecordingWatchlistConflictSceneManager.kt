package com.iwedia.cltv.manager

import android.annotation.SuppressLint
import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.scene.recording_watchlist_conflict_scene.RecordingWatchlistConflictScene
import com.iwedia.cltv.scene.recording_watchlist_conflict_scene.RecordingWatchlistConflictSceneData
import com.iwedia.cltv.scene.recording_watchlist_conflict_scene.RecordingWatchlistConflictSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus

/**
 *  Recording Watchlist Conflict Scene Manager
 *
 *  @author Shubham Kumar
 */


class RecordingWatchlistConflictSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val tvModule: TvInterface,
    val pvrModule: PvrInterface,
    val timeshiftModule: TimeshiftInterface,
    val timeModule: TimeInterface,
    val utilsModule: UtilsInterface,
    private val texttospeechModule: TTSInterface
) : GAndroidSceneManager(
    context,
    worldHandler,
    ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE
), RecordingWatchlistConflictSceneListener {

    private var sceneData: RecordingWatchlistConflictSceneData? = null

    override fun createScene() {
        scene = RecordingWatchlistConflictScene(context!!, this)
    }

    override fun onEventSelected() {
        sceneData?.eventSelectedClickListener?.onEventSelected()
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        texttospeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun isTimeShiftActive(): Boolean {
        return timeshiftModule.isTimeShiftActive
    }

    override fun timeShiftStop(callback: IAsyncCallback) {
        timeshiftModule.timeShiftStop(callback)
    }

    override fun changeChannel(tvChannel: TvChannel, callback: IAsyncCallback) {
        tvModule.changeChannel(tvChannel, callback)
    }

    override fun playChannel(scheduledChannel: TvChannel) {
        InformationBus.submitEvent(Event(Events.WATCHLIST_INPUT))
        when (ReferenceApplication.worldHandler!!.playbackState) {
            ReferenceWorldHandler.PlaybackState.TIME_SHIFT -> {
                InformationBus.submitEvent(
                    Event(
                        Events.SHOW_STOP_TIME_SHIFT_DIALOG,
                        scheduledChannel
                    )
                )
                return
            }

            ReferenceWorldHandler.PlaybackState.RECORDING -> {
                InformationBus.submitEvent(
                    Event(
                        Events.SHOW_STOP_RECORDING_DIALOG,
                        scheduledChannel
                    )
                )
                return
            }

            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE -> {
                InformationBus.submitEvent(Event(utils.information_bus.events.Events.SHOW_SCENE_LOADING))

                changeChannel(scheduledChannel, object : IAsyncCallback {
                    override fun onFailed(error: kotlin.Error) {
                        InformationBus.submitEvent(Event(utils.information_bus.events.Events.HIDE_SCENE_LOADING))
                    }

                    override fun onSuccess() {
                        utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, 0)
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        InformationBus.submitEvent(Event(utils.information_bus.events.Events.HIDE_SCENE_LOADING))
                        InformationBus.submitEvent(Event(Events.CHANNEL_CHANGED, scheduledChannel))
                        Log.i("ReminderConflictSceneManager", " HandlePlayChannel here")

                        if (scheduledChannel.isRadioChannel) {
                            val status =
                                LiveManager.LiveSceneStatus(LiveManager.LiveSceneStatus.Type.IS_RADIO)
                            InformationBus.submitEvent(
                                Event(
                                    Events.PLAYBACK_STATUS_MESSAGE_NONE,
                                    status
                                )
                            )
                        }
                    }
                })
            }

            else -> {

            }
        }
    }

    override fun startRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback) {

        pvrModule.startRecordingByChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }

            override fun onSuccess() {
                callback.onSuccess()
            }
        })
    }

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return timeModule.getCurrentTime(tvChannel)
    }

    @SuppressLint("NewApi")
    override fun onBackPressed(): Boolean {
        ReferenceApplication.worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }

    override fun onSceneInitialized() {
        sceneData = data as RecordingWatchlistConflictSceneData?
        scene!!.refresh(sceneData)
    }
}