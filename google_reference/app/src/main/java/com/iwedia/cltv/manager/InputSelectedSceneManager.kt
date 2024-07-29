package com.iwedia.cltv.manager

import android.os.CountDownTimer
import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.scene.input_scene.InputSceneListener
import com.iwedia.cltv.scene.input_scene.InputSelectedScene
import com.iwedia.guide.android.tools.GAndroidSceneManager


/**
 * Input selected scene manager
 *
 * @author Vandana B
 */
class InputSelectedSceneManager : GAndroidSceneManager, InputSceneListener {


    private var inputSourceModule: InputSourceInterface? = null
    private var selectedData: ArrayList<String> = ArrayList()

    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        inputSourceMoudle: InputSourceInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE
    ) {
        this.inputSourceModule = inputSourceMoudle
    }

    private var updateTimer: CountDownTimer? = null

    override fun createScene() {
        scene = InputSelectedScene(context!!, this)
    }

    override fun onClicked(inputData: InputItem, position: Int, blocked: Boolean) {
    }

    override fun blockInput(inputData: InputSourceData, isBlock: Boolean) {
    }

    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }

    override fun onSceneInitialized() {
        stopUpdateTimer()
        updateTimer = object :
            CountDownTimer(
                3000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                worldHandler?.triggerAction(id, Action.DESTROY)
                if (worldHandler?.active != null && worldHandler?.active?.id == ReferenceWorldHandler.SceneId.ZAP_BANNER) {
                    worldHandler?.triggerAction(
                        ReferenceWorldHandler.SceneId.ZAP_BANNER,
                        Action.DESTROY
                    )
                }
            }
        }
        updateTimer!!.start()
        scene!!.refresh(data)

    }

    override fun onDestroy() {
        super.onDestroy()
        selectedData.clear()
        stopUpdateTimer()
    }

    override fun onPause() {
        super.onPause()
        selectedData.clear()
        stopUpdateTimer()
    }

    private fun stopUpdateTimer() {
        if (updateTimer != null) {
            updateTimer!!.cancel()
            updateTimer = null
        }
    }
}