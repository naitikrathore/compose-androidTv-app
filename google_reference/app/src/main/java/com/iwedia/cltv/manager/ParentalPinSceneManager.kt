package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.scene.parental_control.change_pin.ParentalPinScene
import com.iwedia.cltv.scene.parental_control.change_pin.ParentalPinSceneData
import com.iwedia.cltv.scene.parental_control.change_pin.ParentalPinSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

/**
 * Parent pin scene manager
 *
 * @author Aleksandar Lazic
 */
class ParentalPinSceneManager : GAndroidSceneManager, ParentalPinSceneListener {

    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        utilsModule: UtilsInterface,
        textToSpeechModule: TTSInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.PARENTAL_PIN
    ){
        this.utilsModule = utilsModule
        this.textToSpeechModule = textToSpeechModule
    }

    private var utilsModule: UtilsInterface
    private var textToSpeechModule: TTSInterface
    private lateinit var newPinEntered: String

    override fun createScene() {
        scene = ParentalPinScene(context!!, this)
    }

    override fun onCurrentPinEntered(pin: String) {
        if (pin == utilsModule.getParentalPin()) {
            scene!!.refresh(1)
        } else {
            showToast(ConfigStringsManager.getStringById("wrong_pin_toast"))
            scene!!.refresh(-1)
        }
    }

    override fun onNewPinEntered(pin: String) {
        newPinEntered = pin
        scene!!.refresh(2)
    }

    override fun onConfirmNewPinEntered(pin: String) {
        // Comparing new pin with confirmation pin
        if (pin == newPinEntered) {
            if(!utilsModule.setParentalPin(pin)) {
                showToast(ConfigStringsManager.getStringById("pin_not_allowed_toast"), UtilsInterface.ToastDuration.LENGTH_LONG)
            }
            onBackPressed()
        } else {
            //Show toast when new PIN doesn't match
            showToast(ConfigStringsManager.getStringById("wrong_confirmation_new_pin_toast"), UtilsInterface.ToastDuration.LENGTH_LONG)
            //Ask new pin again
            scene!!.refresh(1)
        }
    }

    override fun isAccessibilityEnabled(): Boolean {
        return utilsModule.isAccessibilityEnabled()
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun onSceneInitialized() {
        if (data != null && data is ParentalPinSceneData) {
            if ((data as ParentalPinSceneData).sceneType == ParentalPinSceneData.CA_CHANGE_PIN_SCENE_TYPE) {
                scene!!.refresh(1)
            }
        }
        scene!!.refresh(null)
    }
}