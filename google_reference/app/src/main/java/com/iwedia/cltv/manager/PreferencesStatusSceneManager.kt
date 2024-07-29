package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.scene.preferences_status.PreferencesStatusScene
import com.iwedia.cltv.scene.preferences_status.PreferencesStatusSceneData
import com.iwedia.guide.android.tools.GAndroidSceneManager
import world.SceneListener

/**
 * Preferences Cam Info Status scene manager
 *
 * @author Dejan Nadj
 */
class PreferencesStatusSceneManager: GAndroidSceneManager, SceneListener {

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE
    ) {
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = PreferencesStatusScene(context!!, this)
    }

    override fun onSceneInitialized() {
        scene!!.refresh(data as PreferencesStatusSceneData)
    }

    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }
}