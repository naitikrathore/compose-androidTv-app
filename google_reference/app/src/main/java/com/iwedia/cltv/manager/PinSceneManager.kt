package com.iwedia.cltv.manager

import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.scene.PIN.PinScene
import com.iwedia.cltv.scene.PIN.PinSceneData
import com.iwedia.cltv.scene.PIN.PinSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

/**
 * PIN Scene Manager
 *
 * @author Nishant Bansal
 */
class PinSceneManager : GAndroidSceneManager, PinSceneListener {

    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        utilsModule: UtilsInterface,
        textToSpeechModule: TTSInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.PIN_SCENE
    ){
        this.utilsModule = utilsModule
        this.textToSpeechModule = textToSpeechModule
    }

    private var utilsModule: UtilsInterface
    private var textToSpeechModule: TTSInterface
    var pinSceneListener : PinSceneData.PinSceneDataListener? =null

    override fun isParentalPinChanged(): Boolean {
        return utilsModule.isParentalPinChanged()
    }

    override fun getParentalPin(): String {
        return utilsModule.getParentalPin()
    }

    override fun isAccessibilityEnabled(): Boolean {
        return utilsModule.isAccessibilityEnabled()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun isDefaultPinNotificationRequired(): Boolean {
        //Added default value as .isFastOnly() so it should behave same on other platform
        return utilsModule.getCountryPreferences(UtilsInterface.CountryPreference.SHOW_DEFAULT_PIN_TIP,
            ((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()
        ) as Boolean
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun createScene() {
        scene = PinScene(context!!, this)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun checkPin(pin: String) {
        if (pin == utilsModule.getParentalPin()) {
            pinSceneListener!!.onPinSuccess()
            //destroy scene
            worldHandler!!.triggerAction(id, Action.DESTROY)
        } else {
            pinSceneListener!!.showToast(ConfigStringsManager.getStringById("wrong_pin_toast"))
            scene!!.refresh(-1)
        }
    }

    override fun onBackPressed(): Boolean {
        //destroy scene
        worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }

    override fun onSceneInitialized() {
        val sceneData = data as PinSceneData
        pinSceneListener = sceneData.listener
        scene!!.refresh(sceneData.getData())
    }

}