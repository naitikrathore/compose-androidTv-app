package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.scene.evaluation_scene.EvalLicenceExpiryScene
import com.iwedia.cltv.scene.evaluation_scene.EvalLicenceExpirySceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event

/**
 * EvalLicenceExpiryScene manager
 */
class EvalLicenceExpirySceneManager : GAndroidSceneManager, EvalLicenceExpirySceneListener {

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.EVAL_LICENSE_EXPIRY_SCENE
    ) {
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = EvalLicenceExpiryScene(context!!, this)
    }

    override fun onSceneInitialized() {
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
    }
}