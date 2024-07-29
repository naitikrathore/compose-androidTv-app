package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.scene.PIN.PinSceneData
import com.iwedia.cltv.scene.parental_control.locked_channel.ParentalChannelLockScene
import com.iwedia.cltv.scene.parental_control.locked_channel.ParentalChannelLockSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

class ParentalLockedChannelManager : GAndroidSceneManager, ParentalChannelLockSceneListener {

    var utilsModule: UtilsInterface
    var textToSpeechModule: TTSInterface

    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        utilsModule: UtilsInterface,
        textToSpeechModule: TTSInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE
    ) {
        this.utilsModule = utilsModule
        this.textToSpeechModule = textToSpeechModule
    }

    var pinSceneListener: PinSceneData.PinSceneDataListener? = null

    override fun onUnlockPressed() {
        scene!!.refresh(2)
    }

    override fun checkPin(pin: String) {
        if (pin == utilsModule.getParentalPin()) {
            pinSceneListener!!.onPinSuccess()
            //destroy scene
            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
        } else {
            utilsModule.showToast(ConfigStringsManager.getStringById("wrong_pin_toast"))
            scene!!.refresh(-1)
        }
    }

    override fun requestActiveChannel() {
    }

    override fun isAccessibilityEnabled(): Boolean {
        return utilsModule.isAccessibilityEnabled()
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun createScene() {
        scene = ParentalChannelLockScene(context!!, this)
    }

    override fun onBackPressed(): Boolean {
        return true
    }

    override fun onSceneInitialized() {
        val sceneData = data as PinSceneData
        pinSceneListener = sceneData.listener
        scene!!.refresh(1)
    }
}