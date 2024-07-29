package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.scene.DummyScene
import com.iwedia.guide.android.tools.GAndroidSceneManager

/**
 * Intro manager
 *
 * @author Veljko Ilkic
 */
class DummyManager : GAndroidSceneManager {

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.DUMMY
    ){
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = DummyScene(context!!, this)
    }

    override fun onSceneInitialized() {
        TODO("Not yet implemented")
    }
}