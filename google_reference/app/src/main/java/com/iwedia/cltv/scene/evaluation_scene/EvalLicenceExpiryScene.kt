package com.iwedia.cltv.scene.evaluation_scene

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * EvalLicenceExpiry Scene
 */
class EvalLicenceExpiryScene(context: Context, sceneListener: EvalLicenceExpirySceneListener) :

    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.EVAL_LICENSE_EXPIRY_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.EVAL_LICENSE_EXPIRY_SCENE),
        sceneListener
    ) {

    /**
     * Xml views
     */
    var messageTv: TextView? = null
    var titleTv: TextView? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_eval_license_expiry, object :
            GAndroidSceneFragmentListener {
            override fun onCreated() {
                findViews()
                updateViews()
                sceneListener.onSceneInitialized()
            }
        })
    }

    private fun findViews() {
        titleTv = view!!.findViewById(R.id.eval_licence_expiry_title)
        messageTv = view!!.findViewById(R.id.eval_licence_expiry_message)
    }

    private fun updateViews() {
        titleTv!!.setTextColor(Color.parseColor("#eeeeee"))
        titleTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        titleTv?.text = ConfigStringsManager.getStringById("eval_licence_expiry_title")

        messageTv!!.setTextColor(Color.parseColor("#b7b7b7"))
        messageTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        messageTv?.text = ConfigStringsManager.getStringById("eval_licence_expiry_sub_title")
    }

    override fun refresh(data: Any?) {
        super.refresh(data)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        return false

    }
}