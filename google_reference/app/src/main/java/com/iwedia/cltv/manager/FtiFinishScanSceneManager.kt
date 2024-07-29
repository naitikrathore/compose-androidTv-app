package com.iwedia.cltv.manager

import android.media.tv.TvInputInfo
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.scene.fti.scanFinished.FtiFinishScanScene
import com.iwedia.cltv.scene.fti.scanFinished.FtiFinishScanSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus

/**
 * Fti finish scan scene manager
 *
 * @author Aleksandar Lazic
 */
class FtiFinishScanSceneManager : GAndroidSceneManager, FtiFinishScanSceneListener {

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.FTI_FINISH_SCAN
    ) {
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = FtiFinishScanScene(context!!, this)
    }

    override fun onSceneInitialized() {
        val item = data!!.getDataByIndex(0) as TvInputInfo
        var channelsNumber = data!!.getDataByIndex(1)
        scene!!.refresh(item)
        scene!!.refresh(channelsNumber)
    }

    override fun onProceedClicked() {
        ReferenceApplication.worldHandler!!.destroyExisting()
        ReferenceApplication.worldHandler!!.triggerAction(
            ReferenceWorldHandler.SceneId.LIVE,
            Action.SHOW
        )
        InformationBus.submitEvent(Event(Events.PROCEED_CLICKED))
    }

    override fun onBackPressed(): Boolean {
        ReferenceApplication.worldHandler!!.triggerAction(id, Action.DESTROY)
        ReferenceApplication.worldHandler!!.triggerAction(data!!.previousSceneId, Action.SHOW)
        return super.onBackPressed()
    }
}