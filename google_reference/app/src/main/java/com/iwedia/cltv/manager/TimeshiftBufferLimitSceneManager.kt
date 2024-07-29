package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.scene.timeshift.timeshiftBuffer.TimeshiftBufferLimitScene
import com.iwedia.cltv.scene.timeshift.timeshiftBuffer.TimeshiftBufferLimitSceneListener

class TimeshiftBufferLimitSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val timeshiftModule: TimeshiftInterface,
) : ReferenceSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.TIMESHIFT_BUFFER_LIMIT_SCENE
), TimeshiftBufferLimitSceneListener {
    override fun createScene() {
        scene = TimeshiftBufferLimitScene(context!!, this)
    }

    override fun onTimeShiftWatchClicked() {
        worldHandler!!.triggerAction(
            ReferenceWorldHandler.SceneId.TIMESHIFT_BUFFER_LIMIT_SCENE, Action.DESTROY
        )
    }

    override fun onReturnToLiveClicked() {
        timeshiftModule.stopTimeshift()
        worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
    }

    override fun onSceneInitialized() {}
    override fun initConfigurableKeys() {}
    override fun onTimeChanged(currentTime: Long) {}

}