package com.iwedia.cltv.scene.custom_webview

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.guide.android.tools.GAndroidSceneManager

/**
 * CustomWebView Manager
 *
 * @author Thanvandh N
 */
class CustomWebViewManager : GAndroidSceneManager {

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.CUSTOM_WEB_VIEW_SCENE
    ){
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = CustomWebViewScene(context!!, this)
    }

    override fun onSceneInitialized() {
        scene!!.refresh(data!!.getData())
    }

    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        worldHandler!!.triggerAction(
            data!!.previousSceneId,
            Action.SHOW
        )
        return true
    }
}