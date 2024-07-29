package com.iwedia.cltv.manager

import android.os.Build
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.TTXInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.RCU.RCUScene
import com.iwedia.cltv.scene.RCU.RCUSceneListener
import com.iwedia.cltv.scene.zap_digit.DigitZapItem
import com.iwedia.guide.android.tools.GAndroidSceneManager
import world.SceneData
import world.WorldHandler

class RCUManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val timeModule: TimeInterface,
    val ttxModule: TTXInterface,
    private val textToSpeechModule: TTSInterface
) : GAndroidSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.RCU_SCENE
), RCUSceneListener {

    val RCU_TAG = "RCUManager"
    private var overlayShown = false

    override fun createScene() {
        scene = RCUScene(context!!, this)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBackPressed(): Boolean {
        if(ttxModule.isTTXActive()){
            (ReferenceApplication.getActivity() as MainActivity).startTtx()
        }
        if (worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
            ReferenceApplication.worldHandler!!.getVisibles()
                .get(WorldHandler.LayerType.OVERLAY)!!.value.forEach { sceneManager ->
                    if (sceneManager.id == ReferenceWorldHandler.SceneId.DIGIT_ZAP) {
                        worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.DIGIT_ZAP, Action.DESTROY)
                    }
                }
        }
        worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }

    override fun onSceneInitialized() {
        // Check if already initialized
        if (ReferenceApplication.isInitalized) {
            scene!!.refresh(true)
            Log.d(Constants.LogTag.CLTV_TAG + RCU_TAG, "onSceneInitialized: Scene is initialized")

        }
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun digitPressed(digit: Int) {
        if(ttxModule.isTTXActive()){
            var keyCode: Int = digit
            if (digit in 0..9) {
                keyCode = KeyEvent.KEYCODE_0 + digit
            }
            (ReferenceApplication.getActivity() as MainActivity).handleKeyInTtx(keyCode)
            return
        }

        // Send digit to the digit zap scene if it is visible
        else if (ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
            ReferenceApplication.worldHandler!!.getVisibles()
                .get(WorldHandler.LayerType.OVERLAY)!!.value.forEach { sceneManager ->
                    if (sceneManager.id == ReferenceWorldHandler.SceneId.DIGIT_ZAP) {
                        if (digit < 10) {
                            (sceneManager as ZapDigitManager).onDigitPressed(digit)
                        } else if(digit == 10) {
                            (sceneManager as ZapDigitManager).onPeriodPressed()
                        } else if(digit == 11) {
                            (sceneManager as ZapDigitManager).onBackspacePressed()
                        }
                    }
                }
        }
        else if (digit < 10) {
            ReferenceApplication.runOnUiThread {
                val sceneData = SceneData(id, instanceId, DigitZapItem(digit), 2000L)
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIGIT_ZAP,
                    Action.SHOW_OVERLAY,
                    sceneData
                )
            }
        }
    }

    override fun okPressed() {
        if (worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
            ReferenceApplication.worldHandler!!.getVisibles()
                .get(WorldHandler.LayerType.OVERLAY)!!.value.forEach { sceneManager ->
                    if (sceneManager.id == ReferenceWorldHandler.SceneId.DIGIT_ZAP) {
                        worldHandler!!.triggerAction(id, Action.DESTROY)
                        (sceneManager as ZapDigitManager).requestFocus()

                    }
                }
        }
    }

    override fun onKey() {
        if (worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
            ReferenceApplication.worldHandler!!.getVisibles()
                .get(WorldHandler.LayerType.OVERLAY)!!.value.forEach { sceneManager ->
                    if (sceneManager.id == ReferenceWorldHandler.SceneId.DIGIT_ZAP) {
                        (sceneManager as ZapDigitManager).restartTimer()
                    }
                    else if (sceneManager.id == ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE) {
                        worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE, Action.HIDE)
                    }
                }
        }
    }

    override fun getCurrentTime(): Long {
        return timeModule.getCurrentTime()
    }

    override fun timerEnd() {
        //if digit zap scene is not visible use this timer to close rcu scene after inactivity
        //if both are visible, zap digit scene will handle timer for inactivity
        //todo musika i think we dont need this any more
//        if (!worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
//            worldHandler!!.triggerAction(id, Action.DESTROY)
//        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun ttxPressed() {
        if (ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
            ReferenceApplication.runOnUiThread{
                worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.DIGIT_ZAP, Action.DESTROY)
            }
        }
        (ReferenceApplication.getActivity() as MainActivity).startTtx(true)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun redPressed() {
        if(ttxModule.isTTXActive()) {
            digitPressed(KeyEvent.KEYCODE_PROG_RED)
        }else{
            (ReferenceApplication.getActivity() as MainActivity).globalKey(KeyEvent.KEYCODE_PROG_RED)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun bluePressed() {
        if(ttxModule.isTTXActive()) {
            digitPressed(KeyEvent.KEYCODE_PROG_BLUE)
        }else{
            (ReferenceApplication.getActivity() as MainActivity).globalKey(KeyEvent.KEYCODE_PROG_BLUE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun greenPressed() {
        if(ttxModule.isTTXActive()) {
            digitPressed(KeyEvent.KEYCODE_PROG_GREEN)
        }else{
            (ReferenceApplication.getActivity() as MainActivity).globalKey(KeyEvent.KEYCODE_PROG_GREEN)
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun yellowPressed() {
        if(ttxModule.isTTXActive()) {
            digitPressed(KeyEvent.KEYCODE_PROG_YELLOW)
        }else{
            (ReferenceApplication.getActivity() as MainActivity).globalKey(KeyEvent.KEYCODE_PROG_YELLOW)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun backPressed() {
        if(ttxModule.isTTXActive()) {
            onBackPressed()
        }else if(ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)){
            digitPressed(11)
        }else{
            worldHandler!!.triggerAction(id, Action.DESTROY)
        }
    }

    override fun ttxState(): Boolean {
        return ttxModule.isTTXActive()
    }

}