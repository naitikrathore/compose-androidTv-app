package com.iwedia.cltv.scene

import android.content.Context
import com.iwedia.cltv.config.ConfigHandler
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.guide.android.tools.GAndroidScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneListener

/**
 * Google reference scene
 *
 * @author Veljko Ilkic
 */
abstract class ReferenceScene(
    context: Context,
    sceneId: Int,
    name: String,
    sceneListener: SceneListener
) :
    GAndroidScene<GAndroidSceneFragment<GAndroidSceneFragmentListener>, SceneListener>(
        context,
        sceneId,
        name,
        sceneListener
    ) {

    var configParam: SceneConfig? = null

    override fun createView() {
        configParam = ConfigHandler.getSceneConfigParam(id)
        super.createView()
    }

    abstract fun parseConfig(sceneConfig: SceneConfig?)

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    override fun onDestroy() {
        configParam = null
        super.onDestroy()
    }

}