package com.iwedia.cltv.manager

import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.scene.reminder_conflict_scene.ReminderConflictScene
import com.iwedia.cltv.scene.reminder_conflict_scene.ReminderConflictSceneListener
import com.iwedia.cltv.scene.reminder_conflict_scene.ReminderSceneData
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus

/**
 * A Scene Manager class for ReminderConflictScene
 *
 * @author Shubham Kumar
 */

class ReminderConflictSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val tvModule: TvInterface,
    val utilsModule: UtilsInterface,
    private val textToSpeechModule: TTSInterface
)  : GAndroidSceneManager(
    context,
    worldHandler,
    ReferenceWorldHandler.SceneId.REMINDER_CONFLICT_SCENE),ReminderConflictSceneListener {

    //Reminder conflict scene data
    private var sceneData: ReminderSceneData?= null

    override fun createScene() {
        scene = ReminderConflictScene(context!!, this)
    }

    override fun onEventSelected() {
        sceneData?.eventSelectedClickListener?.onEventSelected()
    }

    override fun onBackPressed(): Boolean {
        ReferenceApplication.worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }

    override fun onSceneInitialized() {
        sceneData = data as ReminderSceneData?
        scene!!.refresh(sceneData)
    }

    override fun playChannel(scheduledChannel: TvChannel) {
        InformationBus.submitEvent(Event(Events.WATCHLIST_INPUT))
        when( ReferenceApplication.worldHandler!!.playbackState) {
            ReferenceWorldHandler.PlaybackState.TIME_SHIFT -> {
                InformationBus.submitEvent(Event(Events.SHOW_WATCHLIST_TIME_SHIFT_CONFLICT_DIALOG, scheduledChannel))
                return
            }
            ReferenceWorldHandler.PlaybackState.RECORDING -> {
                // Even after onBackPressed() called application was crashing, there is some
                // fragment in background which is leading to crash so, need to destroy other existing scene/dialogs
                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                    ReferenceWorldHandler.SceneId.LIVE
                )
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

                tvModule.changeChannel(scheduledChannel, object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        InformationBus.submitEvent(Event(utils.information_bus.events.Events.HIDE_SCENE_LOADING))
                    }

                    override fun onSuccess() {
                        utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal)
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

            else -> {}
        }
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun onDestroy() {
        ReferenceApplication.noChannelScene = false
        ReferenceApplication.isDialogueCreated = false
        super.onDestroy()
    }
}

