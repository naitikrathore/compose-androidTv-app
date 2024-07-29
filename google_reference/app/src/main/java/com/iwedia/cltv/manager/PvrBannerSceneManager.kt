package com.iwedia.cltv.manager

import android.os.CountDownTimer
import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.TAG
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.scene.pvr_banner.PvrBannerScene
import com.iwedia.cltv.scene.pvr_banner.PvrBannerSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import kotlinx.coroutines.Dispatchers
import utils.information_bus.Event

/**
 * Pvr banner scene manager
 *
 * @author Dragan Krnjaic
 */
class PvrBannerSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val tvModule: TvInterface,
    val utilsModule: UtilsInterface,
    val pvrModule: PvrInterface
) : GAndroidSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE
), PvrBannerSceneListener {

    init {
        isScreenFlowSecured = false
    }

    companion object {
        var previousProgress: Long = 0L
    }

    override fun createScene() {
        scene = PvrBannerScene(context!!, this)
        registerGenericEventListener(Events.SHOW_STOP_RECORDING_DIALOG)
        registerGenericEventListener(Events.USB_DEVICE_DISCONNECTED)
        registerGenericEventListener(Events.REFRESH_PVR_BANNER)
    }
    var refreshProgressTimer :CountDownTimer? = null

    override fun onSceneInitialized() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSceneInitialized: ")
        initSceneData()
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }


    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.HIDE)
        pvrModule.setRecIndication(true)
        return true
    }

    override fun onDestroy() {
        cancelProgressTimer()
        super.onDestroy()
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        if (event?.type == Events.REFRESH_PVR_BANNER) {
            pvrModule.setRecIndication(true)
            ReferenceApplication.runOnUiThread(Runnable {
                (scene as PvrBannerScene).setVisible()
                (scene as PvrBannerScene).widget?.startTimer()
            })
        } else if (event?.type == Events.SHOW_STOP_RECORDING_DIALOG) {
            if (event.data != null && event.getData(0) is TvChannel) {
                var tvChannel = event.getData(0) as TvChannel

                utilsModule.runCoroutine({
                    var sceneData = DialogSceneData(id, instanceId)
                    sceneData.type = DialogSceneData.DialogType.YES_NO
                    sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
                    sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
                    sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
                    sceneData.dialogClickListener =
                        object : DialogSceneData.DialogClickListener {
                            override fun onNegativeButtonClicked() {
                                worldHandler!!.triggerAction(
                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                    Action.DESTROY
                                )
                                (scene as PvrBannerScene).widget?.startTimer()
                            }

                            override fun onPositiveButtonClicked() {
                                if (event.getData(1) != null && event.getData(1) == true) {
                                    if (pvrModule.isRecordingInProgress()) { // startNewRecording(tvChannel)
                                        pvrModule.stopRecordingByChannel(
                                            tvChannel,
                                            object : IAsyncCallback {
                                                override fun onFailed(error: java.lang.Error) {
                                                }

                                                override fun onSuccess() {

                                                }
                                            })
                                    }

                                } else {
                                    pvrModule.stopRecordingByChannel(
                                        tvChannel!!,
                                        object : IAsyncCallback {
                                            override fun onFailed(error: Error) {
                                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "stop recording failed")
                                            }

                                            override fun onSuccess() {
                                                ReferenceApplication.worldHandler!!.playbackState =
                                                    ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                                                previousProgress = 0L
                                                worldHandler!!.triggerAction(
                                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                                    Action.DESTROY
                                                )
                                                worldHandler!!.destroyOtherExisting(
                                                    ReferenceWorldHandler.SceneId.LIVE
                                                )
                                                tvModule.changeChannel(tvChannel!!, object :
                                                    IAsyncCallback {
                                                    override fun onFailed(error: Error) {
                                                    }

                                                    override fun onSuccess() {

                                                    }
                                                })
                                            }
                                        })
                                }
                            }
                        }
                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.SHOW, sceneData
                    )
                }, Dispatchers.Main)
            } else {
                var channelChange: String? = "NULL"
                if (event.data != null && event.getData(0) != null) {
                    if (event.getData(0) == true) {
                        channelChange = "UP"
                    } else {
                        channelChange = "DOWN"
                    }
                }
                showStopRecordingDialog(channelChange!!)
            }
        } else if (event?.type == Events.USB_DEVICE_DISCONNECTED) {
            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: kotlin.Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getActiveChannel -> onFailed() ")
                }

                override fun onReceive(tvChannel: TvChannel) {
                    pvrModule.stopRecordingByChannel(tvChannel!!, object :
                        IAsyncCallback {
                        override fun onFailed(error: Error) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "stop recording failed")
                        }

                        override fun onSuccess() {
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                            previousProgress = 0L
                            worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                Action.DESTROY
                            )
                            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                            tvModule.changeChannel(tvChannel, object :
                                IAsyncCallback {
                                override fun onFailed(error: Error) {
                                }

                                override fun onSuccess() {
                                }
                            })
                        }
                    })
                }
            })
        }
    }

    override fun showStopRecordingDialog(channelChange: String?) {
        utilsModule.runCoroutine({
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    (scene as PvrBannerScene).widget?.startTimer()
                }

                override fun onPositiveButtonClicked() {
                    Log.w(TAG, "onPositiveButtonClicked() here")
                    stopRecordOnActiveTvChannel(channelChange)
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHING)
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }, Dispatchers.Main)
    }

    override fun setRecIndication(boolean: Boolean) {
        pvrModule.setRecIndication(boolean)
    }

    override fun getChannelById(id: Int): TvChannel {
        return tvModule.getChannelById(id)!!
    }

    private fun stopRecordOnActiveTvChannel(channelChange: String?) {
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: kotlin.Error) {
                InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
            }

            override fun onReceive(activeTvChannel: TvChannel) {
                pvrModule.stopRecordingByChannel(activeTvChannel, object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                    }

                    override fun onSuccess() {
                        InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)

                        when (channelChange) {
                            "UP" -> {
                                tvModule.nextChannel(object : IAsyncCallback {
                                    override fun onSuccess() {}

                                    override fun onFailed(error: Error) {
                                    }
                                })
                            }
                            "DOWN" -> {
                                tvModule.previousChannel(object : IAsyncCallback {
                                    override fun onSuccess() {}

                                    override fun onFailed(error: Error) {
                                    }
                                })
                        }

                    }
                        utilsModule.runCoroutine({
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                            previousProgress = 0L
                        })
                    }
                })
            }
        })

    }

    private fun initSceneData() {
        pvrModule.getRecordingInProgress(object :
            IAsyncDataCallback<com.iwedia.cltv.platform.model.recording.RecordingInProgress> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Unable to show pvr banner failed to get recording in progress")
                worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            }

            override fun onReceive(recordingInProgress: com.iwedia.cltv.platform.model.recording.RecordingInProgress) {
                if (recordingInProgress.recordingEnd != null) {
                    ReferenceApplication.runOnUiThread {
                        scene!!.refresh(recordingInProgress.tvEvent)
                    }
                } else {
                    ReferenceApplication.runOnUiThread {
                        scene!!.refresh(recordingInProgress)
                    }
                }

                ReferenceApplication.worldHandler!!.playbackState =
                    ReferenceWorldHandler.PlaybackState.RECORDING

                cancelProgressTimer()
                refreshProgressTimer = object :
                    CountDownTimer(
                        recordingInProgress.maxRecordedDuration ?: Long.MAX_VALUE,
                        1000
                    ) {
                    override fun onTick(millisUntilFinished: Long) {
                        previousProgress = recordingInProgress.currentRecordedDuration
                        ReferenceApplication.runOnUiThread {
                            scene!!.refresh(recordingInProgress)
                        }
                    }

                    override fun onFinish() {
                        cancelProgressTimer()
                    }
                }
                refreshProgressTimer!!.start()
            }
        })
    }

    private fun cancelProgressTimer() {
        refreshProgressTimer?.cancel()
        refreshProgressTimer=null
    }
}