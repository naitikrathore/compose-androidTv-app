package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.scene.postal_code.PostalCodeScene
import com.iwedia.cltv.scene.postal_code.PostalCodeSceneData
import com.iwedia.cltv.scene.postal_code.PostalCodeSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager

/**
 * Postal code scene manager
 *
 * @author Tanvi Raut
 */
class PostalCodeSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    private var utilsModule: UtilsInterface
) : GAndroidSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.POSTAL_SCENE
), PostalCodeSceneListener {

    override fun createScene() {
        scene = PostalCodeScene(context!!, this)
    }

    override fun onSceneInitialized() {}

    override fun isAccessibilityEnabled(): Boolean {
        return utilsModule.isAccessibilityEnabled()
    }
    override fun getPostalCode(): String? {
        return utilsModule.getEWSPostalCode()
    }

    override fun onPostalConfirmed(postalCode: String) {
        (data as PostalCodeSceneData).submitListener!!.onSubmit(postalCode)
        utilsModule.setEWSPostalCode(context!!, postalCode)
        onBackPressed()
    }
}