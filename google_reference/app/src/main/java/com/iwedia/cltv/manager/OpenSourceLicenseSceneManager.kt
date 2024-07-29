package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.scene.open_source_scene.OpenSourceLicenseScene
import com.iwedia.cltv.scene.open_source_scene.OpenSourceLicenseSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import world.SceneManager

/**
 * OpenSourceLicenseScene manager
 */
class OpenSourceLicenseSceneManager : GAndroidSceneManager, OpenSourceLicenseSceneListener {


    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.OPEN_SOURCE_LICENSE_SCENE
    ) {
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = OpenSourceLicenseScene(context!!, this)
    }

    override fun onSceneInitialized() {
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
    }

    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        worldHandler!!.triggerAction(
            ReferenceWorldHandler.SceneId.HOME_SCENE,
            SceneManager.Action.SHOW
        )
        return true
    }
}