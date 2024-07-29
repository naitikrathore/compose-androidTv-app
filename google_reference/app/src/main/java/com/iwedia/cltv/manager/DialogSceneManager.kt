package com.iwedia.cltv.manager

import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.scene.dialog.DialogScene
import com.iwedia.cltv.scene.dialog.DialogSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus

/**
 * Dialog scene manager
 *
 * @author Aleksandar Lazic
 */
class DialogSceneManager : GAndroidSceneManager, DialogSceneListener {

    private val inputSourceModule: InputSourceInterface
    private val textToSpeechModule: TTSInterface
    private val utilsModule: UtilsInterface

    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        inputSourceMoudle: InputSourceInterface,
        textToSpeechModule: TTSInterface,
        utilsModule: UtilsInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.DIALOG_SCENE
    ) {
        isScreenFlowSecured = false
        this.inputSourceModule = inputSourceMoudle
        this.textToSpeechModule = textToSpeechModule
        this.utilsModule = utilsModule
    }
    val TAG = javaClass.simpleName
    /**
     * Dialog scene data
     */
    var sceneData: DialogSceneData?= null

    init {
        registerGenericEventListener(Events.START_USB_FORMATING)
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        if (event?.type == Events.START_USB_FORMATING) {
            (scene as DialogScene)!!.hideButtons()
        }
    }

    override fun createScene() {
        scene = DialogScene(context!!, this)
    }

    override fun onPositiveButtonClicked() {
        stopSpeech()
        sceneData?.dialogClickListener?.onPositiveButtonClicked()
    }

    override fun onNegativeButtonClicked() {
        stopSpeech()
        worldHandler!!.triggerAction(id, Action.DESTROY)
        sceneData?.dialogClickListener?.onNegativeButtonClicked()
    }

    override fun exitApplication() {
        InformationBus.submitEvent(Event(Events.EXIT_APPLICATION_ON_BACK_PRESS))
    }

    override fun onBackPressed(): Boolean {
        try{
            if (sceneData!!.isBackEnabled) {
                worldHandler!!.triggerAction(id, Action.DESTROY)
                sceneData?.dialogClickListener?.onNegativeButtonClicked()
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
        scene!!.refresh(inputSourceModule.getDefaultValue())
        sceneData?.let {
            if (it.title == ConfigStringsManager.getStringById("no_channels_found"))
                ReferenceApplication.isScanChannelsShowed = true
        }
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
        ReferenceApplication.isScanChannelsShowed = false
        super.onDestroy()
    }
}