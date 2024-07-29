package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.scene.preferences_info_scene.PreferencesInfoScene
import com.iwedia.cltv.scene.preferences_info_scene.PreferencesInfoSceneData
import com.iwedia.guide.android.tools.GAndroidSceneManager
import world.SceneListener

/**
 * Preferences info scene manager
 *
 * @author Dejan Nadj
 */
class PreferencesInfoSceneManager : GAndroidSceneManager, SceneListener {

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.PREFERENCES_INFO_SCENE
    ) {
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = PreferencesInfoScene(context!!, this)
    }

    override fun onSceneInitialized() {
        scene!!.refresh(data as PreferencesInfoSceneData)
    }

    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }
}