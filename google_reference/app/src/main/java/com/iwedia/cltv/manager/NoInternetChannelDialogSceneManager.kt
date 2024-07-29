package com.iwedia.cltv.manager

import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.scene.dialog.DialogSceneListener
import com.iwedia.cltv.scene.dialog.NoInternetChannelDialogScene
import com.iwedia.guide.android.tools.GAndroidSceneManager
import kotlinx.coroutines.Dispatchers
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.util.concurrent.TimeUnit

/**
 * No channel or no Internet Dialog scene manager
 * @author Gaurav Jain
 */
class NoInternetChannelDialogSceneManager : GAndroidSceneManager, DialogSceneListener {

    private val textToSpeechModule: TTSInterface
    private val utilsModule: UtilsInterface

    constructor(
        context: MainActivity, worldHandler: ReferenceWorldHandler,
        textToSpeechModule: TTSInterface,
        utilsModule: UtilsInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE
    ) {
        isScreenFlowSecured = false
        this.textToSpeechModule = textToSpeechModule
        this.utilsModule = utilsModule
    }

    /**
     * Dialog scene data
     */
    private var sceneData: DialogSceneData?= null
    val TAG = javaClass.simpleName

    override fun createScene() {
        scene = NoInternetChannelDialogScene(context!!, this)
        context!!.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val screenTimeoutTime = Settings.System.getInt(context!!.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "createScene: screenTimeoutTime == ${TimeUnit.MILLISECONDS.toMinutes(screenTimeoutTime.toLong())}")
    }

    override fun onPositiveButtonClicked() {
        sceneData?.dialogClickListener?.onPositiveButtonClicked()
    }

    override fun onNegativeButtonClicked() {}

    override fun exitApplication() {
        InformationBus.submitEvent(Event(Events.EXIT_APPLICATION_ON_BACK_PRESS))
    }

    override fun onBackPressed(): Boolean {
        try{
            if (sceneData!!.isBackEnabled) {
                CoroutineHelper.runCoroutine({
                    ReferenceApplication.getActivity().finish()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }, Dispatchers.Main)
            }

        }catch (E: Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBackPressed: ${E.printStackTrace()}")
        }
        return true
    }

    override fun onSceneInitialized() {
        ReferenceApplication.isDialogueCreated = true
        sceneData = data as DialogSceneData
        scene!!.refresh(sceneData)
    }

    override fun stopSpeech() {
        textToSpeechModule.stopSpeech()
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun onDestroy() {
        ReferenceApplication.noChannelScene = false
        ReferenceApplication.isDialogueCreated = false
        context!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}