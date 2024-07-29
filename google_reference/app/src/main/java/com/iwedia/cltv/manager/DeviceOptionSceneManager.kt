
package com.iwedia.cltv.manager
import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.entities.ReferenceDeviceItem
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.scene.device_option.DeviceOptionListener
import com.iwedia.cltv.scene.device_option.DeviceOptionScene
import com.iwedia.guide.android.tools.GAndroidSceneManager
import kotlinx.coroutines.Dispatchers
import world.SceneData

class DeviceOptionSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    private var utilsModule: UtilsInterface,
    private val pvrModule: PvrInterface,
    private val textToSpeechModule: TTSInterface
) : GAndroidSceneManager(
    context,
    worldHandler,
    ReferenceWorldHandler.SceneId.DEVICE_OPTION_SCENE),
    DeviceOptionListener
{
    private var sceneData: SceneData? = null
    private var deviceItem: ReferenceDeviceItem? = null
    override fun onSceneInitialized() {
        sceneData = data as SceneData
        deviceItem = sceneData!!.getData() as ReferenceDeviceItem
        scene!!.refresh(sceneData)
    }


    override fun onSelctTimeshift() {
        if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.TIME_SHIFT){
            showToast(ConfigStringsManager.getStringById("set_timeshift_not_allowed_timeshift"))
            return
        }
        if(deviceItem?.moundPoint != null){
            utilsModule.setTimeshiftStoragePath(deviceItem?.moundPoint!!)
        }
        worldHandler!!.destroySpecific(id,instanceId)
    }
    override fun onSelectPvr() {
        if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING){
            showToast(ConfigStringsManager.getStringById("set_pvr_not_allowed_pvr"))
            return
        }
//        TvConfigurationHelper.setPVR(deviceItem!!.moundPoint!!,context!!)
        deviceItem?.moundPoint?.let { point ->
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSelectPvr: mount point $point")
            var diskPath = ""
            val pvrpath: Array<String> = point.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (path in pvrpath) {
                diskPath = path
            }
            Log.i(TAG, "onSelectPvr: setPvrStoragePath $diskPath")
            utilsModule.setPvrStoragePath(diskPath)
        }

        worldHandler!!.destroySpecific(id,instanceId)
    }
    override fun onSelectFormat() {
        if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.TIME_SHIFT){
            showToast(ConfigStringsManager.getStringById("format_not_allowed_timeshift"))
            return
        }
        if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING){
            showToast(ConfigStringsManager.getStringById("format_not_allowed_pvr"))
            return
        }
        val sceneData = DialogSceneData(id, instanceId)
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title =ConfigStringsManager.getStringById("usb_format")
        sceneData.positiveButtonText =ConfigStringsManager.getStringById("yes")
        sceneData.negativeButtonText =ConfigStringsManager.getStringById("no")
        sceneData.dialogClickListener =object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.DESTROY
                )
            }
            override fun onPositiveButtonClicked() {
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.DESTROY
                )
                (scene as DeviceOptionScene).showFormat()
                utilsModule.registerFormatProgressListener(
                    object : UtilsInterface.FormattingProgressListener{
                        override fun reportProgress(visible: Boolean, statusText: String) {
                            CoroutineHelper.runCoroutineWithDelay({
                                (scene as DeviceOptionScene).refresh(statusText)
                            })
                        }
                        override fun onFinished(isSuccess: Boolean) {
                            CoroutineHelper.runCoroutineWithDelay({
                                if(!isSuccess){
                                    showToast(ConfigStringsManager.getStringById("device_format_failed"))
                                } else {
                                    (scene as DeviceOptionScene).hideFormat()
                                }
//                                TvConfigurationHelper.registerProgressListener(null)

                            })
                        }
                    }
                )

                CoroutineHelper.runCoroutine({
                    utilsModule.formatUsbStorage(object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            CoroutineHelper.runCoroutine({
                                showToast(ConfigStringsManager.getStringById("device_format_failed"))
                                (scene as DeviceOptionScene).hideFormat()
                            }, Dispatchers.Main)
                        }

                        override fun onSuccess() {
                            pvrModule.removeAllRecordings(object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onFailed: removeAllRecordings")
                                }

                                override fun onSuccess() {
                                    CoroutineHelper.runCoroutine({
                                        showToast(ConfigStringsManager.getStringById("device_format_success"))
                                        (scene as DeviceOptionScene).hideFormat()
                                    }, Dispatchers.Main)

                                    ReferenceApplication.runOnUiThread {
                                        ReferenceApplication.worldHandler!!.destroySpecific(
                                            id,
                                            instanceId
                                        )
                                    }
                                }

                            })
                        }

                    })
                })


            }
        }
        ReferenceApplication.worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
            Action.SHOW, sceneData
        )
    }
    override fun onSelectSpeedTest() {
        if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.TIME_SHIFT){
            showToast(ConfigStringsManager.getStringById("speed_test_not_allowed_timeshift"))
            return
        }
        if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING){
            showToast(ConfigStringsManager.getStringById("speed_test_not_allowed_pvr"))
            return
        }
        if (deviceItem!!.availableSize!! <= 0) {
            showToast(ConfigStringsManager.getStringById("speed_test_insufficient"))
            return
        }
        if (deviceItem!!.isNtfs!!) {
            showToast(ConfigStringsManager.getStringById("speed_test_ntfs_failed"))
            return
        }

        startSpeedTest()
    }

    private fun startSpeedTest() {
        (scene as DeviceOptionScene).showSpeedTest()
        deviceItem?.moundPoint?.let {
            CoroutineHelper.runCoroutine({
                utilsModule.startSpeedTest(it, object : UtilsInterface.SpeedTestListener {
                    override fun reportProgress(progress: Int) {
                        (scene as DeviceOptionScene).refresh(progress)
                    }

                    override fun onFinished(isSuccess: Boolean, speedRate: Float) {
                        if (isSuccess) (scene as DeviceOptionScene).onFinishSpeedWidget(speedRate)
                        else {
                            // TODO speed test failed
                        }
                    }

                })
            })
        } ?: throw IllegalArgumentException()
    }

    override fun createScene() {
        scene = DeviceOptionScene(context!!, this)
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun onBackPressed(): Boolean {
        ReferenceApplication.worldHandler!!.triggerAction(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
            Action.DESTROY
        )
        return true
    }

    companion object {
        val TAG = "DeviceOptionSceneManager"
    }
}
