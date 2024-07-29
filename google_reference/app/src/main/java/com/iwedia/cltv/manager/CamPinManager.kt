package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.scene.ci_popup.CamPinScene
import com.iwedia.cltv.scene.ci_popup.CamPinSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

class CamPinManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler, var ciPlusModule: CiPlusInterface,
    val textToSpeechModule: TTSInterface
) : GAndroidSceneManager(context, worldHandler, ReferenceWorldHandler.SceneId.CAM_PIN_SCENE), CamPinSceneListener {

    private val TAG = javaClass.simpleName

    override fun createScene() {
        scene = CamPinScene(context!!, this)
    }

    override fun setCamPin(pin: String) {
        ciPlusModule.setCamPin(pin)
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun resolveConfigurableKey(keyCode: Int, action: Int): Boolean {
        return false
    }

    override fun onSceneInitialized() {
    }
}