package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.scene.ci_popup.MmiMenuScene
import com.iwedia.cltv.scene.ci_popup.MmiMenuSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

class MmiMenuSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler, var ciPlusModule: CiPlusInterface
) : GAndroidSceneManager(context, worldHandler, ReferenceWorldHandler.SceneId.MMI_MENU_SCENE),
    MmiMenuSceneListener {

    private val TAG = javaClass.simpleName

    override fun createScene() {
        scene = MmiMenuScene(context!!, this)
    }

    override fun onSelectMenuItem(position: Int) {
        ciPlusModule.selectMenuItem(position)
    }

    override fun onCancelCurrentMenu() {
        ciPlusModule.cancelCurrMenu()
    }

    override fun resolveConfigurableKey(keyCode: Int, action: Int): Boolean {
        return false
    }

    override fun onSceneInitialized() {
        if (data != null) {
            scene!!.refresh(data!!.getData())
        }
    }
}