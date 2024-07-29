package com.iwedia.cltv.manager

import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.scene.walkthrough_scene.WalkthroughScene
import com.iwedia.cltv.scene.walkthrough_scene.WalkthroughSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus

/**
 * Walkthroug manager
 *
 * @author Veljko Ilkic
 */
class WalkthroughSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var utilsModule: UtilsInterface,
    val generalConfigModule: GeneralConfigInterface
) : GAndroidSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.WALKTHROUGH
), WalkthroughSceneListener {

    init {
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = WalkthroughScene(context!!, this)
    }

    override fun onAppInitialized() {
    }

    override fun isAccessibilityEnabled(): Boolean{
        return utilsModule.isAccessibilityEnabled()
    }

    override fun getConfigInfo(nameOfInfo: String): Boolean {
        return generalConfigModule.getGeneralSettingsInfo(nameOfInfo)
    }

    override fun onEnd() {

        ReferenceApplication.runOnUiThread {
            Log.d(Constants.LogTag.CLTV_TAG + "WalkThroughSceneManager", "onEnd: ")
            utilsModule.setPrefsValue("walk", true)

            worldHandler!!.triggerAction(
                id,
                Action.DESTROY
            )
            worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.LIVE,
                Action.SHOW
            )
        }
    }

    override fun exitApplication() {
        InformationBus.submitEvent(Event(Events.EXIT_APPLICATION_ON_BACK_PRESS))
    }

    override fun onSceneInitialized() {
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }
}