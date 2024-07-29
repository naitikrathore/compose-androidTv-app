package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.scene.ci_popup.CiEncryptedScene
import com.iwedia.cltv.scene.ci_popup.CiEncryptedSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

class CiEncryptedManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler, var ciPlusModule: CiPlusInterface, var tvInterface: TvInterface
) : GAndroidSceneManager(context, worldHandler, ReferenceWorldHandler.SceneId.CI_ENCRYPTED_SCENE), CiEncryptedSceneListener {

    private val TAG = javaClass.simpleName

    override fun createScene() {
        scene = CiEncryptedScene(context!!, this)
    }

    override fun enquiryAnswer(abort: Boolean, answer: String) {
        ciPlusModule.enquiryAnswer(abort, answer)
    }

    override fun onNextChannel() {
        ciPlusModule.enquiryAnswer(true,"")
        ciPlusModule.setMMICloseDone()
        ReferenceApplication.worldHandler?.destroyOtherExisting(
            ReferenceWorldHandler.SceneId.LIVE
        )
    }

    override fun onPreviousChannel() {
        ciPlusModule.enquiryAnswer(true,"")
        ciPlusModule.setMMICloseDone()
        ReferenceApplication.worldHandler?.destroyOtherExisting(
            ReferenceWorldHandler.SceneId.LIVE
        )
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